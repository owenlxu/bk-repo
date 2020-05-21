package com.tencent.bkrepo.npm.async

import com.google.gson.JsonObject
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.npm.constants.DEPENDENCIES
import com.tencent.bkrepo.npm.constants.DISTTAGS
import com.tencent.bkrepo.npm.constants.LATEST
import com.tencent.bkrepo.npm.constants.NAME
import com.tencent.bkrepo.npm.constants.VERSIONS
import com.tencent.bkrepo.npm.pojo.enums.NpmOperationAction
import com.tencent.bkrepo.npm.pojo.module.des.service.DepsCreateRequest
import com.tencent.bkrepo.npm.pojo.module.des.service.DepsDeleteRequest
import com.tencent.bkrepo.npm.service.ModuleDepsService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class NpmDependentHandler {

    @Autowired
    private lateinit var moduleDepsService: ModuleDepsService

    // @Async
    fun updatePkgDepts(userId: String, artifactInfo: ArtifactInfo, jsonObj: JsonObject, action: NpmOperationAction) {
        // logger.info("updatePkgDependent current Thread : [${Thread.currentThread().name}]")

        val distTags = getDistTags(jsonObj)!!
        val versionJsonData = jsonObj.getAsJsonObject(VERSIONS).getAsJsonObject(distTags.second)

        when (action) {
            NpmOperationAction.PUBLISH -> {
                doDependentWithPublish(userId, artifactInfo, versionJsonData)
            }
            NpmOperationAction.UNPUBLISH -> {
                doDependentWithUnPublish(userId, artifactInfo, versionJsonData)
            }
            NpmOperationAction.MIGRATION -> {
                doDependentWithPublish(userId, artifactInfo, versionJsonData)
            }
            else -> {
                logger.warn("don't find operation action [${action.name}].")
            }
        }
    }

    private fun doDependentWithPublish(userId: String, artifactInfo: ArtifactInfo, versionJsonData: JsonObject) {
        val name = versionJsonData[NAME].asString
        if (versionJsonData.has(DEPENDENCIES)) {
            val dependenciesSet = versionJsonData.getAsJsonObject(DEPENDENCIES).keySet()
            val createList = mutableListOf<DepsCreateRequest>()
            if (dependenciesSet.isNotEmpty()) {
                dependenciesSet.forEach {
                    createList.add(
                        DepsCreateRequest(
                            projectId = artifactInfo.projectId,
                            repoName = artifactInfo.repoName,
                            name = it,
                            deps = name,
                            overwrite = true,
                            operator = userId
                        )
                    )
                }
            }
            if (createList.isNotEmpty()) {
                moduleDepsService.batchCreate(createList)
            }
            logger.info("publish dependent for package: [$name], size: [${createList.size}] success.")
        }
    }

    private fun doDependentWithUnPublish(userId: String, artifactInfo: ArtifactInfo, versionJsonData: JsonObject) {
        val name = versionJsonData[NAME].asString
        moduleDepsService.deleteAllByName(
            DepsDeleteRequest(
                projectId = artifactInfo.projectId,
                repoName = artifactInfo.repoName,
                deps = name,
                operator = userId
            )
        )
        logger.info("unPublish dependent for [$name] success.")
    }

    private fun doDependentWithMigration(userId: String, artifactInfo: ArtifactInfo, jsonData: JsonObject) {
        val name = jsonData[NAME].asString
        val versionsJsonData = jsonData.getAsJsonObject(VERSIONS)
        val createList = mutableListOf<DepsCreateRequest>()
        val deptsSet = mutableSetOf<String>()
        versionsJsonData.keySet().forEach { version ->
            val versionJsonData = versionsJsonData.getAsJsonObject(version)
            if (versionJsonData.has(DEPENDENCIES)) {
                val dependenciesSet = versionJsonData.getAsJsonObject(DEPENDENCIES).keySet()
                if (dependenciesSet.isNotEmpty()) {
                    deptsSet.addAll(dependenciesSet)
                }
            }
        }
        deptsSet.forEach {
            createList.add(
                DepsCreateRequest(
                    projectId = artifactInfo.projectId,
                    repoName = artifactInfo.repoName,
                    name = it,
                    deps = name,
                    overwrite = true,
                    operator = userId
                )
            )
        }
        if (createList.isNotEmpty()) {
            moduleDepsService.batchCreate(createList)
        }
        logger.info("migration dependent for package: [$name], size: [${createList.size}] success.")
    }

    private fun getDistTags(jsonObj: JsonObject): Pair<String, String>? {
        val distTags = jsonObj.getAsJsonObject(DISTTAGS)
        if (distTags.has(LATEST)) {
            return Pair(LATEST, distTags[LATEST].asString)
        }
        distTags.entrySet().forEach {
            return Pair(it.key, it.value.asString)
        }
        return null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NpmDependentHandler::class.java)
    }
}
