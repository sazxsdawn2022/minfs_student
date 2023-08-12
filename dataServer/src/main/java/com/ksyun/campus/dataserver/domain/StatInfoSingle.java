package com.ksyun.campus.dataserver.domain;

import lombok.Data;

@Data
public class StatInfoSingle {
    public String path;
    public long size;
    public long mtime;
    public FileType type;
    private ReplicaData replicaDataSingle;
}
