package com.cqnews.oss.entiy;

/**
 * @author uYxUuu
 * @Date 2024/6/26
 */
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.io.Serializable;


@Data
@Entity
@Table(name = "appendix_v")
public class AppendixEntity implements Serializable{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "creator_id")
    private Long creatorId;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_id")
    private Long updateId;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "del_flag")
    private Integer delFlag;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "file_md5")
    private String fileMd5;

    @Column(name = "file_ext")
    private String fileExt;

    @Column(name = "file_belong")
    private String fileBelong;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "bucket_name")
    private String bucketName;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "file_size")
    private Integer fileSize;

    @Column(name = "associate_id")
    private Long associateId;

}