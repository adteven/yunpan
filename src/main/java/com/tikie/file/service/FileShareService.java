package com.tikie.file.service;

import com.tikie.file.model.FileShare;

/**
 * @author zhangshitai
 * @date 2018-07-31
 */
public interface FileShareService {

    Boolean insertSelective(String fileTreeIds);

    FileShare selectByCode(String code);
}
