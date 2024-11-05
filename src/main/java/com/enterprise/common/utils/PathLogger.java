package com.enterprise.common.utils;

import com.enterprise.common.models.Node;
import com.enterprise.common.models.Path;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PathLogger {
    private static final String LOG_FILE_PATH = "logs/path_log.txt";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void logPath(Path path) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE_PATH, true))) {
            writer.println("=== 新的路径规划记录 ===");
            writer.println("时间：" + LocalDateTime.now().format(formatter));

            List<Node> nodes = path.getNodes();
            writer.println("规划路径：" + String.join(" -> ", nodes.stream().map(Node::getId).toArray(String[]::new)));
            writer.println();

            writer.println("节点和路径状态：");
            for (int i = 0; i < nodes.size(); i++) {
                Node currentNode = nodes.get(i);
                writer.println("节点 " + currentNode.getId() + "：进入时间 - " + 
                               currentNode.getArrivalTime().format(formatter) + 
                               "，离开时间 - " + currentNode.getArrivalTime().plusSeconds(1).format(formatter));

                if (i < nodes.size() - 1) {
                    Node nextNode = nodes.get(i + 1);
                    writer.println("路径 " + currentNode.getId() + " -> " + nextNode.getId() + 
                                   "：进入时间 - " + currentNode.getArrivalTime().plusSeconds(1).format(formatter) + 
                                   "，离开时间 - " + nextNode.getArrivalTime().format(formatter));
                }
            }
            writer.println();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


