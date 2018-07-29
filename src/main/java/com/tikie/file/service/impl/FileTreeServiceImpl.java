package com.tikie.file.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tikie.file.dao.FileTreeMapper;
import com.tikie.file.model.FileTree;
import com.tikie.file.model.SuperTreeVo;
import com.tikie.file.model.TFile;
import com.tikie.file.service.TFileService;
import com.tikie.file.service.FileTreeService;
import com.tikie.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhangshitai
 * @date 2018-07-23
 */
@Service
@Transactional(propagation=Propagation.NOT_SUPPORTED)
public class FileTreeServiceImpl implements FileTreeService {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private FileTreeMapper fileTreeMapper;

    @Resource
    private TFileService tFileService;

    @Override
    public Boolean insert(FileTree record) {
        return null;
    }

    @Override
    @Transactional(propagation=Propagation.REQUIRED,rollbackFor=Exception.class)
    public Boolean insertSelective(FileTree record) {
        int state = 0;
        try{
            FileTree tree = new FileTree();
            tree.setId(UUIDUtil.getUUID());
            state =  fileTreeMapper.insertSelective(record);
            logger.info("insertSelective@exec:{}",tree);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("insertSelective@err:{}",e);
        }
        return state > 0;
    }

    @Transactional(propagation=Propagation.REQUIRED,rollbackFor=Exception.class)
    public Boolean uploadFile(Map<String, MultipartFile> files, String baseFilePath , String pid){
        int state = 0;
        List<Map<String, Object>> handle = new ArrayList<>();
        for (MultipartFile item : files.values()) {
            String fileName = item.getOriginalFilename();    // 当前上传文件全名称
            String fileType = item.getContentType();         // 当前上传文件类型
            String addr = baseFilePath + fileName;         // 保存到服务器目录的文件全路径
            long size = item.getSize();                    // 文件大小

            logger.info("文件名称：{}", fileName);
            logger.info("文件类型：{}", fileType);
            logger.info("文件物理地址：{}", addr);
            logger.info("文件大小：{}", FileSizeUtil.FormetFileSize(size, FileSizeUtil.SIZETYPE_MB) + "Mb");

            File savedFile = new File(baseFilePath, fileName);
            String md5 = MD5Util.getFileMD5(savedFile);
            logger.info("文件md5：{}", md5);
            String fileId = StringUtils.EMPTY;
            Boolean hasMd5 = tFileService.checkMd5FromDB(md5);

            // 文件已存在,不需要保存文件
            if (hasMd5) {
                fileId = tFileService.selectIdByMd5(md5);

                //  更新文件树到数据库
                FileTree tree = new FileTree();
                tree.setId(UUIDUtil.getUUID());
                tree.setIsFile(true);
                tree.setName(fileName);
                tree.setFileId(fileId);
                tree.setPid("#"); // TODO
                tree.setSize(size);
                state = fileTreeMapper.insertSelective(tree);
                continue;
            }

            fileId = UUIDUtil.getUUID();
            TFile file = new TFile(fileId, fileName, "#", size, addr, fileType, md5);
            // 需要保存文件
            // 查询目录下是否存在同名文件
            Boolean hasFile = FileUtil.checkFileIsExists(addr);

            if (!hasFile) {// 不存在同名文件
                try {
                    item.transferTo(savedFile);// 保文件到服务器物理位置
                    // 更新
                    Boolean isSa = tFileService.insertSelective(file);
                    logger.info("insertSelective@exec:{}",isSa);
                } catch (IOException | IllegalStateException e) {
                    logger.error(e.getMessage());
                    Map<String, Object> failedFile = new HashMap<>();
                    failedFile.put("name", fileName);
                    failedFile.put("size", FileSizeUtil.FormetFileSize(size, FileSizeUtil.SIZETYPE_MB) + "Mb"); // 转化单位
                    handle.add(failedFile);
                    continue;
                }
            }
            // 存在同名文件
            String subfix = StringUtils.substringAfterLast(fileName, ".");
            String savedName = UUIDUtil.getUUID() + "." + subfix;
            File savedFil = new File(baseFilePath, savedName);
            try {
                item.transferTo(savedFil);// 保存
                // 更新文件数据到数据库
                file.setName(savedName);
                Boolean isSa = tFileService.insertSelective(file);
                logger.info("insertSelective@exec:{}",isSa);
            } catch (IOException | IllegalStateException e) {
                logger.error(e.getMessage());
                Map<String, Object> failedFile = new HashMap<>();
                failedFile.put("name", fileName);
                failedFile.put("size", size); // 转化单位
                handle.add(failedFile);
                continue;
            }

            FileTree tree = new FileTree();
            tree.setId(UUIDUtil.getUUID());
            tree.setName(fileName);
            tree.setFileId(fileId);
            tree.setIsFile(true);
            tree.setPid("#");//TODO
            tree.setSize(size);
            // 更新文件树 到数据库
            state = fileTreeMapper.insertSelective(tree);
        }
        return state > 0;
    }

    @Transactional(propagation=Propagation.REQUIRED,rollbackFor=Exception.class)
    @Override
    public Boolean delete(String id) {
        int state = 0;
        try{
            state =  fileTreeMapper.delete(id);
            logger.info("removeFile@exec:{}",state);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("removeFile@err:{}",e);
        }
        return state > 0;
    }

    @Transactional(propagation=Propagation.REQUIRED,rollbackFor=Exception.class)
    @Override
    public PageInfo<SuperTreeVo> selectListTreeBySuper(int pageNo, int pageSize) {
        Page<SuperTreeVo> pages = null;
        PageInfo<SuperTreeVo> pageInfo = null;
        try {
            PageHelper.startPage(pageNo, pageSize);
            pages = fileTreeMapper.selectListTreeBySuper();
            pageInfo = new PageInfo<>(pages);
            logger.info("==== selectListTreeBySuper@exec:{} ====", pageInfo);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("==== selectListTreeBySuper@err:{} ====", e);
        }
        return pageInfo;
    }

    @Transactional(propagation=Propagation.REQUIRED,rollbackFor=Exception.class)
    @Override
    public FileTree selectFileTreeById(String id) {
        FileTree fileTree = null;
        try {
            fileTree = fileTreeMapper.selectFileTreeById(id);
            logger.info("==== selectFileTreeById@exec:{} ====", fileTree);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("==== selectFileTreeById@err:{} ====", e);
        }
        return fileTree;
    }

    @Transactional(propagation=Propagation.REQUIRED,rollbackFor=Exception.class)
    @Override
    public Boolean deleteFileTreeByOneId(String id) {
        int state = 0;
        try {
            FileTree fileTree = fileTreeMapper.selectFileTreeById(id);
            fileTree.setReback(fileTree.getFileId());
            state = fileTreeMapper.deleteFileTreeByOneId(fileTree);
            logger.info("==== deleteFileTreeByOneId@exec:{} ====", state);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("==== deleteFileTreeByOneId@err:{} ====", e);
        }
        return state > 0;
    }

    @Transactional(propagation=Propagation.REQUIRED,rollbackFor=Exception.class)
    @Override
    public Boolean reanameFileTreeByOneId(String id, String name) {
        int state = 0;
        try {
            FileTree fileTree = fileTreeMapper.selectFileTreeById(id);
            String type = StringUtils.substringAfterLast(fileTree.getName(),".");
            name = name + "." + type;
            fileTree.setName(name);
            state = fileTreeMapper.reanameFileTreeByOneId(fileTree);
            logger.info("==== deleteFileTreeByOneId@exec:{} ====", state);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("==== deleteFileTreeByOneId@err:{} ====", e);
        }
        return state > 0;
    }

    @Transactional(propagation=Propagation.REQUIRED,rollbackFor=Exception.class)
    @Override
    public Boolean createNewFolder(String name, String pid) {
        int state = 0;
        try{
            FileTree fileTree = new FileTree();
            fileTree.setId(UUIDUtil.getUUID());
            fileTree.setIsFile(false);
            fileTree.setName(name);
            fileTree.setPid(pid);
            fileTree.setCtime("now");
            fileTree.setUtime("now");
            state =  fileTreeMapper.insertSelective(fileTree);
            logger.info("createNewFolder@exec:{}",state);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("createNewFolder@err:{}",e);
        }
        return state > 0;
    }

    @Transactional(propagation=Propagation.REQUIRED,rollbackFor=Exception.class)
    @Override
    public Boolean copyFile(String id, String pid) {
        int state = 0;
        try{
            FileTree fileTree = fileTreeMapper.selectFileTreeById(id);
            fileTree.setId(UUIDUtil.getUUID());
            if (!pid.equals(fileTree.getPid())){
                fileTree.setPid(pid);
            }
            fileTree.setUtime("now");
            state =  fileTreeMapper.insertSelective(fileTree);
            logger.info("copyFile@exec:{}",state);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("copyFile@err:{}",e);
        }
        return state > 0;
    }

    @Transactional(propagation=Propagation.REQUIRED,rollbackFor=Exception.class)
    @Override
    public Boolean removeFile(String id, String pid) {
        int state = 0;
        try{
            FileTree fileTree = fileTreeMapper.selectFileTreeById(id);
            fileTree.setId(UUIDUtil.getUUID());
            if (!pid.equals(fileTree.getPid())){
                fileTree.setPid(pid);
            }
            fileTree.setUtime("now");
            state =  fileTreeMapper.insertSelective(fileTree);
            if (state > 0){
                fileTreeMapper.delete(id);
            }
            logger.info("copyFile@exec:{}",state);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("copyFile@err:{}",e);
        }
        return state > 0;
    }

    @Transactional(propagation=Propagation.REQUIRED,rollbackFor=Exception.class)
    @Override
    public Map<String,Object> detail(String id) {
        Map<String,Object> map = new HashMap<>();
        try {
            FileTree fileTree = fileTreeMapper.selectFileTreeById(id);
            TFile tFile = tFileService.selectByPrimaryKey(fileTree.getFileId());
            map.put("类型",StringUtils.substringAfterLast(fileTree.getName(),".") + "文件");
            map.put("大小",fileTree.getSize());
            map.put("位置",tFile.getPath());
            map.put("修改时间",fileTree.getUtime());
            logger.info("==== detail@exec:{} ====", map);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("==== detail@err:{} ====", e);
        }
        return map;
    }

    // 下载指定文件
    @Override
    public Boolean downloads(String fileId, HttpServletRequest request, HttpServletResponse response) {

        int state = 0;
        String[] fileIds = fileId.split(",");
        // 打包路径
        String realPath = request.getRealPath("/");
        String[] filePath = new String[fileIds.length];

        for (int i = 0; i < fileIds.length; i++) {
            String id = fileIds[i];
            TFile tFile = tFileService.selectByPrimaryKey(id);
            // 原文件
            String srcFile = tFile.getPath() + tFile.getName();
            // 临时目录
            String destPath = realPath;
            try {
                File file = new File(srcFile);
                //  将文件拷贝到项目根目录
                FileUtils.copyFileToDirectory(file,new File(destPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 文件的临时路径
            filePath[i] = destPath + tFile.getName() + "." + tFile.getType();
        }

        String downloadFilePath = filePath[0];
        if (fileIds.length > 1) {
            // 打包
            String zipPath = realPath + "Files.zip";
            ZipUtil.files2Zip(filePath, zipPath);
            downloadFilePath = zipPath;
        }
        // 下载
        try {
            DownloadUtil.downloadLocal(downloadFilePath,request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return state > 0;

    }
}