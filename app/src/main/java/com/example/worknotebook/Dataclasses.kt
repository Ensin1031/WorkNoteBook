package com.example.worknotebook

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type


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
open class UserBase(
    open val name: String,
    open val login: String,
    open val email: String,
)

data class UserUpdate(
    @SerializedName("name") val name: String? = null,
    @SerializedName("login") val login: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("birthdate_at") val birthdateAt: Long? = null,
    @SerializedName("gender") val gender: GenderType? = null,
    @SerializedName("old_password") val oldPassword: String? = null,
    @SerializedName("new_password") val newPassword: String? = null,
    @SerializedName("updated_at") var updatedAt: Long? = null,
    @SerializedName("verified") var verified: Boolean? = null,
    @SerializedName("is_admin") var isAdmin: Boolean? = null,
)

data class UserCreate(
    @SerializedName("external_id") val externalId: Long,
    @SerializedName("name") val name: String,
    @SerializedName("login") val login: String,
    @SerializedName("email") val email: String,
    @SerializedName("verified") val verified: Boolean,
    @SerializedName("is_admin") val isAdmin: Boolean,
    @SerializedName("created_at") val createdAt: Long?,
    @SerializedName("updated_at") val updatedAt: Long?,
    @SerializedName("birthdate_at") val birthdateAt: Long?,
    @SerializedName("gender") val gender: GenderType,
    @SerializedName("password") val password: String,
)

data class User(
    @SerializedName("id") val id: Long?,
    @SerializedName("external_id") val externalId: Long,
    @SerializedName("name") val name: String,
    @SerializedName("login") val login: String,
    @SerializedName("email") val email: String,
    @SerializedName("verified") val verified: Boolean,
    @SerializedName("is_admin") val isAdmin: Boolean,
    @SerializedName("created_at") val createdAt: Long?,
    @SerializedName("updated_at") val updatedAt: Long?,
    @SerializedName("birthdate_at") val birthdateAt: Long?,
    @SerializedName("gender") val gender: GenderType,
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
    LOW(R.color.greenColor, R.string.low_priority);

    companion object {
        fun fromString(value: String?): NotePriority {
            return entries.find { it.name == value } ?: NORMAL // дефолт NORMAL
        }
    }
}


enum class GenderType(val value: Int, val labelResId: Int) {
    UNSET(0, R.string.gender_not_selected),
    MALE(1, R.string.gender_male),
    FEMALE(2, R.string.gender_female);

    companion object {
        // Получение значения по Int, дефолт UNSET
        fun fromInt(value: Int?): GenderType {
            return entries.find { it.value == value } ?: UNSET
        }
    }
}


class GenderTypeAdapter : JsonDeserializer<GenderType>, JsonSerializer<GenderType> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): GenderType {
        val value = json?.asInt ?: return GenderType.UNSET
        return GenderType.fromInt(value)
    }

    override fun serialize(
        src: GenderType?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src?.value)
    }
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
