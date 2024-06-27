package com.cqnews.oss.storage;

import com.cqnews.oss.storage.dfs.DFsService;
import com.cqnews.oss.storage.impl.MinioOssService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * @author uYxUuu
 * @Date 2024/6/26
 */
@Configuration
public class StorageConfiguration {


    @Bean
    @Conditional(MinioOssService.MinioOssCondition.class)
    public DFsService initMinioOssFs() {
        return new MinioOssService();
    }

}
