package com.ksyun.campus.client.pojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Data
public class DataServerMsg {
    private String host;
    private int port;
    private int fileTotal;
    private long capacity; //单位：字节B
    private long useCapacity;

    public DataServerMsg() {
    }

    public DataServerMsg(String host, int port, int fileTotal, long capacity, long useCapacity) {
        this.host = host;
        this.port = port;
        this.fileTotal = fileTotal;
        this.capacity = capacity;
        this.useCapacity = useCapacity;
    }

    public DataServerMsg(String json) {
        // 使用 Jackson 将 JSON 字符串转换为对象
        ObjectMapper mapper = new ObjectMapper();
        try {
            DataServerMsg dataServerMsg = mapper.readValue(json, DataServerMsg.class);
            // 将 dataServerMsg 的属性值赋给当前对象
            this.host = dataServerMsg.getHost();
            this.port = dataServerMsg.getPort();
            this.fileTotal = dataServerMsg.getFileTotal();
            this.capacity = dataServerMsg.getCapacity();
            this.useCapacity = dataServerMsg.getUseCapacity();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
