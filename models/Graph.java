// src/pathfinding/models/Graph.java
package pathfinding.models;

import java.util.*;

public class Graph {
    private Map<Node, Set<Edge>> adjacencyList;

    public Graph() {
        this.adjacencyList = new HashMap<>();
    }

    public void addNode(Node node) {
        adjacencyList.putIfAbsent(node, new HashSet<>());
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
}
