package com.ksyun.campus.client.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

public class ClusterInfo {
    private MetaServerMsg masterMetaServer;
    private MetaServerMsg slaveMetaServer;
    private List<DataServerMsg> dataServer;


    public MetaServerMsg getMasterMetaServer() {
        return masterMetaServer;
    }

    public void setMasterMetaServer(MetaServerMsg masterMetaServer) {
        this.masterMetaServer = masterMetaServer;
    }

    public MetaServerMsg getSlaveMetaServer() {
        return slaveMetaServer;
    }

    public void setSlaveMetaServer(MetaServerMsg slaveMetaServer) {
        this.slaveMetaServer = slaveMetaServer;
    }

    public List<DataServerMsg> getDataServer() {
        return dataServer;
    }

    public void setDataServer(List<DataServerMsg> dataServer) {
        this.dataServer = dataServer;
    }

    public static class MetaServerMsg{
        private String host;
        private int port;

        @JsonCreator
        public MetaServerMsg(@JsonProperty("host") String host,
                             @JsonProperty("port") int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        @Override
        public String toString() {
            return "MetaServerMsg{" +
                    "host='" + host + '\'' +
                    ", port=" + port +
                    '}';
        }
    }
    public static class DataServerMsg{
        private String host;
        private int port;
        private int fileTotal;
        private long capacity;
        private long useCapacity;

        @JsonCreator
        public DataServerMsg(@JsonProperty("host") String host,
                             @JsonProperty("port") int port,
                             @JsonProperty("fileTotal") int fileTotal,
                             @JsonProperty("capacity") long capacity,
                             @JsonProperty("useCapacity") long useCapacity) {
            this.host = host;
            this.port = port;
            this.fileTotal = fileTotal;
            this.capacity = capacity;
            this.useCapacity = useCapacity;
        }
        @JsonCreator
        public static DataServerMsg fromString(String json) throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, DataServerMsg.class);
        }

        @JsonValue
        public String toJson() throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getFileTotal() {
            return fileTotal;
        }

        public void setFileTotal(int fileTotal) {
            this.fileTotal = fileTotal;
        }

        public long getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public long getUseCapacity() {
            return useCapacity;
        }

        public void setUseCapacity(int useCapacity) {
            this.useCapacity = useCapacity;
        }

        @Override
        public String toString() {
            return "DataServerMsg{" +
                    "host='" + host + '\'' +
                    ", port=" + port +
                    ", fileTotal=" + fileTotal +
                    ", capacity=" + capacity +
                    ", useCapacity=" + useCapacity +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "ClusterInfo{" +
                "masterMetaServer=" + masterMetaServer +
                ", slaveMetaServer=" + slaveMetaServer +
                ", dataServer=" + dataServer +
                '}';
    }
}
