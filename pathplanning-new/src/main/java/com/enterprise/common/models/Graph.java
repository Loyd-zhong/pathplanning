// src/pathfinding/models/Graph.java
package com.enterprise.common.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.enterprise.common.dao.EdgeDAO;

public class Graph implements Cloneable {
    private Map<Node, Set<Edge>> adjacencyList;
    private Map<String, Node> nodeMap;
    private static EdgeDAO edgeDAO = new EdgeDAO();

    public Graph() {
        this.adjacencyList = new HashMap<>();
        this.nodeMap = new HashMap<>();
    }

    public void addNode(Node node) {
        adjacencyList.putIfAbsent(node, new HashSet<>());
        nodeMap.put(node.getId(), node);
    }

    public Edge addCurvedEdge(Node from, Node to, boolean isDirectional) {
        Edge edge = new Edge(from, to, isDirectional, true, 100);
        adjacencyList.get(from).add(edge);
        if (!isDirectional) {
            adjacencyList.get(to).add(edge);
        }
        return edge;
    }

    public Edge addEdge(Node from, Node to, boolean isDirectional, double length, double weight) {
        Edge edge = new Edge(from, to, isDirectional, false, length);
        edge.setWeight(weight);
        adjacencyList.get(from).add(edge);
        if (!isDirectional) {
            adjacencyList.get(to).add(edge);
        }
        return edge;
    }

    public Set<Edge> getEdges(Node node) {
        return adjacencyList.getOrDefault(node, new HashSet<>());
    }

    public Edge getEdge(Node from, Node to) {
        if (from == null || to == null) return null;
        return edgeDAO.getEdge(from.getId(), to.getId());
    }

    public Set<Node> getNodes() {
        return adjacencyList.keySet();
    }

    public Node getNodeById(String id) {
        return nodeMap.get(id);
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
        adjacencyList.values().forEach(edgeSet -> 
            edgeSet.removeIf(edge -> edge.getFrom().equals(node) || edge.getTo().equals(node)));
    }

    @Override
    public Graph clone() {
        Graph cloned = new Graph();
        
        for (Node node : this.getNodes()) {
            Node clonedNode = node.clone();
            cloned.addNode(clonedNode);
        }
        
        for (Edge edge : this.getEdges()) {
            Node fromNode = cloned.getNodeById(edge.getFrom().getId());
            Node toNode = cloned.getNodeById(edge.getTo().getId());
            if (edge.isCurved()) {
                cloned.addCurvedEdge(fromNode, toNode, true);
            } else {
                cloned.addEdge(fromNode, toNode, true, edge.getLength(), edge.getWeight());
            }
        }
        
        return cloned;
    }
}
