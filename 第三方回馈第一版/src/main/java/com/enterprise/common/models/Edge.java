// src/pathfinding/models/Edge.java
package com.enterprise.common.models;

public class Edge {
    private Node from;  // 起始节点
    private Node to;    // 结束节点
    private boolean isCurved; // 是否为弧线
    private double length; // 边的长度（直线或弧线）

    // 构造函数，增加长度和是否为弧线的标志
    public Edge(Node from, Node to, boolean isCurved, double length) {
        this.from = from;
        this.to = to;
        this.isCurved = isCurved;
        this.length = length;
    }

    public Node getFrom() {
        return from;
    }

    public Node getTo() {
        return to;
    }

    public boolean isCurved() {
        return isCurved;
    }

    // 获取边的长度，针对弧线计算实际长度
    public double getLength() {
        if (isCurved) {
            double curveLength = calculateCurveLength(from, to);

            return curveLength;
        } else {
            return length;
        }
    }

    public Node getOpposite(Node node) {
        if (node.equals(from)) {
            return to;
        } else if (node.equals(to)) {
            return from;
        } else {
            throw new IllegalArgumentException("Node is not part of this edge");
        }
    }
    // 示例：计算弧线长度的逻辑（使用贝塞尔曲线作为示例）
    private double calculateCurveLength(Node start, Node end) {
        // 定义控制点的位置，可以根据需要调整
        double controlX = (start.getX() + end.getX()) /2;
        double controlY = (start.getY() + end.getY()) - 50; // 控制点向上偏移50个单位

        return calculateBezierLength(start, new Node((int) controlX, (int) controlY), end);
    }

    // 离散化贝塞尔曲线，近似计算长度
    private double calculateBezierLength(Node start, Node control, Node end) {
        double length = 0;
        Node previous = start;
        for (double t = 0; t <= 1; t += 0.01) {
            double x = Math.pow(1 - t, 2) * start.getX() + 2 * (1 - t) * t * control.getX() + Math.pow(t, 2) * end.getX();
            double y = Math.pow(1 - t, 2) * start.getY() + 2 * (1 - t) * t * control.getY() + Math.pow(t, 2) * end.getY();
            Node current = new Node((int) x, (int) y);
            length += Math.sqrt(Math.pow(current.getX() - previous.getX(), 2) + Math.pow(current.getY() - previous.getY(), 2));
            previous = current;
        }
        return length;
    }
}
