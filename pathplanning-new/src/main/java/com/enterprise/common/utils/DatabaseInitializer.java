package com.enterprise.common.utils;

import com.enterprise.common.models.Graph;
import com.enterprise.common.models.Node;
import com.enterprise.common.models.Edge;
import com.enterprise.common.models.AGV;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashSet;
import java.util.Set;

public class DatabaseInitializer {
    public static void initializeDatabase(Connection conn, Graph graph) throws SQLException {
        createTables(conn);
        initializeNodes(conn, graph);
        initializeEdges(conn, graph);
    }

    public static void createTables(Connection conn) throws SQLException {
        String createNodesTable = 
            "CREATE TABLE IF NOT EXISTS Nodes (" +
            "    node_id VARCHAR(100) PRIMARY KEY," +
            "    x_coordinate DOUBLE NOT NULL," +
            "    y_coordinate DOUBLE NOT NULL," +
            "    capacity INT DEFAULT 3" +
            ")";

        String createEdgesTable = 
            "CREATE TABLE IF NOT EXISTS Edges (" +
            "    edge_id VARCHAR(255) PRIMARY KEY," +
            "    from_node_id VARCHAR(50) NOT NULL," +
            "    to_node_id VARCHAR(50) NOT NULL," +
            "    distance DOUBLE NOT NULL," +
            "    capacity INT DEFAULT 3," +
            "    empty_vehicle_speed DOUBLE DEFAULT " +AGV.Defaultspeed +"," +
            "    back_empty_shelf_speed DOUBLE DEFAULT " +AGV.Defaultspeed +"," +
            "    back_to_back_rack_speed DOUBLE DEFAULT " +AGV.Defaultspeed +"," +
            "    backfill_shelf_speed DOUBLE DEFAULT " +AGV.Defaultspeed +"," +
            "    displaced INT DEFAULT 0," +
            "    FOREIGN KEY (from_node_id) REFERENCES Nodes(node_id)," +
            "    FOREIGN KEY (to_node_id) REFERENCES Nodes(node_id)," +
            "    UNIQUE KEY unique_edge (from_node_id, to_node_id)" +
            ")";

        String createVehiclePassagesTable = 
            "CREATE TABLE IF NOT EXISTS VehiclePassages (" +
            "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
            "    vehicle_id VARCHAR(255) NOT NULL," +
            "    node_id VARCHAR(255) NULL," +
            "    edge_id VARCHAR(255) NULL," +
            "    arrival_time DATETIME NOT NULL," +
            "    departure_time DATETIME NOT NULL," +
            "    planning_batch DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "    FOREIGN KEY (node_id) REFERENCES Nodes(node_id)," +
            "    FOREIGN KEY (edge_id) REFERENCES Edges(edge_id)," +
            "    INDEX idx_node_time (node_id, arrival_time, departure_time)," +
            "    INDEX idx_edge_time (edge_id, arrival_time, departure_time)," +
            "    INDEX idx_planning_batch (planning_batch)" +
            ")";

        String createPathsTable = 
            "CREATE TABLE IF NOT EXISTS paths (" +
            "    path_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
            "    start_node_id VARCHAR(255)," +
            "    end_node_id VARCHAR(255)," +
            "    agv_type VARCHAR(20)," +
            "    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
            ")";

        String createPathNodesTable = 
            "CREATE TABLE IF NOT EXISTS path_nodes (" +
            "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
            "    path_id BIGINT," +
            "    node_id VARCHAR(255)," +
            "    node_x DOUBLE," +
            "    node_y DOUBLE," +
            "    estimated_arrival_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "    FOREIGN KEY (path_id) REFERENCES paths(path_id)" +
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
        try (PreparedStatement stmt = conn.prepareStatement(createPathsTable)) {
            stmt.execute();
        }
        try (PreparedStatement stmt = conn.prepareStatement(createPathNodesTable)) {
            stmt.execute();
        }
    }

    private static void initializeNodes(Connection conn, Graph graph) throws SQLException {
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
        String sql = "INSERT INTO Edges (edge_id, from_node_id, to_node_id, distance, " +
        "empty_vehicle_speed, back_empty_shelf_speed, back_to_back_rack_speed, backfill_shelf_speed) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        // 用于跟踪已插入的边
        Set<String> insertedEdges = new HashSet<>();
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            // 遍历所有节点
            for (Node node : graph.getNodes()) {
                Set<Edge> edges = graph.getEdges(node);
                
                for (Edge edge : edges) {
                    // 定义正向和反向边的ID
                    String forwardEdgeId = edge.getFrom().getId() + "_" + edge.getTo().getId();


                    // 检查是否已经插入了正向或反向边
                    if (!insertedEdges.contains(forwardEdgeId)) {
                        // 添加调试信息
                        System.out.println("准备插入边: " + forwardEdgeId);
                        System.out.println("速度值: " +
                            "\nemptyVehicleSpeed=" + edge.emptyVehicleSpeed +
                            "\nbackEmptyShelfSpeed=" + edge.backEmptyShelfSpeed +
                            "\nbackToBackRackSpeed=" + edge.backToBackRackSpeed +
                            "\nbackfillShelfSpeed=" + edge.backfillShelfSpeed);

                        stmt.setString(1, forwardEdgeId);
                        stmt.setString(2, edge.getFrom().getId());
                        stmt.setString(3, edge.getTo().getId());
                        stmt.setDouble(4, edge.getLength());
                        stmt.setDouble(5, edge.emptyVehicleSpeed);
                        stmt.setDouble(6, edge.backEmptyShelfSpeed);
                        stmt.setDouble(7, edge.backToBackRackSpeed);
                        stmt.setDouble(8, edge.backfillShelfSpeed);

                        try {
                            stmt.executeUpdate();
                            //System.out.println("成功插入边: " + forwardEdgeId);
                            int rowsAffected = stmt.executeUpdate();
                            System.out.println("插入成功，影响行数: " + rowsAffected);
                        } catch (SQLIntegrityConstraintViolationException e) {
                            //System.out.println("跳过重复边: " + forwardEdgeId);
                        }
                        // 将正向边添加到集合中
                        insertedEdges.add(forwardEdgeId);
                    }

                    // 处理反向边
                    String reverseEdgeId = edge.getTo().getId() + "_" + edge.getFrom().getId();
                    if (!insertedEdges.contains(reverseEdgeId)) {
                        System.out.println("准备插入反向边: " + reverseEdgeId);
                        System.out.println("速度值: " +
                            "\nemptyVehicleSpeed=" + edge.emptyVehicleSpeed +
                            "\nbackEmptyShelfSpeed=" + edge.backEmptyShelfSpeed +
                            "\nbackToBackRackSpeed=" + edge.backToBackRackSpeed +
                            "\nbackfillShelfSpeed=" + edge.backfillShelfSpeed);

                        stmt.setString(1, reverseEdgeId);
                        stmt.setString(2, edge.getTo().getId());
                        stmt.setString(3, edge.getFrom().getId());
                        stmt.setDouble(4, edge.getLength());
                        stmt.setDouble(5, edge.emptyVehicleSpeed);
                        stmt.setDouble(6, edge.backEmptyShelfSpeed);
                        stmt.setDouble(7, edge.backToBackRackSpeed);
                        stmt.setDouble(8, edge.backfillShelfSpeed);

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
    }
}
