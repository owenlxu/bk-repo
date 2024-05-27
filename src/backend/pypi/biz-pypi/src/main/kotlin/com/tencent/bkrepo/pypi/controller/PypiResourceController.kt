/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.pypi.controller

import com.tencent.bkrepo.common.artifact.api.ArtifactFileMap
import com.tencent.bkrepo.pypi.artifact.PypiArtifactInfo
import com.tencent.bkrepo.pypi.artifact.PypiSimpleArtifactInfo
import com.tencent.bkrepo.pypi.service.PypiService
import io.swagger.annotations.Api
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

/**
 * pypi服务接口实现类
 */
@Api("pypi client api")
@RestController
class PypiResourceController(
    private var pypiService: PypiService
) {
    @PostMapping(PypiArtifactInfo.PYPI_ROOT_POST_URI)
    fun upload(pypiArtifactInfo: PypiArtifactInfo, artifactFileMap: ArtifactFileMap) {
        logger.info("upload pypi package: $pypiArtifactInfo")
        pypiService.upload(pypiArtifactInfo, artifactFileMap)
    }

    @PostMapping(
        PypiArtifactInfo.PYPI_ROOT_POST_URI,
        consumes = [MediaType.TEXT_XML_VALUE],
        produces = [MediaType.TEXT_XML_VALUE]
    )
    fun search(pypiArtifactInfo: PypiArtifactInfo): String {
        logger.info("search pypi package: $pypiArtifactInfo")
        return pypiService.search(pypiArtifactInfo)
    }

    @GetMapping(
        "/{projectId}/{repoName}/simple", "/{projectId}/{repoName}/simple/{name}",
        produces = [MediaType.TEXT_HTML_VALUE]
    )
    fun simple(artifactInfo: PypiSimpleArtifactInfo): Any? {
        logger.info("simple pypi package: $artifactInfo")
        return pypiService.simple(artifactInfo)
    }

    @GetMapping(PypiArtifactInfo.PYPI_PACKAGES_MAPPING_URI)
    fun packages(artifactInfo: PypiArtifactInfo) {
        logger.info("packages pypi package: $artifactInfo")
        pypiService.packages(artifactInfo)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PypiResourceController::class.java)
    }
}
