package com.ksyun.campus.client.util;

import lombok.Data;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ZkUtil {
    private String zookeeperAddr = "192.168.200.130:2181";
    private HashMap<String, String> metaServerMap = new HashMap<>();

    public ZkUtil() {
        init();
    }

    private void init() {
        try {
            postCons();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


//    @PostConstruct
    public void postCons() throws Exception {
        // todo 初始化，与zk建立连接，注册监听路径，当配置有变化随时更新

        // 建立与 ZooKeeper 的连接
        ZooKeeper zooKeeper = new ZooKeeper(zookeeperAddr, 3000, new Watcher() {
            public void process(WatchedEvent event) {
                System.out.println("Event received: " + event.getType());
            }
        });

        // 等待连接建立完成
        while (zooKeeper.getState() != ZooKeeper.States.CONNECTED) {
            Thread.sleep(100);
        }

        // 注册子节点的 Watcher
        zooKeeper.getChildren("/metaServers", new Watcher() {
            public void process(WatchedEvent event) {
                System.out.println("Event received: " + event.getType());
                // 子节点发生变化时更新子节点集合
                if (event.getType() == Event.EventType.NodeChildrenChanged && event.getPath().equals("/metaServers")) {
                    try {
                        updateChildNodes(zooKeeper);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        //初始化metaServerMap
        updateChildNodes(zooKeeper);
    }

    // 更新子节点集合方法
    private void updateChildNodes(ZooKeeper zooKeeper) throws Exception {
        List<String> childNodes = zooKeeper.getChildren("/metaServers", true);
        // 遍历子节点集合
        for (String childNode : childNodes) {
            // 获取子节点的路径
            String childNodePath = "/metaServers/" + childNode;
            // 获取子节点的数据
            byte[] nodeData = zooKeeper.getData(childNodePath, false, null);
            String nodeValue = new String(nodeData);
            metaServerMap.put(childNode, nodeValue);
        }

        for (Map.Entry<String, String> entry : metaServerMap.entrySet()) {
            String childNode = entry.getKey();
            String nodeValue = entry.getValue();
            // 处理每个子节点和节点值
            System.out.println("Child Node: " + childNode + ", Node Value: " + nodeValue);
        }
    }
}
