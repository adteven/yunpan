package com.tikie.file.controller;

import com.tikie.common.ExceptionConstant;
import com.tikie.file.model.FileShare;
import com.tikie.file.service.FileShareService;
import com.tikie.util.Result;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author zhangshitai
 * @date 2018-07-31
 */
@RestController
@RequestMapping("/file-share")
public class FileShareController {

    private static Logger logger = LoggerFactory.getLogger(FileController.class);

    @Resource
    private FileShareService fileShareService;


    @ApiOperation(value = "插入分享码")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "fileTreeIds", value = "文件树id, 多个用 ','分隔拼接", dataType = "String", paramType = "query", required = true)
    })
    @PostMapping("/shareCode")
    public Result<String> addShareCode(@RequestParam(value = "fileTreeIds") String fileTreeIds){
        if (StringUtils.isBlank(fileTreeIds)){
            return Result.fail(ExceptionConstant.PARAM_IS_NULL);
        }
        try {
            boolean code = fileShareService.insertSelective(fileTreeIds);
            if (code){
                return Result.success("success");
            }
            return Result.fail(ExceptionConstant.TFILE_INSERT_FAIL);
        }catch (Exception e){
            logger.error("insertSelective@err:{}",e);
            return Result.fail(ExceptionConstant.TFILE_INSERT_FAIL);
        }
    }

    @ApiOperation(value = "code")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "code", value = "code", dataType = "String", paramType = "query", required = true)
    })
    @PostMapping("/code")
    public Result<FileShare> selectByCode(@RequestParam(value = "code") String code){
        if (StringUtils.isBlank(code)){
            return Result.fail(ExceptionConstant.PARAM_IS_NULL);
        }
        try {
            FileShare fileShare = fileShareService.selectByCode(code);
            logger.info("selectFileTreeById@exec:{}",fileShare);
            return Result.success(fileShare);
        }catch (Exception e){
            logger.error("insertSelective@err:{}",e);
            return Result.fail(ExceptionConstant.TFILE_SELECT_FAIL);
        }
    }
}
