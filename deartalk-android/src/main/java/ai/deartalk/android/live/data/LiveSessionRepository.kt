package ai.deartalk.android.live.data

import android.content.ContentValues
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 📦 DearTalk Live 세션 및 메시지 로컬 리포지토리
 */
class LiveSessionRepository(context: Context) {

    private val dbHelper = LiveDatabaseHelper(context.applicationContext)

    /**
     * ➕ 신규 세션 생성 및 저장
     */
    suspend fun createSession(session: LiveSession): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(LiveDatabaseHelper.COL_SESSION_ID, session.id)
            put(LiveDatabaseHelper.COL_SESSION_TITLE, session.title)
            put(LiveDatabaseHelper.COL_SESSION_CREATED_AT, session.createdAt)
            put(LiveDatabaseHelper.COL_SESSION_MY_LANG, session.myLang)
            put(LiveDatabaseHelper.COL_SESSION_PARTNER_LANG, session.partnerLang)
        }
        val rowId = db.insert(LiveDatabaseHelper.TABLE_SESSIONS, null, values)
        rowId != -1L
    }

    /**
     * 📋 모든 세션 목록 조회 (최신순)
     */
    suspend fun getAllSessions(): List<LiveSession> = withContext(Dispatchers.IO) {
        val list = mutableListOf<LiveSession>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            LiveDatabaseHelper.TABLE_SESSIONS,
            null,
            null,
            null,
            null,
            null,
            "${LiveDatabaseHelper.COL_SESSION_CREATED_AT} DESC"
        )
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(LiveDatabaseHelper.COL_SESSION_ID)
            val titleIdx = it.getColumnIndexOrThrow(LiveDatabaseHelper.COL_SESSION_TITLE)
            val createdIdx = it.getColumnIndexOrThrow(LiveDatabaseHelper.COL_SESSION_CREATED_AT)
            val myLangIdx = it.getColumnIndexOrThrow(LiveDatabaseHelper.COL_SESSION_MY_LANG)
            val partnerLangIdx = it.getColumnIndexOrThrow(LiveDatabaseHelper.COL_SESSION_PARTNER_LANG)

            while (it.moveToNext()) {
                list.add(
                    LiveSession(
                        id = it.getString(idIdx),
                        title = it.getString(titleIdx),
                        createdAt = it.getLong(createdIdx),
                        myLang = it.getString(myLangIdx),
                        partnerLang = it.getString(partnerLangIdx)
                    )
                )
            }
        }
        list
    }

    /**
     * 🔍 특정 세션 단건 조회
     */
    suspend fun getSessionById(sessionId: String): LiveSession? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            LiveDatabaseHelper.TABLE_SESSIONS,
            null,
            "${LiveDatabaseHelper.COL_SESSION_ID} = ?",
            arrayOf(sessionId),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val idIdx = it.getColumnIndexOrThrow(LiveDatabaseHelper.COL_SESSION_ID)
                val titleIdx = it.getColumnIndexOrThrow(LiveDatabaseHelper.COL_SESSION_TITLE)
                val createdIdx = it.getColumnIndexOrThrow(LiveDatabaseHelper.COL_SESSION_CREATED_AT)
                val myLangIdx = it.getColumnIndexOrThrow(LiveDatabaseHelper.COL_SESSION_MY_LANG)
                val partnerLangIdx = it.getColumnIndexOrThrow(LiveDatabaseHelper.COL_SESSION_PARTNER_LANG)
                LiveSession(
                    id = it.getString(idIdx),
                    title = it.getString(titleIdx),
                    createdAt = it.getLong(createdIdx),
                    myLang = it.getString(myLangIdx),
                    partnerLang = it.getString(partnerLangIdx)
                )
            } else {
                null
            }
        }
    }

    /**
     * 🗑️ 세션 삭제 (CASCADE로 메시지도 자동 삭제)
     */
    suspend fun deleteSession(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val rows = db.delete(
            LiveDatabaseHelper.TABLE_SESSIONS,
            "${LiveDatabaseHelper.COL_SESSION_ID} = ?",
            arrayOf(sessionId)
        )
        rows > 0
    }

    /**
     * 🧹 만료된 세션 자동 삭제 (보관 주기 초과)
     */
    suspend fun purgeExpiredSessions(cutoffTimestamp: Long): Int = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(
            LiveDatabaseHelper.TABLE_SESSIONS,
            "${LiveDatabaseHelper.COL_SESSION_CREATED_AT} < ?",
            arrayOf(cutoffTimestamp.toString())
        )
    }

    /**
     * 🗑️ 모든 세션 및 대화 기록 전체 삭제
     */
    suspend fun deleteAllSessions(): Int = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(LiveDatabaseHelper.TABLE_MESSAGES, null, null)
        db.delete(LiveDatabaseHelper.TABLE_SESSIONS, null, null)
    }

    /**
     * 💬 메시지 저장
     */
    suspend fun insertMessage(message: LiveMessage): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(LiveDatabaseHelper.COL_MSG_ID, message.id)
            put(LiveDatabaseHelper.COL_MSG_SESSION_ID, message.sessionId)
            put(LiveDatabaseHelper.COL_MSG_SENDER, message.sender.name)
            put(LiveDatabaseHelper.COL_MSG_RAW_TEXT, message.rawText)
            put(LiveDatabaseHelper.COL_MSG_REFINED_TEXT, message.refinedText)
            put(LiveDatabaseHelper.COL_MSG_SOURCE_LANG, message.sourceLang)
            put(LiveDatabaseHelper.COL_MSG_TARGET_LANG, message.targetLang)
            put(LiveDatabaseHelper.COL_MSG_TONE, message.tone)
            put(LiveDatabaseHelper.COL_MSG_CREATED_AT, message.createdAt)
            put(LiveDatabaseHelper.COL_MSG_ORIGINAL_RAW_TEXT, message.originalRawText)
        }
        val rowId = db.insert(LiveDatabaseHelper.TABLE_MESSAGES, null, values)
        rowId != -1L
    }

    /**
     * 💬 기존 메시지 업데이트
     */
    suspend fun updateMessage(message: LiveMessage): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(LiveDatabaseHelper.COL_MSG_RAW_TEXT, message.rawText)
            put(LiveDatabaseHelper.COL_MSG_REFINED_TEXT, message.refinedText)
            put(LiveDatabaseHelper.COL_MSG_TONE, message.tone)
        }
        val rows = db.update(
            LiveDatabaseHelper.TABLE_MESSAGES,
            values,
            "${LiveDatabaseHelper.COL_MSG_ID} = ?",
            arrayOf(message.id)
        )
        rows > 0
    }

    /**
     * 📜 세션 내 모든 메시지 조회 (오래된 순 -> 최신순)
     */
    suspend fun getMessagesForSession(sessionId: String): List<LiveMessage> = withContext(Dispatchers.IO) {
        val list = mutableListOf<LiveMessage>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            LiveDatabaseHelper.TABLE_MESSAGES,
            null,
            "${LiveDatabaseHelper.COL_MSG_SESSION_ID} = ?",
            arrayOf(sessionId),
            null,
            null,
            "${LiveDatabaseHelper.COL_MSG_CREATED_AT} ASC"
        )
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(LiveDatabaseHelper.COL_MSG_ID)
            val sessionIdx = it.getColumnIndexOrThrow(LiveDatabaseHelper.COL_MSG_SESSION_ID)
            val senderIdx = it.getColumnIndexOrThrow(LiveDatabaseHelper.COL_MSG_SENDER)
            val rawIdx = it.getColumnIndexOrThrow(LiveDatabaseHelper.COL_MSG_RAW_TEXT)
            val refinedIdx = it.getColumnIndexOrThrow(LiveDatabaseHelper.COL_MSG_REFINED_TEXT)
            val srcIdx = it.getColumnIndexOrThrow(LiveDatabaseHelper.COL_MSG_SOURCE_LANG)
            val tgtIdx = it.getColumnIndexOrThrow(LiveDatabaseHelper.COL_MSG_TARGET_LANG)
            val toneIdx = it.getColumnIndexOrThrow(LiveDatabaseHelper.COL_MSG_TONE)
            val createdIdx = it.getColumnIndexOrThrow(LiveDatabaseHelper.COL_MSG_CREATED_AT)
            val origRawIdx = it.getColumnIndex(LiveDatabaseHelper.COL_MSG_ORIGINAL_RAW_TEXT)

            while (it.moveToNext()) {
                val raw = it.getString(rawIdx)
                val originalRaw = if (origRawIdx != -1 && !it.isNull(origRawIdx)) it.getString(origRawIdx) else raw
                list.add(
                    LiveMessage(
                        id = it.getString(idIdx),
                        sessionId = it.getString(sessionIdx),
                        sender = LiveSender.valueOf(it.getString(senderIdx)),
                        rawText = raw,
                        refinedText = it.getString(refinedIdx),
                        sourceLang = it.getString(srcIdx),
                        targetLang = it.getString(tgtIdx),
                        tone = it.getString(toneIdx),
                        createdAt = it.getLong(createdIdx),
                        originalRawText = originalRaw
                    )
                )
            }
        }
        list
    }
}
