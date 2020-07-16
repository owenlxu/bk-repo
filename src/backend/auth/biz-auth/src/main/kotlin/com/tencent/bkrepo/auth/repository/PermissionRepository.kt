package com.tencent.bkrepo.auth.repository

import com.tencent.bkrepo.auth.model.TPermission
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface PermissionRepository : MongoRepository<TPermission, String> {
    fun findFirstById(id: String): TPermission?
    fun findByResourceType(resourceType: ResourceType): List<TPermission>
    fun findByResourceTypeAndProjectId(resourceType: ResourceType, projectId: String): List<TPermission>
    fun findByResourceTypeAndProjectIdAndReposIn(resourceType: ResourceType, projectId: String, repo: String): List<TPermission>
    fun findOneByPermNameAndProjectIdAndResourceType(permName: String, projectId: String?, resourceType: ResourceType): TPermission?
}
