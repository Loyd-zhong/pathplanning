// src/pathfinding/models/Node.java
package com.enterprise.common.models;

import java.time.LocalDateTime;
import java.util.Objects;

public class Node implements Cloneable {
    private double x;
    private double y;
    private String id; // 新添加的 id 属性
    private LocalDateTime arrivalTime;
    private LocalDateTime departureTime;

    public Node(double x, double y, String id) {
        this.x = x;
        this.y = y;
        this.id = id;
        this.arrivalTime = LocalDateTime.now();
        this.departureTime = arrivalTime.plusSeconds(5);
    }

    public Node(double x, double y) {
        this.x = x;
        this.y = y;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalDateTime departureTime) {
        this.departureTime = departureTime;
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

    @Override
    public Node clone() {
        try {
            Node cloned = (Node) super.clone();
            // 深度复制时间对象
            if (this.arrivalTime != null) {
                cloned.arrivalTime = LocalDateTime.from(this.arrivalTime);
            }
            if (this.departureTime != null) {
                cloned.departureTime = LocalDateTime.from(this.departureTime);
            }
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("克隆Node对象失败", e);
        }
    }
}
