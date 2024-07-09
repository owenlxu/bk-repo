package com.tencent.bkrepo.media.stream

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.api.toArtifactFile
import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import java.io.File

/**
 * 将文件保存为制品构件
 * */
class ArtifactFileConsumer(
    private val storageManager: StorageManager,
    private val repo: RepositoryDetail,
    private val userId: String,
    private val path: String,
    private val expireDays: Int,
) : FileConsumer {

    private val startTime = System.currentTimeMillis()
    override fun accept(t: File) {
        accept(t.toArtifactFile(), t.name)
    }

    override fun accept(file: File, name: String) {
        accept(file.toArtifactFile(), name)
    }

    fun accept(file: ArtifactFile, name: String) {
        val filePath = "$path/$name"
        val artifactInfo = ArtifactInfo(repo.projectId, repo.name, filePath)
        val nodeCreateRequest = buildNodeCreateRequest(artifactInfo, file, userId)
        storageManager.storeArtifactFile(nodeCreateRequest, file, repo.storageCredentials)
    }

    private fun buildNodeCreateRequest(
        artifactInfo: ArtifactInfo,
        file: ArtifactFile,
        userId: String,
    ): NodeCreateRequest {
        with(artifactInfo) {
            val endTime = System.currentTimeMillis()
            return NodeCreateRequest(
                projectId = projectId,
                repoName = repoName,
                folder = false,
                fullPath = artifactInfo.getArtifactFullPath(),
                size = file.getSize(),
                sha256 = file.getFileSha256(),
                md5 = file.getFileMd5(),
                expires = expireDays.toLong(),
                operator = userId,
                nodeMetadata = listOf(
                    MetadataModel(key = METADATA_KEY_MEDIA_START_TIME, value = startTime, system = true),
                    MetadataModel(key = METADATA_KEY_MEDIA_STOP_TIME, value = endTime, system = true),
                ),
            )
        }
    }

    companion object {
        private const val METADATA_KEY_MEDIA_START_TIME = "media.startTime"
        private const val METADATA_KEY_MEDIA_STOP_TIME = "media.stopTime"
    }
}
