package com.cqnews.oss.storage.dfs;

import lombok.Data;

/**
 * @author uYxUuu
 * @Date 2024/6/26
 */
@Data
public class FileLocation {

    /**
     * 存储桶
     */
    private String bucket;

    /**
     * 名称
     */
    private String name;

    /**
     * 类型
     */
    private String contentType;

    /**
     * 大小
     */
    private int size;


    private String fileExt;

    private String originalFileName;

    @Override
    public String toString() {
        return String.format("%s.%s", bucket, name);
    }
}
