package com.enterprise.common.algorithms;

import com.enterprise.common.models.*;
import com.enterprise.common.utils.DatabaseConnection;
import com.enterprise.common.dao.EdgeDAO;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class ImprovedConflictManager {
    private static final int TIME_THRESHOLD = 5; // 时间阈值（秒）
    private static final int MAX_RETRY_COUNT = 30; // 降低最大重试次数
    private static final int MAX_DELAY_TIME = 30; // 最大延迟时间（秒）
    private static final int MAX_CONFLICT_NODES = 3; // 最大冲突节点数
    private static final int TURN_TIME_BASE = 2; // 基础转弯时间（秒）
    private static final double DEFAULT_SPEED = 0.5; // 默认速度
    private static final int DELAY_INCREMENT = 2; // 每次延迟增量（秒）
    private static EdgeDAO edgeDAO = new EdgeDAO();
    
    // 主要处理方法
    public static PathResolution resolvePath(Path originalPath, String vehicleId, 
                                           Graph graph, AStarPathfinder pathfinder) throws Exception {
        long totalDelay = 0;
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
                    return new PathResolution(currentPath, PathResolutionStatus.SUCCESS, totalDelay);
                }
                
                System.out.println("\n" + vehicleId + " 检测到 " + conflicts.size() + " 个冲突点:");
                for (ConflictInfo conflict : conflicts) {
                    System.out.println("冲突节点: " + conflict.node.getId() + 
                                     ", 到达时间: " + conflict.node.getArrivalTime() +
                                     " -> " + conflict.existingDepartureTime);
                }
                
                // 使用渐进式延迟策略
                if (conflicts.size() > MAX_CONFLICT_NODES || totalDelay > MAX_DELAY_TIME) {
                    // 如果冲突太多或总延迟太大，尝试重规划
                    System.out.println("\n开始多级重规划...");
                    Path newPath = multiLevelReplanning(originalPath, conflicts, graph, pathfinder, unavailableNodes);
                    
                    if (newPath != null) {
                        System.out.println("重规划成功，新路径: " + getPathNodesString(newPath));
                        currentPath = newPath;
                    } else {
                        // 如果重规划失败，继续使用延迟策略
                        long requiredDelay = DELAY_INCREMENT; // 使用固定增量
                        totalDelay += requiredDelay;
                        System.out.println("重规划失败，使用延迟策略，当前总延迟: " + totalDelay + " 秒");
                        currentPath = delayPath(currentPath, requiredDelay);
                    }
                } else {
                    // 使用渐进式延迟
                    long requiredDelay = DELAY_INCREMENT; // 每次增加固定时间
                    totalDelay += requiredDelay;
                    System.out.println("\n采用延迟策略，当前总延迟: " + totalDelay + " 秒");
                    currentPath = delayPath(currentPath, requiredDelay);
                }
                
                retryCount++;
                clearTempTable();
            }
            
            return new PathResolution(null, PathResolutionStatus.FAILED_MAX_RETRIES, totalDelay);
            
        } catch (SQLException e) {
            System.err.println("数据库操作失败: " + e.getMessage());
            e.printStackTrace();
            return new PathResolution(null, PathResolutionStatus.FAILED_DATABASE_ERROR, totalDelay);
        }
    }
    
    // 多级重规划策略
    private static Path multiLevelReplanning(Path originalPath, List<ConflictInfo> conflicts,
                                           Graph graph, AStarPathfinder pathfinder, Set<String> unavailableNodes) {
        // 1. 尝试局部路径优化
        Path localOptimizedPath = tryLocalOptimization(originalPath, conflicts, graph, pathfinder);
        if (localOptimizedPath != null) {
            System.out.println("局部路径优化成功");
            return calculateImprovedTiming(localOptimizedPath);
        }
        
        // 2. 尝试完全重规划
        conflicts.forEach(c -> unavailableNodes.add(c.node.getId()));
        Path fullReplanPath = replanPath(originalPath, unavailableNodes, graph, pathfinder);
        if (fullReplanPath != null) {
            System.out.println("完全重规划成功");
            return calculateImprovedTiming(fullReplanPath);
        }
        
        // 3. 尝试混合策略
        return tryHybridStrategy(originalPath, conflicts, graph, pathfinder);
    }
    
    // 局部路径优化
    private static Path tryLocalOptimization(Path originalPath, List<ConflictInfo> conflicts,
                                           Graph graph, AStarPathfinder pathfinder) {
        List<Node> nodes = originalPath.getNodes();
        for (ConflictInfo conflict : conflicts) {
            int conflictIndex = findNodeIndex(nodes, conflict.node);
            if (conflictIndex == -1) continue;
            
            // 获取局部优化范围
            int startIndex = Math.max(0, conflictIndex - 2);
            int endIndex = Math.min(nodes.size() - 1, conflictIndex + 2);
            
            // 创建子图进行局部优化
            Node startNode = nodes.get(startIndex);
            Node endNode = nodes.get(endIndex);
            
            // 在子图中寻找替代路径
            Path subPath = pathfinder.findPath(graph, startNode, endNode);
            if (subPath != null && isValidLocalPath(subPath, conflict)) {
                // 替换原路径中的局部段
                return replacePath(originalPath, subPath, startIndex, endIndex);
            }
        }
        return null;
    }
    
    // 混合策略
    private static Path tryHybridStrategy(Path originalPath, List<ConflictInfo> conflicts,
                                        Graph graph, AStarPathfinder pathfinder) {
        // 对每个冲突点尝试局部延迟和重规划的组合
        Path currentPath = originalPath;
        for (ConflictInfo conflict : conflicts) {
            // 先尝试小范围重规划
            Path localPath = tryLocalOptimization(currentPath, 
                                                Collections.singletonList(conflict),
                                                graph, pathfinder);
            if (localPath != null) {
                currentPath = localPath;
            } else {
                // 如果重规划失败，对该段使用延迟策略
                long delay = calculateSingleNodeDelay(conflict);
                currentPath = delayPathFromNode(currentPath, conflict.node, delay);
            }
        }
        return currentPath;
    }
    
    // 改进的时间计算
    private static Path calculateImprovedTiming(Path path) {
        if (path == null || path.getNodes().size() < 2) {
            return path;
        }

        List<Node> nodes = path.getNodes();
        nodes.get(0).setDepartureTime(LocalDateTime.now().plusSeconds(1));
        
        for (int i = 1; i < nodes.size(); i++) {
            Node prevNode = nodes.get(i-1);
            Node currNode = nodes.get(i);
            
            try {
                Edge edge = edgeDAO.getEdge(prevNode.getId(), currNode.getId());
                if (edge != null) {
                    // 基础行驶时间
                    double distance = edge.getLength();
                    double speed = edge.emptyVehicleSpeed > 0 ? edge.emptyVehicleSpeed : DEFAULT_SPEED;
                    int travelSeconds = (int) Math.ceil(distance / speed);
                    
                    // 转弯时间
                    int turnDelay = 0;
                    if (i < nodes.size() - 1) {
                        Node nextNode = nodes.get(i + 1);
                        double turnAngle = calculateTurnAngle(prevNode, currNode, nextNode);
                        turnDelay = (int)(turnAngle / 90.0 * TURN_TIME_BASE);
                    }
                    
                    // 设置时间戳
                    LocalDateTime arrivalTime = prevNode.getDepartureTime()
                        .plusSeconds(travelSeconds)
                        .plusSeconds(turnDelay);
                    
                    currNode.setArrivalTime(arrivalTime);
                    currNode.setDepartureTime(arrivalTime.plusSeconds(1));
                }
            } catch (Exception e) {
                System.err.println("计算时间时发生错误: " + e.getMessage());
            }
        }
        return path;
    }
    
    // 计算转弯角度
    private static double calculateTurnAngle(Node prev, Node curr, Node next) {
        double angle1 = Math.atan2(curr.getY() - prev.getY(), curr.getX() - prev.getX());
        double angle2 = Math.atan2(next.getY() - curr.getY(), next.getX() - curr.getX());
        double angle = Math.abs(Math.toDegrees(angle2 - angle1));
        return angle > 180 ? 360 - angle : angle;
    }
    
    // 查找节点索引
    private static int findNodeIndex(List<Node> nodes, Node target) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getId().equals(target.getId())) {
                return i;
            }
        }
        return -1;
    }
    
    // 验证局部路径是否有效
    private static boolean isValidLocalPath(Path path, ConflictInfo conflict) {
        return path.getNodes().stream()
            .noneMatch(node -> node.getId().equals(conflict.node.getId()));
    }
    
    // 替换路径的局部段
    private static Path replacePath(Path originalPath, Path subPath, int startIndex, int endIndex) {
        List<Node> newNodes = new ArrayList<>();
        newNodes.addAll(originalPath.getNodes().subList(0, startIndex));
        newNodes.addAll(subPath.getNodes());
        newNodes.addAll(originalPath.getNodes().subList(endIndex + 1, originalPath.getNodes().size()));
        return new Path(newNodes);
    }
    
    // 计算单个节点的延迟时间
    private static long calculateSingleNodeDelay(ConflictInfo conflict) {
        return ChronoUnit.SECONDS.between(conflict.node.getArrivalTime(), 
                                        conflict.existingDepartureTime) + TIME_THRESHOLD;
    }
    
    // 从特定节点开始延迟路径
    private static Path delayPathFromNode(Path path, Node startNode, long delaySeconds) {
        List<Node> nodes = path.getNodes();
        boolean startDelaying = false;
        for (Node node : nodes) {
            if (startDelaying) {
                node.setArrivalTime(node.getArrivalTime().plusSeconds(delaySeconds));
                node.setDepartureTime(node.getDepartureTime().plusSeconds(delaySeconds));
            }
            if (node.getId().equals(startNode.getId())) {
                startDelaying = true;
            }
        }
        return path;
    }
    
    // 以下方法从原ConflictManager复用
    private static void cleanExpiredRecords() throws Exception {
        String sql = "DELETE FROM vehiclepassages WHERE departure_time < ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            int deletedCount = stmt.executeUpdate();
            System.out.println("已清理 " + deletedCount + " 条过期记录");
        }
    }
    
    private static void createTempTable() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS temp_vehiclepassages");
            String createTableSQL = 
                "CREATE TABLE temp_vehiclepassages (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "vehicle_id VARCHAR(50), " +
                "node_id VARCHAR(50), " +
                "edge_id VARCHAR(50) NULL, " +
                "arrival_time TIMESTAMP NULL, " +
                "departure_time TIMESTAMP NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
            stmt.execute(createTableSQL);
        }
    }
    
    private static void saveToTempTable(Path path, String vehicleId, Graph graph) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO temp_vehiclepassages (vehicle_id, node_id, edge_id, arrival_time, departure_time) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                List<Node> nodes = path.getNodes();
                for (Node node : nodes) {
                    stmt.setString(1, vehicleId);
                    stmt.setString(2, node.getId());
                    stmt.setString(3, null);
                    stmt.setTimestamp(4, Timestamp.valueOf(node.getArrivalTime()));
                    stmt.setTimestamp(5, Timestamp.valueOf(node.getDepartureTime()));
                    stmt.executeUpdate();
                }
                for (int i = 0; i < nodes.size() - 1; i++) {
                    Node currentNode = nodes.get(i);
                    Node nextNode = nodes.get(i + 1);
                    String edgeId = currentNode.getId() + "_" + nextNode.getId();
                    LocalDateTime edgeStartTime = currentNode.getDepartureTime();
                    LocalDateTime edgeEndTime = nextNode.getArrivalTime();
                    stmt.setString(1, vehicleId);
                    stmt.setString(2, null);
                    stmt.setString(3, edgeId);
                    stmt.setTimestamp(4, Timestamp.valueOf(edgeStartTime));
                    stmt.setTimestamp(5, Timestamp.valueOf(edgeEndTime));
                    stmt.executeUpdate();
                }
            }
        }
    }
    
    private static List<ConflictInfo> detectConflicts(Path path, String vehicleId) throws Exception {
        List<ConflictInfo> conflicts = new ArrayList<>();
        // 1. 检测节点冲突
        String nodeSql = "SELECT v.node_id, v.arrival_time, v.departure_time " +
                    "FROM vehiclepassages v " +
                    "JOIN temp_vehiclepassages t ON v.node_id = t.node_id " +
                    "WHERE v.vehicle_id != ? " +
                    "AND t.vehicle_id = ? " +
                    "AND ((v.arrival_time <= t.departure_time AND v.departure_time >= t.arrival_time) " +
                    "OR (t.arrival_time <= v.departure_time AND t.departure_time >= v.arrival_time))";
        
        // 2. 检测边冲突（包括对向边）
        String edgeSql = "SELECT v.edge_id, v.arrival_time, v.departure_time " +
                "FROM vehiclepassages v " +
                "JOIN temp_vehiclepassages t " +
                "WHERE v.vehicle_id != ? " +
                "AND t.vehicle_id = ? " +
                "AND v.edge_id IS NOT NULL " +
                "AND (" +
                    // 直接匹配相同的边
                    "v.edge_id = t.edge_id OR " +
                    // 匹配对向边（例如：如果一个是"1_2"，另一个是"2_1"）
                    "v.edge_id = CONCAT(SUBSTRING_INDEX(t.edge_id, '_', -1), '_', SUBSTRING_INDEX(t.edge_id, '_', 1))" +
                ") " +
                "AND ((v.arrival_time <= t.departure_time AND v.departure_time >= t.arrival_time) " +
                "OR (t.arrival_time <= v.departure_time AND t.departure_time >= v.arrival_time))";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // 处理节点冲突
            try (PreparedStatement stmt = conn.prepareStatement(nodeSql)) {
                stmt.setString(1, vehicleId);
                stmt.setString(2, vehicleId);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    String nodeId = rs.getString("node_id");
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
            
            // 处理边冲突
            try (PreparedStatement stmt = conn.prepareStatement(edgeSql)) {
                stmt.setString(1, vehicleId);
                stmt.setString(2, vehicleId);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    String edgeId = rs.getString("edge_id");
                    LocalDateTime existingDeparture = rs.getTimestamp("departure_time").toLocalDateTime();
                    
                    // 从边ID中提取起始和终止节点
                    String[] nodeIds = edgeId.split("_");
                    if (nodeIds.length == 2) {
                        // 检查路径中是否存在这条边或其对向边
                        for (int i = 0; i < path.getNodes().size() - 1; i++) {
                            Node currentNode = path.getNodes().get(i);
                            Node nextNode = path.getNodes().get(i + 1);
                            String currentEdgeId = currentNode.getId() + "_" + nextNode.getId();
                            String oppositeEdgeId = nextNode.getId() + "_" + currentNode.getId();
                            
                            if (edgeId.equals(currentEdgeId) || edgeId.equals(oppositeEdgeId)) {
                                System.out.println("检测到边冲突：");
                                System.out.println("- 当前AGV边：" + currentEdgeId);
                                System.out.println("- 已存在AGV边：" + edgeId);
                                System.out.println("- 冲突时间：" + existingDeparture);
                                conflicts.add(new ConflictInfo(currentNode, existingDeparture));
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        return conflicts;
    }
    
    private static void migrateToMainTable(String vehicleId) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO vehiclepassages (vehicle_id, node_id, edge_id, arrival_time, departure_time) " +
                        "SELECT vehicle_id, node_id, edge_id, arrival_time, departure_time " +
                        "FROM temp_vehiclepassages WHERE vehicle_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, vehicleId);
                stmt.executeUpdate();
            }
        }
    }
    
    private static void clearTempTable() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE temp_vehiclepassages");
        }
    }
    
    private static Path delayPath(Path path, long delaySeconds) {
        // 整体延迟所有节点
        List<Node> newNodes = new ArrayList<>();
        for (Node node : path.getNodes()) {
            Node newNode = node.clone();
            newNode.setArrivalTime(node.getArrivalTime().plusSeconds(delaySeconds));
            newNode.setDepartureTime(node.getDepartureTime().plusSeconds(delaySeconds));
            newNodes.add(newNode);
        }
        return new Path(newNodes);
    }
    
    private static long calculateRequiredDelay(List<ConflictInfo> conflicts) {
        // 计算基础延迟需求
        long baseDelay = conflicts.stream()
            .mapToLong(conflict -> TIME_THRESHOLD - 
                      ChronoUnit.SECONDS.between(conflict.existingDepartureTime, 
                                               conflict.node.getArrivalTime()))
            .max()
            .orElse(0);
        
        // 将基础延迟调整为DELAY_INCREMENT的整数倍
        return ((baseDelay + DELAY_INCREMENT - 1) / DELAY_INCREMENT) * DELAY_INCREMENT;
    }
    
    private static Path replanPath(Path originalPath, Set<String> unavailableNodes, Graph graph, AStarPathfinder pathfinder) {
        Node start = originalPath.getNodes().get(0);
        Node end = originalPath.getNodes().get(originalPath.getNodes().size() - 1);
        
        unavailableNodes.remove(start.getId());
        unavailableNodes.remove(end.getId());
        
        Graph tempGraph = graph.clone();
        for (String nodeId : unavailableNodes) {
            Node node = tempGraph.getNodeById(nodeId);
            if (node != null) {
                tempGraph.removeNode(node);
            }
        }
        
        Path newPath = pathfinder.findPath(tempGraph, start, end);
        if (newPath != null) {
            for (int i = 0; i < newPath.getNodes().size() - 1; i++) {
                Node currentNode = newPath.getNodes().get(i);
                Node nextNode = newPath.getNodes().get(i + 1);
                if (!hasEdgeBetween(tempGraph, currentNode, nextNode)) {
                    System.out.println("发现无效路径：节点 " + currentNode.getId() + 
                                     " 和节点 " + nextNode.getId() + " 之间没有边连接");
                    return null;
                }
            }
            
            LocalDateTime startTime = originalPath.getNodes().get(0).getArrivalTime();
            newPath.getNodes().get(0).setArrivalTime(startTime);
            newPath.getNodes().get(0).setDepartureTime(startTime.plusSeconds(1));
            
            for (int i = 1; i < newPath.getNodes().size(); i++) {
                Node currentNode = newPath.getNodes().get(i-1);
                Node nextNode = newPath.getNodes().get(i);
                Edge edge = tempGraph.getEdge(currentNode, nextNode);
                
                double distance = edge.getLength();
                double speed = 0.5;
                int travelSeconds = (int) Math.ceil(distance / speed);
                
                LocalDateTime arrivalTime = currentNode.getDepartureTime().plusSeconds(travelSeconds);
                nextNode.setArrivalTime(arrivalTime);
                nextNode.setDepartureTime(arrivalTime.plusSeconds(1));
            }
            return newPath;
        }
        return null;
    }
    
    private static String getPathNodesString(Path path) {
        if (path == null) return "null";
        return path.getNodes().stream()
                  .map(Node::getId)
                  .collect(Collectors.joining(" -> "));
    }
    
    private static boolean hasEdgeBetween(Graph graph, Node node1, Node node2) {
        return graph.getEdge(node1, node2) != null;
    }
    
    /**
     * 新的入口方法：处理路径冲突检测和解决
     * @param path 原始路径
     * @param vehicleId 车辆ID
     * @param graph 路网图
     * @param pathfinder 路径规划器
     * @return 路径解决方案，包含处理后的路径、状态和总延迟时间
     */
    public static PathResolution resolvePathConflicts(Path path, String vehicleId, 
                                                    Graph graph, AStarPathfinder pathfinder) throws Exception {
        return resolvePath(path, vehicleId, graph, pathfinder);
    }

    /**
     * @deprecated 已过时，请使用 {@link #resolvePathConflicts(Path, String, Graph, AStarPathfinder)} 代替
     */
    @Deprecated
    public static PathResolution handleMaxRetries(Path originalPath, String vehicleId, 
                                                Graph graph, AStarPathfinder pathfinder) throws Exception {
        System.out.println("警告：此方法已过时，建议使用 resolvePathConflicts 方法代替");
        return resolvePathConflicts(originalPath, vehicleId, graph, pathfinder);
    }
} 