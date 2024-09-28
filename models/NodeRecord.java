// src/pathfinding/models/NodeRecord.java
package pathfinding.models;

public class NodeRecord {
    private final Node node;
    private NodeRecord parent;
    private double costSoFar;
    private double estimatedTotalCost;

    public NodeRecord(Node node, NodeRecord parent, double costSoFar, double estimatedTotalCost) {
        this.node = node;
        this.parent = parent;
        this.costSoFar = costSoFar;
        this.estimatedTotalCost = estimatedTotalCost;
    }

    public Node getNode() {
        return node;
    }

    public NodeRecord getParent() {
        return parent;
    }

    public void setParent(NodeRecord parent) {
        this.parent = parent;
    }

    public double getCostSoFar() {
        return costSoFar;
    }

    public void setCostSoFar(double costSoFar) {
        this.costSoFar = costSoFar;
    }

    public double getEstimatedTotalCost() {
        return estimatedTotalCost;
    }

    public void setEstimatedTotalCost(double estimatedTotalCost) {
        this.estimatedTotalCost = estimatedTotalCost;
    }
}
