package com.cqnews.oss.storage.dfs;

import lombok.Data;

import java.io.File;
import java.io.InputStream;

/**
 * @author uYxUuu
 * @Date 2024/6/26
 */
@Data
public class StoreRequest {

    private transient InputStream fileInputStream;

    private FileLocation fileLocation;
}
