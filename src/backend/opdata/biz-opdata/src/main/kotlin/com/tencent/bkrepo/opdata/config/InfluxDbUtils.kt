package com.tencent.bkrepo.opdata.config

import org.influxdb.InfluxDB
import org.influxdb.InfluxDBFactory
import org.slf4j.LoggerFactory

class InfluxDbUtils(
    private val userName: String?,
    private val password: String?,
    private val url: String?,
    private val database: String?,
    private val retentionPolicy: String?
) {

    // database instance
    private var influxDB: InfluxDB? = null

//    // policy
//    private var retentionPolicy: String? = null

    /**
     * connect database
     *
     * @return influxDb实例
     */
    public fun getInstance(): InfluxDB? {
        try {
            if (null == influxDB) {
                influxDB = InfluxDBFactory.connect(url, userName, password)
                if (!influxDB!!.databaseExists(database)) {
                    influxDB!!.createDatabase(database)
                    influxDB!!.setDatabase(database)
                }
                influxDB!!.setRetentionPolicy(retentionPolicy)
                influxDB!!.setLogLevel(InfluxDB.LogLevel.BASIC)
            }
        } catch (e: Exception) {
            logger.error("create influxdb failed, error: {}", e.message)
            return null
        }
        return influxDB
    }

    companion object {
        private val logger = LoggerFactory.getLogger(InfluxDbUtils::class.java)
    }
}
