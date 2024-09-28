package pathfinding.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

import org.junit.jupiter.api.Test;
import pathfinding.models.AGV;
import pathfinding.models.NetworkState;
import pathfinding.models.PassRecord;


public class TaskManagerPanel extends JPanel {
    private JTextArea taskInfoArea;
    private JTable networkTable; // 用于显示NetworkState的数据表格
    private DefaultTableModel tableModel; // 表格模型
    private NetworkState networkState; // 引入NetworkState实例

    public TaskManagerPanel(List<AGV> agvs, NetworkState networkState) {
        this.networkState = networkState;
        setLayout(new BorderLayout());

        // 设置任务信息显示区域
        taskInfoArea = new JTextArea(10, 30);
        taskInfoArea.setEditable(false);
        updateTaskInfo(agvs);
        add(new JScrollPane(taskInfoArea), BorderLayout.NORTH);

        // 初始化表格模型
        tableModel = new DefaultTableModel(new Object[]{"节点或路段", "通过次数", "最近通过时间"}, 0);
        networkTable = new JTable(tableModel); // 使用表格模型创建JTable
        add(new JScrollPane(networkTable), BorderLayout.CENTER);

        // 设置定时刷新任务
        Timer timer = new Timer(1000, e -> updateNetworkTable());
        timer.start();
    }

    // 更新AGV任务信息

    public void updateTaskInfo(List<AGV> agvs) {
        StringBuilder info = new StringBuilder();
        for (int i = 0; i < agvs.size(); i++) {
            AGV agv = agvs.get(i);
            info.append("AGV ").append(i + 1).append(": Start -> ")
                    .append(agv.getCurrentPath().getNodes().get(0)).append(", Goal -> ")
                    .append(agv.getCurrentPath().getNodes().get(agv.getCurrentPath().getNodes().size() - 1)).append("\n");
            info.append("Path: ").append(agv.getCurrentPath().getNodes()).append("\n\n");
        }
        taskInfoArea.setText(info.toString());
    }

    // 更新NetworkState表格
    private void updateNetworkTable() {
        tableModel.setRowCount(0); // 清空现有数据
        // 更新节点数据
        for (PassRecord record : networkState.getNodePassRecords().values()) {
            tableModel.addRow(new Object[]{record.getIdentifier(), record.getCount(), record.getLastPassTime()});
        }
        // 更新边数据
        for (PassRecord record : networkState.getEdgePassRecords().values()) {
            tableModel.addRow(new Object[]{record.getIdentifier(), record.getCount(), record.getLastPassTime()});
        }
        tableModel.fireTableDataChanged(); // 通知表格模型数据已更新
    }
}
