package com.enterprise.common.algorithms;


import com.enterprise.common.models.Node;
import com.enterprise.common.models.Path;

import java.util.HashMap;
import java.util.Map;

public class TimeWindowManager {
    private Map<Node, Integer> nodeOccupationTimes = new HashMap<>();

    public boolean isAvailable(Node node, int time) {
        return nodeOccupationTimes.getOrDefault(node, -1) < time;
    }

    public void reserve(Node node, int time) {
        nodeOccupationTimes.put(node, time);
    }

    public void applyTimeWindows(Path path) {
        int currentTime = 0;
        for (Node node : path.getNodes()) {
            while (!isAvailable(node, currentTime)) {

                currentTime++;
            }
            reserve(node, currentTime);
            currentTime++;
        }
    }

    public boolean isPathAvailable(Path path) {
        for (Node node : path.getNodes()) {
            if (!isAvailable(node, node.getArrivalTime().getSecond())) {
                return false;
            }
        }
        return true;
    }
}
