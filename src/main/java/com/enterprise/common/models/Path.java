package com.enterprise.common.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Path {
    private List<Node> nodes;
    private List<Double> costs;  // 存储与节点相关的成本

    public Path() {
        this.nodes = new ArrayList<>();
        this.costs = new ArrayList<>();
    }

    public Path(List<Node> nodes) {
        this.nodes = new ArrayList<>();
        this.costs = new ArrayList<>();
        
        // 复制所有节点并初始化对应的成本
        for (Node node : nodes) {
            Node newNode = new Node(node.getX(), node.getY(), node.getId());
            newNode.setArrivalTime(node.getArrivalTime());
            this.nodes.add(newNode);
            this.costs.add(0.0);
        }
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void addNode(Node node) {
        Node newNode = new Node(node.getX(), node.getY(), node.getId());
        if (node.getArrivalTime() == null) {
            LocalDateTime now = LocalDateTime.now();
            newNode.setArrivalTime(now);
            newNode.setDepartureTime(now.plusSeconds(5));
        } else {
            newNode.setArrivalTime(node.getArrivalTime());
            newNode.setDepartureTime(node.getDepartureTime());
        }
        nodes.add(newNode);
        costs.add(0.0);
    }

    public void addNode(Node node, double cost) {
        Node newNode = new Node(node.getX(), node.getY(), node.getId());
        newNode.setArrivalTime(node.getArrivalTime());  // 保留到达时间
        nodes.add(newNode);
        costs.add(cost);
    }

    public List<Double> getCosts() {
        return costs;
    }

    public double getTotalCost() {
        double totalCost = 0.0;
        for (double cost : costs) {
            totalCost += cost;
        }
        return totalCost;
    }

    @Override
    public String toString() {
        return nodes.toString();
    }
}
