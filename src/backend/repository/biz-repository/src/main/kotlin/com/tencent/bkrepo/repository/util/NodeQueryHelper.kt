package com.tencent.bkrepo.repository.util

import com.tencent.bkrepo.common.artifact.path.PathUtils.escapeRegex
import com.tencent.bkrepo.common.artifact.path.PathUtils.isRoot
import com.tencent.bkrepo.common.artifact.path.PathUtils.toPath
import com.tencent.bkrepo.repository.model.TNode
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.time.LocalDateTime

/**
 * 查询条件构造工具
 */
object NodeQueryHelper {

    fun nodeQuery(projectId: String, repoName: String, fullPath: String? = null): Query {
        val criteria = Criteria.where(TNode::projectId.name).`is`(projectId)
            .and(TNode::repoName.name).`is`(repoName)
            .and(TNode::deleted.name).`is`(null)
            .apply { fullPath?.run { and(TNode::fullPath.name).`is`(fullPath) } }
        return Query(criteria)
    }

    fun nodeQuery(projectId: String, repoName: String, fullPath: List<String>): Query {
        val criteria = Criteria.where(TNode::projectId.name).`is`(projectId)
            .and(TNode::repoName.name).`is`(repoName)
            .and(TNode::fullPath.name).`in`(fullPath)
            .and(TNode::deleted.name).`is`(null)
        return Query(criteria)
    }

    fun nodeListCriteria(projectId: String, repoName: String, path: String, includeFolder: Boolean, deep: Boolean): Criteria {
        val nodePath = toPath(path)
        val escapedPath = escapeRegex(nodePath)
        val criteria = Criteria.where(TNode::projectId.name).`is`(projectId)
            .and(TNode::repoName.name).`is`(repoName)
            .and(TNode::deleted.name).`is`(null)
        if (deep) {
            if (!isRoot(nodePath)) {
                criteria.and(TNode::fullPath.name).regex("^$escapedPath")
            }
        } else {
            criteria.and(TNode::path.name).`is`(nodePath)
        }
        if (!includeFolder) {
            criteria.and(TNode::folder.name).`is`(false)
        }
        return criteria
    }

    fun nodeListQuery(
        projectId: String,
        repoName: String,
        path: String,
        includeFolder: Boolean,
        includeMetadata: Boolean,
        deep: Boolean,
        sort: Boolean
    ): Query {
        return Query.query(nodeListCriteria(projectId, repoName, path, includeFolder, deep))
            .apply {
                // 排序
                if (sort) {
                    with(Sort.by(
                        Sort.Order(Sort.Direction.DESC, TNode::folder.name),
                        Sort.Order(Sort.Direction.ASC, TNode::fullPath.name)
                    ))
                }
            }.apply {
                // 查询元数据
                if (!includeMetadata) {
                    this.fields().exclude(TNode::metadata.name)
                }
            }
    }

    fun nodePathUpdate(path: String, name: String, operator: String): Update {
        return update(operator)
            .set(TNode::path.name, path)
            .set(TNode::name.name, name)
            .set(TNode::fullPath.name, path + name)
    }

    fun nodeExpireDateUpdate(expireDate: LocalDateTime?, operator: String): Update {
        return update(operator).apply {
            expireDate?.let { set(TNode::expireDate.name, expireDate) } ?: run { unset(TNode::expireDate.name) }
        }
    }

    fun nodeDeleteUpdate(operator: String): Update {
        return update(operator).set(TNode::deleted.name, LocalDateTime.now())
    }

    private fun update(operator: String): Update {
        return Update()
            .set(TNode::lastModifiedDate.name, LocalDateTime.now())
            .set(TNode::lastModifiedBy.name, operator)
    }
}
