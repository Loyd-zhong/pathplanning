package com.enterprise.common.models;

public class AgvNodeVo {
    private Long id;
    private String curNodeId;
    private String lastNodeId;

    // 构造函数
    public AgvNodeVo(Long id, String curNodeId, String lastNodeId) {
        this.id = id;
        this.curNodeId = curNodeId;
        this.lastNodeId = lastNodeId;
    }

    // Getter 和 Setter 方法

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCurNodeId() {
        return curNodeId;
    }

    public void setCurNodeId(String curNodeId) {
        this.curNodeId = curNodeId;
    }

    public String getLastNodeId() {
        return lastNodeId;
    }

    public void setLastNodeId(String lastNodeId) {
        this.lastNodeId = lastNodeId;
    }
}

