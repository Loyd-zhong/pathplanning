package com.enterprise.common.models;

import com.enterprise.common.utils.*;
import com.enterprise.common.algorithms.AStarPathfinder;
import com.enterprise.common.algorithms.TimeWindowManager;
import com.enterprise.common.utils.PathLogger;
import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

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
    private String agvId;
    private List<Path> alternativePaths;
    private LocalDateTime startTime;
    private static final int MAX_CAPACITY = 3; // 默认通行能力

    
    // 定义速度档位（以米/秒为单位）
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

            // 检���转弯
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

    public AGV(String agvId, List<Path> paths, Color color, Graph graph, TimeWindowManager timeWindowManager, NetworkState networkState) {
        this.agvId = agvId;
        this.paths = paths;
        this.currentPath = paths.get(0); // 初始使用第一条路径
        this.alternativePaths = new ArrayList<>(paths);
        this.color = color;
        this.graph = graph;
        this.timeWindowManager = timeWindowManager;
        this.currentPosition = currentPath.getNodes().get(0); // 初始位置为路径的第一个节点
        this.goalPosition = currentPath.getNodes().get(currentPath.getNodes().size() - 1);
        this.pathfinder = new AStarPathfinder();
        this.progress = 0.0;
        this.networkState = networkState;
        this.startTime = LocalDateTime.now();
    }

    public void planPath() {
        Path initialPath = calculatePath(currentPosition, goalPosition);
        if (initialPath != null) {
            currentPath = initialPath;
            updateArrivalTimes();
            PathLogger.logPath(agvId, currentPath, startTime, "初始路径规划");
        } else {
            System.out.println("无法找到初始路径");
        }
    }

    private Path calculatePath(Node start, Node goal) {
        return pathfinder.findPath(graph, start, goal);
    }

    private boolean checkConflicts() {
        // 读取历史日志并检查冲突
        List<PathLog> historicalLogs = readHistoricalLogs();
        for (Node node : currentPath.getNodes()) {
            if (isNodeOverloaded(node, historicalLogs)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNodeOverloaded(Node node, List<PathLog> logs) {
        int count = 0;
        LocalDateTime nodeArrivalTime = node.getArrivalTime();
        for (PathLog log : logs) {
            if (log.containsNode(node) && log.isTimeOverlapping(nodeArrivalTime)) {
                count++;
            }
        }
        return count >= MAX_CAPACITY;
    }

    private void handleConflicts() {
        Path reroutedPath = calculateAlternativePath();
        long rerouteTime = calculatePathTime(reroutedPath);
        
        long delayTime = calculateDelayTime();
        long originalPathTime = calculatePathTime(currentPath);
        
        if (rerouteTime < (originalPathTime + delayTime)) {
            currentPath = reroutedPath;
            System.out.println("选择绕路");
        } else {
            delayStart(delayTime);
            System.out.println("选择延时");
        }
    }

    private Path calculateAlternativePath() {
        // 实现绕路逻辑
        return pathfinder.findPath(graph, currentPosition, goalPosition, getOverloadedNodes());
    }

    private long calculateDelayTime() {
        long maxDelay = 0;
        List<PathLog> historicalLogs = readHistoricalLogs();
        for (Node node : currentPath.getNodes()) {
            LocalDateTime nodeArrivalTime = node.getArrivalTime();
            long nodeDelay = 0;
            int count = 0;
            for (PathLog log : historicalLogs) {
                if (log.containsNode(node) && log.isTimeOverlapping(nodeArrivalTime)) {
                    count++;
                    if (count >= MAX_CAPACITY) {
                        nodeDelay = Math.max(nodeDelay, ChronoUnit.SECONDS.between(nodeArrivalTime, log.getEndTime()));
                    }
                }
            }
            maxDelay = Math.max(maxDelay, nodeDelay);
        }
        return maxDelay;
    }

    private void delayStart(long delayTimeInMillis) {
        startTime = startTime.plusNanos(delayTimeInMillis * 1000000);
    }

    private long calculatePathTime(Path path) {
        long totalTime = 0;
        Node previousNode = null;
        for (Node node : path.getNodes()) {
            if (previousNode != null) {
                double distance = calculateDistance(previousNode, node);
                totalTime += (long) (distance / getCurrentSpeed());
                if (needToTurn(previousNode, node)) {
                    totalTime += getTurnTime();
                }
            }
            previousNode = node;
        }
        return totalTime;
    }

    private double calculateDistance(Node node1, Node node2) {
        return Math.sqrt(Math.pow(node2.getX() - node1.getX(), 2) + Math.pow(node2.getY() - node1.getY(), 2));
    }

    private boolean needToTurn(Node node1, Node node2) {
        // 简化的转弯判断,实际情况可能需要更复杂的逻辑
        return node1.getX() != node2.getX() && node1.getY() != node2.getY();
    }

    private double getCurrentSpeed() {
        switch (speedLevel) {
            case SLOW:
                return 0.5; // 米/秒
            case NORMAL:
                return 1.0; // 米/秒
            case FAST:
                return 1.5; // 米/秒
            default:
                return 1.0; // 默认速度
        }
    }

    private void logFinalPath() {
        // 将最终路径记录到日志文件
        PathLogger.logPath(agvId, currentPath, startTime, 
                           currentPath == paths.get(0) ? "选择延时" : "选择绕路");
    }

    private List<Node> getOverloadedNodes() {
        List<Node> overloadedNodes = new ArrayList<>();
        List<PathLog> historicalLogs = readHistoricalLogs();
        for (Node node : currentPath.getNodes()) {
            LocalDateTime nodeArrivalTime = node.getArrivalTime();
            int count = 0;
            for (PathLog log : historicalLogs) {
                if (log.containsNode(node) && log.isTimeOverlapping(nodeArrivalTime)) {
                    count++;
                }
            }
            if (count >= MAX_CAPACITY) {
                overloadedNodes.add(node);
            }
        }
        return overloadedNodes;
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
        if (currentPath == null || currentPosition == null || goalPosition == null) {
            return;
        }

        double speed = speedLevel.getSpeed(); // 获取当前速度
        List<Node> nodes = currentPath.getNodes(); // 获取路径中的节点

        for (int i = 1; i < nodes.size(); i++) {
            Node previousNode = nodes.get(i - 1);
            Node currentNode = nodes.get(i);

            // 计算当前节点和前一个节点之间距离
            Edge edge = getEdgeBetweenNodes(previousNode, currentNode);
            double distance = edge.getLength(); // 使用边的长度进行时间计算
            // 计算所需时间，距离除速度
            double travelTime = distance / speed;

            // 更新节点到达时间
            LocalDateTime previousArrivalTime = previousNode.getArrivalTime();
            LocalDateTime estimatedArrivalTime = previousArrivalTime.plusSeconds((long) Math.ceil(travelTime));
            currentNode.setArrivalTime(estimatedArrivalTime);
        }
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
            PathLogger.logPath(agvId, currentPath, startTime, "开始路径");
            currentNode = currentPath.getNodes().get(0);
            nextNodeIndex = 1;
            
        }
    }

    private static double turnTime = 0.5; // 默认转弯时间

    public static void setTurnTime(double time) {
        turnTime = time;
    }

    private double getTurnTime() {
        return turnTime;
    }

    private List<PathLog> readHistoricalLogs() {
        List<PathLog> logs = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("agv_paths.log"))) {
            String line;
            String currentAgvId = null;
            List<Node> currentNodes = new ArrayList<>();
            LocalDateTime currentStartTime = null;
            LocalDateTime currentEndTime = null;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("AGV ID:")) {
                    if (currentAgvId != null) {
                        logs.add(new PathLog(currentAgvId, currentNodes, currentStartTime, currentEndTime));
                        currentNodes = new ArrayList<>();
                    }
                    currentAgvId = line.split(":")[1].trim();
                } else if (line.startsWith("开始时间:")) {
                    currentStartTime = LocalDateTime.parse(line.split(":")[1].trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } else if (line.startsWith("节点ID:")) {
                    String[] parts = line.split(",");
                    String nodeId = parts[0].split(":")[1].trim();
                    LocalDateTime arrivalTime = LocalDateTime.parse(parts[1].split(":")[1].trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    currentNodes.add(new Node(0, 0, nodeId)); // 坐标信息不重要,只需要ID
                    currentEndTime = arrivalTime;
                }
            }
            if (currentAgvId != null) {
                logs.add(new PathLog(currentAgvId, currentNodes, currentStartTime, currentEndTime));
            }
        } catch (IOException e) {
            System.err.println("Error reading log file: " + e.getMessage());
        }
        return logs;
    }
}
