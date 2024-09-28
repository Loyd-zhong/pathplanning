// PassRecord.java
package pathfinding.models;

import java.time.LocalDateTime;

public class PassRecord {
    private final String identifier; // 标识符（节点或边的标识）
    private int count; // 通过次数
    private LocalDateTime lastPassTime; // 最近一次通过时间

    // 构造方法，接受 Edge 类型
    public PassRecord(Edge edge) {
        this.identifier = "Edge-" + edge.getFrom().getX() + "," + edge.getFrom().getY() +
                "->" + edge.getTo().getX() + "," + edge.getTo().getY();
        this.count = 0;
        this.lastPassTime = LocalDateTime.now();
    }

    // 构造方法，接受 Node 类型
    public PassRecord(Node node) {
        this.identifier = "Node-" + node.getX() + "," + node.getY();
        this.count = 0;
        this.lastPassTime = LocalDateTime.now();
    }
    public PassRecord(String identifier) {
        this.identifier = identifier;
        this.count = 0;
        this.lastPassTime = LocalDateTime.now();
    }

    public String getIdentifier() {
        return identifier;
    }

    public int getCount() {
        return count;
    }

    public void incrementCount() {
        this.count++;
    }

    public LocalDateTime getLastPassTime() {
        return lastPassTime;
    }

    public void setLastPassTime(LocalDateTime lastPassTime) {
        this.lastPassTime = lastPassTime;
    }

    @Override
    public String toString() {
        return "ID: " + identifier + ", Count: " + count + ", Last Pass: " + lastPassTime;
    }
}
