package com.ksyun.campus.dataserver.pojo;

import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class DataServerMsg {
    //把这些信息以json格式注册到zk上
    private String host;
    private int port;
    private int fileTotal;
    private long capacity = 1024 * 1024 * 1024; //单位：字节B 设置为(1024 * 1024 * 1024)B 即1GB
    private long useCapacity;
}
