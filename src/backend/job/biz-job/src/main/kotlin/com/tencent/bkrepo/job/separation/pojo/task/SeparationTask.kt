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

package com.tencent.bkrepo.job.separation.pojo.task

import com.tencent.bkrepo.job.separation.model.TSeparationTask
import com.tencent.bkrepo.job.separation.pojo.SeparationContent
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("数据降冷任务")
data class SeparationTask(
    @ApiModelProperty("ID")
    val id: String? = null,
    @ApiModelProperty("创建人")
    val createdBy: String,
    @ApiModelProperty("创建时间")
    val createdDate: LocalDateTime,
    @ApiModelProperty("最后修改人")
    val lastModifiedBy: String,
    @ApiModelProperty("最后修改时间")
    val lastModifiedDate: LocalDateTime,
    @ApiModelProperty("降冷临界时间")
    val separationDate: LocalDateTime,
    @ApiModelProperty("任务开始执行的时间")
    val startDate: LocalDateTime? = null,
    @ApiModelProperty("任务结束执行的时间")
    val endDate: LocalDateTime? = null,
    @ApiModelProperty("已降冷成功的制品数")
    val successCount: Long = 0,
    @ApiModelProperty("降冷失败的制品数")
    val failedCount: Long = 0,
    @ApiModelProperty("跳过的制品数")
    val skippedCount: Long = 0,
    @ApiModelProperty("降冷任务所属项目")
    val projectId: String,
    @ApiModelProperty("降冷项目所属仓库")
    val repoName: String,
    @ApiModelProperty("任务状态")
    val state: String,
    @ApiModelProperty("任务内容")
    val content: SeparationContent,
) {
    companion object {
        fun TSeparationTask.toDto() = SeparationTask(
            id = id,
            createdBy = createdBy,
            createdDate = createdDate,
            lastModifiedBy = lastModifiedBy,
            lastModifiedDate = lastModifiedDate,
            startDate = startDate,
            endDate = endDate,
            successCount = totalCount?.successCount ?: 0,
            failedCount = totalCount?.failedCount ?: 0,
            skippedCount = totalCount?.skippedCount ?: 0,
            projectId = projectId,
            repoName = repoName,
            state = state,
            content = content,
            separationDate = separationDate
        )
    }
}
