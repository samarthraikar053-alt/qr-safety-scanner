package com.qrsafety.scanner.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.qrsafety.scanner.analyzer.ThreatLevel

class ScanHistoryDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "qr_scanner_history.db"
        const val DATABASE_VERSION = 1

        const val TABLE_NAME = "scan_history"
        const val COLUMN_ID = "id"
        const val COLUMN_URL = "url"
        const val COLUMN_HOST = "host"
        const val COLUMN_THREAT_LEVEL = "threat_level"
        const val COLUMN_WARNING_SUMMARY = "warning_summary"
        const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_URL TEXT NOT NULL,
                $COLUMN_HOST TEXT NOT NULL,
                $COLUMN_THREAT_LEVEL TEXT NOT NULL,
                $COLUMN_WARNING_SUMMARY TEXT NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insert(record: ScanRecord): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_URL, record.url)
            put(COLUMN_HOST, record.host)
            put(COLUMN_THREAT_LEVEL, record.threatLevel.name)
            put(COLUMN_WARNING_SUMMARY, record.warningSummary)
            put(COLUMN_TIMESTAMP, record.timestamp)
        }
        return db.insert(TABLE_NAME, null, values)
    }

    fun getAll(): List<ScanRecord> {
        val records = mutableListOf<ScanRecord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC"
        )
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID))
                    val url = it.getString(it.getColumnIndexOrThrow(COLUMN_URL))
                    val host = it.getString(it.getColumnIndexOrThrow(COLUMN_HOST))
                    val threatLevelName = it.getString(it.getColumnIndexOrThrow(COLUMN_THREAT_LEVEL))
                    val summary = it.getString(it.getColumnIndexOrThrow(COLUMN_WARNING_SUMMARY))
                    val timestamp = it.getLong(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP))

                    val threat = when (threatLevelName) {
                        "HIGH_RISK", "DANGEROUS" -> ThreatLevel.HIGH_RISK
                        "SUSPICIOUS" -> ThreatLevel.SUSPICIOUS
                        "SAFE" -> ThreatLevel.SAFE
                        else -> ThreatLevel.SUSPICIOUS
                    }

                    records.add(
                        ScanRecord(
                            id = id,
                            url = url,
                            host = host,
                            threatLevel = threat,
                            warningSummary = summary,
                            timestamp = timestamp
                        )
                    )
                } while (it.moveToNext())
            }
        }
        return records
    }

    fun delete(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun clearAll(): Int {
        val db = writableDatabase
        return db.delete(TABLE_NAME, null, null)
    }
}
