package com.ksyun.campus.client.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ReplicaData {
    public String id; //副本的id
    public String dsNode; //格式为ip:port，副本存的节点信息是哪里
    public String path; //在对应的dataServer上存的文件路径
    public ReplicaData() {
        // 添加无参构造函数
    }

    @JsonCreator
    public ReplicaData(@JsonProperty("id") String id,
                       @JsonProperty("dsNode") String dsNode,
                       @JsonProperty("path") String path) {
        this.id = id;
        this.dsNode = dsNode;
        this.path = path;
    }
}
