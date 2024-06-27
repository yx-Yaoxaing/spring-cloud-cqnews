package com.cqnews.oss.dao;

import com.cqnews.oss.dao.impl.OssDaoImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author uYxUuu
 * @Date 2024/6/26
 */
@Configuration
public class DaoConfiguration {


    @Bean
    public OssDao ossDao(){
        OssDao ossDao = new OssDaoImpl();
        return ossDao;
    }

}
