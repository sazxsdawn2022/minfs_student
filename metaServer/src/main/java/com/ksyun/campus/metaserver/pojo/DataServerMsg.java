package com.ksyun.campus.metaserver.pojo;

import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class DataServerMsg {
    private String host;
    private int port;
    private int fileTotal;
    private int capacity; //单位：字节B
    private int useCapacity;
}
