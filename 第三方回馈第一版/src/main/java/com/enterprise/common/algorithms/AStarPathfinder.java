// src/pathfinding/algorithms/AStarPathfinder.java
package com.enterprise.common.algorithms;

import com.enterprise.common.models.Edge;
import com.enterprise.common.models.Graph;
import com.enterprise.common.models.Node;
import com.enterprise.common.models.Path;

import java.time.LocalDateTime;
import java.util.*;

public class AStarPathfinder {

    private Set<Node> closedSet = new HashSet<>();
    private PriorityQueue<Node> openSet;
    private Map<Node, Node> cameFrom = new HashMap<>();
    private Map<Node, Double> gScore = new HashMap<>();
    private Map<Node, Double> fScore = new HashMap<>();

    public AStarPathfinder() {
        openSet = new PriorityQueue<>(Comparator.comparingDouble(node -> fScore.getOrDefault(node, Double.POSITIVE_INFINITY)));
    }

    public Path findPath(Graph graph, Node start, Node goal) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph cannot be null");
        }
        if (start == null || goal == null) {
            throw new IllegalArgumentException("Start and goal nodes cannot be null");
        }

        gScore.put(start, 0.0);
        fScore.put(start, heuristicCostEstimate(start, goal));
        openSet.add(start);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.equals(goal)) {
                return reconstructPath(current);
            }

            closedSet.add(current);

            // 逐一检查当前节点的所有连接的边，确保路径遵循图中的边
            for (Edge edge : graph.getEdges(current)) {
                Node neighbor = edge.getOpposite(current);
                if (closedSet.contains(neighbor)) continue;

                double tentativeGScore = gScore.get(current) + edge.getLength();
                if (tentativeGScore < gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeGScore);
                    fScore.put(neighbor, tentativeGScore + heuristicCostEstimate(neighbor, goal));
                    openSet.add(neighbor);
                }
            }
        }

        return null;
    }

    public Path findPath(Graph graph, String startId, String goalId) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph cannot be null");
        }
        if (startId == null || goalId == null) {
            throw new IllegalArgumentException("Start and goal IDs cannot be null");
        }

        Node start = graph.getNodeById(startId);
        Node goal = graph.getNodeById(goalId);

        if (start == null || goal == null) {
            throw new IllegalArgumentException("Start or goal node with given ID not found in the graph");
        }

        return findPath(graph, start, goal);
    }

    public Path findPath(Graph graph, Node start, Node goal, List<Node> nodesToAvoid) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph cannot be null");
        }
        if (start == null || goal == null) {
            throw new IllegalArgumentException("Start and goal nodes cannot be null");
        }
        if (nodesToAvoid == null) {
            nodesToAvoid = new ArrayList<>();
        }

        // 重置所有集合和映射
        closedSet.clear();
        openSet.clear();
        cameFrom.clear();
        gScore.clear();
        fScore.clear();

        gScore.put(start, 0.0);
        fScore.put(start, heuristicCostEstimate(start, goal));
        openSet.add(start);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.equals(goal)) {
                return reconstructPath(current);
            }

            closedSet.add(current);

            // 逐一检查当前节点的所有连接的边，确保路径遵循图中的边
            for (Edge edge : graph.getEdges(current)) {
                Node neighbor = edge.getOpposite(current);
                if (closedSet.contains(neighbor) || nodesToAvoid.contains(neighbor)) continue;

                double tentativeGScore = gScore.get(current) + edge.getLength();
                if (tentativeGScore < gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeGScore);
                    fScore.put(neighbor, tentativeGScore + heuristicCostEstimate(neighbor, goal));
                    openSet.add(neighbor);
                }
            }
        }

        return null;
    }

    public Path findPath(Graph graph, String startId, String goalId, List<String> nodeIdsToAvoid) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph cannot be null");
        }
        if (startId == null || goalId == null) {
            throw new IllegalArgumentException("Start and goal IDs cannot be null");
        }

        Node start = graph.getNodeById(startId);
        Node goal = graph.getNodeById(goalId);

        if (start == null || goal == null) {
            throw new IllegalArgumentException("Start or goal node with given ID not found in the graph");
        }

        List<Node> nodesToAvoid = new ArrayList<>();
        if (nodeIdsToAvoid != null) {
            for (String id : nodeIdsToAvoid) {
                Node node = graph.getNodeById(id);
                if (node != null) {
                    nodesToAvoid.add(node);
                }
            }
        }

        return findPath(graph, start, goal, nodesToAvoid);
    }

    private Path reconstructPath(Node current) {
        List<Node> nodes = new ArrayList<>();
        LocalDateTime currentTime = LocalDateTime.now();

        while (current != null) {
            nodes.add(current);
            current.setArrivalTime(currentTime);
            currentTime = currentTime.plusSeconds(1);
            current = cameFrom.get(current);
        }

        Collections.reverse(nodes);
        Path path = new Path();
        for (Node node : nodes) {
            path.addNode(node, 0.0);
        }
        return path;
    }

    private double heuristicCostEstimate(Node start, Node goal) {
        return Math.sqrt(Math.pow(start.getX() - goal.getX(), 2) + Math.pow(start.getY() - goal.getY(), 2));
    }
}
