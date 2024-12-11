package com.enterprise.common.models;

import com.enterprise.common.utils.*;
import com.enterprise.common.algorithms.AStarPathfinder;
import com.enterprise.common.algorithms.TimeWindowManager;
import com.enterprise.common.dao.PathDAO;
import com.enterprise.common.dao.VehiclePassageDAO;
import com.enterprise.common.algorithms.ConflictManager;
import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import javax.swing.Timer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// 修改的AGV类代码

public class AGV {
    private String agvId;
    private List<Path> paths; // 存储多条路径
    private Path currentPath; // 当前使用的路径
    private Color color;
    private Node currentPosition;
    private Node goalPosition;
    private AStarPathfinder pathfinder;
    private TimeWindowManager timeWindowManager;
    private Graph graph;
    private double progress; // 用于跟踪AGV在当前边上的进度
    private NetworkState networkState;// 引入NetworkState实例
    private Runnable onUpdate;
    private int nextNodeIndex;
    private Node currentNode;
    private VehiclePassageDAO vehiclePassageDAO = new VehiclePassageDAO();
    public static double Defaultspeed = 4.0;
    public double speed;
    
    public  double getDefaultspeed() {
        return Defaultspeed;
    }

    public void setDefaultspeed(double Defaultspeed) {
        this.Defaultspeed = Defaultspeed;
    }
    
    // 定义速度档位以米/秒为单位）
    public enum SpeedLevel {
        SLOW(0.2),     // 每秒25米
        NORMAL(0.5),   // 每秒50米
        FAST(1),     // 每秒100米
        VERY_FAST(1.5); // 每秒150米
        private double speed; // 移动速度（米/秒）

        SpeedLevel(double speed) {
            this.speed = speed;
        }

        public double getSpeed() {
            return speed;
        }
    }
    public enum currentstate{
        emptyVehicle,
        backEmptyShelf,
        backToBackRack,
        backfillShelf;
        
    }
    public double getSpeedByStateAndNodes(currentstate state, Node startNode, Node endNode) {
        try {
            // 构建edge_id
            String edgeId = startNode.getId() + "_" + endNode.getId();
            double speed = Defaultspeed; // 默认速度
            
            // SQL查询语句
            String sql = "SELECT empty_vehicle_speed, back_empty_shelf_speed, back_to_back_rack_speed, backfill_shelf_speed " +
                         "FROM Edges WHERE edge_id = ?";
                         
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, edgeId);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    // 根据状态选择对应的速度
                    switch (state) {
                        case emptyVehicle:
                            speed = rs.getDouble("empty_vehicle_speed");
                            break;
                        case backEmptyShelf:
                            speed = rs.getDouble("back_empty_shelf_speed");
                            break;
                        case backToBackRack:
                            speed = rs.getDouble("back_to_back_rack_speed");
                            break;
                        case backfillShelf:
                            speed = rs.getDouble("backfill_shelf_speed");
                            break;
                        default:
                            speed = Defaultspeed;
                    }
                }
                
                
            } catch (SQLException e) {
                System.err.println("获取边速度失败: " + e.getMessage());
                e.printStackTrace();
                return Defaultspeed;
            }
            
            return speed;
        } catch (Exception e) {
            System.err.println("获取边速度失败: " + e.getMessage());
            e.printStackTrace();
            return Defaultspeed;
        }
    }
    // AGV.java
    public void updateArrivalTimes(Path path) {
        LocalDateTime currentTime = LocalDateTime.now();
        List<Node> nodes = path.getNodes();
        
        // 设置起点时间
        Node startNode = nodes.get(0);
        startNode.setArrivalTime(currentTime);
        startNode.setDepartureTime(currentTime.plusSeconds(1));  // 在起点停留1秒
        
        LocalDateTime nextArrivalTime = startNode.getDepartureTime();
        
        for (int i = 0; i < nodes.size() - 1; i++) {
            Node currentNode = nodes.get(i);
            Node nextNode = nodes.get(i + 1);
            
            // 计算边的通过时间
            double distance = calculateDistance(currentNode, nextNode);
            double timeNeeded = calculateTimeNeeded(distance, speedLevel.getSpeed());
            
            // 设置下一个节点的到达时间
            nextArrivalTime = nextArrivalTime.plusSeconds((long)Math.ceil(timeNeeded));
            nextNode.setArrivalTime(nextArrivalTime);
            nextNode.setDepartureTime(nextArrivalTime.plusSeconds(1));  // 在每个节点停留1秒
            
            // 更新下一段路径的开始时间
            nextArrivalTime = nextNode.getDepartureTime();
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

        // 允许一定的误差，因为浮点数计算可能不精确
        return Math.abs(cosTheta) < 0.01; // cos(90°) ≈ 0
    }



    // 通过图获取两个节点之间的边
    private Edge getEdgeBetweenNodes(Node a, Node b) {
        for (Edge edge : graph.getEdges(a)) {
            if (edge.getOpposite(a).equals(b)) {
                return edge;
            }
        }
        return null; // 或处理找不到的情况
    }


    private SpeedLevel speedLevel = SpeedLevel.NORMAL; // 默认速度档位为NORMAL

    public AGV(List<Path> paths, Color color, Graph graph, TimeWindowManager timeWindowManager, NetworkState networkState, AGVType type, String agvId) {
        this.agvId = agvId;
        this.paths = paths;
        if (!paths.isEmpty()) {
            // 确保路径中的所有节点都有时间信息
            LocalDateTime startTime = LocalDateTime.now();
            for (Path path : paths) {
                for (Node node : path.getNodes()) {
                    if (node.getArrivalTime() == null) {
                        node.setArrivalTime(startTime);
                        startTime = startTime.plusSeconds(5); // 每个节点间隔5秒
                    }
                }
            }
            this.currentPath = paths.get(0);
        }
        this.color = color;
        this.graph = graph;
        this.timeWindowManager = timeWindowManager;
        this.currentPosition = currentPath.getNodes().get(0);
        this.goalPosition = currentPath.getNodes().get(currentPath.getNodes().size() - 1);
        this.pathfinder = new AStarPathfinder();
        this.progress = 0.0;
        this.networkState = networkState;
        this.agvType = type;
        
        PathDAO pathDAO = new PathDAO();
        for (Path path : paths) {
            pathDAO.savePath(path, type.toString());
        }
    }

    public void moveToNextPosition() {
        int nextIndex = currentPath.getNodes().indexOf(currentPosition) + 1;
        if (nextIndex < currentPath.getNodes().size()) {
            Node nextNode = currentPath.getNodes().get(nextIndex);
            currentPosition = nextNode;
        } else {
            currentPosition = null;  // 到达终点
        }

        if (currentPosition != null && isPathConflict(currentPosition)) {
            switchToAlternatePath();
        }
        networkState.printNetworkState(); // 测试时输出当前网络状态
    }

    public Path getCurrentPath() {
        return currentPath;
    }

    public Color getColor() {
        return color;
    }

    public Node getCurrentPosition() {
        return currentPosition;
    }

    public void setSpeedLevel(SpeedLevel speedLevel) {
        this.speedLevel = speedLevel;
    }

    public SpeedLevel getSpeedLevel() {
        return speedLevel;
    }

    // 更新AGV的位置，按照每秒行驶指定米数的方式
    public void updatePosition(double deltaTime) {
        if (currentPath == null || currentPosition == null) {
            return;
        }

        List<Node> nodes = currentPath.getNodes();
        int currentIndex = nodes.indexOf(currentPosition);
        
        if (currentIndex >= 0 && currentIndex < nodes.size() - 1) {
            Node currentNode = nodes.get(currentIndex);
            Node nextNode = nodes.get(currentIndex + 1);
            
            // 计算移动进度
            double speed = speedLevel.getSpeed();
            progress += speed * deltaTime;
            
            // 获取当前边
            Edge currentEdge = getEdgeBetweenNodes(currentNode, nextNode);
            double edgeLength = currentEdge != null ? currentEdge.getLength() : 0;
            
            // 只更新位置，不记录通过信息
            if (progress >= edgeLength) {
                currentPosition = nextNode;
                progress = 0;
            }
        }
    }

    // 计算两个节点之间的欧几里得距离
    private double calculateDistance(Node a, Node b) {
        return Math.sqrt(Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getY() - b.getY(), 2));
    }

    private Node getNextNode() {
        int currentIndex = currentPath.getNodes().indexOf(currentPosition);
        return currentIndex + 1 < currentPath.getNodes().size() ? currentPath.getNodes().get(currentIndex + 1) : goalPosition;
    }


    private boolean isPathConflict(Node node) {
        return timeWindowManager.isAvailable(node, node.getArrivalTime().getSecond());
    }

    private void switchToAlternatePath() {
        for (Path path : paths) {
            if (path != currentPath) {
                if (timeWindowManager.isPathAvailable(path)) {
                    currentPath = path;
                    System.out.println("Switched to alternate path for AGV at node: " + currentPosition);
                    return;
                }
            }
        }
        System.out.println();
    }

    // 以每秒行驶米数的方式平滑移动AGV
    public void start(Runnable onUpdate) {
        this.onUpdate = onUpdate;
        if (currentPath != null && !currentPath.getNodes().isEmpty()) {
            updateArrivalTimes(currentPath);
            PathLogger.logPath(currentPath);
            currentNode = currentPath.getNodes().get(0);
            nextNodeIndex = 1;
            
            // 添加定时器，每100毫秒更新一次位置
            Timer timer = new Timer(100, e -> {
                updatePosition(0.1); // 传入时间间隔（秒）
                if (onUpdate != null) {
                    onUpdate.run();
                }
            });
            timer.start();
        }
    }

    private static double turnTime = 0.5; // 默认转弯时间

    public static void setTurnTime(double time) {
        turnTime = time;
    }

    private double getTurnTime() {
        return agvType.getTurnTime();
    }

    // AGV类型枚举
    public enum AGVType {
        TYPE_A(2.0),  // 转弯时间2秒
        TYPE_B(4.0);  // 转弯时间4秒

        private final double turnTime;

        AGVType(double turnTime) {
            this.turnTime = turnTime;
        }

        public double getTurnTime() {
            return turnTime;
        }
    }

    private AGVType agvType = AGVType.TYPE_A;  // 默认为Type A

    public void setAGVType(AGVType type) {
        this.agvType = type;
    }

    public AGVType getAGVType() {
        return agvType;
    }

    public void preRecordPath() {
        if (currentPath == null || currentPath.getNodes().isEmpty()) {
            return;
        }
        
        try {
            PathResolution resolution = ConflictManager.resolvePath(
                currentPath,
                this.agvId,
                graph,
                new AStarPathfinder()
            );
            
            if (resolution.getStatus() == PathResolutionStatus.SUCCESS) {
                currentPath = resolution.getPath();
                System.out.println(this.agvId + " 路径预记录成功");
            } else {
                System.err.println(this.agvId + " 路径预记录失败: " + 
                                 resolution.getStatus().getDescription());
            }
        } catch (Exception e) {
            System.err.println("路径预记录过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updatePath(Path newPath) {
        if (newPath != null) {
            this.paths = List.of(newPath);
            this.currentPath = newPath;
            updateArrivalTimes(currentPath);
        }
    }

    private double calculateTimeNeeded(double distance, double speed) {
        return distance / speed;
    }

    // 在AGV类的开头添加静态初始化块
    static {
        try {
            Class.forName("com.enterprise.common.algorithms.ConflictManager");
            System.out.println("ConflictManager类已成功加载");
        } catch (ClassNotFoundException e) {
            System.err.println("无法加载ConflictManager类: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
