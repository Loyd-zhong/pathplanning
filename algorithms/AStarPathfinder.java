// src/pathfinding/algorithms/AStarPathfinder.java
package pathfinding.algorithms;

import pathfinding.models.Graph;
import pathfinding.models.Node;
import pathfinding.models.Path;
import pathfinding.models.Edge;

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
