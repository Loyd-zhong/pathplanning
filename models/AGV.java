package pathfinding.models;

import pathfinding.algorithms.AStarPathfinder;
import pathfinding.algorithms.TimeWindowManager;
import pathfinding.algorithms.AStarPathfinder;
import pathfinding.algorithms.TimeWindowManager;

import java.awt.Color;
import java.util.List;
import java.time.Duration;
import java.awt.Color;
import java.time.LocalDateTime;
import java.util.List;


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

        double speed = speedLevel.getSpeed(); // 获取 AGV 的速度
        LocalDateTime arrivalTime = LocalDateTime.now(); // 起点的到达时间

        List<Node> nodes = currentPath.getNodes();
        nodes.get(0).setArrivalTime(arrivalTime); // 设置起点的到达时间

        for (int i = 1; i < nodes.size(); i++) {
            Node previousNode = nodes.get(i - 1);
            Node currentNode = nodes.get(i);

            // 获取边信息，确保使用的是边的实际长度（包括弧线）
            Edge edge = getEdgeBetweenNodes(previousNode, currentNode);
            double distance = edge.getLength(); // 使用边的长度进行时间计算
            double travelTimeInSeconds = distance / speed; // 确保这里使用的是正确的距离

            // 输出调试信息以确认计算是否正确
            System.out.println("Using edge length for time calculation: " + distance + " meters");

            // 使用 Duration 计算精确的时间间隔
            Duration duration = Duration.ofMillis((long) (travelTimeInSeconds * 1000));
            arrivalTime = arrivalTime.plus(duration); // 使用 Duration 进行时间增加

            currentNode.setArrivalTime(arrivalTime); // 设置当前节点的到达时间
        }
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

    public AGV(List<Path> paths, Color color, Graph graph, TimeWindowManager timeWindowManager,NetworkState networkState) {
        this.paths = paths;
        this.currentPath = paths.get(0); // 初始使用第一条路径
        this.color = color;
        this.graph = graph;
        this.timeWindowManager = timeWindowManager;
        this.currentPosition = currentPath.getNodes().get(0); // 初始位置为路径的第一个节点
        this.goalPosition = currentPath.getNodes().get(currentPath.getNodes().size() - 1);
        this.pathfinder = new AStarPathfinder();
        this.progress = 0.0;
        this.networkState = networkState;
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

            // 计算当前节点和前一个节点之间的距离
            Edge edge = getEdgeBetweenNodes(previousNode, currentNode);
            double distance = edge.getLength(); // 使用边的长度进行时间计算
            // 计算所需时间，距离除以速度
            double travelTime = distance / speed;

            // 更新节点到达时间
            LocalDateTime previousArrivalTime = previousNode.getArrivalTime();
            LocalDateTime estimatedArrivalTime = previousArrivalTime.plusSeconds((long) Math.ceil(travelTime));
            currentNode.setArrivalTime(estimatedArrivalTime);
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
    public void start(Runnable onMove) {
        new Thread(() -> {
            long lastTime = System.currentTimeMillis();
            while (currentPosition != null && !currentPosition.equals(goalPosition)) {
                long currentTime = System.currentTimeMillis();
                double deltaTime = (currentTime - lastTime) / 1000.0; // 转换为秒
                updatePosition(deltaTime); // 更新位置
                onMove.run();
                lastTime = currentTime;

                try {
                    Thread.sleep(50); // 刷新频率，每50毫秒刷新一次位置
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
