package com.enterprise.common.models;

import java.util.ArrayList;
import java.util.List;

public class Path {
    private List<Node> nodes;
    private List<Double> costs;  // 存储与节点相关的成本

    public Path() {
        this.nodes = new ArrayList<>();
        this.costs = new ArrayList<>();
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void addNode(Node node) {
        nodes.add(node);
        costs.add(0.0);  // 默认成本为0
    }

    public void addNode(Node node, double cost) {
        nodes.add(node);
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
