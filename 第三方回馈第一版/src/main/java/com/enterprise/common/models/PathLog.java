package com.enterprise.common.models;

import java.time.LocalDateTime;
import java.util.List;

public class PathLog {
    private String agvId;
    private List<Node> nodes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public PathLog(String agvId, List<Node> nodes, LocalDateTime startTime, LocalDateTime endTime) {
        this.agvId = agvId;
        this.nodes = nodes;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public boolean containsNode(Node node) {
        return nodes.stream().anyMatch(n -> n.getId().equals(node.getId()));
    }

    public boolean isTimeOverlapping(LocalDateTime time) {
        return !time.isBefore(startTime) && !time.isAfter(endTime);
    }

    // Getters
    public String getAgvId() { return agvId; }
    public List<Node> getNodes() { return nodes; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
}
