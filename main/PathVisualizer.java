// src/pathfinding/main/PathVisualizer.java
package pathfinding.main;

import javax.swing.*;

import pathfinding.algorithms.AStarPathfinder;
import pathfinding.models.Node;
import pathfinding.models.Edge;
import pathfinding.models.Graph;
import pathfinding.models.AGV;
import pathfinding.models.Path;
import pathfinding.models.NetworkState;
import pathfinding.algorithms.ThetaStarPathfinder;
import pathfinding.algorithms.TimeWindowManager;
import pathfinding.utils.MapLoader;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.QuadCurve2D;
import java.util.ArrayList;
import java.util.List;

public class PathVisualizer extends JPanel {

    private static final int NODE_RADIUS = 5; // 节点的显示半径
    private List<AGV> agvs = new ArrayList<>(); // AGV列表
    private Graph graph; // 图形结构对象
    private Node hoveredNode = null;  // 当前鼠标悬停的节点

    public PathVisualizer() {
        String xmlFilePath = "D:/AGV规划系统（4.25启动）/甲方文件/真实版本.xml"; // 请确保路径正确
        this.graph = MapLoader.loadMap(xmlFilePath); // 创建图形对象并初始化节点和边
        setPreferredSize(new Dimension(800, 800)); // 设置首选大小

        initializeTasks(); // 初始化 AGV 任务

        // 添加鼠标移动监听器，处理节点悬停功能
        addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseMoved(MouseEvent e) {
                hoveredNode = getNodeAtPosition(e.getX(), e.getY()); // 获取鼠标位置下的节点
                repaint();  // 触发重新绘制界面
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                // 此方法不需要实现
            }
        });
    }

    // 根据坐标获取当前鼠标位置下的节点
    private Node getNodeAtPosition(int x, int y) {
        for (Node node : graph.getNodes()) {
            double distance = Math.sqrt(Math.pow(node.getX() - x, 2) + Math.pow(node.getY() - y, 2));
            if (distance <= NODE_RADIUS) {
                return node;
            }
        }
        return null;
    }

    // 初始化 AGV 的任务和路径
    private void initializeTasks() {
        Color[] colors = {Color.RED, Color.GREEN, Color.BLUE}; // 用于不同 AGV 的颜色
        NetworkState networkState = new NetworkState();  // 创建网络状态实例

        // 添加一些任务示例，设置 AGV 的起点和终点
        List<Node[]> tasks = new ArrayList<>();
        tasks.add(new Node[]{new Node(605.133, 202.33), new Node(615.939, 202.262)});
        tasks.add(new Node[]{new Node(200, 250), new Node(100, 400)});
        tasks.add(new Node[]{new Node(700, 50), new Node(450, 650)});

        for (int i = 0; i < tasks.size(); i++) {
            Node startNode = tasks.get(i)[0];
            Node goalNode = tasks.get(i)[1];


            List<Path> paths = calculateMultiplePaths(startNode, goalNode);
            if (!paths.isEmpty()) {
                AGV agv = new AGV(paths, colors[i % colors.length], graph, new TimeWindowManager(), networkState);
                agv.start(this::repaint); // 启动 AGV 并在更新时触发界面重绘

                // 根据索引设置不同的速度级别
                switch (i) {
                    case 0:
                        agv.setSpeedLevel(AGV.SpeedLevel.SLOW);
                        break;
                    case 1:
                        agv.setSpeedLevel(AGV.SpeedLevel.SLOW);
                        break;
                    case 2:
                        agv.setSpeedLevel(AGV.SpeedLevel.SLOW);
                        break;
                    default:
                        agv.setSpeedLevel(AGV.SpeedLevel.NORMAL);
                        break;
                }

                agvs.add(agv); // 将 AGV 添加到列表中
            } else {
                System.out.println("No valid path found for task.");
            }
        }
    }

    // 使用 AStarPathfinder 计算路径
    private List<Path> calculateMultiplePaths(Node start, Node goal) {
        AStarPathfinder pathfinder = new AStarPathfinder();
        List<Path> paths = new ArrayList<>();

        // 计算主路径
        Path primaryPath = pathfinder.findPath(graph, start, goal);
        System.out.println("Starting node: " + start + ", Goal node: " + goal);
        if (primaryPath != null) {
            paths.add(primaryPath); // 添加主路径
        }

        // 可以添加备用路径的逻辑，这里仅添加一个主路径
        return paths;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        drawEdges(g2); // 绘制图中的边
        drawNodes(g2); // 绘制图中的节点
        drawPathsAndAGVs(g2); // 绘制路径和 AGVs

        // 如果有节点被鼠标悬停，显示其坐标
        if (hoveredNode != null) {
            g2.setColor(Color.RED);
            String coordinates = String.format("(%f, %f)", hoveredNode.getX(), hoveredNode.getY());
            g2.drawString(coordinates, ((int)hoveredNode.getX()) + 10, (int)hoveredNode.getY() - 10);
        }
    }

    // 绘制图中的边
    private void drawEdges(Graphics2D g2) {
        g2.setColor(Color.GRAY);
        for (Node node : graph.getNodes()) {
            for (Edge edge : graph.getEdges(node)) {
                if (edge.isCurved()) {
                    drawCurvedLine(g2, edge.getFrom(), edge.getTo()); // 绘制弧线
                } else {
                    g2.drawLine((int)edge.getFrom().getX(), (int)edge.getFrom().getY(), (int)edge.getTo().getX(),(int) edge.getTo().getY()); // 绘制直线
                }
            }
        }
    }
    private void drawCurvedLine(Graphics2D g2, Node from, Node to) {
        double midX = (from.getX() + to.getX()) / 2;
        double midY = (from.getY() + to.getY()) / 2;
        double controlX = midX + 20; // 控制点偏移量调整以实现弯曲效果
        double controlY = midY - 20;

        QuadCurve2D curve = new QuadCurve2D.Float();
        curve.setCurve(from.getX(), from.getY(), controlX, controlY, to.getX(), to.getY());
        g2.draw(curve);
    }

    // 绘制图中的节点
    private void drawNodes(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        for (Node node : graph.getNodes()) {
            g2.fillOval((int)node.getX() - NODE_RADIUS, (int)node.getY() - NODE_RADIUS, 2 * NODE_RADIUS, 2 * NODE_RADIUS);
        }
    }

    // 绘制路径和 AGV
    private void drawPathsAndAGVs(Graphics2D g2) {
        for (AGV agv : agvs) {
            g2.setColor(agv.getColor());
            drawPath(g2, agv.getCurrentPath());
            drawAGV(g2, agv);
        }
    }

    // 绘制路径
    private void drawPath(Graphics2D g2, Path path) {
        if (path == null) {
            return;
        }

        Node previousNode = null;
        for (Node node : path.getNodes()) {
            if (previousNode != null) {
                g2.drawLine((int)previousNode.getX(), (int)previousNode.getY(), (int)node.getX(), (int)node.getY());
            }
            previousNode = node;
        }
    }

    // 绘制 AGV
    private void drawAGV(Graphics2D g2, AGV agv) {
        Node position = agv.getCurrentPosition();
        if (position != null) {
            g2.fillOval((int)position.getX() - NODE_RADIUS, (int)position.getY() - NODE_RADIUS, 2 * NODE_RADIUS, 2 * NODE_RADIUS);
        }
    }

    // 动态创建不规则的图形
    // src/pathfinding/main/PathVisualizer.java
    // src/pathfinding/main/PathVisualizer.java
    private Graph createGraphFromMap() {
        Graph graph = new Graph();

        // 添加节点（不规则位置）
        Node n1 = new Node(50, 50);
        Node n2 = new Node(150, 120);
        Node n3 = new Node(300, 250);
        Node n4 = new Node(500, 400);
        Node n5 = new Node(650, 550);
        Node n6 = new Node(100, 300);
        Node n7 = new Node(200, 450);
        Node n8 = new Node(400, 300);
        Node n9 = new Node(550, 150);
        Node n10 = new Node(700, 50);
        Node n11 = new Node(50, 400);
        Node n12 = new Node(150, 500);
        Node n13 = new Node(250, 600);
        Node n14 = new Node(350, 700);
        Node n15 = new Node(450, 650);
        Node n16 = new Node(600, 700);
        Node n17 = new Node(750, 600);
        Node n18 = new Node(800, 400);
        Node n19 = new Node(850, 200);
        Node n20 = new Node(900, 50);
        Node n21 = new Node(300, 50);
        Node n22 = new Node(400, 100);
        Node n23 = new Node(500, 50);
        Node n24 = new Node(600, 100);
        Node n25 = new Node(750, 300);
        Node n26 = new Node(200, 200);
        Node n27 = new Node(350, 400);
        Node n28 = new Node(450, 500);
        Node n29 = new Node(650, 250);
        Node n30 = new Node(800, 100);

        // 将节点添加到图中
        graph.addNode(n1);
        graph.addNode(n2);
        graph.addNode(n3);
        graph.addNode(n4);
        graph.addNode(n5);
        graph.addNode(n6);
        graph.addNode(n7);
        graph.addNode(n8);
        graph.addNode(n9);
        graph.addNode(n10);
        graph.addNode(n11);
        graph.addNode(n12);
        graph.addNode(n13);
        graph.addNode(n14);
        graph.addNode(n15);
        graph.addNode(n16);
        graph.addNode(n17);
        graph.addNode(n18);
        graph.addNode(n19);
        graph.addNode(n20);
        graph.addNode(n21);
        graph.addNode(n22);
        graph.addNode(n23);
        graph.addNode(n24);
        graph.addNode(n25);
        graph.addNode(n26);
        graph.addNode(n27);
        graph.addNode(n28);
        graph.addNode(n29);
        graph.addNode(n30);

        // 添加边及其长度和权重
        graph.addEdge(n1, n2, 110, 1.0);
        graph.addEdge(n2, n3, 160, 1.2);
        graph.addEdge(n3, n4, 170, 1.1);
        graph.addEdge(n4, n5, 140, 1.3);
        graph.addEdge(n1, n6, 270, 1.5);
        graph.addEdge(n6, n7, 180, 1.2);
        graph.addEdge(n7, n8, 230, 1.4);
        graph.addEdge(n8, n4, 120, 1.1);
        graph.addEdge(n2, n7, 190, 1.6);
        graph.addEdge(n3, n8, 160, 1.3);
        graph.addEdge(n8, n9, 250, 1.2);
        graph.addEdge(n9, n10, 130, 1.0);
        graph.addEdge(n5, n10, 200, 1.4);
        graph.addEdge(n11, n12, 150, 1.1);
        graph.addEdge(n12, n13, 130, 1.3);
        graph.addEdge(n13, n14, 160, 1.2);
        graph.addEdge(n14, n15, 140, 1.0);
        graph.addEdge(n15, n16, 180, 1.4);
        graph.addCurvedEdge(n1, n16);

        graph.addEdge(n16, n17, 1, 1.4);
        graph.addEdge(n17, n18, 170, 1.2);
        graph.addEdge(n18, n19, 140, 1.5);
        graph.addEdge(n19, n20, 160, 1.1);
        graph.addEdge(n3, n21, 130, 1.2);
        graph.addEdge(n21, n22, 110, 1.3);
        graph.addEdge(n22, n23, 140, 1.4);
        graph.addEdge(n23, n24, 150, 1.1);
        graph.addEdge(n24, n25, 160, 1.0);
        graph.addEdge(n25, n18, 120, 1.3);
        graph.addEdge(n2, n26, 120, 1.5);
        graph.addEdge(n26, n27, 110, 1.0);
        graph.addEdge(n27, n28, 130, 1.2);
        graph.addEdge(n28, n4, 150, 1.3);
        graph.addEdge(n4, n29, 140, 1.1);
        graph.addEdge(n29, n30, 130, 1.4);
        graph.addEdge(n30, n19, 110, 1.2);

        return graph;
    }



    public List<AGV> getAGVs() {
        return agvs; // 返回 AGV 列表
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Pathfinding Visualization");
        PathVisualizer visualizer = new PathVisualizer();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(visualizer);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
