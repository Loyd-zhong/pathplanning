// src/pathfinding/main/PathVisualizer.java
package com.enterprise.common.main;


import com.enterprise.common.algorithms.AStarPathfinder;
import com.enterprise.common.algorithms.TimeWindowManager;
import com.enterprise.common.models.*;
import com.enterprise.common.utils.MapLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.QuadCurve2D;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.MouseAdapter;
import java.util.HashSet;
import java.util.Set;
// ... 其他现有的导入语句 ...
public class PathVisualizer extends JPanel {

    private static final int NODE_RADIUS = 5; // 节点的显示半径
    private List<AGV> agvs = new ArrayList<>(); // AGV列表
    private Graph graph; // 图形结构对象
    private Node hoveredNode = null;  // 当前鼠标悬停的节点
    private boolean pathPrinted = false;
    private double scale = 1.0;
    private double translateX = 0;
    private double translateY = 0;
    private Point lastMousePosition;

    public PathVisualizer() {
        String xmlFilePath = "src/main/java/com/enterprise/common/resources/真实版本.xml"; // 请确保路径正确
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

        addMouseWheelListener(e -> {
            double zoomFactor = 1.05;
            if (e.getWheelRotation() < 0) {
                scale *= zoomFactor;
            } else {
                scale /= zoomFactor;
            }
            repaint();
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMousePosition = e.getPoint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastMousePosition.x;
                int dy = e.getY() - lastMousePosition.y;
                translateX += dx;
                translateY += dy;
                lastMousePosition = e.getPoint();
                repaint();
            }
        });

        
    }

    // 根据坐标获取当前鼠标位置下的节点
    private Node getNodeAtPosition(int x, int y) {
        // 考虑缩放和平移
        double adjustedX = (x - translateX) / scale;
        double adjustedY = (y - translateY) / scale;
        for (Node node : graph.getNodes()) {
            double distance = Math.sqrt(Math.pow(node.getX() - adjustedX, 2) + Math.pow(node.getY() - adjustedY, 2));
            if (distance <= NODE_RADIUS / scale) {
                return node;
            }
        }
        return null;
    }

    // 初始化 AGV 的任务和路径
    private void initializeTasks() {
        Color[] colors = {Color.RED, Color.GREEN, Color.BLUE};
        NetworkState networkState = new NetworkState();

        List<Node[]> tasks = new ArrayList<>();
        tasks.add(new Node[]{new Node(605.133, 202.33,"263"), new Node(615.939, 202.262,"524")});
        tasks.add(new Node[]{new Node(603.133, 202.33, "263"), new Node(615.939, 202.262, "524")});
        tasks.add(new Node[]{new Node(6053.133, 202.33, "263"), new Node(615.939, 202.262, "524")});

        for (int i = 0; i < tasks.size(); i++) {
            Node startNode = tasks.get(i)[0];
            Node goalNode = tasks.get(i)[1];

            List<Path> paths = calculateMultiplePaths(startNode, goalNode);
            if (!paths.isEmpty()) {
                AGV agv = new AGV("AGV_" + i, paths, colors[i % colors.length], graph, new TimeWindowManager(), networkState);
                agv.planPath(); // 执行路径规划和冲突处理
                
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

                agvs.add(agv);
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
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 应用缩放和平移
        g2.translate(translateX, translateY);
        g2.scale(scale, scale);

        drawEdges(g2);
        drawNodes(g2);
        drawAGVs(g2);

        // 如果有悬停的节点，绘信息
        if (hoveredNode != null) {
            drawNodeInfo(g2, hoveredNode);
        }
    }

    // 绘制图中的边
    private void drawEdges(Graphics2D g2) {
        g2.setColor(Color.GRAY);
        for (Node node : graph.getNodes()) {
            for (Edge edge : graph.getEdges(node)) {
                int x1 = (int) ((edge.getFrom().getX() + translateX) * scale);
                int y1 = (int) ((edge.getFrom().getY() + translateY) * scale);
                int x2 = (int) ((edge.getTo().getX() + translateX) * scale);
                int y2 = (int) ((edge.getTo().getY() + translateY) * scale);
                if (edge.isCurved()) {
                    drawCurvedLine(g2, edge.getFrom(), edge.getTo());
                } else {
                    g2.drawLine(x1, y1, x2, y2);
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
            int x = (int) ((node.getX() + translateX) * scale);
            int y = (int) ((node.getY() + translateY) * scale);
            g2.fillOval(x - NODE_RADIUS, y - NODE_RADIUS, 2 * NODE_RADIUS, 2 * NODE_RADIUS);
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
        if (!pathPrinted) {
            System.out.println("Path: ");
            for (Node node : path.getNodes()) {
                System.out.printf("Node(id: %s, x: %.2f, y: %.2f)%n", node.getId(), node.getX(), node.getY());
            }
            pathPrinted = true;
        }
        for (Node node : path.getNodes()) {
            if (previousNode != null) {
                g2.drawLine((int) previousNode.getX(), (int) previousNode.getY(), (int) node.getX(), (int) node.getY());
            }
            previousNode = node;
        }
    }

    // 绘 AGV
    private void drawAGV(Graphics2D g2, AGV agv) {
        Node position = agv.getCurrentPosition();
        if (position != null) {
            g2.fillOval((int) position.getX() - NODE_RADIUS, (int) position.getY() - NODE_RADIUS, 2 * NODE_RADIUS, 2 * NODE_RADIUS);
        }
    }

    private void drawAGVs(Graphics2D g2) {
        for (AGV agv : agvs) {
            g2.setColor(agv.getColor());
            Node position = agv.getCurrentPosition();
            if (position != null) {
                int x = (int) ((position.getX() + translateX) * scale);
                int y = (int) ((position.getY() + translateY) * scale);
                g2.fillOval(x - NODE_RADIUS, y - NODE_RADIUS, 2 * NODE_RADIUS, 2 * NODE_RADIUS);
            }
        }
    }

    private void drawNodeInfo(Graphics2D g2, Node node) {
        String info = String.format("节点ID: %s, X: %.2f, Y: %.2f", node.getId(), node.getX(), node.getY());
        g2.setColor(Color.BLACK);
        g2.drawString(info, (int)node.getX() + 10, (int)node.getY() - 10);
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
