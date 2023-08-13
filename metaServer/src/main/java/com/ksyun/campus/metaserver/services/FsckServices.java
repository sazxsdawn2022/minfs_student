package com.ksyun.campus.metaserver.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksyun.campus.metaserver.domain.ReplicaData;
import com.ksyun.campus.metaserver.domain.StatInfo;
import com.ksyun.campus.metaserver.pojo.DataServerMsg;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

@Service
public class FsckServices {

    @Resource
    private RegistService registService;

    @Resource
    private MetaService metaService;

    private final String STAT_INFOS_NODE = "/statInfos";

    //@Scheduled(cron = "0 0 0 * * ?") // 每天 0 点执行
    @Scheduled(fixedRate = 30 * 60 * 1000) // 每隔 30 分钟执行一次
    public void fsckTask() throws Exception {
        // todo 全量扫描文件列表
        // todo 检查文件副本数量是否正常
        // todo 更新文件副本数：3副本、2副本、单副本
        // todo 记录当前检查结果
        // 扫描==》清除
        scanToUpdateStatInfos();

        //扫描==》尝试恢复
        ObjectMapper objectMapper = new ObjectMapper();
        String zkStatInfos = metaService.zkStatInfos();
        HashMap<String, String> zkStatInfoMap = null;
        if (!("".equals(zkStatInfos) || "Initialdata".equals(zkStatInfos))) {
            zkStatInfoMap = objectMapper.readValue(zkStatInfos, new TypeReference<HashMap<String, String>>() {});
        }



    }

    // 清除/statInfos中保存的已经死亡节点的副本，然后重新设置到/statInfos节点
    public void scanToUpdateStatInfos() throws Exception {

        //获取实时的/dataServers下子节点集合
        List<String> dataServerMsgJsons = registService.childNodesRef.get();
        //DataServer节点集合
        List<DataServerMsg> dataServerMsgList = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        for (String dataServerMsgJson : dataServerMsgJsons) {
            DataServerMsg dataServerMsg = objectMapper.readValue(dataServerMsgJson, DataServerMsg.class);
            dataServerMsgList.add(dataServerMsg);
        }

        //如果有死亡的dataServer
        if (dataServerMsgList.size() != 4) {
            System.out.println("有死亡的dataServer");
            //获取已经死亡的dataServer的端口集合
            List<String> portList = new ArrayList<>();
            for (DataServerMsg dataServerMsg : dataServerMsgList) {
                portList.add(String.valueOf(dataServerMsg.getPort()));
            }
            List<String> diedDataServerPort = new ArrayList<>();
            List<String> targetPorts = Arrays.asList("9001", "9002", "9003", "9004");
            for (String targetPort : targetPorts) {
                if (!portList.contains(targetPort)) {
                    diedDataServerPort.add(targetPort);
                }
            }

            // 扫描/statInfos元信息，
            // 把StatInfo中ReplicaData集合中元素的dsDode包含在diedDataServerPort集合中的ReplicaData移除
            //最后全部扫描一遍后把更新后的statInfos重新设置到/statInfos节点
            String zkStatInfos = metaService.zkStatInfos();
            HashMap<String, String> zkStatInfoMap = null;
            if (!("".equals(zkStatInfos) || "Initialdata".equals(zkStatInfos))) {
                zkStatInfoMap = objectMapper.readValue(zkStatInfos, new TypeReference<HashMap<String, String>>() {});
            }
            if(zkStatInfoMap != null){
                for (Map.Entry<String, String> entry : zkStatInfoMap.entrySet()) {
                    String path = entry.getKey();
                    String json = entry.getValue();

                    try {
                        StatInfo statInfo = objectMapper.readValue(json, StatInfo.class);
                        List<ReplicaData> replicaDataList = statInfo.getReplicaData();

                        // 判断并移除位于 diedDataServer 中的副本
                        Iterator<ReplicaData> iterator = replicaDataList.iterator();
                        while (iterator.hasNext()) {
                            ReplicaData replicaData = iterator.next();
                            String[] dsNodeParts = replicaData.getDsNode().split(":");
                            String port = dsNodeParts[1];

                            if (diedDataServerPort.contains(port)) {
                                iterator.remove();
                            }
                        }

                        // 更新元信息的 JSON 字符串
                        String updatedJson = objectMapper.writeValueAsString(statInfo);
                        zkStatInfoMap.put(path, updatedJson);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            //得到去除死亡节点的ReplicaData后的zkStatInfoMap
            //重新设置到/statInfos节点
            ZooKeeper zooKeeper = metaService.connectToZooKeeper();
            String zkStatInfoMapJson = objectMapper.writeValueAsString(zkStatInfoMap);
            byte[] data = zkStatInfoMapJson.getBytes();
            Stat stat = zooKeeper.exists(STAT_INFOS_NODE, false);
            zooKeeper.setData(STAT_INFOS_NODE, data, stat.getVersion());
            zooKeeper.close();
        }
    }


}
