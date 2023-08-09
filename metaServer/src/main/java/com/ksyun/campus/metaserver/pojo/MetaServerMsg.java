package com.ksyun.campus.metaserver.pojo;

import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class MetaServerMsg {
    //把这些信息以json格式注册到zk上
    private String host;
    private int port;
}
