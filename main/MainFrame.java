package pathfinding.main;
import pathfinding.utils.MapLoader;
import javax.swing.*;
import pathfinding.models.Graph;
import pathfinding.models.NetworkState;
import pathfinding.ui.TaskManagerPanel;
import java.awt.*;
import java.util.List;
import pathfinding.models.AGV;
import pathfinding.models.NetworkState;
public class MainFrame extends JFrame {

    public MainFrame() {
        setTitle("AGV Pathfinding System");
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // 加载地图
        String xmlFilePath = "D:/AGV规划系统（4.25启动）/甲方文件/尝试版本.xml"; // 请确保路径正确
        Graph graph = MapLoader.loadMap(xmlFilePath);

        // 创建 PathVisualizer 和 TaskManagerPanel
        PathVisualizer visualizer = new PathVisualizer();
        List<AGV> agvs = visualizer.getAGVs();

        // 使用 JTabbedPane 来管理不同的视图
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Path Visualization", visualizer);
        NetworkState networkState = new NetworkState();
        tabbedPane.addTab("Task Manager", new TaskManagerPanel(agvs, networkState));

        // 将 tabbedPane 添加到框架中
        add(tabbedPane, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);

        });
        NetworkState networkState = new NetworkState();
        networkState.printNetworkState();
    }
}
