package com.ksyun.campus.metaserver.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksyun.campus.metaserver.services.MetaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController("/")
public class MetaController {
    @Resource
    private MetaService metaService;

    //获取三个可用的dataServer，用来写（现在是4个，如果有的话）
    @RequestMapping("getDataServers")
    public ResponseEntity getDataServers() throws JsonProcessingException {
        String dataServerListJson = metaService.pickDataServer();
        return new ResponseEntity(dataServerListJson, HttpStatus.OK);
    }
    // 查看文件元信息
    @RequestMapping("stats")
    public ResponseEntity stats(@RequestHeader String fileSystem,@RequestParam String path){
        return new ResponseEntity(HttpStatus.OK);
    }
    // 创建文件
    @RequestMapping("create")
    public ResponseEntity createFile(@RequestHeader String fileSystem, @RequestParam String path){
        return new ResponseEntity(HttpStatus.OK);
    }
    // 创建目录
    @RequestMapping("mkdir")
    public ResponseEntity mkdir(@RequestHeader String fileSystem, @RequestParam String path){
        return new ResponseEntity(HttpStatus.OK);
    }
    // 查看目录下的文件
    @RequestMapping("listdir")
    public ResponseEntity listdir(@RequestHeader String fileSystem,@RequestParam String path){
        return new ResponseEntity(HttpStatus.OK);
    }
    // 删除文件
    @RequestMapping("delete")
    public ResponseEntity delete(@RequestParam String path){
        boolean b = false;
        try {
            b = metaService.deleteByPath(path);
        } catch (Exception e) {
            return new ResponseEntity("删除失败",HttpStatus.OK);
        }

        if(b){
            return new ResponseEntity("删除成功",HttpStatus.OK);
        }
        return new ResponseEntity("删除失败",HttpStatus.OK);
    }

    /**
     * 写文件，提交写
     * 调完dataService需要把元信息提交一下
     * 保存文件写入成功后的元数据信息，包括文件path、size、三副本信息等
     *
     */
    @RequestMapping(value = "write", method = RequestMethod.POST)
    public ResponseEntity commitWrite(@RequestBody byte[] statInfoSinglesBytes) throws Exception {
        String responseBody = metaService.toSaveStatInfo(statInfoSinglesBytes);

        return new ResponseEntity(responseBody, HttpStatus.OK);
    }

    /**
     * 根据文件path查询三副本的位置，返回客户端具体ds、文件分块信息
     * @param path
     * @return
     */
    @RequestMapping("open")
    public ResponseEntity open(@RequestParam String path){

        String statInfo = "";
        try {
            statInfo = metaService.getStatInfoByPath(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //返回path对应的元信息
        return new ResponseEntity(statInfo, HttpStatus.OK);
    }

    /**
     * 关闭退出进程
     */
    @RequestMapping("shutdown")
    public void shutdownServer(){
        System.exit(-1);
    }

}
