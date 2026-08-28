package ai.deartalk.android.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * 어노테이션 프로세서 의존성 없이 100% 안전하고 초고속으로 동작하는 Android Native SQLite 저장소
 */
class ContextRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context.applicationContext)
    private val suggestionsCache = ConcurrentHashMap<String, List<String>>()

    suspend fun getAppSuggestions(packageName: String): List<String> = withContext(Dispatchers.IO) {
        val cached = suggestionsCache[packageName]
        if (cached != null) return@withContext cached

        val result = mutableListOf<String>()
        try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT transformedText FROM app_history WHERE packageName = ? ORDER BY timestamp DESC LIMIT 5",
                arrayOf(packageName)
            )
            cursor.use {
                while (it.moveToNext()) {
                    result.add(it.getString(0))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        suggestionsCache[packageName] = result
        return@withContext result
    }

    suspend fun recordInteraction(
        packageName: String,
        originalVoice: String,
        transformedText: String,
        intentType: String
    ) = withContext(Dispatchers.IO) {
        if (packageName.isBlank() || transformedText.isBlank()) return@withContext

        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("packageName", packageName)
                put("originalVoice", originalVoice)
                put("transformedText", transformedText)
                put("intentType", intentType)
                put("timestamp", System.currentTimeMillis())
            }
            db.insert("app_history", null, values)

            // 🔒 DB 무한 비대화 방지: 앱별 최대 100건, 전체 최대 500건
            pruneOldRecords(db, packageName)

            // 캐시 갱신
            val updated = getAppSuggestions(packageName)
            suggestionsCache[packageName] = updated
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * DB 비대화 방지 자동 정리
     * - 앱별 최대 100건 유지 (초과 시 오래된 것부터 삭제)
     * - 전체 최대 500건 유지
     */
    private fun pruneOldRecords(db: SQLiteDatabase, packageName: String) {
        try {
            // 앱별 100건 초과 삭제
            db.execSQL(
                """
                DELETE FROM app_history WHERE id IN (
                    SELECT id FROM app_history WHERE packageName = ?
                    ORDER BY timestamp DESC LIMIT -1 OFFSET 100
                )
                """.trimIndent(),
                arrayOf(packageName)
            )
            // 전체 500건 초과 삭제
            db.execSQL(
                """
                DELETE FROM app_history WHERE id IN (
                    SELECT id FROM app_history ORDER BY timestamp DESC LIMIT -1 OFFSET 500
                )
                """.trimIndent()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 사용자 요청으로 전체 히스토리 초기화
     */
    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        try {
            dbHelper.writableDatabase.execSQL("DELETE FROM app_history")
            suggestionsCache.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "deartalk_db.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS app_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    packageName TEXT NOT NULL,
                    originalVoice TEXT NOT NULL,
                    transformedText TEXT NOT NULL,
                    intentType TEXT NOT NULL,
                    timestamp INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_pkg_time ON app_history(packageName, timestamp DESC)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS app_history")
            onCreate(db)
        }
    }
}
