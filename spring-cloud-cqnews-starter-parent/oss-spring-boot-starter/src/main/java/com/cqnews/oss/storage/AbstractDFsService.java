package com.cqnews.oss.storage;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.druid.pool.DruidDataSource;
import com.cqnews.oss.entiy.AppendixEntity;
import com.cqnews.oss.storage.dfs.DFsService;
import com.cqnews.oss.storage.dfs.FileLocation;
import com.cqnews.oss.storage.dfs.StoreRequest;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Properties;

/**
 * @author uYxUuu
 * @Date 2024/6/26
 */
@Slf4j
public abstract class AbstractDFsService implements DFsService, ApplicationContextAware, DisposableBean {

    private static String INIT_SQL = "";

    private ApplicationContext applicationContext;

    protected SessionFactory sessionFactory;


    protected static final String PROPERTY_KEY = "cqnews.storage.dfs";

    protected static String fetchProperty(Environment environment, String dfsType, String key) {
        String pKey = String.format("%s.%s.%s", PROPERTY_KEY, dfsType, key);
        return environment.getProperty(pKey);
    }

    abstract protected void init(ApplicationContext applicationContext);

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        // 具体的分布式存储实现
        init(applicationContext);
    }

    protected void initDb(String url, String password, String userName, String driverClassName) {
        log.info("[DFS] init db hibernate start");
        if (this.sessionFactory == null) {
            try {
                DruidDataSource dataSource = new DruidDataSource();
                dataSource.setUrl(url);
                dataSource.setUsername(userName);
                dataSource.setPassword(password);
                dataSource.setDriverClassName(driverClassName);

                Properties settings = new Properties();
                settings.put("hibernate.dialect", "org.hibernate.dialect.MySQL5Dialect");
                settings.put("hibernate.show_sql", "true");
                settings.put("hibernate.hbm2ddl.auto", "update");
                settings.put("hibernate.connection.datasource", dataSource);

                Configuration configuration = new Configuration();
                configuration.setProperties(settings);
                configuration.addAnnotatedClass(AppendixEntity.class); // 显式添加实体类


                ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                        .applySettings(configuration.getProperties())
                        .build();

                this.sessionFactory = configuration.buildSessionFactory(serviceRegistry);
                log.info("[DFS] init db hibernate success");
            } catch (Throwable var9) {
                throw new ExceptionInInitializerError(var9);
            }
        }
    }


    protected AppendixEntity checkFile(InputStream stream, StoreRequest storeRequest) {
        String md5 = getFileMd5(stream);
        Session session = this.sessionFactory.openSession();
        AppendixEntity appendixEntity = null;
        try {
            // 使用原生 SQL 查询
            String sql = "SELECT * FROM appendix_v WHERE file_md5 = :md5Value";
            SQLQuery query = session.createSQLQuery(sql);
            query.setParameter("md5Value", md5);
            query.addEntity(AppendixEntity.class);
            appendixEntity = (AppendixEntity) query.uniqueResult();

            if (appendixEntity != null && appendixEntity.getId() != null) {
                return appendixEntity;
            } else {
                appendixEntity = new AppendixEntity();
                FileLocation fileLocation = storeRequest.getFileLocation();
                appendixEntity.setFileExt(fileLocation.getFileExt());
                appendixEntity.setDelFlag(0);
                appendixEntity.setFileSize(fileLocation.getSize());
                appendixEntity.setFileName(fileLocation.getName());
                appendixEntity.setOriginalFileName(fileLocation.getOriginalFileName());
                appendixEntity.setFileMd5(md5);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            session.close();
        }
        return appendixEntity;
    }


    protected String getFileMd5(InputStream stream) {
        try {
            // Ensure the stream supports mark/reset
            if (!stream.markSupported()) {
                stream = new BufferedInputStream(stream);
            }

            // Set initial mark at the beginning of the stream
            stream.mark(Integer.MAX_VALUE); // Set a large enough limit for mark/reset

            // Calculate MD5
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = stream.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            byte[] md5Bytes = md.digest();

            // Convert to hexadecimal string
            StringBuilder sb = new StringBuilder();
            for (byte b : md5Bytes) {
                sb.append(String.format("%02x", b));
            }

            // Reset the stream to the initial mark position
            stream.reset();

            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }



    protected void saveOssDb(AppendixEntity appendixEntity){
        Session session = sessionFactory.openSession();
        session.save(appendixEntity);
    }

}
