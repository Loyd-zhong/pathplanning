package com.enterprise.common.main;

import com.enterprise.common.main.getLateAgv;
import com.enterprise.common.models.*;
import com.enterprise.common.utils.MapLoader;
import java.util.ArrayList;
import java.util.List;
import com.enterprise.common.models.AgvNodeVo;
public class GetLateAgvTest {
    public static void main(String[] args) {
        // 1. 初始化 Graph
        String xmlFilePath = "D:/AGV规划系统（4.25启动）/路径规划算法代码/pathplanning-good/pathplanning-new/src/main/java/com/enterprise/common/resources/新建文本文档 (4).xml";  // 替换为实际的地图文件路径
        Graph graph = MapLoader.loadMap(xmlFilePath);
        getLateAgv.setGraph(graph);
        
        // 2. 创建测试用的 AGV 列表
        List<AgvNodeVo> testAgvs = new ArrayList<>();
        
        // 添加20个测试用的 AGV，分布在不同的节点
        testAgvs.add(new AgvNodeVo(1L, "696", null));
        //testAgvs.add(new AgvNodeVo(2L, "474", null));
        testAgvs.add(new AgvNodeVo(3L, "056", null));
        //testAgvs.add(new AgvNodeVo(4L, "826", null));
        //testAgvs.add(new AgvNodeVo(5L, "532", null));
        testAgvs.add(new AgvNodeVo(6L, "708", null));
        testAgvs.add(new AgvNodeVo(7L, "590", null));
        testAgvs.add(new AgvNodeVo(8L, "236", null));
        testAgvs.add(new AgvNodeVo(9L, "354", null));
        testAgvs.add(new AgvNodeVo(10L, "118", null));
        testAgvs.add(new AgvNodeVo(11L, "944", null));
        testAgvs.add(new AgvNodeVo(12L, "472", null));
        testAgvs.add(new AgvNodeVo(13L, "590", null));
        testAgvs.add(new AgvNodeVo(14L, "708", null));
        //testAgvs.add(new AgvNodeVo(15L, "826", null));
        testAgvs.add(new AgvNodeVo(16L, "944", null));
        testAgvs.add(new AgvNodeVo(17L, "118", null));
        testAgvs.add(new AgvNodeVo(18L, "236", null));
        testAgvs.add(new AgvNodeVo(19L, "354", null));
        testAgvs.add(new AgvNodeVo(20L, "472", null));
        
        // 3. 测试获取最近 AGV
        getLateAgv finder = new getLateAgv();
        String targetNode = "532";  // 目标节点
        
        System.out.println("开始测试获取最近AGV...");
        System.out.println("目标节点: " + targetNode);
        System.out.println("可用AGV数量: " + testAgvs.size());
        
        LateAgvResult result = finder.getLateAgv(targetNode, testAgvs);
        
        if (result != null) {
            System.out.println("\n测试结果:");
            System.out.println("最近的AGV ID: " + result.getAgvId());
            System.out.println("预计到达时间: " + result.getDelayTime() + "秒");
            
            // 打印选中的AGV的起始位置
            AgvNodeVo selectedAgv = testAgvs.stream()
                .filter(agv -> agv.getId().equals(result.getAgvId()))
                .findFirst()
                .orElse(null);
                
            if (selectedAgv != null) {
                System.out.println("选中AGV的起始节点: " + 
                    (selectedAgv.getCurNodeId() != null ? selectedAgv.getCurNodeId() : selectedAgv.getLastNodeId()));
            }
        } else {
            System.out.println("\n未找到合适的AGV");
        }
    }
}