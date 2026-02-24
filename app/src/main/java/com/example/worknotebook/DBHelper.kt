package com.example.worknotebook

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.core.database.getLongOrNull
import androidx.core.database.sqlite.transaction
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.apply
import kotlin.text.trimIndent


object PasswordHasher {
    private const val SALT_LENGTH = 16
    private const val HASH_ALGORITHM = "SHA-256"

    /**
     * Генерирует случайную соль в hex
     */
    fun generateSalt(): String {
        val random = SecureRandom()
        val bytes = ByteArray(SALT_LENGTH)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Хеширует пароль с солью и возвращает хеш в hex
     */
    fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        val input = (password + salt).toByteArray()
        val hashBytes = digest.digest(input)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Проверяет, соответствует ли введённый пароль сохранённому хешу
     */
    fun verifyPassword(inputPassword: String, storedHash: String, salt: String): Boolean {
        val computedHash = hashPassword(inputPassword, salt)
        return computedHash == storedHash
    }
}

const val TIMESTAMP_1900: Long = -2208988800000


class DBHelper(
    context: Context,
    factory: SQLiteDatabase.CursorFactory? = null,
) : SQLiteOpenHelper(context, "workbook.db", factory, 1) {

    private val currentTimeSql = "(strftime('%s','now') * 1000)"

    override fun onCreate(db: SQLiteDatabase?) {
        Log.d("DB_DEBUG", "-----===== onCreate called =====-----")  // TODO нужен на этапе разработки. потом убрать.
        // Создание таблицы пользователя
        val createUsersTable = """
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                external_id INTEGER UNIQUE,
                name TEXT,
                login TEXT UNIQUE,
                email TEXT UNIQUE,
                verified INTEGER DEFAULT 0,
                is_admin INTEGER DEFAULT 0,
                created_at INTEGER DEFAULT NULL,
                updated_at INTEGER DEFAULT NULL,
                birthdate_at INTEGER DEFAULT NULL,
                gender INTEGER DEFAULT ${GenderType.UNSET.value},
                password TEXT,
                salt TEXT
            )
        """.trimIndent()
        db?.execSQL(createUsersTable)

        // Создание таблицы встреч пользователя
        db?.execSQL("""
            CREATE TABLE meetings (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                title TEXT,
                description TEXT,
                meeting_at INTEGER,
                location TEXT,
                created_at INTEGER DEFAULT $currentTimeSql,
                updated_at INTEGER DEFAULT $currentTimeSql,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
        """.trimIndent())

        db?.execSQL("""
            CREATE TRIGGER meetings_update_trigger
            AFTER UPDATE ON meetings
            FOR EACH ROW
            BEGIN
                UPDATE meetings
                SET updated_at = $currentTimeSql
                WHERE id = OLD.id;
            END;
        """.trimIndent())

        // Создание таблицы заметок пользователя
        db?.execSQL("""
            CREATE TABLE notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                parent_note_id INTEGER,
                meeting_id INTEGER,
                title TEXT,
                content TEXT,
                created_at INTEGER DEFAULT $currentTimeSql,
                updated_at INTEGER DEFAULT $currentTimeSql,
                priority TEXT DEFAULT '${NotePriority.NORMAL.name}',
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (parent_note_id) REFERENCES notes(id) ON DELETE SET NULL,
                FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE SET NULL
            )
        """.trimIndent())

        db?.execSQL("""
            CREATE TRIGGER notes_update_trigger
            AFTER UPDATE ON notes
            FOR EACH ROW
            BEGIN
                UPDATE notes
                SET updated_at = $currentTimeSql
                WHERE id = OLD.id;
            END;
        """.trimIndent())
    }

    override fun onUpgrade(
        db: SQLiteDatabase?,
        oldVersion: Int,
        newVersion: Int
    ) {
        db?.execSQL("DROP TRIGGER IF EXISTS meetings_update_trigger")
        db?.execSQL("DROP TRIGGER IF EXISTS notes_update_trigger")

        db?.execSQL("DROP TABLE IF EXISTS meetings")
        db?.execSQL("DROP TABLE IF EXISTS notes")
        db?.execSQL("DROP TABLE IF EXISTS users")

        onCreate(db)
    }

    // ========== Вспомогательные методы ==========
    private fun cursorToUser(cursor: Cursor): User {
        return User(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            externalId = cursor.getLong(cursor.getColumnIndexOrThrow("external_id")),
            name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
            login = cursor.getString(cursor.getColumnIndexOrThrow("login")),
            email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
            createdAt = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("created_at")),
            updatedAt = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("updated_at")),
            verified = cursor.getInt(cursor.getColumnIndexOrThrow("verified")) == 1,
            isAdmin = cursor.getInt(cursor.getColumnIndexOrThrow("is_admin")) == 1,
            birthdateAt = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("birthdate_at")),
            gender = GenderType.fromInt(cursor.getInt(cursor.getColumnIndexOrThrow("gender")))
//            birthdateAt = if (cursor.isNull(birthdateIndex)) {
//                null
//            } else {
//                cursor.getLong(birthdateIndex)
//            },
        )
    }

    private fun cursorToMeeting(cursor: Cursor): Meeting {
        return Meeting(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            userId = cursor.getLong(cursor.getColumnIndexOrThrow("user_id")),
            title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
            description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
            meetingAt = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("meeting_at")),
            location = cursor.getString(cursor.getColumnIndexOrThrow("location")),
            createdAt = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("created_at")),
            updatedAt = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("updated_at"))
        )
    }

    private fun cursorToNote(cursor: Cursor): Note {
        return Note(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            userId = cursor.getLong(cursor.getColumnIndexOrThrow("user_id")),
            parentNoteId = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("parent_note_id")),
            meetingId = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("meeting_id")),
            title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
            content = cursor.getString(cursor.getColumnIndexOrThrow("content")),
            createdAt = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("created_at")),
            updatedAt = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("updated_at")),
            priority = NotePriority.fromString(cursor.getString(cursor.getColumnIndexOrThrow("priority")))
//            priority = NotePriority.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("priority")))
        )
    }

    // ========== Методы для users ==========
    fun addUser(user: UserCreate): Long {
        val salt = PasswordHasher.generateSalt()
        val passwordHash = PasswordHasher.hashPassword(user.password, salt)

        val values = ContentValues().apply {
            put("external_id", user.externalId)
            put("name", user.name)
            put("login", user.login)
            put("email", user.email)
            put("created_at", user.createdAt)
            put("updated_at", user.updatedAt)
            put("birthdate_at", user.birthdateAt)
            put("verified", user.verified)
            put("is_admin", user.isAdmin)
            put("gender", user.gender.value)
            put("password", passwordHash)
            put("salt", salt)
        }
        val db = writableDatabase
        val id = db.insert("users", null, values)
        db.close()
        return id
    }

    // Конкатенация пользователя по данным с бэка
    fun getMergedUser(userData: UserCreate): User? {
        val db = writableDatabase
        return try {
            db.transaction {
                var targetUserId: Long? = null

                // Поиск пользователя по externalId
                val cursorById = db.query(
                    "users",
                    arrayOf("id"),
                    "external_id = ?",
                    arrayOf(userData.externalId.toString()),
                    null, null, null
                )
                if (cursorById.moveToFirst()) {
                    targetUserId = cursorById.getLong(cursorById.getColumnIndexOrThrow("id"))
                }
                cursorById.close()

                // Если не найден по externalId, ищем по логину или email
                if (targetUserId == null) {
                    val cursorByLoginEmail = db.query(
                        "users",
                        arrayOf("id"),
                        "login = ? OR email = ?",
                        arrayOf(userData.login, userData.email),
                        null, null, "id ASC" // берём первого по порядку
                    )
                    if (cursorByLoginEmail.moveToFirst()) {
                        targetUserId = cursorByLoginEmail.getLong(cursorByLoginEmail.getColumnIndexOrThrow("id"))
                    }
                    cursorByLoginEmail.close()
                }

                // Если всё ещё нет – создаём нового пользователя
                if (targetUserId == null) {
                    val salt = PasswordHasher.generateSalt()
                    val passHash = userData.password?.let { PasswordHasher.hashPassword(it, salt) }
                    val newUserValues = ContentValues().apply {
                        put("external_id", userData.externalId)
                        put("name", userData.name)
                        put("login", userData.login)
                        put("email", userData.email)
                        put("created_at", userData.createdAt)
                        put("updated_at", userData.updatedAt)
                        put("birthdate_at", userData.birthdateAt)
                        put("verified", userData.verified)
                        put("is_admin", userData.isAdmin)
                        put("gender", userData.gender.value)
                        put("password", passHash)
                        put("salt", salt)
                    }
                    targetUserId = db.insert("users", null, newUserValues)
                    if (targetUserId == -1L) {
                        throw RuntimeException("Failed to insert user")
                    }
                }

                // Обработка дубликатов (других пользователей с таким же login/email, кроме целевого)
                val duplicateCursor = db.query(
                    "users",
                    arrayOf("id"),
                    "(login = ? OR email = ?) AND id != ?",
                    arrayOf(userData.login, userData.email, targetUserId.toString()),
                    null, null, null
                )
                val duplicateIds = mutableListOf<Long>()
                while (duplicateCursor.moveToNext()) {
                    duplicateIds.add(duplicateCursor.getLong(duplicateCursor.getColumnIndexOrThrow("id")))
                }
                duplicateCursor.close()

                for (dupId in duplicateIds) {
                    // Перенос встреч на целевого пользователя
                    val meetingValues = ContentValues().apply { put("user_id", targetUserId) }
                    db.update("meetings", meetingValues, "user_id = ?", arrayOf(dupId.toString()))

                    // Перенос заметок на целевого пользователя
                    val noteValues = ContentValues().apply { put("user_id", targetUserId) }
                    db.update("notes", noteValues, "user_id = ?", arrayOf(dupId.toString()))

                    // Удаление дублирующего пользователя
                    db.delete("users", "id = ?", arrayOf(dupId.toString()))
                }

                // Обновление данных целевого пользователя
                val updateValues = ContentValues().apply {
                    put("external_id", userData.externalId)
                    put("name", userData.name)
                    put("login", userData.login)
                    put("email", userData.email)
                    put("created_at", userData.createdAt)
                    put("updated_at", userData.updatedAt)
                    put("birthdate_at", userData.birthdateAt)
                    put("verified", userData.verified)
                    put("is_admin", userData.isAdmin)
                    put("gender", userData.gender.value)
                    if (userData.password.isNotBlank()) {
                        val salt = PasswordHasher.generateSalt()
                        put("password", PasswordHasher.hashPassword(userData.password, salt))
                        put("salt", salt)
                    }
                }
                db.update("users", updateValues, "id = ?", arrayOf(targetUserId.toString()))

                // Возвращаем обновлённого пользователя
                val resultCursor = db.query("users", null, "id = ?", arrayOf(targetUserId.toString()), null, null, null)
                val resultUser = if (resultCursor.moveToFirst()) cursorToUser(resultCursor) else null
                resultCursor.close()
                resultUser
            }
        } catch (e: Exception) {
            // Любое исключение приводит к откату транзакции и возврату null
            null
        }
    }

    fun getAuthorizationVerifyUser(userAuthData: LoginRequest): User? {
        val db = readableDatabase
        val cursor = db.query(
            "users",
            null,
            "login = ? OR email = ?",
            arrayOf(userAuthData.login, userAuthData.login),
            null, null, null
        )
        return try {
            if (cursor.moveToFirst()) {
                val storedHash = cursor.getString(cursor.getColumnIndexOrThrow("password"))
                val salt = cursor.getString(cursor.getColumnIndexOrThrow("salt"))
                if (PasswordHasher.verifyPassword(userAuthData.password, storedHash, salt)) {
                    cursorToUser(cursor)
                } else {
                    null
                }
            } else {
                null
            }
        } finally {
            cursor.close()
            db.close()
        }
    }

    fun getUserById(id: Long): User? {
        val db = readableDatabase
        val cursor = db.query(
            "users",
            null,
            "id = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        val user = if (cursor.moveToFirst()) cursorToUser(cursor) else null
        cursor.close()
        db.close()
        return user
    }

    fun getUserByLogin(login: String): User? {
        val db = readableDatabase
        val cursor = db.query(
            "users",
            null,
            "login = ?",
            arrayOf(login),
            null, null, null
        )
        val user = if (cursor.moveToFirst()) cursorToUser(cursor) else null
        cursor.close()
        db.close()
        return user
    }

    fun getUserByEmail(email: String): User? {
        val db = readableDatabase
        val cursor = db.query(
            "users",
            null,
            "email = ?",
            arrayOf(email),
            null, null, null
        )
        val user = if (cursor.moveToFirst()) cursorToUser(cursor) else null
        cursor.close()
        db.close()
        return user
    }

    fun updateUser(user: UserUpdate, userId: Long): Int {
        val values = ContentValues().apply {
            if (!user.name.isNullOrEmpty()) {
                put("name", user.name)
            }
            if (!user.login.isNullOrEmpty()) {
                put("login", user.login)
            }
            if (!user.email.isNullOrEmpty()) {
                put("email", user.email)
            }
            if (user.birthdateAt != null) {
                val birthdateAt = if (user.birthdateAt <= TIMESTAMP_1900) { null } else { user.birthdateAt }
                put("birthdate_at", birthdateAt)
            }
            if (!user.newPassword.isNullOrBlank()) {
                val salt = PasswordHasher.generateSalt()
                put("password", PasswordHasher.hashPassword(user.newPassword, salt))
                put("salt", salt)
            }
            if (user.gender != null) {
                put("gender", user.gender.value)
            }
            if (user.verified != null) {
                val verified = if (user.verified == true) { 1 } else { 0 }
                put("verified", verified)
            }
            if (user.isAdmin != null) {
                val isAdmin = if (user.isAdmin == true) { 1 } else { 0 }
                put("is_admin", isAdmin)
            }
        }
        val db = writableDatabase
        val updated = db.update("users", values, "id = ?", arrayOf(userId.toString()))
        db.close()
        return updated
    }

    fun deleteUser(id: Long): Int {
        val db = writableDatabase
        val deleted = db.delete("users", "id = ?", arrayOf(id.toString()))
        db.close()
        return deleted
    }

    // ========== Методы для notes ==========
    fun addNote(note: Note): Long {
        val values = ContentValues().apply {
            put("user_id", note.userId)
            put("parent_note_id", note.parentNoteId)
            put("meeting_id", note.meetingId)
            put("title", note.title)
            put("content", note.content)
            put("created_at", note.createdAt)
            put("updated_at", note.updatedAt)
            put("priority", note.priority.name)
        }
        val db = writableDatabase
        val id = db.insert("notes", null, values)
        db.close()
        return id
    }

    fun getNotesByUser(userId: Long): List<Note> {
        val notes = mutableListOf<Note>()
        val db = readableDatabase
        val cursor = db.query(
            "notes",
            null,
            "user_id = ?",
            arrayOf(userId.toString()),
            null, null, "created_at DESC"
        )
        while (cursor.moveToNext()) {
            notes.add(cursorToNote(cursor))
        }
        cursor.close()
        db.close()
        return notes
    }

    fun getNoteById(id: Long): Note? {
        val db = readableDatabase
        val cursor = db.query(
            "notes",
            null,
            "id = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        val note = if (cursor.moveToFirst()) cursorToNote(cursor) else null
        cursor.close()
        db.close()
        return note
    }

    fun updateNote(note: Note): Int {
        val values = ContentValues().apply {
            put("user_id", note.userId)
            put("parent_note_id", note.parentNoteId)
            put("meeting_id", note.meetingId)
            put("title", note.title)
            put("content", note.content)
            put("created_at", note.createdAt)
            put("updated_at", note.updatedAt)
            put("priority", note.priority.name)
        }
        val db = writableDatabase
        val updated = db.update("notes", values, "id = ?", arrayOf(note.id.toString()))
        db.close()
        return updated
    }

    fun deleteNote(id: Long): Int {
        val db = writableDatabase
        val deleted = db.delete("notes", "id = ?", arrayOf(id.toString()))
        db.close()
        return deleted
    }

    // ========== Методы для meetings ==========
    fun addMeeting(meeting: Meeting): Long {
        val values = ContentValues().apply {
            put("user_id", meeting.userId)
            put("title", meeting.title)
            put("description", meeting.description)
            put("meeting_at", meeting.meetingAt)
            put("location", meeting.location)
            put("created_at", meeting.createdAt)
            put("updated_at", meeting.updatedAt)
        }
        val db = writableDatabase
        val id = db.insert("meetings", null, values)
        db.close()
        return id
    }

    fun getMeetingsByUser(userId: Long): List<Meeting> {
        val meetings = mutableListOf<Meeting>()
        val db = readableDatabase
        val cursor = db.query(
            "meetings",
            null,
            "user_id = ?",
            arrayOf(userId.toString()),
            null, null, "meeting_at ASC"
        )
        while (cursor.moveToNext()) {
            meetings.add(cursorToMeeting(cursor))
        }
        cursor.close()
        db.close()
        return meetings
    }

    fun getMeetingById(id: Long): Meeting? {
        val db = readableDatabase
        val cursor = db.query(
            "meetings",
            null,
            "id = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        val meeting = if (cursor.moveToFirst()) cursorToMeeting(cursor) else null
        cursor.close()
        db.close()
        return meeting
    }

    fun updateMeeting(meeting: Meeting): Int {
        val values = ContentValues().apply {
            put("user_id", meeting.userId)
            put("title", meeting.title)
            put("description", meeting.description)
            put("meeting_at", meeting.meetingAt)
            put("location", meeting.location)
            put("created_at", meeting.createdAt)
            put("updated_at", meeting.updatedAt)
        }
        val db = writableDatabase
        val updated = db.update("meetings", values, "id = ?", arrayOf(meeting.id.toString()))
        db.close()
        return updated
    }

    fun deleteMeeting(id: Long): Int {
        val db = writableDatabase
        val deleted = db.delete("meetings", "id = ?", arrayOf(id.toString()))
        db.close()
        return deleted
    }

}