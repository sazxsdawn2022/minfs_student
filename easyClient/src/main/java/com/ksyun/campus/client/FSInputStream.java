package com.ksyun.campus.client;

import com.ksyun.campus.client.domain.ReplicaData;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class FSInputStream extends InputStream {

    private List<ReplicaData> replicaData;
    private int currentReplicaIndex;

    public FSInputStream(List<ReplicaData> replicaData) {
        this.replicaData = replicaData;
        this.currentReplicaIndex = 0;
    }

    @Override
    public int read() throws IOException {
        byte[] buffer = new byte[1];
        int bytesRead = read(buffer);
        if (bytesRead == -1) {
            return -1;
        }
        return buffer[0] & 0xFF;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (currentReplicaIndex >= replicaData.size()) {
            return -1; // No more replicas to read from
        }

        ReplicaData currentReplica = replicaData.get(currentReplicaIndex);
        String dsNode = currentReplica.getDsNode();
        String path = currentReplica.getPath();

        String url = "http://" + dsNode + "/read?path=" + path + "&off=" + off + "&len=" + len;
        FileSystem fileSystem = new FileSystem();
        try {
            byte[] responseData = fileSystem.callRemote("get", url, null).getBytes();
            if (responseData != null) {
                int bytesRead = responseData.length;
                if (bytesRead > 0) {
                    System.arraycopy(responseData, 0, b, off, bytesRead);
                }
                return bytesRead;
            }
        } catch (Exception e) {
            // 出现异常，用下一个副本
            currentReplicaIndex++;
            return read(b, off, len);
        }

        return -1; // 没数据可读
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}