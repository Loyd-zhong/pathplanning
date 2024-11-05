package com.enterprise.common.dao;

import com.enterprise.common.models.Node;
import com.enterprise.common.models.Edge;
import com.enterprise.common.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;

public class VehiclePassageDAO {
    public void recordNodePassage(String vehicleId, Node node, LocalDateTime arrivalTime, LocalDateTime departureTime) {
        String sql = "INSERT INTO VehiclePassages (vehicle_id, node_id, arrival_time, departure_time) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, vehicleId);
            stmt.setString(2, node.getId());
            stmt.setTimestamp(3, java.sql.Timestamp.valueOf(arrivalTime));
            stmt.setTimestamp(4, java.sql.Timestamp.valueOf(departureTime));
            int result = stmt.executeUpdate();
            System.out.println("插入VehiclePassage记录结果: " + result + ", vehicleId=" + vehicleId + ", nodeId=" + node.getId());
        } catch (Exception e) {
            System.err.println("记录节点通过失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void recordEdgePassage(String vehicleId, Edge edge, LocalDateTime arrivalTime, LocalDateTime departureTime) {
        String sql = "INSERT INTO VehiclePassages (vehicle_id, edge_id, arrival_time, departure_time) VALUES (?, ?, ?, ?)";
        String edgeId = edge.getFrom().getId() + "_" + edge.getTo().getId();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, vehicleId);
            stmt.setString(2, edgeId);
            stmt.setTimestamp(3, java.sql.Timestamp.valueOf(arrivalTime));
            stmt.setTimestamp(4, java.sql.Timestamp.valueOf(departureTime));
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
