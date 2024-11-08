// src/pathfinding/models/Graph.java
package com.enterprise.common.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Graph implements Cloneable {
    private Map<Node, Set<Edge>> adjacencyList;
    private Map<String, Node> nodeMap; // 新增一个用于存储 Node 的 id 和 Node 之间的映射

    public Graph() {
        this.adjacencyList = new HashMap<>();
        this.nodeMap = new HashMap<>(); // 初始化 nodeMap
    }

    public void addNode(Node node) {
        adjacencyList.putIfAbsent(node, new HashSet<>());
        nodeMap.put(node.getId(), node); // 将节点添加到 nodeMap 中
    }

    // 增加对弧线的添加
    public void addCurvedEdge(Node from, Node to) {
        // 计算弧线的长度并添加边
        Edge edge = new Edge(from, to, true, 100); // 初始长度为 0，实际长度在 Edge 中计算
        adjacencyList.get(from).add(edge);
        adjacencyList.get(to).add(edge);
    }

    // 普通直线边添加
    public void addEdge(Node node1, Node node2, double length, double weight) {
        Edge edge = new Edge(node1, node2, false, length);
        adjacencyList.get(node1).add(edge);
        adjacencyList.get(node2).add(edge);
    }

    public Set<Edge> getEdges(Node node) {
        return adjacencyList.getOrDefault(node, new HashSet<>());
    }

    public Set<Node> getNodes() {
        return adjacencyList.keySet();
    }

    // 新增方法：根据节点的 id 获取节点
    public Node getNodeById(String id) {
        return nodeMap.get(id); // 返回与 id 对应的 Node
    }

    public List<Edge> getEdges() {
        return adjacencyList.values()
                .stream()
                .flatMap(Set::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    public void removeNode(Node node) {
        adjacencyList.remove(node);
        nodeMap.remove(node.getId());
        // 移除与该节点相关的所有边
        adjacencyList.values().forEach(edgeSet -> edgeSet.removeIf(edge -> 
            edge.getFrom().equals(node) || edge.getTo().equals(node)));
    }

    public void removeNodeAndEdges(Node node) {
        adjacencyList.remove(node);
        nodeMap.remove(node.getId());
        // 移除与该节点相关的所有边
        adjacencyList.values().forEach(edgeSet -> 
            edgeSet.removeIf(edge -> edge.getFrom().equals(node) || edge.getTo().equals(node)));
    }

    /*public void addNode(Node node) {
        nodes.add(node);
    }*/

    @Override
    public Graph clone() {
        Graph cloned = new Graph();
        
        // 复制所有节点
        for (Node node : this.getNodes()) {
            Node clonedNode = node.clone();
            cloned.addNode(clonedNode);
        }
        
        // 复制所有边
        for (Edge edge : this.getEdges()) {
            Node fromNode = cloned.getNodeById(edge.getFrom().getId());
            Node toNode = cloned.getNodeById(edge.getTo().getId());
            if (edge.isCurved()) {
                cloned.addCurvedEdge(fromNode, toNode);
            } else {
                cloned.addEdge(fromNode, toNode, edge.getLength(), 1.0); // 使用默认权重 1.0
            }
        }
        
        return cloned;
    }
}
