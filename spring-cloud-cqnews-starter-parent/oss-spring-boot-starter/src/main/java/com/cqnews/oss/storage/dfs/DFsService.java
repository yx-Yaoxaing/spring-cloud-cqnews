package com.cqnews.oss.storage.dfs;

import com.cqnews.oss.entiy.AppendixEntity;

import java.io.IOException;

/**
 * @author uYxUuu
 * @Date 2024/6/26
 */
public interface DFsService {


    /**
     * 存储文件
     * @param storeRequest 存储请求
     * @throws IOException 异常
     */
    AppendixEntity store(StoreRequest storeRequest) throws IOException;


}
