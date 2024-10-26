// src/pathfinding/utils/Heuristic.java
package com.enterprise.common.utils;


import com.enterprise.common.models.Node;

public class Heuristic {
    public static double calculate(Node current, Node goal) {
        // 使用曼哈顿距离作为启发式函数
        return Math.abs(current.getX() - goal.getX()) + Math.abs(current.getY() - goal.getY());
    }
}
