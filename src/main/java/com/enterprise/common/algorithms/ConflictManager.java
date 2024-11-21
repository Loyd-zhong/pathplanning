package com.enterprise.common.algorithms;

import com.enterprise.common.models.*;
import com.enterprise.common.utils.DatabaseConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class ConflictManager {
    private static final int TIME_THRESHOLD = 5; // 时间阈值（秒）
    private static final int MAX_RETRY_COUNT = 3; // 最大重试次数
    private static final int MAX_DELAY_TIME = 9; // 最大延迟时间（秒）
    private static final int MAX_CONFLICT_NODES = 3; // 最大冲突节点数
    private static final int CLEANUP_INTERVAL = 5000; // 清理间隔（毫秒）
    private static Timer cleanupTimer;
    
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
    private static void saveToTempTable(Path path, String vehicleId, Graph graph) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO temp_vehiclepassages (vehicle_id, node_id, edge_id, arrival_time, departure_time) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                List<Node> nodes = path.getNodes();
                
                // 记录所有节点
                for (Node node : nodes) {
                    stmt.setString(1, vehicleId);
                    stmt.setString(2, node.getId());
                    stmt.setString(3, null);  // 节点记录的edge_id为null
                    stmt.setTimestamp(4, Timestamp.valueOf(node.getArrivalTime()));
                    stmt.setTimestamp(5, Timestamp.valueOf(node.getDepartureTime()));
                    stmt.executeUpdate();
                }
                
                // 记录所有边
                for (int i = 0; i < nodes.size() - 1; i++) {
                    Node currentNode = nodes.get(i);
                    Node nextNode = nodes.get(i + 1);
                    Edge edge = graph.getEdge(currentNode, nextNode);
                    
                    if (edge != null) {
                        String edgeId = currentNode.getId() + "_" + nextNode.getId();
                        stmt.setString(1, vehicleId);
                        stmt.setString(2, null);  // 边记录的node_id为null
                        stmt.setString(3, edgeId);
                        stmt.setTimestamp(4, Timestamp.valueOf(currentNode.getDepartureTime()));
                        stmt.setTimestamp(5, Timestamp.valueOf(nextNode.getArrivalTime()));
                        stmt.executeUpdate();
                    }
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
    
    // 计算需要的延迟时
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
        Node start = originalPath.getNodes().get(0);
        Node end = originalPath.getNodes().get(originalPath.getNodes().size() - 1);
        
        // 从unavailableNodes中移除起点和终点
        unavailableNodes.remove(start.getId());
        unavailableNodes.remove(end.getId());
        
        Graph tempGraph = graph.clone();
        for (String nodeId : unavailableNodes) {
            Node node = tempGraph.getNodeById(nodeId);
            if (node != null) {
                tempGraph.removeNodeAndEdges(node);
            }
        }
        
        Path newPath = pathfinder.findPath(tempGraph, start, end);
        if (newPath != null) {
            // 验证所有相邻节点之间是否有边连接
            for (int i = 0; i < newPath.getNodes().size() - 1; i++) {
                Node currentNode = newPath.getNodes().get(i);
                Node nextNode = newPath.getNodes().get(i + 1);
                if (!hasEdgeBetween(tempGraph, currentNode, nextNode)) {
                    System.out.println("发现无效路径：节点 " + currentNode.getId() + 
                                     " 和节点 " + nextNode.getId() + " 之间没有边连接");
                    return null;  // 如果发现无效连接，返回null表示规划失败
                }
            }
            
            // 设置时间
            LocalDateTime startTime = originalPath.getNodes().get(0).getArrivalTime();
            newPath.getNodes().get(0).setArrivalTime(startTime);
            newPath.getNodes().get(0).setDepartureTime(startTime.plusSeconds(1));
            
            for (int i = 1; i < newPath.getNodes().size(); i++) {
                Node currentNode = newPath.getNodes().get(i-1);
                Node nextNode = newPath.getNodes().get(i);
                Edge edge = tempGraph.getEdge(currentNode, nextNode);
                
                // 使用实际的边长度
                double distance = edge.getLength();
                double speed = 0.5; // AGV.SpeedLevel.NORMAL
                int travelSeconds = (int) Math.ceil(distance / speed);
                
                LocalDateTime arrivalTime = currentNode.getDepartureTime().plusSeconds(travelSeconds);
                nextNode.setArrivalTime(arrivalTime);
                nextNode.setDepartureTime(arrivalTime.plusSeconds(1));
            }
            return newPath;
        }
        return null;
    }
    
    private static boolean hasEdgeBetween(Graph graph, Node node1, Node node2) {
        return graph.getEdge(node1, node2) != null;
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
            
            System.out.println("\n开始处理 " + vehicleId + " 的路径冲突检测...");
            System.out.println("原始路径节点序列: " + getPathNodesString(originalPath));
            
            while (retryCount < MAX_RETRY_COUNT) {
                saveToTempTable(currentPath, vehicleId, graph);
                List<ConflictInfo> conflicts = detectConflicts(currentPath, vehicleId);
                
                if (conflicts.isEmpty()) {
                    System.out.println(vehicleId + " 没有检测到冲突，路径可用");
                    migrateToMainTable(vehicleId);
                    clearTempTable();
                    return new PathResolution(currentPath, PathResolutionStatus.SUCCESS);
                }
                
                System.out.println("\n" + vehicleId + " 检测到 " + conflicts.size() + " 个冲突点:");
                for (ConflictInfo conflict : conflicts) {
                    System.out.println("冲突节点: " + conflict.node.getId() + 
                                     ", 到达时间: " + conflict.node.getArrivalTime() +
                                     " -> " + conflict.existingDepartureTime);
                }
                
                // 尝试重新规划路径
                if (conflicts.size() > MAX_CONFLICT_NODES || calculateRequiredDelay(conflicts) > MAX_DELAY_TIME) {
                    System.out.println("\n尝试重新规划路径...");
                    conflicts.forEach(c -> unavailableNodes.add(c.node.getId()));
                    Path newPath = replanPath(originalPath, unavailableNodes, graph, pathfinder);
                    
                    if (newPath != null) {
                        System.out.println("重新规划成功，新路径: " + getPathNodesString(newPath));
                        currentPath = newPath;
                    } else {
                        // 重新规划失败，使用延迟策略作为保底方案
                        System.out.println("重新规划失败，使用延迟策略作为保底方案");
                        long requiredDelay = calculateRequiredDelay(conflicts);
                        System.out.println("采用延迟策略，延迟时间: " + requiredDelay + " 秒");
                        currentPath = delayPath(originalPath, requiredDelay);
                    }
                } else {
                    // 直接使用延迟策略
                    long requiredDelay = calculateRequiredDelay(conflicts);
                    System.out.println("\n采用延迟策略，延迟时间: " + requiredDelay + " 秒");
                    currentPath = delayPath(currentPath, requiredDelay);
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
    
    private static String getPathNodesString(Path path) {
        if (path == null) return "null";
        return path.getNodes().stream()
                  .map(Node::getId)
                  .collect(Collectors.joining(" -> "));
    }
}
