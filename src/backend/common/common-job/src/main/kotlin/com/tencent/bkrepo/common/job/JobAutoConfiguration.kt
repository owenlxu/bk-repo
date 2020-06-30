package com.tencent.bkrepo.common.job

import com.mongodb.client.MongoClient
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.mongo.MongoLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.boot.autoconfigure.mongo.MongoProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 *
 * @author: carrypan
 * @date: 2019/12/23
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "60s", defaultLockAtLeastFor = "1s")
class JobAutoConfiguration {

    @Bean
    fun lockProvider(mongoClient: MongoClient, mongoProperties: MongoProperties): LockProvider {
        val databaseName = mongoProperties.mongoClientDatabase
        val database = mongoClient.getDatabase(databaseName)
        val collection = database.getCollection(SHED_LOCK_COLLECTION_NAME)
        return MongoLockProvider(collection)
    }

    companion object {
        const val SHED_LOCK_COLLECTION_NAME = "shed_lock"
    }
}
