package com.enterprise.common.utils;

import javax.swing.Timer;
import java.sql.*;
import java.time.LocalDateTime;
import com.enterprise.common.utils.DatabaseConnection;
public class DatabaseCleanupManager {
    private static final int CLEANUP_INTERVAL = 5000; // 清理间隔（毫秒）
    private Timer cleanupTimer;
    private static DatabaseCleanupManager instance;

    private DatabaseCleanupManager() {
        // 私有构造函数
    }

    public static DatabaseCleanupManager getInstance() {
        if (instance == null) {
            instance = new DatabaseCleanupManager();
        }
        return instance;
    }

    public void startPeriodicCleanup() {
        if (cleanupTimer != null) {
            cleanupTimer.stop();
        }
        
        cleanupTimer = new Timer(CLEANUP_INTERVAL, e -> {
            try {
                cleanExpiredRecords();
            } catch (Exception ex) {
                System.err.println("定时清理过期记录失败: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
        
        cleanupTimer.start();
        System.out.println("已启动数据库定时清理任务，间隔: " + CLEANUP_INTERVAL/1000 + "秒");
    }

    public void stopPeriodicCleanup() {
        if (cleanupTimer != null) {
            cleanupTimer.stop();
            cleanupTimer = null;
            System.out.println("已停止数据库定时清理任务");
        }
    }

    private void cleanExpiredRecords() throws SQLException {
        String sql = "DELETE FROM vehiclepassages WHERE departure_time < ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            stmt.setTimestamp(1, Timestamp.valueOf(now));
            int deletedCount = stmt.executeUpdate();
            if (deletedCount > 0) {
                System.out.println(now + " - 已清理 " + deletedCount + " 条过期记录");
            }
        } catch (Exception e) {
            System.err.println("清理过期记录失败: " + e.getMessage());
            throw new SQLException(e);
        }
    }
}
