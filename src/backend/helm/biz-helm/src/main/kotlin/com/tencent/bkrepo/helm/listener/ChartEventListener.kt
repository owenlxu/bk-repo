/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.helm.listener

import com.tencent.bkrepo.common.artifact.event.ArtifactUploadedEvent
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.redis.RedisOperation
import com.tencent.bkrepo.helm.config.HelmProperties
import com.tencent.bkrepo.helm.constants.CHART
import com.tencent.bkrepo.helm.constants.FILE_TYPE
import com.tencent.bkrepo.helm.constants.META_DETAIL
import com.tencent.bkrepo.helm.listener.event.ChartDeleteEvent
import com.tencent.bkrepo.helm.listener.event.ChartVersionDeleteEvent
import com.tencent.bkrepo.helm.listener.operation.ChartDeleteOperation
import com.tencent.bkrepo.helm.listener.operation.ChartPackageDeleteOperation
import com.tencent.bkrepo.helm.listener.operation.ChartUploadOperation
import com.tencent.bkrepo.helm.service.impl.AbstractChartService
import com.tencent.bkrepo.helm.utils.HelmMetadataUtils
import com.tencent.bkrepo.helm.utils.HelmUtils
import com.tencent.bkrepo.helm.utils.ObjectBuilderUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ChartEventListener(
    private val redisOperation: RedisOperation,
    private val helmProperties: HelmProperties
) : AbstractChartService() {

    /**
     * 删除chart版本，更新index.yaml文件
     */
    @EventListener(ChartVersionDeleteEvent::class)
    fun handle(event: ChartVersionDeleteEvent) {
        // 如果index.yaml文件不存在，说明还没有初始化该文件，return
        // 如果index.yaml文件存在，则进行更新
        with(event.request) {
            logger.info("handling chart version delete event for [$name@$version] in repo [$projectId/$repoName]")
            if (!exist(projectId, repoName, HelmUtils.getIndexCacheYamlFullPath())) {
                logger.warn("Index yaml file is not initialized in repo [$projectId/$repoName], return.")
                return
            }
            val task = ChartDeleteOperation(event.request, redisOperation)
            threadPoolExecutor.submit(task)
        }
    }


    /**
     * 删除chart的package，更新index.yaml文件
     */
    @EventListener(ChartDeleteEvent::class)
    fun handle(event: ChartDeleteEvent) {
        with(event.requestPackage) {
            logger.info("Handling package delete event for [$name] in repo [$projectId/$repoName]")
            if (!exist(projectId, repoName, HelmUtils.getIndexCacheYamlFullPath())) {
                logger.info(
                    "Index yaml file is not initialized in repo [$projectId/$repoName], refresh index.yaml interrupted."
                )
                return
            }
            val task = ChartPackageDeleteOperation(event.requestPackage, redisOperation)
            threadPoolExecutor.submit(task)
        }
    }



    /**
     * Chart文件上传成功后，进行后续操作，如创建package/packageVersion
     */
    @EventListener(ArtifactUploadedEvent::class)
    fun handle(event: ArtifactUploadedEvent) {
        with(event) {
            initPackageInfo(context)
            handleChartUploadEvent(context)
        }
    }

    /**
     * 当chart新上传成功后，更新index.yaml
     */
    private fun handleChartUploadEvent(context: ArtifactUploadContext) {
        with(context) {
            if (CHART != getStringAttribute(FILE_TYPE)) return
            val helmChartMetadataMap = getAttribute<Map<String, Any>?>(META_DETAIL)
            helmChartMetadataMap?.let {
                val helmChartMetadata = HelmMetadataUtils.convertToObject(helmChartMetadataMap)
                val request = ObjectBuilderUtil.buildChartUploadRequest(userId, artifactInfo, helmChartMetadata)
                val task = ChartUploadOperation(request, redisOperation, helmChartMetadata, helmProperties.domain)
                threadPoolExecutor.submit(task)
            }

        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ChartEventListener::class.java)
    }
}
