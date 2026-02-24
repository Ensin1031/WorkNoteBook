package com.example.worknotebook

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.core.content.edit
import org.json.JSONObject


object JwtUtils {

    fun isExpired(token: String, userExternalId: Long?): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return true
            if (userExternalId == null) return true

            val payload = parts[1]
            val decoded = Base64.decode(payload, Base64.URL_SAFE)
            val json = JSONObject(String(decoded))

            val exp = json.optLong("exp", 0L)
            val now = System.currentTimeMillis() / 1000

            val externalId = json.optLong("user_id", 0L)

            (now >= exp) && (externalId == userExternalId)
        } catch (e: Exception) {
            true
        }
    }
}


class UserSessionManager(context: Context) {

    private val dbHelper: DBHelper = DBHelper(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {

        private const val KEY_USER_ID = "user_id"
        private const val KEY_TOKEN = "auth_token"

        @Volatile
        private var instance: UserSessionManager? = null

        fun getInstance(context: Context): UserSessionManager {
            return instance ?: synchronized(this) {
                instance ?: UserSessionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    init {
        loadUserAndTokenFromPrefs()
    }

    /**
     * Загружает пользователя и токен из SharedPreferences.
     * Если токен просрочен – удаляет его.
     */
    private fun loadUserAndTokenFromPrefs() {
        val userId = prefs.getLong(KEY_USER_ID, -1)
        if (userId != -1L) {
            val user = dbHelper.getUserById(userId)
            if (user != null) {
                _currentUser.value = user
                // Проверяем токен и удаляем, если недействителен
                val token = prefs.getString(KEY_TOKEN, null)
                if (token != null && JwtUtils.isExpired(token, user.externalId)) {
                    clearToken()
                }
            } else {
                // Пользователь не найден в БД – очищаем всю сессию
                prefs.edit { clear() }
            }
        }
    }

    /**
     * Сохраняет пользователя и опционально токен.
     * Токен проверяется на валидность перед сохранением.
     */
    fun setUserAndToken(user: User, token: String?) {
        _currentUser.value = user
        prefs.edit { putLong(KEY_USER_ID, user.id!!) }
        if (token != null && !JwtUtils.isExpired(token, user.externalId)) {
            prefs.edit { putString(KEY_TOKEN, token) }
        } else {
            clearToken()
        }
    }

    /**
     * Обновляет только токен (например, после refresh).
     */
    fun updateToken(user: User, token: String?) {
        if (token != null && !JwtUtils.isExpired(token, user.externalId)) {
            prefs.edit { putString(KEY_TOKEN, token) }
        } else {
            clearToken()
        }
    }

    /**
     * Возвращает текущий токен, если он существует и не просрочен.
     * Если просрочен – удаляет и возвращает null.
     */
    fun getToken(): String? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        return if (!JwtUtils.isExpired(token, _currentUser.value?.externalId)) {
            token
        } else {
            clearToken()
            null
        }
    }

    /**
     * Проверяет, действителен ли текущий токен.
     */
    fun isTokenValid(): Boolean = getToken() != null

    /**
     * Признак наличия валидного токена (онлайн-режим).
     */
    val isOnline: Boolean
        get() = isTokenValid()

    /**
     * Удаляет только токен (пользователь остаётся).
     */
    fun clearToken() {
        prefs.edit { remove(KEY_TOKEN) }
    }

    /**
     * Полная очистка сессии (пользователь + токен).
     */
    fun logout() {
        _currentUser.value = null
        prefs.edit { clear() }
    }

    // ===== Методы получения / проверки данных пользователя =====
    fun getMergedUser(userData: UserCreate): User? {
        return dbHelper.getMergedUser(userData)
    }
    fun updateSessionUser(userData: UserUpdate): User? {
        val userId = prefs.getLong(KEY_USER_ID, -1)
        if (userId != -1L) {
            val updated = dbHelper.updateUser(user = userData, userId = userId)
            if (updated > 0) {
                val user = dbHelper.getUserById(userId)
                _currentUser.value = user
                return user
            }
        }
        return null
    }
    fun getAuthorizationVerifyUser(userAuthData: LoginRequest): User? {
        return dbHelper.getAuthorizationVerifyUser(userAuthData)
    }
    fun getCheckedUser(login: String): User? {
        return getCheckedUserByLogin(login) ?: getCheckedUserByEmail(login)
    }
    fun getCheckedUserByLogin(login: String): User? {
        return dbHelper.getUserByLogin(login)
    }
    fun getCheckedUserByEmail(login: String): User? {
        return dbHelper.getUserByEmail(login)
    }

    // ===== Методы аутентификации =====
    fun login(user: User, token: String?): Boolean {
        return if (user.id != null) {
            setUserAndToken(user, token)
            true
        } else {
            false
        }
    }

    fun register(user: UserCreate, token: String?): Boolean {
        val existing = dbHelper.getUserByLogin(user.login) ?: dbHelper.getUserByEmail(user.email)
        if (existing != null) return false
        val id = dbHelper.addUser(user)
        return if (id != -1L) {
            // После регистрации нужно получить полного пользователя из БД
            val newUser = dbHelper.getUserById(id)
            if (newUser != null) {
                setUserAndToken(newUser, token)
                true
            } else false
        } else false
    }

    // ===== Методы для работы с заметками текущего пользователя =====
    fun getNotes(): List<Note> {
        val userId = _currentUser.value?.id ?: return emptyList()
        return dbHelper.getNotesByUser(userId)
    }

    fun addNote(note: Note): Long {
        return dbHelper.addNote(note)
    }

    fun updateNote(note: Note): Int {
        return dbHelper.updateNote(note)
    }

    fun deleteNote(noteId: Long): Int {
        return dbHelper.deleteNote(noteId)
    }

    fun getNoteById(noteId: Long): Note? {
        return dbHelper.getNoteById(noteId)
    }

    // ===== Методы для работы со встречами текущего пользователя =====
    fun getMeetings(): List<Meeting> {
        val userId = _currentUser.value?.id ?: return emptyList()
        return dbHelper.getMeetingsByUser(userId)
    }

    fun addMeeting(meeting: Meeting): Long {
        return dbHelper.addMeeting(meeting)
    }

    fun updateMeeting(meeting: Meeting): Int {
        return dbHelper.updateMeeting(meeting)
    }

    fun deleteMeeting(meetingId: Long): Int {
        return dbHelper.deleteMeeting(meetingId)
    }

    fun getMeetingById(meetingId: Long): Meeting? {
        return dbHelper.getMeetingById(meetingId)
    }

    fun getUser(): User? = _currentUser.value
}
