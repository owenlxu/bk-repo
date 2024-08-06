/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2024 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.common.artifact.cache.pojo

import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("预加载策略创建请求")
data class ArtifactPreloadStrategyCreateRequest(
    @ApiModelProperty("策略所属项目")
    val projectId: String,
    @ApiModelProperty("策略所属仓库")
    val repoName: String,
    @ApiModelProperty("文件路径正则，匹配成功才会执行预加载")
    val fullPathRegex: String,
    @ApiModelProperty("仅加载大于指定大小的文件")
    val minSize: Long,
    @ApiModelProperty("限制只对最近一段时间内创建的制品执行预加载")
    val recentSeconds: Long,
    @ApiModelProperty("预加载执行时间")
    val preloadCron: String? = null,
    @ApiModelProperty("策略类型")
    val type: String = PreloadStrategyType.CUSTOM.name,
    @ApiModelProperty("操作人")
    val operator: String = SYSTEM_USER,
)
