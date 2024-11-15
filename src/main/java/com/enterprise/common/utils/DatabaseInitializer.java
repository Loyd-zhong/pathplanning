package com.enterprise.common.utils;

import com.enterprise.common.models.Graph;
import com.enterprise.common.models.Node;
import com.enterprise.common.models.Edge;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashSet;
import java.util.Set;

public class DatabaseInitializer {
    public static void initializeDatabase(Connection conn, Graph graph) throws SQLException {
        createTables(conn);
    }

    public static void createTables(Connection conn) throws SQLException {
        String createNodesTable = 
            "CREATE TABLE IF NOT EXISTS Nodes (" +
            "    node_id VARCHAR(50) PRIMARY KEY," +
            "    x_coordinate DOUBLE NOT NULL," +
            "    y_coordinate DOUBLE NOT NULL," +
            "    capacity INT DEFAULT 3" +
            ")";

        String createEdgesTable = 
            "CREATE TABLE IF NOT EXISTS Edges (" +
            "    edge_id VARCHAR(100) PRIMARY KEY," +
            "    from_node_id VARCHAR(50) NOT NULL," +
            "    to_node_id VARCHAR(50) NOT NULL," +
            "    distance DOUBLE NOT NULL," +
            "    capacity INT DEFAULT 3," +
            "    FOREIGN KEY (from_node_id) REFERENCES Nodes(node_id)," +
            "    FOREIGN KEY (to_node_id) REFERENCES Nodes(node_id)," +
            "    UNIQUE KEY unique_edge (from_node_id, to_node_id)" +
            ")";

        String createVehiclePassagesTable = 
            "CREATE TABLE IF NOT EXISTS VehiclePassages (" +
            "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
            "    vehicle_id VARCHAR(50) NOT NULL," +
            "    node_id VARCHAR(50) NULL," +
            "    edge_id VARCHAR(100) NULL," +
            "    arrival_time DATETIME NOT NULL," +
            "    departure_time DATETIME NOT NULL," +
            "    planning_batch DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "    FOREIGN KEY (node_id) REFERENCES Nodes(node_id)," +
            "    FOREIGN KEY (edge_id) REFERENCES Edges(edge_id)," +
            "    INDEX idx_node_time (node_id, arrival_time, departure_time)," +
            "    INDEX idx_edge_time (edge_id, arrival_time, departure_time)," +
            "    INDEX idx_planning_batch (planning_batch)" +
            ")";

        try (PreparedStatement stmt = conn.prepareStatement(createNodesTable)) {
            stmt.execute();
        }
        try (PreparedStatement stmt = conn.prepareStatement(createEdgesTable)) {
            stmt.execute();
        }
        try (PreparedStatement stmt = conn.prepareStatement(createVehiclePassagesTable)) {
            stmt.execute();
        }
    }

    /*private static void initializeNodes(Connection conn, Graph graph) throws SQLException {
        // 先清空现有数据
        String deleteSql = "DELETE FROM Nodes";
        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
            deleteStmt.execute();
        }
        
        // 然后插入新数据
        String insertSql = "INSERT INTO Nodes (node_id, x_coordinate, y_coordinate) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            for (Node node : graph.getNodes()) {
                stmt.setString(1, node.getId());
                stmt.setDouble(2, node.getX());
                stmt.setDouble(3, node.getY());
                try {
                    stmt.executeUpdate();
                } catch (SQLIntegrityConstraintViolationException e) {
                    System.out.println("跳过重复节点: " + node.getId());
                }
            }
        }
    }

    public static void initializeEdges(Connection conn, Graph graph) throws SQLException {
        // 先清空现有数据
        String deleteSql = "DELETE FROM Edges";
        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
            deleteStmt.execute();
        }

        // 准备插入语句
        String sql = "INSERT INTO Edges (edge_id, from_node_id, to_node_id, distance) VALUES (?, ?, ?, ?)";
        
        // 用于跟踪已插入的边
        Set<String> insertedEdges = new HashSet<>();
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            // 遍历所有节点
            for (Node node : graph.getNodes()) {
                Set<Edge> edges = graph.getEdges(node);
                
                for (Edge edge : edges) {
                    // 定义正向和反向边的ID
                    String forwardEdgeId = edge.getFrom().getId() + "_" + edge.getTo().getId();
                    String reverseEdgeId = edge.getTo().getId() + "_" + edge.getFrom().getId();

                    // 检查是否已经插入了正向或反向边
                    if (!insertedEdges.contains(forwardEdgeId)) {
                        // 插入正向边
                        stmt.setString(1, forwardEdgeId);
                        stmt.setString(2, edge.getFrom().getId());
                        stmt.setString(3, edge.getTo().getId());
                        stmt.setDouble(4, edge.getLength());
                        try {
                            stmt.executeUpdate();
                            //System.out.println("成功插入边: " + forwardEdgeId);
                        } catch (SQLIntegrityConstraintViolationException e) {
                            //System.out.println("跳过重复边: " + forwardEdgeId);
                        }
                        // 将正向边添加到集合中
                        insertedEdges.add(forwardEdgeId);
                    }

                    // 对于反向边，检查是否已经插入
                    if (!insertedEdges.contains(reverseEdgeId)) {
                        // 插入反向边
                        stmt.setString(1, reverseEdgeId);
                        stmt.setString(2, edge.getTo().getId());
                        stmt.setString(3, edge.getFrom().getId());
                        stmt.setDouble(4, edge.getLength());
                        try {
                            stmt.executeUpdate();
                            //System.out.println("成功插入边: " + reverseEdgeId);
                        } catch (SQLIntegrityConstraintViolationException e) {
                            //System.out.println("跳过重复边: " + reverseEdgeId);
                        }
                        // 将反向边添加到集合中
                        insertedEdges.add(reverseEdgeId);
                    }
                }
            }
        }
    }*/
}
