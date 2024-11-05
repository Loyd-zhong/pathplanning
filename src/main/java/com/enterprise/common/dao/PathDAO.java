package com.enterprise.common.dao;

import com.enterprise.common.models.Path;
import com.enterprise.common.models.Node;
import com.enterprise.common.utils.DatabaseConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

public class PathDAO {
    public void savePath(Path path, String agvType) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // 保存路径信息
            String pathSql = "INSERT INTO paths (start_node_id, end_node_id, agv_type) VALUES (?, ?, ?)";
            PreparedStatement pathStmt = conn.prepareStatement(pathSql, Statement.RETURN_GENERATED_KEYS);
            
            List<Node> nodes = path.getNodes();
            pathStmt.setString(1, nodes.get(0).getId());
            pathStmt.setString(2, nodes.get(nodes.size()-1).getId());
            pathStmt.setString(3, agvType);
            pathStmt.executeUpdate();
            
            // 获取生成的路径ID
            ResultSet rs = pathStmt.getGeneratedKeys();
            long pathId = rs.next() ? rs.getLong(1) : 0;
            
            // 保存节点信息
            String nodeSql = "INSERT INTO path_nodes (path_id, node_id, node_x, node_y, estimated_arrival_time) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement nodeStmt = conn.prepareStatement(nodeSql);
            
            for (Node node : nodes) {
                nodeStmt.setLong(1, pathId);
                nodeStmt.setString(2, node.getId());
                nodeStmt.setDouble(3, node.getX());
                nodeStmt.setDouble(4, node.getY());
                nodeStmt.setTimestamp(5, Timestamp.valueOf(node.getArrivalTime()));
                nodeStmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
