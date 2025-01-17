package com.enterprise.common.dao;

import com.enterprise.common.models.Edge;
import com.enterprise.common.models.Node;
import com.enterprise.common.utils.DatabaseConnection;
import java.sql.*;

public class EdgeDAO {
    public Edge getEdge(String fromNodeId, String toNodeId) {
        String edgeId = fromNodeId + "_" + toNodeId;
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT e.*, n1.x_coordinate as from_x, n1.y_coordinate as from_y, " +
                        "n2.x_coordinate as to_x, n2.y_coordinate as to_y " +
                        "FROM edges e " +
                        "JOIN nodes n1 ON e.from_node_id = n1.node_id " +
                        "JOIN nodes n2 ON e.to_node_id = n2.node_id " +
                        "WHERE e.edge_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, edgeId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    Node fromNode = new Node(rs.getDouble("from_x"), rs.getDouble("from_y"), rs.getString("from_node_id"));
                    Node toNode = new Node(rs.getDouble("to_x"), rs.getDouble("to_y"), rs.getString("to_node_id"));
                    Edge edge = new Edge(fromNode, toNode, true, false, rs.getDouble("distance"));
                    edge.setId(edgeId);
                    edge.emptyVehicleSpeed = rs.getDouble("empty_vehicle_speed");
                    edge.backEmptyShelfSpeed = rs.getDouble("back_empty_shelf_speed");
                    edge.backToBackRackSpeed = rs.getDouble("back_to_back_rack_speed");
                    edge.backfillShelfSpeed = rs.getDouble("backfill_shelf_speed");
                    edge.setDisplaced(rs.getInt("displaced"));
                    return edge;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateDisplaced(String fromNodeId, String toNodeId, int displaced) {
        String edgeId = fromNodeId + "_" + toNodeId;
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE edges SET displaced = ? WHERE edge_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, displaced);
                stmt.setString(2, edgeId);
                return stmt.executeUpdate() > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
} 