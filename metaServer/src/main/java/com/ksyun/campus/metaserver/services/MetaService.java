package com.ksyun.campus.metaserver.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksyun.campus.metaserver.domain.FileType;
import com.ksyun.campus.metaserver.domain.ReplicaData;
import com.ksyun.campus.metaserver.domain.StatInfo;
import com.ksyun.campus.metaserver.domain.StatInfoSingle;
import com.ksyun.campus.metaserver.pojo.DataServerMsg;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CountDownLatch;

@Service
public class MetaService {
    @Resource
    private RegistService registService;
    @Value("${zookeeper.addr}")
    private String ZOOKEEPER_ADDRESS;

    private HashMap<String, String> statInfos = new HashMap<>();
    private final int SESSION_TIMEOUT = 5000;
    private final String STAT_INFOS_NODE = "/statInfos";

    public String pickDataServer() throws JsonProcessingException {
        // todo 通过zk内注册的ds列表，选择出来一个ds，用来后续的wirte
        //直接选三个
        // 需要考虑选择ds的策略？负载
        List<String> dataServerChildNodes = registService.childNodesRef.get();
        return new ObjectMapper().writeValueAsString(dataServerChildNodes);

        //这里改成直接返回四个（如果有四个的话）dataServer，排序的工作放到client
//        ObjectMapper objectMapper = new ObjectMapper();
//        List<DataServerMsg> dataServerMsgList = new ArrayList<>();
//        for (String json : dataServerChildNodes) {
//            DataServerMsg dataServerMsg = objectMapper.readValue(json, DataServerMsg.class);
//            // 将转换后的对象添加到列表中
//            dataServerMsgList.add(dataServerMsg);
//        }
//
//        // 对 dataServerMsgList 进行排序，按照剩余容量从大到小排序
//        Collections.sort(dataServerMsgList, new Comparator<DataServerMsg>() {
//            public int compare(DataServerMsg msg1, DataServerMsg msg2) {
//                int remainingCapacity1 = msg1.getCapacity() - msg1.getUseCapacity();
//                int remainingCapacity2 = msg2.getCapacity() - msg2.getUseCapacity();
//                // 从大到小排序
//                return Integer.compare(remainingCapacity2, remainingCapacity1);
//            }
//        });
//
//        // 获取剩余容量最大的三个对象
//        List<DataServerMsg> selectedDataServerList = dataServerMsgList.subList(0, Math.min(3, dataServerMsgList.size()));
//
//
//        String selectedDataServerListJson = objectMapper.writeValueAsString(selectedDataServerList);
//
//        //返回选出的现有剩余容量最大的三个DataServer
//        return selectedDataServerListJson;
    }

    //保存元信息
    public String toSaveStatInfo(byte[] statInfoSinglesBytes) throws Exception {

        // 把接收的数据转为List<StatInfoSingle>
        List<StatInfoSingle> statInfoSingles = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            statInfoSingles = objectMapper.readValue(statInfoSinglesBytes, new TypeReference<List<StatInfoSingle>>() {
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        //再转化为statusInfo元信息
        StatInfo statInfo = combine(statInfoSingles);
        String statInfoJson = objectMapper.writeValueAsString(statInfo);
        System.out.println("statInfoJson = " + statInfoJson);


        //获取现有的数据
        String zkStatInfos = zkStatInfos();
        if ("".equals(zkStatInfos) || "Initialdata".equals(zkStatInfos)) {
            // /statInfos中没有存元信息
            ZooKeeper zooKeeper = connectToZooKeeper();

            // 这里要递归将文件的前面目录都存上元信息
            HashMap<String, String> dirStatInfoMap = getDirStatInfoMap(statInfo);
            for (Map.Entry<String, String> entry : dirStatInfoMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                statInfos.put(key, value);
            }
            //把自己也加上
            statInfos.put(statInfo.getPath(), statInfoJson);

            String statInfosJson = objectMapper.writeValueAsString(statInfos);
            System.out.println("第一次，添加的元信息statInfosJson = " + statInfosJson);
            // 创建并设置/statInfos节点的数据
            byte[] data = statInfosJson.getBytes();
            Stat stat = zooKeeper.exists(STAT_INFOS_NODE, false);
            zooKeeper.setData(STAT_INFOS_NODE, data, stat.getVersion());
            zooKeeper.close();
        } else {
            // /statInfos节点中已经存放了一些元数据
            HashMap<String, String> zkStatInfoMap = objectMapper.readValue(zkStatInfos, new TypeReference<HashMap<String, String>>() {});
            // 把zkStatInfoMap映射成statInfos集合
            statInfos = zkStatInfoMap;

            //添加新的statInfo到statInfos集合
            // 这里要递归将文件的前面目录都存上元信息
            HashMap<String, String> dirStatInfoMap = getDirStatInfoMap(statInfo);
            for (Map.Entry<String, String> entry : dirStatInfoMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                statInfos.put(key, value);
            }
            statInfos.put(statInfo.getPath(), statInfoJson);

            //把statInfos集合转为json字符串
            String updatedStatInfosJson = objectMapper.writeValueAsString(statInfos);
            System.out.println("原先已经有元信息，添加后的元信息map = " + updatedStatInfosJson);

            ZooKeeper zooKeeper = connectToZooKeeper();
            // 更新/statInfos节点的数据
            byte[] data = updatedStatInfosJson.getBytes();
            Stat stat = zooKeeper.exists(STAT_INFOS_NODE, false);
            zooKeeper.setData(STAT_INFOS_NODE, data, stat.getVersion());
            zooKeeper.close();
        }

        //如果新增的是文件就更新，是目录就不用更新
        if(statInfo.getType().equals(FileType.File)) {
            //最后更新三个dataServer节点的数据
            updateDataServer(statInfo, 1);
        }

        return "文件元信息保存成功";
    }

    //组合文件元信息
    public StatInfo combine(List<StatInfoSingle> statInfoSingles) {
        StatInfo statInfo = new StatInfo();

        // 取出statInfoSingles集合中的最后一个元素
        StatInfoSingle lastStatInfoSingle = statInfoSingles.get(statInfoSingles.size() - 1);

        // 设置StatInfo对象的path, size, mtime, type属性
        statInfo.setPath(lastStatInfoSingle.getPath());
        statInfo.setSize(lastStatInfoSingle.getSize());
        statInfo.setMtime(lastStatInfoSingle.getMtime());
        statInfo.setType(lastStatInfoSingle.getType());

        // 创建并设置StatInfo对象的replicaData属性
        List<ReplicaData> replicaDataList = new ArrayList<>();
        for (StatInfoSingle statInfoSingle : statInfoSingles) {
            ReplicaData replicaData = statInfoSingle.getReplicaDataSingle();
            replicaDataList.add(replicaData);
        }
        statInfo.setReplicaData(replicaDataList);

        return statInfo;
    }

    //获取/statInfos节点目前存有的数据
    public String zkStatInfos() throws Exception {

        ZooKeeper zooKeeper = connectToZooKeeper();
        // 检查/statInfos节点是否存在
        Stat stat = zooKeeper.exists(STAT_INFOS_NODE, false);
        if (stat == null) {
            // /statInfos节点不存在，注册节点
            createStatInfosNode(zooKeeper);
        } else {
            // /statInfos节点存在，获取节点数据
            byte[] data = zooKeeper.getData(STAT_INFOS_NODE, false, stat);
            if ("Initialdata".equals(new String(data))) {
                // 节点数据为空
                System.out.println("节点数据为空");
                return "";
            } else {
                // 节点数据不为空
                String nodeData = new String(data);
                System.out.println("节点数据：" + nodeData);
                return nodeData;
            }
        }
        zooKeeper.close();
        return "";
    }

    //连接到zookeeper
    public ZooKeeper connectToZooKeeper() throws IOException {
        //确保在与ZooKeeper服务器成功建立连接之前，主线程不会继续执行后续的操作，以避免出现连接尚未建立的情况下执行操作而导致错误
        final CountDownLatch connectedSignal = new CountDownLatch(1);
        ZooKeeper zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, new Watcher() {
            public void process(WatchedEvent event) {
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    connectedSignal.countDown();
                }
            }
        });

        try {
            connectedSignal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return zooKeeper;
    }

    //创造节点并初始化
    public void createStatInfosNode(ZooKeeper zooKeeper) throws KeeperException, InterruptedException {
        byte[] data = "Initialdata".getBytes();
        zooKeeper.create(STAT_INFOS_NODE, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        System.out.println("创建/statInfos节点并设置初始数据");
    }

    //更新dataServer节点存放的部分数据，即fileTotal, useCapacity
    //只有新增，删除文件才更新 flag标识1代表增加，0代表删除
    public void updateDataServer(StatInfo statInfo, int flag) throws IOException, InterruptedException, KeeperException {
        //用的容量
        long useSize = statInfo.getSize();
        List<ReplicaData> replicaData = statInfo.getReplicaData();

        ZooKeeper zooKeeper = connectToZooKeeper();
        for (ReplicaData replicaDatum : replicaData) {
            //dataServer的ip:port
            String address = replicaDatum.getDsNode();
            String port = (address.split(":"))[1];
            String dataServerNode = "/dataServers/dataServer" + port;


            Stat stat = zooKeeper.exists(dataServerNode, false);
            byte[] data = zooKeeper.getData(dataServerNode, false, stat);
            String dataServerJson = new String(data);
            System.out.println("dataServerJson = " + dataServerJson);
            DataServerMsg dataServerMsg = new ObjectMapper().readValue(dataServerJson, DataServerMsg.class);

            if(flag == 1) {  //新增文件
                //更新文件总数fileTotal
                dataServerMsg.setFileTotal(dataServerMsg.getFileTotal() + 1);
                //更新已用容量useCapacity
                dataServerMsg.setUseCapacity(dataServerMsg.getUseCapacity() + useSize);
            } else if (flag == 0) {  //删除文件
                //更新文件总数fileTotal
                dataServerMsg.setFileTotal(dataServerMsg.getFileTotal() - 1);
                //更新已用容量useCapacity
                dataServerMsg.setUseCapacity(dataServerMsg.getUseCapacity() - useSize);
            }

            //重新设置到dataServer节点
            String dataServerMsgJson = new ObjectMapper().writeValueAsString(dataServerMsg);
            Stat stat1 = zooKeeper.exists(dataServerNode, false);
            zooKeeper.setData(dataServerNode,dataServerMsgJson.getBytes(), stat1.getVersion());
            //zookeeper不用关，最后用完再关
        }

        zooKeeper.close();
    }

    // 增加由path获取path对应的元信息
    public String getStatInfoByPath(String path) throws Exception {

        // 获取/statInfos节点的数据
        String statInfosJson = zkStatInfos();
        ObjectMapper objectMapper = new ObjectMapper();
        HashMap<String, String> zkStatInfoMap = objectMapper.readValue(statInfosJson, new TypeReference<HashMap<String, String>>() {});
        // 获取path对应的statInfo
        String statInfo = zkStatInfoMap.get(path);
        return statInfo;
    }

    //增加由path删除文件和文件夹的元信息，如果删除的是文件夹则不用更新dataServer节点信息
    public boolean deleteByPath(String path) throws Exception {
        String toDeleteStatInfo = "";

        String zkStatInfos = zkStatInfos();

        if ("".equals(zkStatInfos) || "Initialdata".equals(zkStatInfos)) {
            // /statInfos中没有存元信息
            return false;
        } else {
            // /statInfos节点中已经存放了一些元数据
            ObjectMapper objectMapper = new ObjectMapper();
            HashMap<String, String> zkStatInfoMap = objectMapper.readValue(zkStatInfos, new TypeReference<HashMap<String, String>>() {});
            // 把zkStatInfoMap映射成statInfos集合
            statInfos = zkStatInfoMap;

            //获取要删除的元信息
            toDeleteStatInfo = statInfos.get(path);
            //删除path对应的元信息
            statInfos.remove(path);
            String updatedStatInfosJson = objectMapper.writeValueAsString(statInfos);

            ZooKeeper zooKeeper = connectToZooKeeper();
            // 更新/statInfos节点的数据
            byte[] data = updatedStatInfosJson.getBytes();
            Stat stat = zooKeeper.exists(STAT_INFOS_NODE, false);
            zooKeeper.setData(STAT_INFOS_NODE, data, stat.getVersion());
            zooKeeper.close();
        }

        StatInfo statInfo = new ObjectMapper().readValue(toDeleteStatInfo, StatInfo.class);

        //最后更新三个dataServer节点的数据
        if(statInfo.getType().equals(FileType.File)) {
            updateDataServer(statInfo, 0);
        }
        return true;
    }

    //增加由path返回path下面一层的所有文件和目录
    public String listdir(String path) throws Exception {
        // 获取/statInfos节点的数据
        String statInfosJson = zkStatInfos();
        ObjectMapper objectMapper = new ObjectMapper();
        HashMap<String, String> zkStatInfoMap = objectMapper.readValue(statInfosJson, new TypeReference<HashMap<String, String>>() {});
        ArrayList<String> selectedStatInfoList = new ArrayList<>();

        String newPath = path.endsWith("/") ? path : path + "/";
        for (Map.Entry<String, String> entry : zkStatInfoMap.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(newPath) && !key.substring(newPath.length()).contains("/")) {
                selectedStatInfoList.add(entry.getValue());
            }
        }
        String selectedStatInfoJson = new ObjectMapper().writeValueAsString(selectedStatInfoList);
        return selectedStatInfoJson;

    }

    //增加方法，通过传进来的statInfo对象，返回该对象对应path前递归的目录map(path,statInfo)
    public HashMap<String, String> getDirStatInfoMap(StatInfo statInfo) {
        String path = statInfo.getPath();
        List<String> dirPathList = new ArrayList<>();
        HashMap<String, String> dirPathStatInfo = new HashMap<>();

        // 递归分解路径，提取目录路径
        String[] pathSegments = path.split("/");
        StringBuilder currentDirPath = new StringBuilder();
        for (String segment : pathSegments) {
            if (!segment.isEmpty()) {
                currentDirPath.append("/").append(segment);
                dirPathList.add(currentDirPath.toString());
            }
        }

        // 生成对应的 StatInfo 对象字符串
        for (String dirPath : dirPathList) {
            StatInfo dirStatInfo = new StatInfo();
            dirStatInfo.setPath(dirPath);
            dirStatInfo.setSize(0);
            dirStatInfo.setMtime(System.currentTimeMillis());
            dirStatInfo.setType(FileType.Directory);

            List<ReplicaData> replicaDataList = new ArrayList<>();
            for (ReplicaData replicaData : statInfo.getReplicaData()) {
                ReplicaData newReplicaData = new ReplicaData();
                newReplicaData.setId(UUID.randomUUID().toString());
                newReplicaData.setDsNode(replicaData.getDsNode());
                newReplicaData.setPath(dirPath);
                replicaDataList.add(newReplicaData);
            }
            dirStatInfo.setReplicaData(replicaDataList);

            String dirStatInfoString = null;
            try {
                dirStatInfoString = new ObjectMapper().writeValueAsString(dirStatInfo);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            dirPathStatInfo.put(dirPath, dirStatInfoString);
        }

        // 打印结果
        for (String dirPath : dirPathStatInfo.keySet()) {
            String statInfoString = dirPathStatInfo.get(dirPath);
            System.out.println("Directory Path: " + dirPath);
            System.out.println("StatInfo: " + statInfoString);
            System.out.println();
        }
        return dirPathStatInfo;
    }

}
