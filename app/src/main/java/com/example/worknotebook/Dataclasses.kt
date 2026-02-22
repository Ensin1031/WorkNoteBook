package com.example.worknotebook

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName


/** Дата-класс, ответ аутентефикации пользователя */
data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("user") val user: User
)


/** Дата-класс ошибок, полученных с бэка */
data class ErrorResponse(
    @SerializedName("detail") val detail: String?
)

data class ErrorDetailWrapper(
    @SerializedName("detail") val detail: Map<String, String>? = null
)

/** Дата-класс Регистрации пользователя */
data class RegisterRequest(
    val name: String,
    val login: String,
    val email: String,
    val password: String
)
data class ErrorRegisterResponse(
    @SerializedName("name") var nameError: String? = null,
    @SerializedName("login") var loginError: String? = null,
    @SerializedName("email") var emailError: String? = null,
    @SerializedName("password") var passwordError: String? = null,
    @SerializedName("detail") var unknownError: String? = null
)


/** Дата-класс Аутентификации пользователя */
data class LoginRequest(
    val login: String,
    val password: String
)


/** Дата-классы Пользователя */
data class UserRegister(
    override val name: String,
    override val login: String,
    override val email: String,
    val password: String,
) : UserBase(name, login, email)

open class UserBase(
    open val name: String,
    open val login: String,
    open val email: String,
)

data class UserCreateOrUpdate(
    val externalId: Long,
    override val name: String,
    override val login: String,
    override val email: String,
    val password: String? = null,
) : UserBase(name, login, email)

data class User(
    @SerializedName("id") val id: Long?,
    @SerializedName("external_id") val externalId: Long,
    @SerializedName("name") val name: String,
    @SerializedName("login") val login: String,
    @SerializedName("email") val email: String,
)


/** Дата-класс мероприятия пользователя */
data class Meeting(
    val id: Long? = null,
    val userId: Long,
    val title: String,
    val description: String,
    val meetingAt: Long? = null,
    val location: String,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readString() ?: "",
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readValue(Long::class.java.classLoader) as? Long
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeLong(userId)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeValue(meetingAt)
        parcel.writeValue(location)
        parcel.writeValue(createdAt)
        parcel.writeValue(updatedAt)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Meeting> {
        override fun createFromParcel(parcel: Parcel): Meeting = Meeting(parcel)
        override fun newArray(size: Int): Array<Meeting?> = arrayOfNulls(size)
    }
}

enum class NotePriority(val colorResId: Int, val displayNameResId: Int) {
    HIGH(R.color.redColor, R.string.high_priority),
    NORMAL(R.color.yellowColor, R.string.normal_priority),
    LOW(R.color.greenColor, R.string.low_priority)
}


/** Дата-класс заметки пользователя */
data class Note(
    val id: Long? = null,
    val userId: Long,
    val parentNoteId: Long? = null,
    val meetingId: Long? = null,
    val title: String,
    val content: String,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    var priority: NotePriority
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readLong(),
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readValue(Long::class.java.classLoader) as? Long,
        NotePriority.valueOf(parcel.readString() ?: NotePriority.NORMAL.name)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeLong(userId)
        parcel.writeValue(parentNoteId)
        parcel.writeValue(meetingId)
        parcel.writeString(title)
        parcel.writeString(content)
        parcel.writeValue(createdAt)
        parcel.writeValue(updatedAt)
        parcel.writeValue(priority.name)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Note> {
        override fun createFromParcel(parcel: Parcel): Note = Note(parcel)
        override fun newArray(size: Int): Array<Note?> = arrayOfNulls(size)
    }
}
