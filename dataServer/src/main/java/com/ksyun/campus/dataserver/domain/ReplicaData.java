package com.ksyun.campus.dataserver.domain;

import lombok.Data;

@Data
public class ReplicaData {
    public String id; //副本的id
    public String dsNode; //格式为ip:port，副本存的节点信息是哪里
    public String path; //在对应的dataServer上存的文件路径

}
