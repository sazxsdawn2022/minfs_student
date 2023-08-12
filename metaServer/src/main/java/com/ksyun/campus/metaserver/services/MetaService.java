package com.ksyun.campus.metaserver.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksyun.campus.metaserver.pojo.DataServerMsg;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class MetaService {
    @Resource
    private RegistService registService;

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
}
