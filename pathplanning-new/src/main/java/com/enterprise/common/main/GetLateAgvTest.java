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
        
        // 添加几个测试用的 AGV
        AgvNodeVo agv1 = new AgvNodeVo(1L, "696", null);  // id, curNodeId, lastNodeId
        AgvNodeVo agv2 = new AgvNodeVo(2L, null, "474");
        AgvNodeVo agv3 = new AgvNodeVo(3L, "056", null);
        
        testAgvs.add(agv1);
        testAgvs.add(agv2);
        testAgvs.add(agv3);
        
        // 3. 测试获取最近 AGV
        getLateAgv finder = new getLateAgv();
        String targetNode = "696";  // 目标节点
        
        System.out.println("开始测试获取最近AGV...");
        System.out.println("目标节点: " + targetNode);
        System.out.println("可用AGV数量: " + testAgvs.size());
        
        LateAgvResult result = finder.getLateAgv(targetNode, testAgvs);
        
        if (result != null) {
            System.out.println("\n测试结果:");
            System.out.println("最近的AGV ID: " + result.getAgvId());
            System.out.println("预计到达时间: " + result.getDelayTime() + "秒");
        } else {
            System.out.println("\n未找到合适的AGV");
        }
    }
}