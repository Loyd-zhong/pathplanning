// src/pathfinding/models/Node.java
package pathfinding.models;

import java.time.LocalDateTime;
import java.util.Objects;

public class Node {
    private double x, y;
    private LocalDateTime arrivalTime;

    public Node(double x, double y) {
        this.x = x;
        this.y = y;
        this.arrivalTime = null;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalDateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return x == node.x && y == node.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + (arrivalTime != null ? " at " + arrivalTime : "") + ")";
    }
}
