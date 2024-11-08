package com.enterprise.common.models;

import java.time.LocalDateTime;

public class ConflictInfo {
    public final Node node;
    public final LocalDateTime existingDepartureTime;
    public int retryCount;
    public long totalDelay;
    
    public ConflictInfo(Node node, LocalDateTime existingDepartureTime) {
        this.node = node;
        this.existingDepartureTime = existingDepartureTime;
        this.retryCount = 0;
        this.totalDelay = 0;
    }
}
