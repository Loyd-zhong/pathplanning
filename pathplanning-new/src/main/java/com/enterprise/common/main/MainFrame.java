 package com.enterprise.common.main;

import com.enterprise.common.models.AGV;
import com.enterprise.common.models.Graph;
import com.enterprise.common.models.NetworkState;
import com.enterprise.common.ui.TaskManagerPanel;
import com.enterprise.common.utils.MapLoader;
import com.enterprise.common.main.PathVisualizer;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import com.enterprise.common.algorithms.*;
import com.enterprise.common.utils.DatabaseCleanupManager;

public class MainFrame extends JFrame {
    private final NetworkState networkState;
    private final PathVisualizer visualizer;

    public MainFrame() {
        this.networkState = new NetworkState();

        setTitle("AGV Pathfinding System");
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // 加载地图
        String xmlFilePath = "pathplanning-new\\src\\main\\java\\com\\enterprise\\common\\resources\\海康冲突算法.xml";
        Graph graph = MapLoader.loadMap(xmlFilePath);

        // 创建 PathVisualizer 和 TaskManagerPanel
        visualizer = new PathVisualizer(networkState);
        List<AGV> agvs = visualizer.getAGVs();

        // 添加窗口关闭时的清理操作
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                DatabaseCleanupManager.getInstance().stopPeriodicCleanup();
            }
        });

        // 使用 JTabbedPane 来管理不同的视图
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Path Visualization", visualizer);
        tabbedPane.addTab("Task Manager", new com.enterprise.common.ui.TaskManagerPanel(agvs, networkState));

        // 将 tabbedPane 添加到框架中
        add(tabbedPane, BorderLayout.CENTER);
    }

    public NetworkState getNetworkState() {
        return networkState;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
            mainFrame.getNetworkState().printNetworkState();
        });
    }
}
