package com.ksyun.campus.metaserver.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksyun.campus.metaserver.pojo.MetaServerMsg;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class RegistService implements ApplicationRunner {

    @Value("${zookeeper.addr}")
    private String zookeeperAddr; // ZooKeeper服务器的连接信息

    @Value("${metaServer.host}")
    private String metaServerHost; //本dataServer节点的ip

    @Value("${server.port}")
    private int metaServerPort;

    @Resource
    private MetaServerMsg metaServerMsg;

    ZooKeeper zooKeeper;
    AtomicReference<List<String>> childNodesRef = new AtomicReference<>(); // 父节点/dataServers下的子节点
    String listenDataServerParentPath = "/dataServers"; //父路径




    //把本节点注册到zookeeper中，同时监听/dataServers父节点下下的子节点
    public void registToCenter() throws IOException, InterruptedException, KeeperException {
        zooKeeper = new ZooKeeper(zookeeperAddr, 5000, new Watcher() {
            @Override
            public void process(WatchedEvent event) {

            }
        });

        // todo 将本实例信息注册至zk中心，包含信息 ip、port

        metaServerMsg.setHost(metaServerHost);
        metaServerMsg.setPort(metaServerPort);
        String metaServerMsgJson = new ObjectMapper().writeValueAsString(metaServerMsg);

        // 等待连接建立完成
        while (zooKeeper.getState() != ZooKeeper.States.CONNECTED) {
            Thread.sleep(100);
        }
        // 创建父路径
        String parentPath = "/metaServers";
        Stat parentStat = zooKeeper.exists(parentPath, false);
        if (parentStat == null) {
            // 父节点不存在，创建父节点，持久节点
            zooKeeper.create(parentPath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }

        // 注册dataServer到ZooKeeper
        String dataServerPath = "/metaServers/metaServer" + metaServerPort; // 指定dataServer在ZooKeeper中的路径
        Stat dataServerStat = zooKeeper.exists(dataServerPath, false);
        if (dataServerStat == null) {
            // 子节点不存在，创建子节点，临时节点
            zooKeeper.create(dataServerPath, metaServerMsgJson.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        }

        // 其他逻辑...
        System.out.println(dataServerPath + "注册成功！");

        // 监控/dataServers节点下的子节点
        listenDataServers();


    }


    // 保证了metaServer的主从节点都可以拿到dataServer四个节点的最新状态数据
    public void listenDataServers() throws InterruptedException, KeeperException {

        //先为/dataServers父节点下的每个子节点注册监听
        firstListenChildNodes();

        // 只要监听的/dataServer父节点下的子节点有变化，就刷新其子节点childNodesRef -> dataServerList
        zooKeeper.getChildren(listenDataServerParentPath, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                System.out.println("======第二波的监听器-开始=======");

                //父节点监听到子节点的变化（新增，删除）
                if (event.getType() == Event.EventType.NodeChildrenChanged) {
                    try {
                        List<String> newChildNodes = zooKeeper.getChildren(listenDataServerParentPath, this);

                        // 更新 childNodesRef，保证childNodesRef里的子节点都是最新的
                        //获取子节点数据集合
                        List<String> childDataList = new ArrayList<>();
                        for (String child : newChildNodes) {
                            String childPath = listenDataServerParentPath + "/" + child;
                            byte[] data = zooKeeper.getData(childPath, null, null);
                            String childData = new String(data, StandardCharsets.UTF_8);
                            // 将子节点的内容添加到集合中
                            childDataList.add(childData);
                        }
                        childNodesRef.set(childDataList);

                        // 处理新的子节点列表
                        System.out.println("New child nodes: " + childNodesRef.get());

                        // 注册子节点的数据变化监听器
                        for (String childNode : newChildNodes) {
                            String childNodePath = listenDataServerParentPath + "/" + childNode;
                            zooKeeper.exists(childNodePath, this);
                        }

                    } catch (KeeperException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                //这个数据内容变化的事件类型一定是被/dataServers的子节点监听到的
                if (event.getType() == Event.EventType.NodeDataChanged) {
                    try {
                        byte[] data1 = zooKeeper.getData(event.getPath(), this, null);
                        String nodeData = new String(data1);
                        // 处理修改节点数据的逻辑
                        System.out.println("Node data changed: " + event.getPath());
                        System.out.println("New data: " + nodeData);

                        // 更新 childNodesRef，保证childNodesRef里的子节点都是最新的
                        List<String> newChildNodes = zooKeeper.getChildren(listenDataServerParentPath, this);
                        List<String> childDataList = new ArrayList<>();
                        for (String child : newChildNodes) {
                            String childPath = listenDataServerParentPath + "/" + child;
                            byte[] data = zooKeeper.getData(childPath, null, null);
                            String childData = new String(data, StandardCharsets.UTF_8);
                            // 将子节点的内容添加到集合中
                            childDataList.add(childData);
                        }
                        childNodesRef.set(childDataList);
                    } catch (KeeperException | InterruptedException e) {
                        e.printStackTrace();
                    }

                }

                System.out.println("event.getType() = " + event.getType());
                System.out.println("======第二波的监听器-结束=======");

                // 重新注册/dataServers父节点的监听器
                try {
                    zooKeeper.getChildren(listenDataServerParentPath, this);
                } catch (KeeperException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }

        });

        // 处理初始的子节点列表
        System.out.println("Initial child nodes: " + childNodesRef.get());
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        registToCenter();
    }


    //先监听/dataServers下现有的所有子节点
    public void firstListenChildNodes() throws InterruptedException, KeeperException {

        // 获取子节点列表，并为每个子节点设置监听器
        List<String> children = zooKeeper.getChildren(listenDataServerParentPath, null);

        //获取子节点数据集合，初始化childNodesRef
        List<String> childDataList = new ArrayList<>();
        for (String child : children) {
            String childPath = listenDataServerParentPath + "/" + child;
            byte[] data = zooKeeper.getData(childPath, null, null);
            String childData = new String(data, StandardCharsets.UTF_8);
            // 将子节点的内容添加到集合中
            childDataList.add(childData);
        }
        childNodesRef.set(childDataList);

        System.out.println("childNodesRef.get() = " + childNodesRef.get());
        // 注册子节点的数据变化监听器，相当于刚开始就给子节点注册了，
        // 避免下面listenDataServers()通过/dataServers父节点给子节点注册监听器，但第一次子节点数据变化不会被监控到的问题
        //这里给子节点注册的监听器终会被下面注册的覆盖掉
        for (String childNode : children) {
            String childNodePath = listenDataServerParentPath + "/" + childNode;
            zooKeeper.exists(childNodePath, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    System.out.println("======第一波的监听器-开始=======");
                    if (event.getType() == Event.EventType.NodeDataChanged) {
                        try {
                            byte[] data1 = zooKeeper.getData(event.getPath(), this, null);
                            String nodeData = new String(data1);
                            // 处理修改节点数据的逻辑
                            System.out.println("Node data changed: " + event.getPath());
                            System.out.println("New data: " + nodeData);

                            // 更新 childNodesRef，保证childNodesRef里的子节点都是最新的
                            List<String> newChildNodes = zooKeeper.getChildren(listenDataServerParentPath, this);
                            List<String> childDataList = new ArrayList<>();
                            for (String child : newChildNodes) {
                                String childPath = listenDataServerParentPath + "/" + child;
                                byte[] data = zooKeeper.getData(childPath, null, null);
                                String childData = new String(data, StandardCharsets.UTF_8);
                                // 将子节点的内容添加到集合中
                                childDataList.add(childData);
                            }
                            childNodesRef.set(childDataList);
                        } catch (KeeperException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    System.out.println("======第一波的监听器-结束=======");

                }
            });
        }
    }


}
