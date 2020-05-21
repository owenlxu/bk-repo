package com.tencent.bkrepo.npm.pojo.module.des.service

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("创建依赖关系请求")
data class DepsCreateRequest(
    @ApiModelProperty("所属项目", required = true)
    val projectId: String,
    @ApiModelProperty("仓库名称", required = true)
    val repoName: String,
    @ApiModelProperty("资源名称", required = true)
    val name: String,
    @ApiModelProperty("被依赖资源名称", required = true)
    val deps: String,
    @ApiModelProperty("操作用户", required = false)
    val operator: String = "system",
    @ApiModelProperty("是否覆盖", required = false)
    val overwrite: Boolean = false
)
