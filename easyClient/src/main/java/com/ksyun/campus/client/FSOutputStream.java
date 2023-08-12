package com.ksyun.campus.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksyun.campus.client.pojo.DataServerMsg;
import com.ksyun.campus.client.pojo.MetaServerMsg;
import com.ksyun.campus.client.service.FileSystemService;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FSOutputStream extends OutputStream {
    private String path;
    private List<DataServerMsg> dataServerMsgList;
    private DataServerMsg dataServerMsgBac;
    private List<byte[]> memoryBuffer; //字节数组集合
    private int memoryBufferSize;
    private static final int MEMORY_BUFFER_SIZE = 1024 * 1024; // 1MB
    private ExecutorService executorService;
    private int flag = 0; //是否是本次写的最后一次callRemote，1是，0否
    private List<String> statInfoSingles = new ArrayList<>();
    private FileSystemService fileSystemService = new FileSystemService();

    public FSOutputStream(String path, List<DataServerMsg> dataServerMsgList, DataServerMsg dataServerMsgBac) {
        this.path = path;
        this.dataServerMsgList = dataServerMsgList;
        this.dataServerMsgBac = dataServerMsgBac;
        this.memoryBuffer = new ArrayList<>();
        this.memoryBufferSize = 0;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void write(int b) throws IOException {
        byte[] byteArr = { (byte) b };
        write(byteArr, 0, 1);
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    //将数据分块写入内存缓冲区 memoryBuffer，以防止一次性写入过大的数据量。当内存缓冲区的大小达到1MB时才进行一次callRemote
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        int remainingBytes = len;
        int offset = off;

        while (remainingBytes > 0) {
            int bytesToWrite = Math.min(remainingBytes, MEMORY_BUFFER_SIZE - memoryBufferSize);

            byte[] buffer = new byte[bytesToWrite];
            System.arraycopy(b, offset, buffer, 0, bytesToWrite);

            memoryBuffer.add(buffer); //字节数组集合
            memoryBufferSize += bytesToWrite;
            offset += bytesToWrite;
            remainingBytes -= bytesToWrite;

            if (memoryBufferSize >= MEMORY_BUFFER_SIZE) {
                flushMemoryBuffer();
            }
        }
    }


    //关闭流。将当前对象的 flushRemainingData() 方法作为任务提交给执行器服务
    @Override
    public void close() throws IOException {
        flushMemoryBuffer();
        executorService.submit(this::flushRemainingData);//通过将任务提交给执行器服务，可以异步地执行，而不会阻塞当前的关闭操作
        executorService.shutdown(); //优雅地关闭执行器服务，执行器服务将不再接受新的任务，但会继续执行已经提交的任务直至完成
        super.close();
    }

    //将内存缓冲区中的数据发送到dataServer，将缓冲区中的字节数组合并为一个字节数组
    private void flushMemoryBuffer() {
        if (!memoryBuffer.isEmpty()) {
            byte[] mergedBuffer = mergeBuffers(memoryBuffer);
            callRemote(mergedBuffer);
            memoryBuffer.clear();
            memoryBufferSize = 0;
        }
    }

    //在流关闭时，将剩余的内存缓冲区数据发送到dataServer并清空缓冲区
    private void flushRemainingData() {
        //无论memoryBuffer是否有数据都再发一次请求，做个flag标记
        flag = 1;
//        if (!memoryBuffer.isEmpty()) {
            byte[] mergedBuffer = mergeBuffers(memoryBuffer);
            callRemote(mergedBuffer);
            memoryBuffer.clear();
            memoryBufferSize = 0;
//        }
    }

    //将多个字节数组缓冲区合并成一个字节数组
    private byte[] mergeBuffers(List<byte[]> buffers) {
        int totalSize = 0;
        for (byte[] buffer : buffers) {
            totalSize += buffer.length;
        }
        byte[] mergedBuffer = new byte[totalSize];
        int destPos = 0;
        for (byte[] buffer : buffers) {
            System.arraycopy(buffer, 0, mergedBuffer, destPos, buffer.length);
            destPos += buffer.length;
        }
        return mergedBuffer;
    }

    private void callRemote(byte[] data) {
        for (DataServerMsg dataServerMsg : dataServerMsgList) {
//            //测一下挂掉一个dataServer后的反应
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
            String host = dataServerMsg.getHost();
            int port = dataServerMsg.getPort();

            // 构建请求路径
            String url = "http://" + host + ":" + port + "/write?path=" + path;

            FileSystem fileSystem = new FileSystem();
            try {
                String responseBody = fileSystem.callRemote("post", url, data);
                if(flag == 1){
                    statInfoSingles.add(responseBody);
                }
                if(statInfoSingles.size() == 3){
                    //保存元信息
                    toMetaServerSaveStatInfo(statInfoSingles);

                }
                System.out.println("responseBody=" + responseBody);
            } catch (Exception e) {
                System.out.println("ops中发送callRemote出现异常，就重试备用dataServer");
                url = "http://" + dataServerMsgBac.getHost() + ":" + dataServerMsgBac.getPort() + "/write?path=" + path;
                try {
                    String responseBody = fileSystem.callRemote("post", url, data);
                    System.out.println("responseBody=" + responseBody);

                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                //更新dataServerMsgList，有第二次传输的话就用新的三个dataServer
                dataServerMsgList.remove(dataServerMsg);
                dataServerMsgList.add(dataServerMsgBac);
            }

        }
    }

    //向metaServer发送请求保存元信息
    public void toMetaServerSaveStatInfo(List<String> statInfoSingles) throws Exception {
        MetaServerMsg metaServer = fileSystemService.getMetaServer();
        String metaServerUrl = "http://" + metaServer.getHost() + ":" + metaServer.getPort() + "/write";
        System.out.println("toMetaServerSaveStatInfo中的metaServerUrl = " + metaServerUrl);
        for (String statInfoSingle : statInfoSingles) {
            System.out.println("statInfoSingle = " + statInfoSingle);
        }
        byte[] statInfoSinglesBytes = new ObjectMapper().writeValueAsString(statInfoSingles).getBytes();
        FileSystem fileSystem = new FileSystem();
        String post = fileSystem.callRemote("post", metaServerUrl, statInfoSinglesBytes);
        System.out.println("post = " + post);
    }
}