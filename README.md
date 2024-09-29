# AGV路径规划算法库

该项目包含多个用于AGV（自动导引车）路径规划的算法实现，这些算法可以作为AGV控制系统的一部分，用于路径寻找和路径优化。代码库中提供了几种主要的路径规划策略和一些实用工具，便于将其集成到更大规模的AGV控制系统中。

## 目录结构

src/
├── pathfinding/
│   ├── algorithms/
│   │   ├── AStarPathfinder.java         # A*路径规划算法的实现
│   │   ├── ThetaStarPathfinder.java     # Theta*路径规划算法的实现
│   │   └── TimeWindowManager.java       # 时间窗口管理器，用于节点占用和时间冲突管理
│   ├── main/
│   │   ├── MainFrame.java               # 主程序框架，用于AGV路径可视化和任务管理
│   │   └── PathVisualizer.java          # 路径可视化器，用于图形化显示AGV路径和动态更新
│   ├── models/
│   │   ├── Node.java                    # 节点模型类
│   │   ├── Edge.java                    # 边模型类，包含直线和弧线的支持
│   │   ├── Graph.java                   # 图结构类，用于管理节点和边的拓扑关系
│   │   ├── AGV.java                     # AGV类，负责处理AGV状态、路径跟踪和速度管理
│   │   ├── NetworkState.java            # 网络状态类，用于记录节点和边的通过记录
│   │   └── PassRecord.java              # 通过记录类，用于记录每个节点和边的通过次数
│   ├── ui/
│   │   └── TaskManagerPanel.java        # 任务管理面板，用于展示AGV任务和网络状态
│   └── utils/
│       └── MapLoader.java               # 地图加载工具类，用于从XML文件中加载地图和邻接关系
└── resources/
    └── map.xml                          # XML地图文件，包含节点、边和邻接信息，



注：可克隆代码库到本地：

	https://github.com/Loyd-zhong/pathplanning.git

	使用您的IDE（如IntelliJ IDEA或Eclipse，我使用的是IDEA）打开项目，确保JDK 1.8或更高版本已配置，将所需的依赖库（如Java Swing、XML解析库等）添加到项目中。

 在`resources/`目录下有地图XML文件，“真实版本”是甲方给到的地图文件，“尝试版本”是以甲方给的XML地图文件格式构建的简化测试地图版本，或根据需要修改`MapLoader.java`中的文件路径。

## 使用说明

### 1. 主程序运行

运行`src/pathfinding/main/MainFrame.java`中的`main()`方法以启动AGV路径规划可视化系统。该系统包括以下功能模块：

- **路径可视化（Path Visualization）**：图形化显示AGV的路径、节点和边。
- **任务管理（Task Manager）**：显示当前AGV的任务分配情况，包括起始点、目标点及路径规划信息。

### 2. 路径规划算法说明

#### 2.1 输入

路径规划算法的输入参数包括以下内容：

1. AGV速度（四种模式的不同速度，在AGV类中有定义）

2. **起始点与目标点（Node start, Node goal）**：
   - 起始点（`start`）：AGV的出发节点。
   - 目标点（`goal`）：AGV的目的地节点。

3. **启发式估计函数（Heuristic Function）**：
   - 用于估算当前节点到目标节点的最优路径距离（如欧几里得距离）。

4. **时间约束（可选）**：
   - 使用`TimeWindowManager`进行时间窗口的管理，以避免多个AGV在同一节点或边发生冲突。

#### 2.2 输出

路径规划算法的输出为最优路径（`Path`）：

- **路径（Path）**：
  - 包含按顺序排列的一系列节点（`List<Node>`），表示AGV从起点到目标点的最优行驶路线。
  - 每个节点记录其到达时间、位置坐标，以及与该节点关联的路径成本。

- **路径总成本（Total Cost）**：
  - 所有节点和边的累积权重和距离总和，用于衡量该路径的优劣。

- **节点到达时间（Arrival Time）**：
  - 每个节点的到达时间（`LocalDateTime`），用于路径冲突检查和AGV调度。

### 3. 路径规划算法实现

- **A*路径规划算法** (`AStarPathfinder.java`)：
  - 适用于规则网格或图形结构中的最短路径查找。
  - 通过启发式函数（欧几里得距离）估算最优路径。
  

- **时间窗口管理器** (`TimeWindowManager.java`)：
  - 处理节点的时间冲突和AGV之间的竞争情况，确保在复杂路网中的路径安全性。

### 4. 路径可视化与AGV调度

- 打开`MainFrame.java`，程序将自动加载地图并初始化AGV。
- 使用`PathVisualizer`进行路径显示，并通过鼠标交互查看各个节点的具体坐标和AGV状态。

### 5. 地图加载与配置

- 使用`MapLoader.java`从XML文件中读取地图信息。地图文件包含节点（`PointInfo`）及其相邻节点（`NeighbInfo`）的信息。
- 修改`resources/map.xml`文件中的节点和邻接关系，或使用其他格式的地图文件，但需要确保`MapLoader`中的解析逻辑与文件格式匹配。





