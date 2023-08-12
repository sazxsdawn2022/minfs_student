package com.ksyun.campus.metaserver.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

@Data
public class StatInfoSingle {
    public String path;
    public long size;
    public long mtime;
    public FileType type;
    public ReplicaData replicaDataSingle;

    public StatInfoSingle() {
        // 添加无参构造函数
    }

    @JsonCreator
    public static StatInfoSingle fromString(String jsonString) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(jsonString, StatInfoSingle.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}