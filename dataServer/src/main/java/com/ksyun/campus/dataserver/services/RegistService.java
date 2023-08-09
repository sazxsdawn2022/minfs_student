package com.ksyun.campus.dataserver.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksyun.campus.dataserver.pojo.DataServerMsg;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class RegistService implements ApplicationRunner {

    @Value("${zookeeper.addr}")
    private String zookeeperAddr; // ZooKeeper服务器的连接信息

    @Value("${dataServer.host}")
    private String dataServerHost; //本dataServer节点的ip

    @Value("${server.port}")
    private int dataServerPort;

    @Resource
    private DataServerMsg dataServerMsg;


    @Override
    public void run(ApplicationArguments args) throws Exception {
        registToCenter();
    }

    public void registToCenter() throws IOException, InterruptedException, KeeperException {
        // todo 将本实例信息注册至zk中心，包含信息 ip、port、capacity、rack、zone
        // 注册的信息应该是DataServerMsg中的内容

        dataServerMsg.setHost(dataServerHost);
        dataServerMsg.setPort(dataServerPort);
        String dataServerMsgJson = new ObjectMapper().writeValueAsString(dataServerMsg);

        ZooKeeper zooKeeper = new ZooKeeper(zookeeperAddr, 5000, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                // 处理ZooKeeper事件
            }
        });

        // 等待连接建立完成
        while (zooKeeper.getState() != ZooKeeper.States.CONNECTED) {
            Thread.sleep(100);
        }

        // 创建父路径
        String parentPath = "/dataServers";
        Stat parentStat = zooKeeper.exists(parentPath, false);
        if (parentStat == null) {
            // 父节点不存在，创建父节点，持久节点
            zooKeeper.create(parentPath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }

        // 注册dataServer到ZooKeeper
        String dataServerPath = "/dataServers/dataServer" + dataServerPort; // 指定dataServer在ZooKeeper中的路径
        Stat dataServerStat = zooKeeper.exists(dataServerPath, false);
        if (dataServerStat == null) {
            // 子节点不存在，创建子节点，临时节点
            zooKeeper.create(dataServerPath, dataServerMsgJson.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        }

        // 其他逻辑...
        System.out.println(dataServerPath + "注册成功！");

//        // 关闭ZooKeeper客户端
//        zooKeeper.close();
    }

    public List<Map<String, Integer>> getDslist() {
        return null;
    }


}
