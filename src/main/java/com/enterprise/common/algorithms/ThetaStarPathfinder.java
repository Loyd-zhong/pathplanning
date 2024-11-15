// src/pathfinding/algorithms/ThetaStarPathfinder.java
package com.enterprise.common.algorithms;


import com.enterprise.common.models.Edge;
import com.enterprise.common.models.Graph;
import com.enterprise.common.models.Node;
import com.enterprise.common.models.Path;

import java.time.LocalDateTime;
import java.util.*;

public class ThetaStarPathfinder {

    private Set<Node> closedSet = new HashSet<>();
    private PriorityQueue<Node> openSet;
    private Map<Node, Node> cameFrom = new HashMap<>();
    private Map<Node, Double> gScore = new HashMap<>();
    private Map<Node, Double> fScore = new HashMap<>();

    public ThetaStarPathfinder() {
        openSet = new PriorityQueue<>(Comparator.comparingDouble(node -> fScore.getOrDefault(node, Double.POSITIVE_INFINITY)));
    }

    public Path findPath(Graph graph, Node start, Node goal) {
        gScore.put(start, 0.0);
        fScore.put(start, heuristicCostEstimate(start, goal));
        openSet.add(start);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            // 如果到达目标节点，重建并返回路径
            if (current.equals(goal)) {
                return reconstructPath(current);
            }

            closedSet.add(current);

            // 获取所有连接当前节点的边，确保使用图的拓扑结构
            for (Edge edge : graph.getEdges(current)) {
                Node neighbor = edge.getOpposite(current); // 获取相邻节点
                if (closedSet.contains(neighbor)) continue; // 如果相邻节点已评估，跳过

                Node parent = cameFrom.getOrDefault(current, current);

                // 正确计算从父节点到相邻节点的路径成本
                double tentativeGScore = gScore.get(parent) + edge.getLength();

                // 如果新的路径成本更低，则更新路径信息
                if (tentativeGScore < gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    cameFrom.put(neighbor, current); // 更新路径来源为当前节点
                    gScore.put(neighbor, tentativeGScore); // 更新实际路径成本
                    fScore.put(neighbor, tentativeGScore + heuristicCostEstimate(neighbor, goal)); // 更新启发式总成本
                    openSet.add(neighbor); // 将相邻节点加入待评估队列
                }
            }
        }

        return null; // 如果没有找到路径，返回 null
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
