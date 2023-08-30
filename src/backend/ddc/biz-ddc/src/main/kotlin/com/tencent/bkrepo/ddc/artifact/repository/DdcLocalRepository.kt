/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2023 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.ddc.artifact.repository

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.exception.BadRequestException
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.ddc.artifact.CompressedBlobArtifactInfo
import com.tencent.bkrepo.ddc.artifact.ReferenceArtifactInfo
import com.tencent.bkrepo.ddc.exception.NotImplementedException
import com.tencent.bkrepo.ddc.exception.PartialReferenceResolveException
import com.tencent.bkrepo.ddc.exception.ReferenceIsMissingBlobsException
import com.tencent.bkrepo.ddc.pojo.Blob
import com.tencent.bkrepo.ddc.pojo.Reference
import com.tencent.bkrepo.ddc.pojo.UploadCompressedBlobResponse
import com.tencent.bkrepo.ddc.serialization.CbObject
import com.tencent.bkrepo.ddc.service.BlobService
import com.tencent.bkrepo.ddc.service.ReferenceResolver
import com.tencent.bkrepo.ddc.service.ReferenceService
import com.tencent.bkrepo.ddc.utils.BlakeUtils.blake3
import com.tencent.bkrepo.ddc.utils.BlakeUtils.hex
import com.tencent.bkrepo.ddc.utils.MEDIA_TYPE_JUPITER_INLINED_PAYLOAD
import com.tencent.bkrepo.ddc.utils.MEDIA_TYPE_UNREAL_COMPACT_BINARY
import com.tencent.bkrepo.ddc.utils.MEDIA_TYPE_UNREAL_UNREAL_COMPRESSED_BUFFER
import com.tencent.bkrepo.ddc.utils.NODE_METADATA_KEY_BLOB_ID
import com.tencent.bkrepo.ddc.utils.NODE_METADATA_KEY_CONTENT_ID
import com.tencent.bkrepo.ddc.utils.hasAttachments
import com.tencent.bkrepo.ddc.utils.isAttachment
import com.tencent.bkrepo.ddc.utils.isBinaryAttachment
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.time.format.DateTimeFormatter

@Component
class DdcLocalRepository(
    private val referenceService: ReferenceService,
    private val refResolver: ReferenceResolver,
    private val blobService: BlobService,
) : LocalRepository() {
    override fun onUploadBefore(context: ArtifactUploadContext) {
        super.onUploadBefore(context)
        var uploadBlake3: String? = null
        val artifactInfo = context.artifactInfo
        if (artifactInfo is ReferenceArtifactInfo) {
            uploadBlake3 = artifactInfo.inlineBlobHash!!
        }
        if (uploadBlake3 != null && uploadBlake3 != context.getStreamArtifactFile().blake3().hex()) {
            throw ErrorCodeException(ArtifactMessageCode.DIGEST_CHECK_FAILED, "blake3")
        }
    }

    override fun onUpload(context: ArtifactUploadContext) {
        val artifactInfo = context.artifactInfo
        if (artifactInfo is ReferenceArtifactInfo) {
            onUploadReference(context)
        } else {
            onUploadBlob(context)
        }
    }

    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        val artifactInfo = context.artifactInfo
        return if (artifactInfo is ReferenceArtifactInfo) {
            onDownloadReference(context)
        } else {
            onDownloadBlob(context)
        }
    }

    fun finalizeRef(artifactInfo: ReferenceArtifactInfo) {
        with(artifactInfo) {
            val ref = referenceService.getReference(projectId, repoName, bucket, refId.toString())
                ?: throw BadRequestException(CommonMessageCode.PARAMETER_INVALID, "No blob when attempting to finalize")
            val cbObject = CbObject(ByteBuffer.wrap(ref.inlineBlob!!))
            if (ref.blobId!!.toString() != artifactInfo.inlineBlobHash) {
                throw ErrorCodeException(ArtifactMessageCode.DIGEST_CHECK_FAILED, "blake3")
            }
            val res = referenceService.finalize(ref, cbObject)
            HttpContextHolder.getResponse().writer.println(res.toJsonString())
        }
    }

    private fun onUploadReference(context: ArtifactUploadContext) {
        val contentType = context.request.contentType
        val artifactInfo = context.artifactInfo as ReferenceArtifactInfo
        when (contentType) {
            MEDIA_TYPE_UNREAL_COMPACT_BINARY -> {
                val payload = context.getArtifactFile().getInputStream().readBytes()
                val cbObject = CbObject(ByteBuffer.wrap(payload))
                val finalized = !cbObject.hasAttachments()
                val ref = referenceService.create(Reference.from(artifactInfo, payload, finalized))
                ref.inlineBlob?.let {
                    // inlineBlob为null时表示inlineBlob过大，需要存到文件中
                    val nodeCreateRequest = buildRefNodeCreateRequest(context)
                    storageManager.storeArtifactFile(
                        nodeCreateRequest, context.getArtifactFile(), context.storageCredentials
                    )
                }
                val res = referenceService.finalize(ref, cbObject)
                HttpContextHolder.getResponse().writer.println(res.toJsonString())
            }

            else -> throw BadRequestException(
                CommonMessageCode.PARAMETER_INVALID, "Unknown request type $contentType"
            )
        }
    }

    private fun buildRefNodeCreateRequest(context: ArtifactUploadContext): NodeCreateRequest {
        val artifactInfo = context.artifactInfo as ReferenceArtifactInfo
        // TODO 后续鉴权调整后此处需要改为系统级元数据
        val metadata = ArrayList<MetadataModel>()
        metadata.add(
            MetadataModel(
                key = NODE_METADATA_KEY_BLOB_ID,
                value = artifactInfo.inlineBlobHash!!,
            )
        )

        return buildNodeCreateRequest(context).copy(
            overwrite = true,
            nodeMetadata = metadata
        )
    }

    private fun onUploadBlob(context: ArtifactUploadContext) {
        with(context) {
            val artifactInfo = artifactInfo as CompressedBlobArtifactInfo
            // TODO 校验解压后blob hash与contentId是否相等
            // TODO 改为读取流时直接计算blake3，避免重复读流
            artifactInfo.compressedContentId = getStreamArtifactFile().blake3().hex()

            storageManager.storeArtifactFile(buildBlobNodeCreateRequest(context), getArtifactFile(), storageCredentials)
            blobService.create(Blob.from(artifactInfo, getArtifactSha256(), getArtifactFile().getSize()))
            HttpContextHolder
                .getResponse()
                .writer
                .println(UploadCompressedBlobResponse(artifactInfo.contentId).toJsonString())
        }
    }

    private fun buildBlobNodeCreateRequest(context: ArtifactUploadContext): NodeCreateRequest {
        val artifactInfo = context.artifactInfo as CompressedBlobArtifactInfo
        // TODO 后续鉴权调整后此处需要改为系统级元数据
        val metadata = ArrayList<MetadataModel>()
        metadata.add(
            MetadataModel(
                key = NODE_METADATA_KEY_BLOB_ID,
                value = artifactInfo.compressedContentId!!,
            )
        )
        metadata.add(
            MetadataModel(
                key = NODE_METADATA_KEY_CONTENT_ID,
                value = artifactInfo.contentId
            )
        )

        return buildNodeCreateRequest(context).copy(
            overwrite = true,
            nodeMetadata = metadata
        )
    }

    private fun onDownloadReference(context: ArtifactDownloadContext): ArtifactResource? {
        with(context) {
            val artifactInfo = context.artifactInfo as ReferenceArtifactInfo
            val ref = referenceService.getReference(
                projectId,
                repoName,
                artifactInfo.bucket,
                artifactInfo.refId.toString()
            ) ?: return null
            response.addHeader(HEADER_NAME_HASH, ref.blobId.toString())
            response.addHeader(HEADER_NAME_LAST_ACCESS, ref.lastAccessDate!!.format(DATE_TIME_FORMATTER))

            return when (val responseType = response.contentType) {
                MEDIA_TYPE_UNREAL_COMPACT_BINARY -> {
                    val ais = ArtifactInputStream(
                        ByteArrayInputStream(ref.inlineBlob),
                        Range.full(ref.inlineBlob!!.size.toLong())
                    )
                    ArtifactResource(ais, artifactInfo.getResponseName()).apply { contentType = responseType }
                }

                MEDIA_TYPE_JUPITER_INLINED_PAYLOAD -> {
                    val cb = CbObject(ByteBuffer.wrap(ref.inlineBlob!!))
                    onInlineDownload(context, cb, responseType)
                }

                else -> throw NotImplementedException("Unknown expected response type $responseType")
            }
        }
    }

    private fun onDownloadBlob(context: ArtifactDownloadContext): ArtifactResource {
        with(context) {
            val artifactInfo = context.artifactInfo as CompressedBlobArtifactInfo
            val acceptContentType = request.getHeaders("Accept").toList()
            val blob = blobService
                .getBlobsByContentId(projectId, repoName, artifactInfo.contentId)
                .minByOrNull { it.size }
                ?: throw NotFoundException(CommonMessageCode.RESOURCE_NOT_FOUND, artifactInfo.contentId)
            val blobId = blob.blobId.toString()
            artifactInfo.compressedContentId = blobId

            val responseType = if (blobId == artifactInfo.contentId) {
                MediaTypes.APPLICATION_OCTET_STREAM
            } else {
                MEDIA_TYPE_UNREAL_UNREAL_COMPRESSED_BUFFER
            }

            if (acceptContentType.isNotEmpty() &&
                !acceptContentType.contains("*/*") &&
                !acceptContentType.contains(responseType)
            ) {
                throw ErrorCodeException(
                    status = HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    messageCode = CommonMessageCode.MEDIA_TYPE_UNSUPPORTED
                )
            }

            val blobInputStream = blobService.loadBlob(blob)
            val resource = ArtifactResource(blobInputStream, artifactInfo.getResponseName())
            resource.contentType = responseType
            // TODO 支持分片存储的Blob
            return resource
        }
    }

    /**
     * 客户端调用获取ref的接口时，直接返回缓存内容
     */
    private fun onInlineDownload(
        context: ArtifactDownloadContext,
        cb: CbObject,
        responseType: String,
    ): ArtifactResource? {
        with(context) {
            val (binaryAttachmentCount, attachmentCount) = countAttachment(cb)
            val artifactInfo = artifactInfo as ReferenceArtifactInfo

            // 只允许在ref只包含了单个binaryAttachment的情况下，直接通过ref接口直接获取缓存文件
            if (binaryAttachmentCount > 1 || attachmentCount > 1 || binaryAttachmentCount != attachmentCount) {
                throw BadRequestException(
                    CommonMessageCode.PARAMETER_INVALID,
                    "Object ${artifactInfo.bucket} ${artifactInfo.refId} had more then 1 binary attachment field," +
                            " unable to inline this object. Use compact object response instead."
                )
            }

            return if (binaryAttachmentCount == 1) {
                try {
                    val blobs = refResolver.getReferencedBlobs(projectId, repoName, cb)
                    if (blobs.size == 1) {
                        blobToArtifactResource(context, blobs[0], responseType)
                    } else if (blobs.isEmpty()) {
                        null
                    } else {
                        throw BadRequestException(
                            CommonMessageCode.PARAMETER_INVALID,
                            "Object ${artifactInfo.bucket} ${artifactInfo.refId} contained a content id " +
                                    "which resolved to more then 1 blob, unable to inline this object. " +
                                    "Use compact object response instead."
                        )
                    }
                } catch (e: PartialReferenceResolveException) {
                    null
                } catch (e: ReferenceIsMissingBlobsException) {
                    null
                }
            } else {
                val cbByteBuffer = cb.innerField.fieldData.asReadOnlyBuffer()
                val byteArray = ByteArray(cbByteBuffer.remaining())
                ByteBuffer.wrap(byteArray).put(cbByteBuffer)
                val ais = ArtifactInputStream(ByteArrayInputStream(byteArray), Range.full(byteArray.size.toLong()))
                ArtifactResource(ais, artifactInfo.getResponseName()).apply { contentType = responseType }
            }
        }
    }

    private fun blobToArtifactResource(
        context: ArtifactDownloadContext,
        blob: Blob,
        responseType: String
    ): ArtifactResource? {
        with(context) {
            response.addHeader(HEADER_NAME_INLINE_PAYLOAD_HASH, blob.toString())
            return nodeClient.getNodeDetail(projectId, repoName, blob.fullPath).data?.let {
                storageManager.loadArtifactInputStream(it, storageCredentials)
            }?.let {
                ArtifactResource(it, artifactInfo.getResponseName()).apply { contentType = responseType }
            }
        }
    }

    private fun countAttachment(cb: CbObject): Pair<Int, Int> {
        var countOfAttachmentFields = 0
        var countOfBinaryAttachmentFields = 0
        cb.iterateAttachments {
            if (it.isBinaryAttachment()) {
                countOfBinaryAttachmentFields++
            }
            if (it.isAttachment()) {
                countOfAttachmentFields++
            }
        }

        return Pair(countOfBinaryAttachmentFields, countOfAttachmentFields)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DdcLocalRepository::class.java)
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss")
        const val HEADER_NAME_HASH = "X-Jupiter-IoHash"
        private const val HEADER_NAME_LAST_ACCESS = "X-Jupiter-LastAccess"
        private const val HEADER_NAME_INLINE_PAYLOAD_HASH = "X-Jupiter-InlinePayloadHash"
    }
}
