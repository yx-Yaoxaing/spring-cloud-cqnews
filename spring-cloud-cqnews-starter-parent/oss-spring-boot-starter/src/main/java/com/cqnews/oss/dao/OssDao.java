package com.cqnews.oss.dao;


import com.cqnews.oss.domain.OssVO;

import java.util.List;

/**
 * @author uYUuu
 */


public interface OssDao {

    /**
     * 根据id查询
     * @param ids
     * @return
     */
    List<OssVO> findByIds(String ids);




}
