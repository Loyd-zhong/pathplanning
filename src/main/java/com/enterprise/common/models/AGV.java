package com.enterprise.common.models;

import com.enterprise.common.utils.*;
import com.enterprise.common.algorithms.AStarPathfinder;
import com.enterprise.common.algorithms.TimeWindowManager;
import com.enterprise.common.dao.PathDAO;
import com.enterprise.common.dao.VehiclePassageDAO;
import com.enterprise.common.algorithms.ConflictDetector;
import java.awt.*;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import javax.swing.Timer;


// 修改的AGV类代码

public class AGV {
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

    
    // 定义速度档位以米/秒为单位）
    public enum SpeedLevel {
        SLOW(0.2),     // 每秒25米
        NORMAL(0.5),   // 每秒50米
        FAST(1),     // 每秒100米
        VERY_FAST(1.5); // 每秒150米

        private final double speed; // 移动速度（米/秒）

        SpeedLevel(double speed) {
            this.speed = speed;
        }

        public double getSpeed() {
            return speed;
        }
    }

    // AGV.java
    public void updateArrivalTimes() {
        if (currentPath == null || currentPath.getNodes().isEmpty()) {
            return;
        }

        double speed = speedLevel.getSpeed();
        LocalDateTime arrivalTime = LocalDateTime.now();

        List<Node> nodes = currentPath.getNodes();
        nodes.get(0).setArrivalTime(arrivalTime);

        for (int i = 1; i < nodes.size(); i++) {
            Node previousNode = nodes.get(i - 1);
            Node currentNode = nodes.get(i);

            Edge edge = getEdgeBetweenNodes(previousNode, currentNode);
            double distance = edge.getLength();
            double travelTimeInSeconds = distance / speed;

            // 检测转弯
            if (i < nodes.size() - 1) {
                Node nextNode = nodes.get(i + 1);
                if (isTurn(previousNode, currentNode, nextNode)) {
                    travelTimeInSeconds += getTurnTime();
                    System.out.printf("在节点 %s 进行转弯，到达节点 %s 预计时间增加 %.1f 秒。%n", 
                                      currentNode.getId(), nextNode.getId(), getTurnTime());
                }
            }

            Duration duration = Duration.ofMillis((long) (travelTimeInSeconds * 1000));
            arrivalTime = arrivalTime.plus(duration);

            currentNode.setArrivalTime(arrivalTime);
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

    public AGV(List<Path> paths, Color color, Graph graph, TimeWindowManager timeWindowManager, NetworkState networkState, AGVType type) {
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
            updateArrivalTimes();
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
        
        List<Node> nodes = currentPath.getNodes();
        
        // 遍历所有节点和边
        for (int i = 0; i < nodes.size(); i++) {
            Node currentNode = nodes.get(i);
            
            // 记录节点通过
            if (i < nodes.size() - 1) {
                Node nextNode = nodes.get(i + 1);
                
                // 记录节点
                vehiclePassageDAO.recordNodePassage(
                    "AGV-" + hashCode(),
                    currentNode,
                    currentNode.getArrivalTime(),
                    nextNode.getArrivalTime()
                );
                
                // 记录边
                Edge currentEdge = getEdgeBetweenNodes(currentNode, nextNode);
                if (currentEdge != null) {
                    vehiclePassageDAO.recordEdgePassage(
                        "AGV-" + hashCode(),
                        currentEdge,
                        currentNode.getArrivalTime(),
                        nextNode.getArrivalTime()
                    );
                }
            }
        }
    }

    public void updatePath(Path newPath) {
        if (newPath != null) {
            this.paths = List.of(newPath);
            this.currentPath = newPath;
            updateArrivalTimes();
        }
    }
}
