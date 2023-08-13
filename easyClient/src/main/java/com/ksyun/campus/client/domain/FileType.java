package com.ksyun.campus.client.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum FileType {
    Unknown("Unknown"), Volume("Volume"), File("File"), Directory("Directory");

    private String code;

    FileType(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCodeValue() {
        return code;
    }
}