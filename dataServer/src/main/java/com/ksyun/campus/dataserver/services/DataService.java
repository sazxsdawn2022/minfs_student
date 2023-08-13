package com.ksyun.campus.dataserver.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksyun.campus.dataserver.domain.FileType;
import com.ksyun.campus.dataserver.domain.ReplicaData;
import com.ksyun.campus.dataserver.domain.StatInfoSingle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Path;
import java.util.UUID;


@Service
public class DataService {
    @Resource
    private ServletContext servletContext;
    @Value("${server.port}")
    private String port;
    @Value("${dataServer.host}")
    private String host;

    public String write(String path, byte[] data) throws Exception {
        // 获取本项目的绝对路径
        String projectPath = System.getProperty("user.dir");
        // 拼接文件路径
        String filePath = projectPath + File.separator + "data" + port + File.separator + path;

        if(data != null) {
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs(); // 创建父目录及其父目录中的所有不存在的目录
            }
            try (FileOutputStream fos = new FileOutputStream(filePath, true)) {
                fos.write(data);
            } catch (IOException e) {
                // 处理写入文件时的异常
                e.printStackTrace();
            }
        }


        //返回单模块的文件元信息，包括path,size,mtime,type,ReplicaData中的id,dsNode,path，三副本在Client再组合
        //创建一个表示文件的Path对象
        Path path1 = new File(filePath).toPath();

        // 获取文件的大小
        long size = Files.size(path1);
        // 获取文件的类型
        BasicFileAttributes attributes = Files.readAttributes(path1, BasicFileAttributes.class);
        boolean isDirectory = attributes.isDirectory(); // 是否为目录
        boolean isRegularFile = attributes.isRegularFile(); // 是否为普通文件
        // 根据文件类型进行判断
        FileType type;
        if (isDirectory) {
            type = FileType.Directory;
        } else if (isRegularFile) {
            type = FileType.File;
        } else {
            type = FileType.Unknown;
        }

        StatInfoSingle statInfoSingle = new StatInfoSingle();
        statInfoSingle.setPath(path);
        statInfoSingle.setSize(size);
        statInfoSingle.setMtime(System.currentTimeMillis());
        statInfoSingle.setType(type);
        //设置ReplicaData
        ReplicaData replicaDataSingle = new ReplicaData();
        replicaDataSingle.setId(UUID.randomUUID().toString());
        replicaDataSingle.setDsNode(host + ":" + port);
        replicaDataSingle.setPath(path);
        statInfoSingle.setReplicaDataSingle(replicaDataSingle);

        String statInfoSingleJson = new ObjectMapper().writeValueAsString(statInfoSingle);
        System.out.println("statInfoSingleJson = " + statInfoSingleJson);

        return statInfoSingleJson;


    }

    public byte[] read(String path, int offset, int length) {
        //todo 根据path读取指定大小的内容

        // 获取本项目的绝对路径
        String projectPath = System.getProperty("user.dir");
        // 拼接文件路径
        String filePath = projectPath + File.separator + "data" + port + File.separator + path;

        File file = new File(filePath);
        byte[] buffer = new byte[length];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.skip(offset);
            int bytesRead = fis.read(buffer);
            if (bytesRead != -1) {
                if (bytesRead < length) {
                    byte[] trimmedBuffer = new byte[bytesRead];
                    System.arraycopy(buffer, 0, trimmedBuffer, 0, bytesRead);
                    return trimmedBuffer;
                } else {
                    return buffer;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "null".getBytes();
    }
}
