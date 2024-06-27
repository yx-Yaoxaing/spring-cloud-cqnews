package com.cqnews.oss.storage.impl;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.cqnews.oss.entiy.AppendixEntity;
import com.cqnews.oss.spring.condition.PropertyAndOneBeanCondition;
import com.cqnews.oss.storage.AbstractDFsService;
import com.cqnews.oss.storage.dfs.DFsService;
import com.cqnews.oss.storage.dfs.FileLocation;
import com.cqnews.oss.storage.dfs.StoreRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 *
 * minio分布式存储实现
 * 关于几个属性字段的解释
 * 1.domain 在业务开发中回显的url可能与minio地址不一样 可能是内网上传 互联网代理显示 这里就填写代理地址即可
 * 2.为什么这里要求填写数据库地址 可能服务使用的db和minio存储的db不一致
 *
 * cqnews.storage.dfs.minio.endpoint = "your endpoint"
 * cqnews.storage.dfs.minio.bucketName = "your bucketName"
 * cqnews.storage.dfs.minio.accessKey = "your accessKey"
 * cqnews.storage.dfs.minio.secretKey = "your secretKey"
 * cqnews.storage.dfs.minio.domain = "your domain"
 * cqnews.storage.dfs.minio.url = "your url"
 * cqnews.storage.dfs.minio.password = "your password"
 * cqnews.storage.dfs.minio.username = "your password"
 * cqnews.storage.dfs.minio.driverClassName = "your driverClassName"
 *
 *cqnews:
 *   storage:
 *     dfs:
 *       minio:
 *         endpoint: your endpoint
 *         bucketName: your bucketName
 *         accessKey: your accessKey
 *         secretKey: your secretKey
 *         domain: your domain
 *         url: your url
 *         password: your password
 *         driverClassName: your driverClassName
 * @author uYxUuu
 * @Date 2024/6/26
 */
@Slf4j
public class MinioOssService extends AbstractDFsService {
    private static  String TYPE_MINIO = "minio";
    private static  String KEY_ENDPOINT = "endpoint";
    private static  String KEY_BUCKET_NAME = "bucketName";
    private static  String ACCESS_KEY = "accessKey";
    private static  String SECRET_KEY = "secretKey";

    private static  String URL = "url";

    private static  String USER_NAME = "username";

    private static  String PASSWORD = "password";

    private static  String DRIVER_CLASS_NAME = "driverClassName";

    private static String DOMAIN = "domain";

    private String domain;

    private AmazonS3 amazonS3;
    private String bucket;
    private static final String NOT_FOUNT = "404 Not Found";
    /**
     * 具体实现存储
     * @param storeRequest 存储请求
     * @throws IOException
     */
    @Override
    public AppendixEntity store(StoreRequest storeRequest) throws IOException {
        storeRequest.setFileInputStream(new BufferedInputStream(storeRequest.getFileInputStream()));
        AppendixEntity appendixEntity = checkFile(storeRequest.getFileInputStream(),storeRequest);
        if (appendixEntity != null && appendixEntity.getId()!= null) {
            return appendixEntity;
        }
        try {
            String fileName = parseFileName(storeRequest.getFileLocation());
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(storeRequest.getFileLocation().getContentType());
            objectMetadata.setContentLength(storeRequest.getFileLocation().getSize());
            // 创建 PutObjectRequest 对象
            PutObjectRequest request = new PutObjectRequest(this.bucket, fileName,  storeRequest.getFileInputStream(),objectMetadata);
            amazonS3.putObject(request);

            appendixEntity.setFileUrl(domain + "/" +  bucket + "/" + fileName);
            appendixEntity.setBucketName(bucket);
            // 存入db
            saveOssDb(appendixEntity);
            return appendixEntity;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
    private  String parseFileName(FileLocation fileLocation) {
        return String.format("%s/%s", this.bucket, fileLocation.getName());
    }
    @Override
    public void destroy() throws Exception {

    }

    @Override
    protected void init(ApplicationContext applicationContext) {
        Environment environment = applicationContext.getEnvironment();

        String endpoint = fetchProperty(environment, TYPE_MINIO, KEY_ENDPOINT);
        String bucketName = fetchProperty(environment, TYPE_MINIO, KEY_BUCKET_NAME);
        String accessKey = fetchProperty(environment, TYPE_MINIO, ACCESS_KEY);
        String secretKey = fetchProperty(environment, TYPE_MINIO, SECRET_KEY);
         domain = fetchProperty(environment,TYPE_MINIO,DOMAIN);
        // db
         String url = fetchProperty(environment, TYPE_MINIO, URL);
        String username = fetchProperty(environment, TYPE_MINIO, USER_NAME);
        String password = fetchProperty(environment, TYPE_MINIO, PASSWORD);
        String driverClassName = fetchProperty(environment, TYPE_MINIO, DRIVER_CLASS_NAME);
        try {
            initOssClient(endpoint, bucketName, accessKey, secretKey);
        } catch (Exception e) {

        }
        initDb(url,password,username,driverClassName);
    }
    /**
     * 创建minio连接并且创建桶
     *
     * @param endpoint   端口
     * @param bucketName 桶名
     * @param accessKey  访问密钥
     * @param secretKey  秘密密钥
     */
    public void initOssClient(String endpoint, String bucketName, String accessKey, String secretKey) {
        log.info("[Minio] init OSS by config: endpoint={}, bucketName={}, accessKey={}, secretKey={}", endpoint, bucketName, accessKey, secretKey);
        if (StringUtils.isEmpty(bucketName)) {
            throw new IllegalArgumentException("'oms.storage.dfs.minio.bucketName' can't be empty, please creat a bucket in minio oss console then config it to powerjob");
        }

        // 创建凭证对象
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);

        // 创建AmazonS3客户端并指定终端节点和凭证
        this.amazonS3 = AmazonS3ClientBuilder.standard()
                // 当使用 AWS Java SDK 连接到非AWS服务（如MinIO）时，指定区域（Region）是必需的，即使这个区域对于你的MinIO实例并不真正适用。原因在于AWS SDK的客户端构建器需要一个区域来配置其服务端点，即使在连接到本地或第三方S3兼容服务时也是如此。使用 "us-east-1" 作为占位符是很常见的做法，因为它是AWS最常用的区域之一。这不会影响到实际的连接或数据传输，因为真正的服务地址是由你提供的终端节点URL决定的。如果你的代码主要是与MinIO交互，并且不涉及AWS服务，那么这个区域设置只是形式上的要求。
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, "us-east-1"))
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withPathStyleAccessEnabled(true) // 重要：启用路径样式访问
                .build();
        this.bucket = bucketName;
        createBucket(bucketName);
        log.info("[Minio] initialize OSS successfully!");
    }


    /**
     * 创建 bucket
     *
     * @param bucketName 桶名
     */
    @SneakyThrows(Exception.class)
    public void createBucket(String bucketName) {

        // 建议自行创建 bucket，设置好相关的策略
        if (bucketExists(bucketName)) {
            return;
        }

        Bucket createBucketResult = amazonS3.createBucket(bucketName);
        log.info("[Minio] createBucket successfully, bucketName: {}, createResult: {}", bucketName, createBucketResult);

        String policy = "{\n" +
                "    \"Version\": \"2012-10-17\",\n" +
                "    \"Statement\": [\n" +
                "        {\n" +
                "            \"Action\": [\n" +
                "                \"s3:GetObject\"\n" +
                "            ],\n" +
                "            \"Effect\": \"Allow\",\n" +
                "            \"Principal\": {\n" +
                "                \"AWS\": [\n" +
                "                    \"*\"\n" +
                "                ]\n" +
                "            },\n" +
                "            \"Resource\": [\n" +
                "                \"arn:aws:s3:::" + bucketName + "/*\"\n" +
                "            ]\n" +
                "        }\n" +
                "    ]\n" +
                "}";
        try {
            amazonS3.setBucketPolicy(bucketName, policy);
        } catch (Exception e) {
            log.warn("[Minio] setBucketPolicy failed, maybe you need to setBucketPolicy by yourself!", e);
        }
    }

    /**
     * 判断 bucket是否存在
     *
     * @param bucketName: 桶名
     * @return boolean
     */
    @SneakyThrows(Exception.class)
    public boolean bucketExists(String bucketName) {

        return amazonS3.doesBucketExistV2(bucketName);
    }

    public static class MinioOssCondition extends PropertyAndOneBeanCondition {

        @Override
        protected List<String> anyConfigKey() {
            return Arrays.asList("cqnews.storage.dfs.minio.endpoint");
        }

        @Override
        protected Class<?> beanType() {
            return DFsService.class;
        }
    }
}
