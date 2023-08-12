package com.ksyun.campus.dataserver.domain;

/**
 * 定义了一个简单的文件类型枚举，通过code获取相应的文件类型枚举常量，并提供获取枚举常量整数码值(code)的方法
 */

public enum FileType
{
    Unknown(0),  Volume(1),  File(2),  Directory(3);

    private int code;
    FileType(int code) {
        this.code=code;
    }
    public int getCode(){
        return code;
    }
    public static FileType get(int code){
        switch (code){
            case 1:
                return Volume;
            case 2:
                return File;
            case 3:
                return Directory;
            default:
                return Unknown;
        }
    }
}
