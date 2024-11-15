package com.enterprise.common.dao;

import com.enterprise.common.utils.DatabaseConnection;
import java.sql.*;
import java.time.LocalDateTime;

public class VehiclePassageDAO {
    public void recordPassage(String vehicleId, String nodeId, String edgeId, LocalDateTime arrivalTime, LocalDateTime departureTime) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO vehiclepassages (vehicle_id, node_id, edge_id, arrival_time, departure_time) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, vehicleId);
                stmt.setString(2, nodeId);
                stmt.setString(3, edgeId);
                stmt.setTimestamp(4, Timestamp.valueOf(arrivalTime));
                stmt.setTimestamp(5, Timestamp.valueOf(departureTime));
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
