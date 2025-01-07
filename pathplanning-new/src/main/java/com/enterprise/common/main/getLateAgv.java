package com.enterprise.common.main;
import com.enterprise.common.models.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import com.enterprise.common.algorithms.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.enterprise.common.utils.DatabaseConnection;
import com.enterprise.common.dao.EdgeDAO;

// 主类
public class getLateAgv {
    private static Graph graph;  // 改为静态变量
    private static EdgeDAO edgeDAO = new EdgeDAO();
    
    // 提供静态方法来设置 Graph 实例
    public static void setGraph(Graph g) {
        graph = g;
    }
    
    // 移除构造函数，改为无参构造
    public getLateAgv() {
        if (graph == null) {
            throw new IllegalStateException("Graph has not been initialized. Please call setGraph first.");
        }
    }
    
    public LateAgvResult getLateAgv(String node, List<AgvNodeVo> agvs) {
        long startTime = System.currentTimeMillis();  // 开始计时
        
        // 方法内部直接使用静态 graph 变量
        Node originNode = graph.getNodeById(node);
        if (originNode == null) {
            return null;
        }
        
        // 第一步：将AGV按象限分类
        Map<Integer, List<AgvNodeVo>> quadrantAgvs = new HashMap<>();
        for (int i = 1; i <= 4; i++) {
            quadrantAgvs.put(i, new ArrayList<>());
        }
        
        // 分类AGV到不同象限
        for (AgvNodeVo agv : agvs) {
            String nodeId = agv.getCurNodeId();
            if (nodeId == null || nodeId.isEmpty()) {
                nodeId = agv.getLastNodeId();
            }
            if (nodeId == null || nodeId.isEmpty()) {
                continue;
            }
            
            Node agvNode = graph.getNodeById(nodeId);
            if (agvNode == null) {
                continue;
            }
            
            int quadrant = getQuadrant(originNode, agvNode);
            quadrantAgvs.get(quadrant).add(agv);
        }
        
        // 第二步：在每个象限中找到最近的AGV
        Map<Integer, AgvNodeVo> nearestAgvs = new HashMap<>();
        for (int quadrant = 1; quadrant <= 4; quadrant++) {
            AgvNodeVo nearest = findNearestAgv(originNode, quadrantAgvs.get(quadrant));
            if (nearest != null) {
                nearestAgvs.put(quadrant, nearest);
            }
        }
        
        // 第三步：对最近的AGV进行路径规划，找出耗时最短的
        AgvNodeVo bestAgv = null;
        long shortestTime = Long.MAX_VALUE;
        
        for (AgvNodeVo agv : nearestAgvs.values()) {
            String agvNodeId = agv.getCurNodeId() != null ? agv.getCurNodeId() : agv.getLastNodeId();
            Node startNode = graph.getNodeById(agvNodeId);
            Node endNode = originNode;
            
            long pathTime = calculatePathTime(startNode, endNode);
            if (pathTime < shortestTime) {
                shortestTime = pathTime;
                bestAgv = agv;
            }
        }
        
        if (bestAgv != null) {
            long endTime = System.currentTimeMillis();  // 结束计时
            System.out.println("计算耗时: " + (endTime - startTime) + "毫秒");  // 输出耗时
            return new LateAgvResult(bestAgv, shortestTime);
        }
        
        long endTime = System.currentTimeMillis();  // 结束计时
        System.out.println("计算耗时: " + (endTime - startTime) + "毫秒");  // 输出耗时
        return null;
    }
    
    private int getQuadrant(Node origin, Node point) {
        double dx = point.getX() - origin.getX();
        double dy = point.getY() - origin.getY();
        
        if (dx >= 0 && dy >= 0) return 1;
        if (dx < 0 && dy >= 0) return 2;
        if (dx < 0 && dy < 0) return 3;
        return 4;
    }
    
    private AgvNodeVo findNearestAgv(Node origin, List<AgvNodeVo> agvs) {
        if (agvs.isEmpty()) {
            return null;
        }
        
        AgvNodeVo nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (AgvNodeVo agv : agvs) {
            String nodeId = agv.getCurNodeId() != null ? agv.getCurNodeId() : agv.getLastNodeId();
            Node agvNode = graph.getNodeById(nodeId);
            
            double distance = calculateDistance(origin, agvNode);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = agv;
            }
        }
        
        return nearest;
    }
    
    private double calculateDistance(Node n1, Node n2) {
        Edge edge = getEdgeBetweenNodes(n1, n2);
        if (edge != null) {
            return edge.getLength();  // 使用边的实际长度
        }
        // 如果找不到边，才使用欧几里得距离作为后备方案
        return Math.sqrt(Math.pow(n2.getX() - n1.getX(), 2) + 
                        Math.pow(n2.getY() - n1.getY(), 2));
    }
    
    private Edge getEdgeBetweenNodes(Node a, Node b) {
        for (Edge edge : graph.getEdges(a)) {
            if (edge.getOpposite(a).equals(b)) {
                return edge;
            }
        }
        return null;
    }
    
    private long calculatePathTime(Node start, Node end) {
        AStarPathfinder pathfinder = new AStarPathfinder();
        Path originalPath = pathfinder.findPath(graph, start, end);
        
        if (originalPath == null) {
            return Long.MAX_VALUE;
        }
        
        try {
            String tempAgvId = "TEMP_" + System.currentTimeMillis();
            PathResolution resolution = ConflictManager.resolvePath(
                originalPath,
                tempAgvId,
                graph,
                pathfinder,
                5
            );
            
            // 删除临时记录
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM vehiclepassages WHERE vehicle_id = ?")) {
                stmt.setString(1, tempAgvId);
                stmt.executeUpdate();
            }
            
            if (resolution.getStatus() == PathResolutionStatus.SUCCESS) {
                Path resolvedPath = resolution.getPath();
                long totalTime = 0;
                List<Node> nodes = resolvedPath.getNodes();
                
                // 计算每段路径的时间
                for (int i = 0; i < nodes.size() - 1; i++) {
                    Node currentNode = nodes.get(i);
                    Node nextNode = nodes.get(i + 1);
                    Edge edge = graph.getEdge(currentNode, nextNode);
                    
                    // 计算行驶时间
                    double distance = edge.getLength();
                    double speed = 0.5; // 使用默认速度
                    int travelSeconds = (int) Math.ceil(distance / speed);
                    
                    // 加上行驶时间
                    totalTime += travelSeconds;
                    
                    // 每个节点的停留时间（1秒）
                    totalTime += 1;
                    
                    // 如果需要转弯，加上转弯时间
                    if (i > 0 && isTurn(nodes.get(i-1), currentNode, nextNode)) {
                        totalTime += 3; // 假设转弯时间为3秒，可以根据实际情况调整
                    }
                }
                
                // 加上最后一个节点的停留时间
                totalTime += 1;
                
                // 加上冲突解决带来的延迟
                totalTime += resolution.delaySeconds;
                
                return totalTime;
            }
            
            return Long.MAX_VALUE;
            
        } catch (Exception e) {
            e.printStackTrace();
            return Long.MAX_VALUE;
        }
    }
    
    private double getSpeedByStateAndNodes(AGV.currentstate state, Node startNode, Node endNode) {
        try {
            String edgeId = startNode.getId() + "_" + endNode.getId();
            String sql = "SELECT empty_vehicle_speed FROM Edges WHERE edge_id = ?";
            
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, edgeId);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    return rs.getDouble("empty_vehicle_speed");
                }
                
                return AGV.Defaultspeed;
                
            } catch (SQLException e) {
                e.printStackTrace();
                return AGV.Defaultspeed;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return AGV.Defaultspeed;
        }
    }
    
    private boolean isTurn(Node n1, Node n2, Node n3) {
        double vector1X = n2.getX() - n1.getX();
        double vector1Y = n2.getY() - n1.getY();
        double vector2X = n3.getX() - n2.getX();
        double vector2Y = n3.getY() - n2.getY();

        double dotProduct = vector1X * vector2X + vector1Y * vector2Y;
        double magnitude1 = Math.sqrt(vector1X * vector1X + vector1Y * vector1Y);
        double magnitude2 = Math.sqrt(vector2X * vector2X + vector2Y * vector2Y);

        double cosTheta = dotProduct / (magnitude1 * magnitude2);
        return Math.abs(cosTheta) < 0.01; // cos(90°) ≈ 0
    }
    
    private static void calculateTravelTime(Path resolvedPath) {
        List<Node> nodes = resolvedPath.getNodes();
        for (int i = 0; i < nodes.size() - 1; i++) {
            Node currentNode = nodes.get(i);
            Node nextNode = nodes.get(i + 1);
            Edge edge = edgeDAO.getEdge(currentNode.getId(), nextNode.getId());
            
            if (edge != null) {
                // 计算行驶时间
                double distance = edge.getLength();
                double speed = edge.emptyVehicleSpeed > 0 ? edge.emptyVehicleSpeed : 0.5;
                int travelSeconds = (int) Math.ceil(distance / speed);
                
                // 设置到达时间和离开时间
                if (i == 0) {
                    currentNode.setDepartureTime(LocalDateTime.now().plusSeconds(1));
                }
                LocalDateTime arrivalTime = currentNode.getDepartureTime().plusSeconds(travelSeconds);
                nextNode.setArrivalTime(arrivalTime);
                nextNode.setDepartureTime(arrivalTime.plusSeconds(1));
            }
        }
    }
}

// 返回结果的类
class LateAgvResult {
    private Long agvId;
    private Long delayTime;
    
    // 修改构造函数，直接接收 AgvNodeVo 对象
    public LateAgvResult(AgvNodeVo agv, Long delayTime) {
        this.agvId = agv.getId();  // 直接使用 AgvNodeVo 的 id
        this.delayTime = delayTime;
    }
    

    
    // 保留原有构造函数
    public LateAgvResult(Long agvId, Long delayTime) {
        this.agvId = agvId;
        this.delayTime = delayTime;
    }
    
    // getter和setter
    public Long getAgvId() {
        return agvId;
    }
    
    public void setAgvId(Long agvId) {
        this.agvId = agvId;
    }
    
    public Long getDelayTime() {
        return delayTime;
    }
    
    public void setDelayTime(Long delayTime) {
        this.delayTime = delayTime;
    }
}

