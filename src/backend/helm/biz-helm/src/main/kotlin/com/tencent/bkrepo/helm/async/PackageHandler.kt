/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.helm.async

import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.helm.artifact.HelmArtifactInfo
import com.tencent.bkrepo.helm.constants.NAME
import com.tencent.bkrepo.helm.constants.VERSION
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class PackageHandler {
    @Autowired
    private lateinit var packageClient: PackageClient

    /**
     * 创建包版本
     */
    @Async
    fun createVersion(
        userId: String,
        artifactInfo: HelmArtifactInfo,
        chartInfo: Map<String, Any>,
        size: Long
    ) {
        val name = chartInfo[NAME] as String
        val description = chartInfo["description"] as? String
        val version = chartInfo[VERSION] as String
        val contentPath = getContentPath(name, version)
        with(artifactInfo) {
            val packageVersionCreateRequest =
                PackageVersionCreateRequest(
                    projectId,
                    repoName,
                    name,
                    PackageKeys.ofHelm(name),
                    PackageType.HELM,
                    description,
                    version,
                    size,
                    null,
                    contentPath,
                    null,
                    null,
                    false,
                    userId
                )
            packageClient.createVersion(packageVersionCreateRequest).apply {
                logger.info("user: [$userId] create package version [$packageVersionCreateRequest] success!")
            }
        }
    }

    /**
     * 删除包
     */
    @Async
    fun deletePackage(userId: String, name: String, artifactInfo: HelmArtifactInfo) {
        val packageKey = PackageKeys.ofHelm(name)
        with(artifactInfo) {
            packageClient.deletePackage(projectId, repoName, packageKey).apply {
                logger.info("user: [$userId] delete package [$name] in repo [$projectId/$repoName] success!")
            }
        }
    }

    /**
     * 删除版本
     */
    @Async
    fun deleteVersion(userId: String, name: String, version: String, artifactInfo: HelmArtifactInfo) {
        val packageKey = PackageKeys.ofHelm(name)
        with(artifactInfo) {
            packageClient.deleteVersion(projectId, repoName, packageKey, version).apply {
                logger.info("user: [$userId] delete package [$name] with version [$version] in repo [$projectId/$repoName] success!")
            }
        }
    }

    fun getContentPath(name: String, version: String): String {
        return String.format("/%s-%s.tgz", name, version)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PackageHandler::class.java)
    }
}