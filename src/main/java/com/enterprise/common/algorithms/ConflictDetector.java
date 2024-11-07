package com.enterprise.common.algorithms;

import com.enterprise.common.models.*;
import com.enterprise.common.utils.DatabaseConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class ConflictDetector {
    private static final int MAX_VEHICLES_PER_NODE = 3;
    private static final int DELAY_SECONDS = 5;
    
    public static Path detectAndResolveConflicts(Path originalPath, Graph graph, AStarPathfinder pathfinder) throws SQLException {
        // 检查原始路径是否存在冲突
        if (!hasConflict(originalPath)) {
            return originalPath;
        }
        
        System.out.println("\n检测到路径冲突，开始寻找替代方案...");
        
        // 尝试两种修正方式
        Path detourPath = tryDetour(originalPath, graph, pathfinder);
        Path delayedPath = tryDelay(originalPath);
        
        // 比较两种方案，选择到达时间更早的
        if (detourPath == null) {
            System.out.println("已选择延迟策略处理冲突");
            return delayedPath;
        } else if (delayedPath == null) {
            System.out.println("已选择绕行策略处理冲突");
            return detourPath;
        }
        
        LocalDateTime detourArrival = detourPath.getNodes().get(detourPath.getNodes().size() - 1).getArrivalTime();
        LocalDateTime delayArrival = delayedPath.getNodes().get(delayedPath.getNodes().size() - 1).getArrivalTime();
        
        if (detourArrival.isBefore(delayArrival)) {
            System.out.println("比较后选择绕行策略处理冲突（到达时间更早）");
            return detourPath;
        } else {
            System.out.println("比较后选择延迟策略处理冲突（到达时间更早）");
            return delayedPath;
        }
    }
    
    private static boolean hasConflict(Path path) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            for (Node node : path.getNodes()) {
                if (node.getArrivalTime() == null || node.getDepartureTime() == null) {
                    LocalDateTime now = LocalDateTime.now();
                    node.setArrivalTime(now);
                    node.setDepartureTime(now.plusSeconds(5));
                    System.out.println("设置节点时间 - 节点ID: " + node.getId() + 
                        ", 到达时间: " + node.getArrivalTime() + 
                        ", 离开时间: " + node.getDepartureTime());
                }
                
                String sql = "SELECT COUNT(*) as vehicle_count, GROUP_CONCAT(vehicle_id) as vehicles " +
                           "FROM vehiclepassages " +
                           "WHERE node_id = ? " +
                           "AND NOT (departure_time < ? OR arrival_time > ?)";
                           
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, node.getId());
                stmt.setTimestamp(2, Timestamp.valueOf(node.getArrivalTime()));
                stmt.setTimestamp(3, Timestamp.valueOf(node.getDepartureTime()));
                
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int count = rs.getInt("vehicle_count");
                    String vehicles = rs.getString("vehicles");
                    System.out.println("节点冲突检查 - 节点ID: " + node.getId() + 
                        "\n当前车辆数: " + count + 
                        "\n已存在车辆: " + vehicles +
                        "\n最大容量: " + MAX_VEHICLES_PER_NODE +
                        "\n是否冲突: " + (count >= MAX_VEHICLES_PER_NODE));
                    
                    if (count >= MAX_VEHICLES_PER_NODE) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("冲突检测发生错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return false;
    }
    
    private static Path tryDetour(Path originalPath, Graph graph, AStarPathfinder pathfinder) {
        Set<String> conflictNodes = getConflictNodes(originalPath);
        if (conflictNodes.isEmpty()) {
            return null;
        }
        
        // 临时从图中移除冲突节点
        List<Node> removedNodes = new ArrayList<>();
        for (String nodeId : conflictNodes) {
            Node node = graph.getNodeById(nodeId);
            if (node != null) {
                removedNodes.add(node);
                graph.removeNode(node);
            }
        }
        
        // 重新规划路径
        Node start = originalPath.getNodes().get(0);
        Node end = originalPath.getNodes().get(originalPath.getNodes().size() - 1);
        Path newPath = pathfinder.findPath(graph, start, end);
        
        // 恢复图的原始状态
        for (Node node : removedNodes) {
            graph.addNode(node);
        }
        
        return newPath;
    }
    
    private static Path tryDelay(Path originalPath) {
        Path delayedPath = new Path();
        LocalDateTime delayTime = LocalDateTime.now().plusSeconds(DELAY_SECONDS);
        
        for (Node node : originalPath.getNodes()) {
            Node delayedNode = new Node(node.getX(), node.getY(), node.getId());
            delayedNode.setArrivalTime(node.getArrivalTime().plusSeconds(DELAY_SECONDS));
            delayedPath.addNode(delayedNode);
        }
        try{
            if (hasConflict(delayedPath)) {
                return null;
            }
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
        
        return delayedPath;
    }
    
    private static Set<String> getConflictNodes(Path path) {
        Set<String> conflictNodes = new HashSet<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            for (Node node : path.getNodes()) {
                String sql = "SELECT COUNT(*) FROM vehiclepassages WHERE node_id = ? " +
                           "AND arrival_time <= ? AND departure_time >= ?";
                           
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, node.getId());
                stmt.setTimestamp(2, Timestamp.valueOf(node.getArrivalTime()));
                stmt.setTimestamp(3, Timestamp.valueOf(node.getArrivalTime()));
                
                ResultSet rs = stmt.executeQuery();
                if (rs.next() && rs.getInt(1) >= MAX_VEHICLES_PER_NODE) {
                    conflictNodes.add(node.getId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conflictNodes;
    }
    
    public static List<Path> batchDetectAndResolveConflicts(List<Path> paths, Graph graph, AStarPathfinder pathfinder) {
        List<Path> resolvedPaths = new ArrayList<>();
        for (Path path : paths) {
            try {
                Path resolvedPath = detectAndResolveConflicts(path, graph, pathfinder);
                if (resolvedPath != null) {
                    resolvedPaths.add(resolvedPath);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return resolvedPaths;
    }
}
