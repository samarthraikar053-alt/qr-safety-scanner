package com.qrsafety.scanner.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class ScanHistoryRepository(context: Context) {
    private val dbHelper = ScanHistoryDbHelper(context.applicationContext)
    private val _history = MutableStateFlow<List<ScanRecord>>(emptyList())
    val history: StateFlow<List<ScanRecord>> = _history.asStateFlow()

    suspend fun refresh() {
        withContext(Dispatchers.IO) {
            val records = dbHelper.getAll()
            _history.value = records
        }
    }

    suspend fun addRecord(record: ScanRecord) {
        withContext(Dispatchers.IO) {
            dbHelper.insert(record)
            val updated = dbHelper.getAll()
            _history.value = updated
        }
    }

    suspend fun deleteRecord(id: Long) {
        withContext(Dispatchers.IO) {
            dbHelper.delete(id)
            val updated = dbHelper.getAll()
            _history.value = updated
        }
    }

    suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            dbHelper.clearAll()
            _history.value = emptyList()
        }
    }
}
