package com.enterprise.common.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Path {
    private List<Node> nodes;
    private List<Double> costs;  // 存储与节点相关的成本
    private Map<String, Double> nodeDepartureSpeed; // 存储节点出发速度的映射

    public Path() {
        this.nodes = new ArrayList<>();
        this.costs = new ArrayList<>();
        this.nodeDepartureSpeed = new HashMap<>();
    }

    public Path(List<Node> nodes) {
        this.nodes = new ArrayList<>();
        this.costs = new ArrayList<>();
        this.nodeDepartureSpeed = new HashMap<>();
        
        // 复制所有节点并初始化对应的成本
        for (Node node : nodes) {
            Node newNode = new Node(node.getX(), node.getY(), node.getId());
            newNode.setArrivalTime(node.getArrivalTime());
            this.nodes.add(newNode);
            this.costs.add(0.0);
            this.nodeDepartureSpeed.put(node.getId(), 0.0); // 初始化速度为0
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
        nodeDepartureSpeed.put(node.getId(), 0.0); // 初始化速度为0
    }

    public void addNode(Node node, double cost) {
        Node newNode = new Node(node.getX(), node.getY(), node.getId());
        newNode.setArrivalTime(node.getArrivalTime());  // 保留到达时间
        nodes.add(newNode);
        costs.add(cost);
        nodeDepartureSpeed.put(node.getId(), 0.0); // 初始化速度为0
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

    // 设置节点出发速度
    public void setNodeDepartureSpeed(String nodeId, double speed) {
        nodeDepartureSpeed.put(nodeId, speed);
    }

    // 获取节点出发速度
    public double getNodeDepartureSpeed(String nodeId) {
        return nodeDepartureSpeed.getOrDefault(nodeId, 0.0);
    }

    // 获取所有节点速度映射
    public Map<String, Double> getAllNodeDepartureSpeeds() {
        return new HashMap<>(nodeDepartureSpeed);
    }

    @Override
    public String toString() {
        return nodes.toString();
    }
}
