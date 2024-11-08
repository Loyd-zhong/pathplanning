package com.enterprise.common.models;

public enum PathResolutionStatus {
    SUCCESS("路径规划成功"),
    FAILED_TOO_MANY_CONFLICTS("冲突节点数量过多"),
    FAILED_MAX_RETRIES("超过最大重试次数"),
    FAILED_NO_ALTERNATIVE("无可用替代路径"),
    FAILED_DATABASE_ERROR("数据库操作失败");
    
    private final String description;
    
    PathResolutionStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
