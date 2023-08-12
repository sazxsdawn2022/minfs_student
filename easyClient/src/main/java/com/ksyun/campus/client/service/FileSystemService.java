package com.ksyun.campus.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksyun.campus.client.pojo.MetaServerMsg;
import com.ksyun.campus.client.util.ZkUtil;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FileSystemService {


    //获取指定path下的子节点所存数据的集合，提供给getClusterInfo()使用
    public List<String> getChildrenData(String path) throws Exception {
        String zookeeperAddr = "192.168.200.130:2181";
        ZooKeeper zooKeeper = new ZooKeeper(zookeeperAddr, 3000, new Watcher() {
            public void process(WatchedEvent event) {
                System.out.println("Event received: " + event.getType());
            }
        });

        // 等待连接建立完成
        while (zooKeeper.getState() != ZooKeeper.States.CONNECTED) {
            Thread.sleep(100);
        }

        List<String> childrenData = new ArrayList<>();

        List<String> children = zooKeeper.getChildren(path, false);
        for (String child : children) {
            String childPath = path + "/" + child;
            byte[] data = zooKeeper.getData(childPath, false, null);
            childrenData.add(new String(data));
        }

        zooKeeper.close();

        return childrenData;
    }

    //返回一个可用的metaServer，有主选主，无主选从。提供给create()使用
    //参数path是创建的文件的路径
    public MetaServerMsg getMetaServer() throws Exception {

        String metaServerValue = "";
        // 选择一个可用的metaServer，如果主可用就用主/metaServer8001，如果主不可用就用从/metaServer8002
        String masterMetaServerValue = "";
        String slaveMetaServerValue = "";
        HashMap<String, String> metaServerMap = new ZkUtil().getMetaServerMap();
        System.out.println("metaServerMap = " + metaServerMap);
        for (Map.Entry<String, String> entry : metaServerMap.entrySet()) {
            String childNode = entry.getKey();
            String nodeValue = entry.getValue();
            // 处理每个子节点和节点值
            System.out.println("Child Node: " + childNode + ", Node Value: " + nodeValue);
        }

        for (Map.Entry<String, String> entry : metaServerMap.entrySet()) {
            String childNode = entry.getKey();
            //主节点存在
            if ("metaServer8001".equals(childNode)) {
                masterMetaServerValue = entry.getValue();
            } else if ("metaServer8002".equals(childNode)) {
                slaveMetaServerValue = entry.getValue();
            }
        }
        if (masterMetaServerValue != "") {
            metaServerValue = masterMetaServerValue;
        } else {
            metaServerValue = slaveMetaServerValue;
        }

        if(metaServerValue == ""){
            return null;
        }

        MetaServerMsg metaServerMsg = new ObjectMapper().readValue(metaServerValue, MetaServerMsg.class);
        return metaServerMsg;

    }

}
