package com.tikie.file.service;

import com.github.pagehelper.Page;
import com.tikie.file.model.FileTree;
import com.tikie.file.model.SuperTreeVo;
import com.tikie.file.model.TFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * @author zhangshitai
 * @date 2018-07-23
 */
public interface TFileTreeService {
    Boolean insert(FileTree record);

    Boolean insertSelective(FileTree record);

    Boolean uploadFile(Map<String, MultipartFile> files, String baseFilePath, String pid);

    Page<SuperTreeVo> selectListTreeBySuper(int pageNo, int pageSize);
}
