package com.cqnews.oss.domain;

import lombok.Data;

/**
 * @author uYxUuu
 * @Date 2024/6/26
 */
@Data
public class OssVO {

    /**
     * 新的文件名称
     */
    private String newFileName;

    /**
     * 源文件名称
     */
    private String originalFileName;


    /**
     * 文件下载地址
     */
    private String fileNetWorkUrl;

}
