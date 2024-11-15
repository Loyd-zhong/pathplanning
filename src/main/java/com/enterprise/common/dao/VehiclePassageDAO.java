package com.enterprise.common.dao;

import com.enterprise.common.models.Node;
import com.enterprise.common.models.Edge;
import com.enterprise.common.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;

public class VehiclePassageDAO {
    public void recordPassage(String vehicleId, String nodeId, String edgeId, 
                               LocalDateTime arrivalTime, LocalDateTime departureTime) {
        String sql = "INSERT INTO vehiclepassages (vehicle_id, node_id, edge_id, arrival_time, departure_time) " +
                    "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, vehicleId);
            stmt.setString(2, nodeId);
            stmt.setString(3, edgeId);
            stmt.setTimestamp(4, java.sql.Timestamp.valueOf(arrivalTime));
            stmt.setTimestamp(5, java.sql.Timestamp.valueOf(departureTime));
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
