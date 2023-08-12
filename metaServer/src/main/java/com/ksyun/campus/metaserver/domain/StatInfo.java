package com.ksyun.campus.metaserver.domain;

import lombok.Data;
import java.util.List;

@Data
public class StatInfo
{
    public String path;
    public long size;
    public long mtime;
    public FileType type;
    public List<ReplicaData> replicaData;

}
