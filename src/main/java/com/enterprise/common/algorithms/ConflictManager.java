package com.enterprise.common.algorithms;

import com.enterprise.common.models.*;
import com.enterprise.common.utils.DatabaseConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ConflictManager {
    private static final int TIME_THRESHOLD = 5; // 时间阈值（秒）
    private static final int MAX_RETRY_COUNT = 3; // 最大重试次数
    private static final int MAX_DELAY_TIME = 9; // 最大延迟时间（秒）
    private static final int MAX_CONFLICT_NODES = 3; // 最大冲突节点数
    
    // 清理过期记录
    private static void cleanExpiredRecords() throws SQLException,Exception {
        String sql = "DELETE FROM vehiclepassages WHERE departure_time < ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            int deletedCount = stmt.executeUpdate();
            System.out.println("已清理 " + deletedCount + " 条过期记录");
        }
    }
    
    // 创建和管理临时表
    private static void createTempTable() throws SQLException, Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            // 先删除已存在的临时表
            stmt.execute("DROP TABLE IF EXISTS temp_vehiclepassages");
            
            // 创建新的临时表，移除外键约束
            String createTableSQL = 
                "CREATE TABLE temp_vehiclepassages (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "vehicle_id VARCHAR(50), " +
                "node_id VARCHAR(50), " +
                "edge_id VARCHAR(50) NULL, " +  // 允许为空
                "arrival_time TIMESTAMP NULL, " +
                "departure_time TIMESTAMP NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
            
            stmt.execute(createTableSQL);
        }
    }
    
    // 保存路径到临时表
    private static void saveToTempTable(Path path, String vehicleId) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO temp_vehiclepassages (vehicle_id, node_id, arrival_time, departure_time) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (Node node : path.getNodes()) {
                    // 检查并设置默认时间
                    if (node.getArrivalTime() == null) {
                        LocalDateTime now = LocalDateTime.now();
                        node.setArrivalTime(now);
                        node.setDepartureTime(now.plusSeconds(5));
                    }
                    
                    stmt.setString(1, vehicleId);
                    stmt.setString(2, node.getId());
                    stmt.setTimestamp(3, Timestamp.valueOf(node.getArrivalTime()));
                    stmt.setTimestamp(4, node.getDepartureTime() != null ? 
                        Timestamp.valueOf(node.getDepartureTime()) : 
                        Timestamp.valueOf(node.getArrivalTime().plusSeconds(5)));
                    stmt.executeUpdate();
                }
            }
        }
    }
    
    // 检测时间冲突
    private static boolean isTimeConflict(LocalDateTime newArrival, LocalDateTime newDeparture,
                                        LocalDateTime existingArrival, LocalDateTime existingDeparture) {
        long timeDiff1 = ChronoUnit.SECONDS.between(existingDeparture, newArrival);
        long timeDiff2 = ChronoUnit.SECONDS.between(newDeparture, existingArrival);
        
        return Math.abs(timeDiff1) < TIME_THRESHOLD || Math.abs(timeDiff2) < TIME_THRESHOLD;
    }
    
    // 计算需要的延迟时��
    private static long calculateRequiredDelay(List<ConflictInfo> conflicts) {
        return conflicts.stream()
            .mapToLong(conflict -> TIME_THRESHOLD - 
                      ChronoUnit.SECONDS.between(conflict.existingDepartureTime, 
                                               conflict.node.getArrivalTime()))
            .max()
            .orElse(0);
    }
    
    // 延迟整个路径
    private static Path delayPath(Path path, long delaySeconds) {
        List<Node> newNodes = new ArrayList<>();
        for (Node node : path.getNodes()) {
            Node newNode = node.clone();
            newNode.setArrivalTime(node.getArrivalTime().plusSeconds(delaySeconds));
            newNode.setDepartureTime(node.getDepartureTime().plusSeconds(delaySeconds));
            newNodes.add(newNode);
        }
        return new Path(newNodes);
    }
    
    // 重新规划路径
    private static Path replanPath(Path originalPath, Set<String> unavailableNodes, 
                                 Graph graph, AStarPathfinder pathfinder) {
        Graph tempGraph = graph.clone(); // 创建图的副本
        
        // 从临时图中移除不可用节点及其相关边
        for (String nodeId : unavailableNodes) {
            Node node = tempGraph.getNodeById(nodeId);
            if (node != null) {
                tempGraph.removeNodeAndEdges(node); // 移除节点及其相关边
            }
        }
        
        // 重新规划路径
        Node start = originalPath.getNodes().get(0);
        Node end = originalPath.getNodes().get(originalPath.getNodes().size() - 1);
        return pathfinder.findPath(tempGraph, start, end);
    }
    
    // 将临时表数据迁移到主表
    private static void migrateToMainTable(String vehicleId) throws SQLException,Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // 修改 SQL 语句，包含 edge_id 字段
            String sql = "INSERT INTO vehiclepassages (vehicle_id, node_id, edge_id, arrival_time, departure_time) " +
                        "SELECT vehicle_id, node_id, edge_id, arrival_time, departure_time " +
                        "FROM temp_vehiclepassages WHERE vehicle_id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, vehicleId);
                stmt.executeUpdate();
            }
        }
    }
    
    // 清空临时表
    private static void clearTempTable() throws SQLException,Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE temp_vehiclepassages");
        }
    }
    
    // 主要处理方法
    public static PathResolution resolvePath(Path originalPath, String vehicleId, 
                                           Graph graph, AStarPathfinder pathfinder) throws Exception {
        try {
            cleanExpiredRecords();
            createTempTable();
            
            int retryCount = 0;
            Set<String> unavailableNodes = new HashSet<>();
            Path currentPath = originalPath;
            
            while (retryCount < MAX_RETRY_COUNT) {
                saveToTempTable(currentPath, vehicleId);
                List<ConflictInfo> conflicts = detectConflicts(currentPath, vehicleId);
                
                if (conflicts.isEmpty()) {
                    migrateToMainTable(vehicleId);
                    clearTempTable();
                    return new PathResolution(currentPath, PathResolutionStatus.SUCCESS);
                }
                
                System.out.println("检测到 " + conflicts.size() + " 个冲突点，尝解决...");
                
                if (conflicts.size() > MAX_CONFLICT_NODES) {
                    conflicts.forEach(c -> unavailableNodes.add(c.node.getId()));
                    currentPath = replanPath(originalPath, unavailableNodes, graph, pathfinder);
                    if (currentPath == null) {
                        return new PathResolution(null, PathResolutionStatus.FAILED_NO_ALTERNATIVE);
                    }
                } else {
                    long requiredDelay = calculateRequiredDelay(conflicts);
                    if (requiredDelay > MAX_DELAY_TIME) {
                        conflicts.forEach(c -> unavailableNodes.add(c.node.getId()));
                        currentPath = replanPath(originalPath, unavailableNodes, graph, pathfinder);
                        if (currentPath == null) {
                            return new PathResolution(null, PathResolutionStatus.FAILED_NO_ALTERNATIVE);
                        }
                    } else {
                        currentPath = delayPath(currentPath, requiredDelay);
                        System.out.println("路径延迟 " + requiredDelay + " 秒");
                    }
                }
                
                retryCount++;
                clearTempTable();
            }
            
            return new PathResolution(null, PathResolutionStatus.FAILED_MAX_RETRIES);
            
        } catch (SQLException e) {
            System.err.println("数据库操作失败: " + e.getMessage());
            e.printStackTrace();
            return new PathResolution(null, PathResolutionStatus.FAILED_DATABASE_ERROR, e.getMessage());
        }
    }
    
    private static List<ConflictInfo> detectConflicts(Path path, String vehicleId) throws Exception {
        List<ConflictInfo> conflicts = new ArrayList<>();
        String sql = "SELECT v.node_id, v.arrival_time, v.departure_time " +
                    "FROM vehiclepassages v " +
                    "JOIN temp_vehiclepassages t ON v.node_id = t.node_id " +
                    "WHERE v.vehicle_id != ? " +
                    "AND t.vehicle_id = ? " +
                    "AND ((v.arrival_time <= t.departure_time AND v.departure_time >= t.arrival_time) " +
                    "OR (t.arrival_time <= v.departure_time AND t.departure_time >= v.arrival_time))";
                
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, vehicleId);
            stmt.setString(2, vehicleId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String nodeId = rs.getString("node_id");
                LocalDateTime existingArrival = rs.getTimestamp("arrival_time").toLocalDateTime();
                LocalDateTime existingDeparture = rs.getTimestamp("departure_time").toLocalDateTime();
                
                Node conflictNode = path.getNodes().stream()
                    .filter(n -> n.getId().equals(nodeId))
                    .findFirst()
                    .orElse(null);
                
                if (conflictNode != null) {
                    conflicts.add(new ConflictInfo(conflictNode, existingDeparture));
                }
            }
        }
        return conflicts;
    }
}
