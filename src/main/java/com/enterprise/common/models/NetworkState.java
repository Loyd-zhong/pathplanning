package com.enterprise.common.models;

import java.time.LocalDateTime;
import java.util.*;
import java.util.HashMap;
import java.util.Map;

public class NetworkState {
    private  Map<String, PassRecord> nodePassRecords = new HashMap<>(); // 存储节点的通过记录
    private  Map<String, PassRecord> edgePassRecords = new HashMap<>(); // 存储边的通过记录

    // 记录节点通过情况
    // 在 NetworkState 的记录方法中添加输出，确认数据被记录
    public void recordNodePass(Node node) {
        String nodeId = "Node-" + node.getX() + "," + node.getY();  // 唯一标识符
        PassRecord record = nodePassRecords.getOrDefault(nodeId, new PassRecord(nodeId));
        record.incrementCount();
        record.setLastPassTime(LocalDateTime.now());
        nodePassRecords.put(nodeId, record);  // 更新记录
        System.out.println("Recorded node pass: " + record); // 调试输出
    }






    // 记录边通过情况
    public void recordEdgePass(Edge edge) {
        String edgeId = "Edge-" + edge.getFrom().getId() + "_" + edge.getTo().getId();
        PassRecord record = edgePassRecords.getOrDefault(edgeId, new PassRecord(edgeId));
        record.incrementCount();
        record.setLastPassTime(LocalDateTime.now());
        edgePassRecords.put(edgeId, record);  // 更新记录
        System.out.println("Recorded edge pass: " + record); // 调试输出
    }


    // 获取节点通过记录
    public Map<String, PassRecord> getNodePassRecords() {
        return nodePassRecords;
    }

    // 获取边通过记录
    public Map<String, PassRecord> getEdgePassRecords() {
        return edgePassRecords;
    }

    // 获取格式化后的记录数据，用于显示在表格中
    public List<Object[]> getFormattedRecords() {
        List<Object[]> records = new ArrayList<>();

        // 添加节点记录
        for (Map.Entry<String, PassRecord> entry : nodePassRecords.entrySet()) {
            PassRecord record = entry.getValue();
            records.add(new Object[]{entry.getKey().toString(), record.getCount(), record.getLastPassTime()});
        }

        // 添加边记录
        for (Map.Entry<String, PassRecord> entry : edgePassRecords.entrySet()) {
            PassRecord record = entry.getValue();
            records.add(new Object[]{entry.getKey().toString(), record.getCount(), record.getLastPassTime()});
        }
        return records;
    }

    // 打印当前的路网状态（可选的调试功能）
    public void printNetworkState() {
        System.out.println("Node Pass Records:");
        nodePassRecords.forEach((node, record) -> System.out.println(node + " -> Count: " + record.getCount() + ", Last Pass: " + record.getLastPassTime()));

        System.out.println("Edge Pass Records:");
        edgePassRecords.forEach((edge, record) ->
                System.out.println(edge + " -> Count: " + record.getCount() + ", Last Pass: " + record.getLastPassTime())

        );
    }
}