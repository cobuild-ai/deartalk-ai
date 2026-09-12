package ai.deartalk.android.live.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 🛡️ 100% 오프라인 SQLite 로컬 데이터베이스 헬퍼
 * - 외부 서버 통신 없이 단말 로컬의 비공개 스토리지에만 보관
 */
class LiveDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "deartalk_live.db"
        const val DATABASE_VERSION = 1

        // Sessions Table
        const val TABLE_SESSIONS = "live_sessions"
        const val COL_SESSION_ID = "id"
        const val COL_SESSION_TITLE = "title"
        const val COL_SESSION_CREATED_AT = "created_at"
        const val COL_SESSION_MY_LANG = "my_lang"
        const val COL_SESSION_PARTNER_LANG = "partner_lang"

        // Messages Table
        const val TABLE_MESSAGES = "live_messages"
        const val COL_MSG_ID = "id"
        const val COL_MSG_SESSION_ID = "session_id"
        const val COL_MSG_SENDER = "sender"
        const val COL_MSG_RAW_TEXT = "raw_text"
        const val COL_MSG_REFINED_TEXT = "refined_text"
        const val COL_MSG_SOURCE_LANG = "source_lang"
        const val COL_MSG_TARGET_LANG = "target_lang"
        const val COL_MSG_TONE = "tone"
        const val COL_MSG_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createSessionsQuery = """
            CREATE TABLE $TABLE_SESSIONS (
                $COL_SESSION_ID TEXT PRIMARY KEY,
                $COL_SESSION_TITLE TEXT NOT NULL,
                $COL_SESSION_CREATED_AT INTEGER NOT NULL,
                $COL_SESSION_MY_LANG TEXT NOT NULL,
                $COL_SESSION_PARTNER_LANG TEXT NOT NULL
            )
        """.trimIndent()

        val createMessagesQuery = """
            CREATE TABLE $TABLE_MESSAGES (
                $COL_MSG_ID TEXT PRIMARY KEY,
                $COL_MSG_SESSION_ID TEXT NOT NULL,
                $COL_MSG_SENDER TEXT NOT NULL,
                $COL_MSG_RAW_TEXT TEXT NOT NULL,
                $COL_MSG_REFINED_TEXT TEXT NOT NULL,
                $COL_MSG_SOURCE_LANG TEXT NOT NULL,
                $COL_MSG_TARGET_LANG TEXT NOT NULL,
                $COL_MSG_TONE TEXT,
                $COL_MSG_CREATED_AT INTEGER NOT NULL,
                FOREIGN KEY($COL_MSG_SESSION_ID) REFERENCES $TABLE_SESSIONS($COL_SESSION_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        val createIndexQuery = """
            CREATE INDEX idx_live_messages_session ON $TABLE_MESSAGES($COL_MSG_SESSION_ID)
        """.trimIndent()

        db.execSQL(createSessionsQuery)
        db.execSQL(createMessagesQuery)
        db.execSQL(createIndexQuery)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SESSIONS")
        onCreate(db)
    }
}
