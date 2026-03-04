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

const val USER_TABLE = "users"
const val USER_SETTINGS_TABLE = "user_settings"
const val MEETING_TABLE = "meetings"
const val NOTE_TABLE = "notes"


class DBHelper(
    context: Context,
    factory: SQLiteDatabase.CursorFactory? = null,
) : SQLiteOpenHelper(context, "workbook.db", factory, 7) {

    override fun onCreate(db: SQLiteDatabase?) {
        Log.d("DB_DEBUG", "-----===== onCreate called =====-----")  // TODO нужен на этапе разработки. потом убрать.
        // Создание таблицы пользователя
        val createUsersTable = """
            CREATE TABLE $USER_TABLE (
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

        // Создание таблицы настроек пользователя
        db?.execSQL("""
            CREATE TABLE $USER_SETTINGS_TABLE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER UNIQUE,
                sync_meetings_immediately INTEGER DEFAULT 1,
                sync_notes_immediately INTEGER DEFAULT 1,
                FOREIGN KEY (user_id) REFERENCES $USER_TABLE(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // Создание таблицы встреч пользователя
        db?.execSQL("""
            CREATE TABLE $MEETING_TABLE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                external_id INTEGER DEFAULT NULL,
                user_id INTEGER NOT NULL,
                external_user_id INTEGER DEFAULT NULL,
        
                title TEXT,
                description TEXT,
                location TEXT,
                start_date INTEGER NOT NULL,
                end_date INTEGER DEFAULT NULL,
                start_time INTEGER NOT NULL,
                end_time INTEGER DEFAULT NULL,
        
                created_at INTEGER DEFAULT NULL,
                updated_at INTEGER DEFAULT NULL,
        
                is_active INTEGER DEFAULT 1,
                is_sync INTEGER DEFAULT 0,
        
                FOREIGN KEY (user_id) REFERENCES $USER_TABLE(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // Создание таблицы заметок пользователя
        db?.execSQL("""
            CREATE TABLE $NOTE_TABLE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                external_id INTEGER DEFAULT NULL,
                user_id INTEGER NOT NULL,
                external_user_id INTEGER DEFAULT NULL,
                parent_note_id INTEGER DEFAULT NULL,
                external_parent_note_id INTEGER DEFAULT NULL,
                meeting_id INTEGER DEFAULT NULL,
                external_meeting_id INTEGER DEFAULT NULL,
                
                title TEXT,
                content TEXT,
                priority TEXT DEFAULT '${NotePriority.NORMAL.name}',
                
                created_at INTEGER DEFAULT NULL,
                updated_at INTEGER DEFAULT NULL,
                
                is_active INTEGER DEFAULT 1,
                is_sync INTEGER DEFAULT 0,
                
                FOREIGN KEY (user_id) REFERENCES $USER_TABLE(id) ON DELETE CASCADE,
                FOREIGN KEY (parent_note_id) REFERENCES $NOTE_TABLE(id) ON DELETE SET NULL,
                FOREIGN KEY (meeting_id) REFERENCES $MEETING_TABLE(id) ON DELETE SET NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(
        db: SQLiteDatabase?,
        oldVersion: Int,
        newVersion: Int
    ) {
        db?.execSQL("DROP TABLE IF EXISTS $MEETING_TABLE")
        db?.execSQL("DROP TABLE IF EXISTS $NOTE_TABLE")
        db?.execSQL("DROP TABLE IF EXISTS $USER_SETTINGS_TABLE")
        db?.execSQL("DROP TABLE IF EXISTS $USER_TABLE")

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
        )
    }

    private fun cursorToUserSettings(cursor: Cursor): UserSettings {
        return UserSettings(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            userId = cursor.getLong(cursor.getColumnIndexOrThrow("user_id")),
            syncMeetingsImmediately = cursor.getInt(cursor.getColumnIndexOrThrow("sync_meetings_immediately")) == 1,
            syncNotesImmediately = cursor.getInt(cursor.getColumnIndexOrThrow("sync_notes_immediately")) == 1,
        )
    }

    private fun cursorToMeeting(cursor: Cursor): Meeting {
        return Meeting(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            externalId = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("external_id")),
            userId = cursor.getLong(cursor.getColumnIndexOrThrow("user_id")),
            externalUserId = cursor.getLong(cursor.getColumnIndexOrThrow("external_user_id")),
            title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
            description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
            startDate = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("start_date")),
            endDate = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("end_date")),
            startTime = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("start_time")),
            endTime = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("end_time")),
            location = cursor.getString(cursor.getColumnIndexOrThrow("location")),
            createdAt = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("created_at")),
            updatedAt = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("updated_at")),
            isActive = cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1,
            isSync = cursor.getInt(cursor.getColumnIndexOrThrow("is_sync")) == 1,
        )
    }

    private fun cursorToNote(cursor: Cursor): Note {
        return Note(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            externalId = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("external_id")),
            userId = cursor.getLong(cursor.getColumnIndexOrThrow("user_id")),
            externalUserId = cursor.getLong(cursor.getColumnIndexOrThrow("external_user_id")),
            parentNoteId = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("parent_note_id")),
            externalParentNoteId = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("external_parent_note_id")),
            meetingId = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("meeting_id")),
            externalMeetingId = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("external_meeting_id")),
            title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
            content = cursor.getString(cursor.getColumnIndexOrThrow("content")),
            createdAt = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("created_at")),
            updatedAt = cursor.getLongOrNull(cursor.getColumnIndexOrThrow("updated_at")),
            isActive = cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1,
            priority = NotePriority.fromString(cursor.getString(cursor.getColumnIndexOrThrow("priority"))),
            isSync = cursor.getInt(cursor.getColumnIndexOrThrow("is_sync")) == 1,
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
        val id = db.insert(USER_TABLE, null, values)
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
                    USER_TABLE,
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
                        USER_TABLE,
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
                    targetUserId = db.insert(USER_TABLE, null, newUserValues)
                    if (targetUserId == -1L) {
                        throw RuntimeException("Failed to insert user")
                    }
                }

                // Обработка дубликатов (других пользователей с таким же login/email, кроме целевого)
                val duplicateCursor = db.query(
                    USER_TABLE,
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
                    db.update(MEETING_TABLE, meetingValues, "user_id = ?", arrayOf(dupId.toString()))

                    // Перенос заметок на целевого пользователя
                    val noteValues = ContentValues().apply { put("user_id", targetUserId) }
                    db.update(NOTE_TABLE, noteValues, "user_id = ?", arrayOf(dupId.toString()))

                    // Удаление дублирующего пользователя
                    db.delete(USER_TABLE, "id = ?", arrayOf(dupId.toString()))
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
                db.update(USER_TABLE, updateValues, "id = ?", arrayOf(targetUserId.toString()))

                // Возвращаем обновлённого пользователя
                val resultCursor = db.query(USER_TABLE, null, "id = ?", arrayOf(targetUserId.toString()), null, null, null)
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
            USER_TABLE,
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
            USER_TABLE,
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
            USER_TABLE,
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
            USER_TABLE,
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
        val updated = db.update(USER_TABLE, values, "id = ?", arrayOf(userId.toString()))
        db.close()
        return updated
    }

    // ========== Методы для user_settings ==========
    fun getOrCreateUserSettings(userId: Long): UserSettings {
        writableDatabase.use { db ->

            db.execSQL(
                "INSERT OR IGNORE INTO $USER_SETTINGS_TABLE(user_id) VALUES(?)",
                arrayOf(userId)
            )

            db.query(
                USER_SETTINGS_TABLE,
                null,
                "user_id = ?",
                arrayOf(userId.toString()),
                null, null, null
            ).use { cursor ->

                if (cursor.moveToFirst()) {
                    return cursorToUserSettings(cursor)
                } else {
                    error("Системная ошибка")
                }
            }
        }
    }
    fun updateUserSettings(userSettingsId: Long, syncMeetingsImmediately: Boolean, syncNotesImmediately: Boolean): Boolean {
        val values = ContentValues().apply {
            put("sync_meetings_immediately", if (syncMeetingsImmediately) { 1 } else { 0 })
            put("sync_notes_immediately", if (syncNotesImmediately) { 1 } else { 0 })
        }
        val db = writableDatabase
        val updated = db.update(USER_SETTINGS_TABLE, values, "id = ?", arrayOf(userSettingsId.toString()))
        db.close()
        return updated >= 1
    }

    // ========== Методы для notes ==========
    fun addNote(note: Note): Long {
        val values = ContentValues().apply {
            put("user_id", note.userId)
            put("external_user_id", note.externalUserId)
            put("external_id", note.externalId)
            put("parent_note_id", note.parentNoteId)
            put("external_parent_note_id", note.externalParentNoteId)
            put("meeting_id", note.meetingId)
            put("external_meeting_id", note.externalMeetingId)
            put("title", note.title)
            put("content", note.content)
            put("created_at", note.createdAt)
            put("updated_at", note.updatedAt)
            put("priority", note.priority.name)
            put("is_active", note.isActive)
            put("is_sync", note.isSync)
        }
        val db = writableDatabase
        val id = db.insert(NOTE_TABLE, null, values)
        db.close()
        return id
    }

    fun getNotesByUser(userId: Long, filters: NodeFilters, parentNoteId: Long? = null, meetingId: Long? = null): List<Note> {
        val notes = mutableListOf<Note>()
        val db = readableDatabase

        val selectionParts = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()

        // Базовое условие фильтрации
        selectionParts.add("user_id = ?")
        selectionArgs.add(userId.toString())

        if (parentNoteId != null) {
            selectionParts.add("parent_note_id = ?")
            selectionArgs.add(parentNoteId.toString())
        }

        if (meetingId != null) {
            selectionParts.add("meeting_id = ?")
            selectionArgs.add(meetingId.toString())
        }

        // Фильтр по наличию родительских связей
        if (filters.onlyWithoutParents) {
            selectionParts.add("parent_note_id IS NULL AND meeting_id IS NULL")
        }

        // Фильтр по приоритету
        val priorityConditions = mutableListOf<String>()
        if (filters.byPriorityOnlyHigh) priorityConditions.add("priority = 'HIGH'")
        if (filters.byPriorityOnlyNormal) priorityConditions.add("priority = 'NORMAL'")
        if (filters.byPriorityOnlyLow) priorityConditions.add("priority = 'LOW'")
        if (priorityConditions.size == 1) {
            selectionParts.add(priorityConditions.joinToString())
        } else if (priorityConditions.size > 1) {
            // Если выбрано несколько приоритетов, объединяем через OR
            val prioritiesStr = priorityConditions.joinToString(" OR ")
            selectionParts.add("($prioritiesStr)")
        }

        // Фильтр по поисковому запросу
        if (filters.search.isNotBlank()) {
            val searchTerm = "%${filters.search}%"
            selectionParts.add("(title LIKE ? OR content LIKE ?)")
            selectionArgs.add(searchTerm)
            selectionArgs.add(searchTerm)
        }

        // Фильтр, активна ли запись. Случаи, когда активны оба фильтра или оба неактивны - отсекаем.
        // По умолчанию считаем в этих случаях, что нужно показывать и те и те
        if (filters.viewOnlyActive && !filters.viewOnlyNotActive) {
            selectionParts.add("is_active = 1")
        } else if (filters.viewOnlyNotActive && !filters.viewOnlyActive) {
            selectionParts.add("is_active = 0")
        }

        // Фильтр по синхронизации. Случаи, когда активны оба фильтра или оба неактивны - отсекаем.
        // По умолчанию считаем в этих случаях, что нужно показывать и те и те
        if (filters.viewOnlySyncByBack && !filters.viewOnlyNotSyncByBack) {
            selectionParts.add("is_sync = 1")
        }
        if (filters.viewOnlyNotSyncByBack && !filters.viewOnlySyncByBack) {
            selectionParts.add("is_sync = 0")
        }

        // Построение ORDER BY
        val orderBy = buildString {
            // По активности
            when {
                filters.byActiveDesc -> append("is_active DESC")
                filters.byActiveAsc -> append("is_active ASC")
                else -> {}
            }
            // По факту синхронизации с бэком
            when {
                filters.bySyncByBackDesc -> {
                    if (isNotEmpty()) append(", ")
                    append("is_sync DESC")
                }
                filters.bySyncByBackAsc -> {
                    if (isNotEmpty()) append(", ")
                    append("is_sync ASC")
                }
                else -> {}
            }
            // По приоритету
            val priorityPref = "CASE priority WHEN 'HIGH' THEN 3 WHEN 'NORMAL' THEN 2 WHEN 'LOW' THEN 1 ELSE 0 END"
            when {
                filters.byPriorityDesc -> {
                    if (isNotEmpty()) append(", ")
                    append("$priorityPref DESC")
                }
                filters.byPriorityAsc -> {
                    if (isNotEmpty()) append(", ")
                    append("$priorityPref ASC")
                }
                else -> {}
            }
            // По дате обновления
            when {
                filters.byUpdatedAtDesc -> {
                    if (isNotEmpty()) append(", ")
                    append("updated_at DESC")
                }
                filters.byUpdatedAtAsc -> {
                    if (isNotEmpty()) append(", ")
                    append("updated_at ASC")
                }
                else -> {}
            }
            // Если ни один не задан, сортируем по умолчанию
            if (isEmpty()) {
                append("created_at DESC")  // TODO подумать, правильно ли так?
            }
        }

        val cursor = db.query(
            NOTE_TABLE, null,
            selectionParts.joinToString(" AND "),
            selectionArgs.toTypedArray(),
            null, null, orderBy
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
            NOTE_TABLE,
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
            put("external_user_id", note.externalUserId)
            put("external_id", note.externalId)
            put("parent_note_id", note.parentNoteId)
            put("external_parent_note_id", note.externalParentNoteId)
            put("meeting_id", note.meetingId)
            put("external_meeting_id", note.externalMeetingId)
            put("title", note.title)
            put("content", note.content)
            put("created_at", note.createdAt)
            put("updated_at", note.updatedAt)
            put("priority", note.priority.name)
            put("is_active", note.isActive)
            put("is_sync", note.isSync)
        }
        val db = writableDatabase
        val updated = db.update(NOTE_TABLE, values, "id = ?", arrayOf(note.id.toString()))
        db.close()
        return updated
    }

    fun deleteNote(id: Long, archive: Boolean, isSync: Boolean): Int {
        val db = writableDatabase
        var deleted: Int
        if (archive) {
            val values = ContentValues().apply {
                put("is_active", 0)
                put("is_sync", if (isSync) { 1 } else { 0 })
            }
            deleted = db.update(NOTE_TABLE, values, "id = ?", arrayOf(id.toString()))
        } else {
            deleted = db.delete(NOTE_TABLE, "id = ?", arrayOf(id.toString()))
        }
        db.close()
        return deleted
    }

    // ========== Методы для meetings ==========
    fun addMeeting(meeting: Meeting): Long {
        val values = ContentValues().apply {
            put("user_id", meeting.userId)
            put("external_user_id", meeting.externalUserId)
            put("external_id", meeting.externalId)
            put("title", meeting.title)
            put("description", meeting.description)
            put("start_date", meeting.startDate)
            put("end_date", meeting.endDate)
            put("start_time", meeting.startTime)
            put("end_time", meeting.endTime)
            put("location", meeting.location)
            put("created_at", meeting.createdAt)
            put("updated_at", meeting.updatedAt)
            put("is_active", meeting.isActive)
            put("is_sync", meeting.isSync)
        }
        val db = writableDatabase
        val id = db.insert(MEETING_TABLE, null, values)
        db.close()
        return id
    }

    fun getMeetingsByUser(userId: Long, filters: MeetingFilters): List<Meeting> {
        val meetings = mutableListOf<Meeting>()
        val db = readableDatabase
        val cursor = db.query(
            MEETING_TABLE,
            null,
            "user_id = ?",
            arrayOf(userId.toString()),
            null, null, "start_date ASC"
        )
        while (cursor.moveToNext()) {
            meetings.add(cursorToMeeting(cursor))
        }
        cursor.close()
        db.close()
        return meetings
    }

    fun getMeetingsInDateRange(userId: Long, start: Long, end: Long, filters: MeetingFilters): List<Meeting> {
        val db = readableDatabase

        val selectionParts = mutableListOf("user_id = ? AND start_date BETWEEN ? AND ?")
        val selectionArgs = mutableListOf(userId.toString(), start.toString(), end.toString())

        // Фильтр, активна ли запись. Случаи, когда активны оба фильтра или оба неактивны - отсекаем.
        // По умолчанию считаем в этих случаях, что нужно показывать и те и те
        if (filters.viewOnlyActive && !filters.viewOnlyNotActive) {
            selectionParts.add("is_active = 1")
        } else if (filters.viewOnlyNotActive && !filters.viewOnlyActive) {
            selectionParts.add("is_active = 0")
        }

        // Фильтр по синхронизации. Случаи, когда активны оба фильтра или оба неактивны - отсекаем.
        // По умолчанию считаем в этих случаях, что нужно показывать и те и те
        if (filters.viewOnlySyncByBack && !filters.viewOnlyNotSyncByBack) {
            selectionParts.add("is_sync = 1")
        }
        if (filters.viewOnlyNotSyncByBack && !filters.viewOnlySyncByBack) {
            selectionParts.add("is_sync = 0")
        }

        val cursor = db.query(
            MEETING_TABLE,
            null,
            selectionParts.joinToString(" AND "),
            selectionArgs.toTypedArray(),
            null,
            null,
            "start_date ASC, start_time ASC"
        )
        val meetings = mutableListOf<Meeting>()
        while (cursor.moveToNext()) {
            meetings.add(cursorToMeeting(cursor))
        }
        cursor.close()
        return meetings
    }

    fun getMeetingById(id: Long): Meeting? {
        val db = readableDatabase
        val cursor = db.query(
            MEETING_TABLE,
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
            put("external_id", meeting.externalId)
            put("user_id", meeting.userId)
            put("external_user_id", meeting.externalUserId)
            put("title", meeting.title)
            put("description", meeting.description)
            put("start_date", meeting.startDate)
            put("end_date", meeting.endDate)
            put("start_time", meeting.startTime)
            put("end_time", meeting.endTime)
            put("location", meeting.location)
            put("created_at", meeting.createdAt)
            put("updated_at", meeting.updatedAt)
            put("is_active", meeting.isActive)
            put("is_sync", meeting.isSync)
        }
        val db = writableDatabase
        val updated = db.update(MEETING_TABLE, values, "id = ?", arrayOf(meeting.id.toString()))
        db.close()
        return updated
    }

    fun deleteMeeting(id: Long, archive: Boolean, isSync: Boolean): Int {
        val db = writableDatabase
        var deleted: Int
        if (archive) {
            val values = ContentValues().apply {
                put("is_active", 0)
                put("is_sync", if (isSync) { 1 } else { 0 })
            }
            deleted = db.update(MEETING_TABLE, values, "id = ?", arrayOf(id.toString()))
        } else {
            deleted = db.delete(MEETING_TABLE, "id = ?", arrayOf(id.toString()))
        }
        db.close()
        return deleted
    }

}
