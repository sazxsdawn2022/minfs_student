package com.ksyun.campus.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksyun.campus.client.domain.ClusterInfo;
import com.ksyun.campus.client.domain.ReplicaData;
import com.ksyun.campus.client.domain.StatInfo;
import com.ksyun.campus.client.pojo.DataServerMsg;
import com.ksyun.campus.client.pojo.MetaServerMsg;
import com.ksyun.campus.client.service.FileSystemService;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EFileSystem extends FileSystem{

    private FileSystemService fileSystemService = new FileSystemService();

    private String fileName="default";
    public EFileSystem() {
    }

    public EFileSystem(String fileName) {
        this.fileName = fileName;
    }

    public FSInputStream open(String path) throws Exception {
        MetaServerMsg metaServer = fileSystemService.getMetaServer();
        String metaServerUrl = "http://" + metaServer.getHost() + ":" + metaServer.getPort() + "/open?path=" + path;
        String statInfo = callRemote("get", metaServerUrl, null);
        StatInfo statInfo1 = new ObjectMapper().readValue(statInfo, StatInfo.class);
        List<ReplicaData> replicaData = statInfo1.getReplicaData();
        FSInputStream fsInputStream = new FSInputStream(replicaData);

        return fsInputStream;
    }

    public FSOutputStream create(String path) throws Exception {
        MetaServerMsg metaServer = fileSystemService.getMetaServer();
        String metaServerUrl = "http://" + metaServer.getHost() + ":" + metaServer.getPort() + "/getDataServers";
        //获取的是4个
        String dataServerListJson = callRemote("get", metaServerUrl, null);
        System.out.println("dataServerListJson = " + dataServerListJson);

        // 将 JSON 字符串转换为 DataServerMsg 对象的 List 集合
        ObjectMapper objectMapper = new ObjectMapper();
        List<DataServerMsg> dataServerMsgList = objectMapper.readValue(dataServerListJson, new TypeReference<List<DataServerMsg>>() {});
        // 对 dataServerMsgList 进行排序，按照剩余容量从大到小排序
        Collections.sort(dataServerMsgList, new Comparator<DataServerMsg>() {
            public int compare(DataServerMsg msg1, DataServerMsg msg2) {
                long remainingCapacity1 = msg1.getCapacity() - msg1.getUseCapacity();
                long remainingCapacity2 = msg2.getCapacity() - msg2.getUseCapacity();
                // 从大到小排序
                return Integer.compare((int) remainingCapacity2, (int) remainingCapacity1);
            }
        });
        // 获取剩余容量最大的三个对象
        List<DataServerMsg> selectedDataServerList = dataServerMsgList.subList(0, Math.min(3, dataServerMsgList.size()));
        DataServerMsg dataServerMsgBac = null;
        //获取备用的，如果存在的话
        if (dataServerMsgList.size() >= 4) {
            dataServerMsgBac = dataServerMsgList.get(3);
        }
        for (DataServerMsg dataServerMsg : selectedDataServerList) {
            System.out.println("dataServerMsg = " + dataServerMsg);
        }
        System.out.println("dataServerMsgBac = " + dataServerMsgBac);
        FSOutputStream fsOutputStream = new FSOutputStream(path, selectedDataServerList, dataServerMsgBac);


        return fsOutputStream;
    }
    public boolean mkdir(String path) throws Exception {
        FSOutputStream fsOutputStream = create(path + "duskgaga");
        fsOutputStream.close();

        return true;
    }
    public boolean delete(String path) throws Exception {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        MetaServerMsg metaServer = fileSystemService.getMetaServer();
        String metaServerUrl = "http://" + metaServer.getHost() + ":" + metaServer.getPort() + "/delete?path=" + path;
        String get = callRemote("get", metaServerUrl, null);
        System.out.println("get = " + get);

        return true;
    }
    public StatInfo getFileStats(String path) throws Exception {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        MetaServerMsg metaServer = fileSystemService.getMetaServer();
        String metaServerUrl = "http://" + metaServer.getHost() + ":" + metaServer.getPort() + "/stats?path=" + path;
        String statInfoJson = callRemote("get", metaServerUrl, null);
        StatInfo statInfo = new ObjectMapper().readValue(statInfoJson, StatInfo.class);
        return statInfo;
    }
    public List<StatInfo> listFileStats(String path){
        return null;
    }
    public ClusterInfo getClusterInfo() throws Exception {
        List<String> dataServersChildrenMsg = fileSystemService.getChildrenData("/dataServers");
        List<String> metaServersChildrenMsg = fileSystemService.getChildrenData("/metaServers");
        String masterJson = metaServersChildrenMsg.get(0);
        String slaveJson = metaServersChildrenMsg.get(1);
        // 将dataServersChildrenMsg集合转换为JSON字符串
        List<String> dataServerJsonList = dataServersChildrenMsg;

        // TODO 需考虑主从节点有空的情况，另外需要确定主从节点
        // 将masterJson、slaveJson、dataServerJsonList分别映射到ClusterInfo对象的属性
        ObjectMapper objectMapper = new ObjectMapper();
        ClusterInfo clusterInfo = new ClusterInfo();
        clusterInfo.setMasterMetaServer(objectMapper.readValue(masterJson, ClusterInfo.MetaServerMsg.class));
        clusterInfo.setSlaveMetaServer(objectMapper.readValue(slaveJson, ClusterInfo.MetaServerMsg.class));
        clusterInfo.setDataServer(objectMapper.readValue(objectMapper.writeValueAsString(dataServerJsonList), new TypeReference<List<ClusterInfo.DataServerMsg>>() {}));

        return clusterInfo;
    }
}
