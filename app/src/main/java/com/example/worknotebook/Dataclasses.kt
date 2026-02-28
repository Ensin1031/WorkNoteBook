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

/** Дата-класс настройки пользователя */
data class UserSettings(
    @SerializedName("id") val id: Long,
    @SerializedName("user_id") val userId: Long,
    @SerializedName("sync_meetings_immediately") val syncMeetingsImmediately: Boolean,
    @SerializedName("sync_notes_immediately") val syncNotesImmediately: Boolean,
)


/** Дата-класс мероприятия пользователя */
data class MeetingFilters(
    @SerializedName("by_meeting_at_desc") var byMeetingAtDesc: Boolean = false,
    @SerializedName("by_meeting_at_asc") var byMeetingAtAsc: Boolean = false,
    @SerializedName("by_updated_at_desc") var byUpdatedAtDesc: Boolean = false,
    @SerializedName("by_updated_at_asc") var byUpdatedAtAsc: Boolean = false,
    @SerializedName("by_active_desc") var byActiveDesc: Boolean = false,
    @SerializedName("by_active_asc") var byActiveAsc: Boolean = false,
    @SerializedName("view_only_active") var viewOnlyActive: Boolean = false,
    @SerializedName("view_only_not_active") var viewOnlyNotActive: Boolean = false,
    @SerializedName("by_sync_by_back_desc") var bySyncByBackDesc: Boolean = false,
    @SerializedName("by_sync_by_back_asc") var bySyncByBackAsc: Boolean = false,
    @SerializedName("view_only_sync_by_back") var viewOnlySyncByBack: Boolean = false,
    @SerializedName("view_only_not_sync_by_back") var viewOnlyNotSyncByBack: Boolean = false,
    @SerializedName("search") var search: String = "",
) : Parcelable {
    constructor(parcel: Parcel) : this(
        byMeetingAtDesc = parcel.readByte() != 0.toByte(),
        byMeetingAtAsc = parcel.readByte() != 0.toByte(),
        byUpdatedAtDesc = parcel.readByte() != 0.toByte(),
        byUpdatedAtAsc = parcel.readByte() != 0.toByte(),
        byActiveDesc = parcel.readByte() != 0.toByte(),
        byActiveAsc = parcel.readByte() != 0.toByte(),
        viewOnlyActive = parcel.readByte() != 0.toByte(),
        viewOnlyNotActive = parcel.readByte() != 0.toByte(),
        bySyncByBackDesc = parcel.readByte() != 0.toByte(),
        bySyncByBackAsc = parcel.readByte() != 0.toByte(),
        viewOnlySyncByBack = parcel.readByte() != 0.toByte(),
        viewOnlyNotSyncByBack = parcel.readByte() != 0.toByte(),
        search = parcel.readString() ?: ""
    )
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (byMeetingAtDesc) 1 else 0)
        parcel.writeByte(if (byMeetingAtAsc) 1 else 0)
        parcel.writeByte(if (byUpdatedAtDesc) 1 else 0)
        parcel.writeByte(if (byUpdatedAtAsc) 1 else 0)
        parcel.writeByte(if (byActiveDesc) 1 else 0)
        parcel.writeByte(if (byActiveAsc) 1 else 0)
        parcel.writeByte(if (viewOnlyActive) 1 else 0)
        parcel.writeByte(if (viewOnlyNotActive) 1 else 0)
        parcel.writeByte(if (bySyncByBackDesc) 1 else 0)
        parcel.writeByte(if (bySyncByBackAsc) 1 else 0)
        parcel.writeByte(if (viewOnlySyncByBack) 1 else 0)
        parcel.writeByte(if (viewOnlyNotSyncByBack) 1 else 0)
        parcel.writeString(search)
    }
    override fun describeContents(): Int = 0
    companion object CREATOR : Parcelable.Creator<MeetingFilters> {
        override fun createFromParcel(parcel: Parcel): MeetingFilters = MeetingFilters(parcel)
        override fun newArray(size: Int): Array<MeetingFilters?> = arrayOfNulls(size)
    }
}
data class Meeting(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("external_id") val externalId: Long? = null,
    @SerializedName("user_id") val userId: Long,
    @SerializedName("external_user_id") val externalUserId: Long,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("meeting_at") val meetingAt: Long? = null,
    @SerializedName("location") val location: String,
    @SerializedName("created_at") val createdAt: Long? = null,
    @SerializedName("updated_at") val updatedAt: Long? = null,
    @SerializedName("is_active") val isActive: Boolean = true,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readLong(),
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readString() ?: "",
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readByte() != 0.toByte(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeValue(externalId)
        parcel.writeLong(userId)
        parcel.writeLong(externalUserId)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeValue(meetingAt)
        parcel.writeValue(location)
        parcel.writeValue(createdAt)
        parcel.writeValue(updatedAt)
        parcel.writeByte(if (isActive) 1 else 0)
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
data class NodeFilters(
    @SerializedName("by_priority_desc") var byPriorityDesc: Boolean = false,
    @SerializedName("by_priority_asc") var byPriorityAsc: Boolean = false,
    @SerializedName("by_updated_at_desc") var byUpdatedAtDesc: Boolean = false,
    @SerializedName("by_updated_at_asc") var byUpdatedAtAsc: Boolean = false,
    @SerializedName("by_priority_only_high") var byPriorityOnlyHigh: Boolean = false,
    @SerializedName("by_priority_only_normal") var byPriorityOnlyNormal: Boolean = false,
    @SerializedName("by_priority_only_low") var byPriorityOnlyLow: Boolean = false,
    @SerializedName("by_active_desc") var byActiveDesc: Boolean = false,
    @SerializedName("by_active_asc") var byActiveAsc: Boolean = false,
    @SerializedName("view_only_active") var viewOnlyActive: Boolean = false,
    @SerializedName("view_only_not_active") var viewOnlyNotActive: Boolean = false,
    @SerializedName("by_sync_by_back_desc") var bySyncByBackDesc: Boolean = false,
    @SerializedName("by_sync_by_back_asc") var bySyncByBackAsc: Boolean = false,
    @SerializedName("view_only_sync_by_back") var viewOnlySyncByBack: Boolean = false,
    @SerializedName("view_only_not_sync_by_back") var viewOnlyNotSyncByBack: Boolean = false,
    @SerializedName("search") var search: String = "",
) : Parcelable {
    constructor(parcel: Parcel) : this(
        byPriorityDesc = parcel.readByte() != 0.toByte(),
        byPriorityAsc = parcel.readByte() != 0.toByte(),
        byUpdatedAtDesc = parcel.readByte() != 0.toByte(),
        byUpdatedAtAsc = parcel.readByte() != 0.toByte(),
        byPriorityOnlyHigh = parcel.readByte() != 0.toByte(),
        byPriorityOnlyNormal = parcel.readByte() != 0.toByte(),
        byPriorityOnlyLow = parcel.readByte() != 0.toByte(),
        byActiveDesc = parcel.readByte() != 0.toByte(),
        byActiveAsc = parcel.readByte() != 0.toByte(),
        viewOnlyActive = parcel.readByte() != 0.toByte(),
        viewOnlyNotActive = parcel.readByte() != 0.toByte(),
        bySyncByBackDesc = parcel.readByte() != 0.toByte(),
        bySyncByBackAsc = parcel.readByte() != 0.toByte(),
        viewOnlySyncByBack = parcel.readByte() != 0.toByte(),
        viewOnlyNotSyncByBack = parcel.readByte() != 0.toByte(),
        search = parcel.readString() ?: ""
    )
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (byPriorityDesc) 1 else 0)
        parcel.writeByte(if (byPriorityAsc) 1 else 0)
        parcel.writeByte(if (byUpdatedAtDesc) 1 else 0)
        parcel.writeByte(if (byUpdatedAtAsc) 1 else 0)
        parcel.writeByte(if (byPriorityOnlyHigh) 1 else 0)
        parcel.writeByte(if (byPriorityOnlyNormal) 1 else 0)
        parcel.writeByte(if (byPriorityOnlyLow) 1 else 0)
        parcel.writeByte(if (byActiveDesc) 1 else 0)
        parcel.writeByte(if (byActiveAsc) 1 else 0)
        parcel.writeByte(if (viewOnlyActive) 1 else 0)
        parcel.writeByte(if (viewOnlyNotActive) 1 else 0)
        parcel.writeByte(if (bySyncByBackDesc) 1 else 0)
        parcel.writeByte(if (bySyncByBackAsc) 1 else 0)
        parcel.writeByte(if (viewOnlySyncByBack) 1 else 0)
        parcel.writeByte(if (viewOnlyNotSyncByBack) 1 else 0)
        parcel.writeString(search)
    }
    override fun describeContents(): Int = 0
    companion object CREATOR : Parcelable.Creator<NodeFilters> {
        override fun createFromParcel(parcel: Parcel): NodeFilters = NodeFilters(parcel)
        override fun newArray(size: Int): Array<NodeFilters?> = arrayOfNulls(size)
    }
}
data class Note(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("external_id") val externalId: Long? = null,
    @SerializedName("user_id") val userId: Long,
    @SerializedName("external_user_id") val externalUserId: Long,
    @SerializedName("parent_note_id") val parentNoteId: Long? = null,
    @SerializedName("external_parent_note_id") val externalParentNoteId: Long? = null,
    @SerializedName("meeting_id") val meetingId: Long? = null,
    @SerializedName("external_meeting_id") val externalMeetingId: Long? = null,
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String,
    @SerializedName("created_at") val createdAt: Long? = null,
    @SerializedName("updated_at") var updatedAt: Long? = null,
    @SerializedName("is_active") var isActive: Boolean = true,
    @SerializedName("priority") var priority: NotePriority
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readLong(),
        parcel.readLong(),
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readByte() != 0.toByte(),
        NotePriority.valueOf(parcel.readString() ?: NotePriority.NORMAL.name)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeValue(externalId)
        parcel.writeLong(userId)
        parcel.writeLong(externalUserId)
        parcel.writeValue(parentNoteId)
        parcel.writeValue(externalParentNoteId)
        parcel.writeValue(meetingId)
        parcel.writeValue(externalMeetingId)
        parcel.writeString(title)
        parcel.writeString(content)
        parcel.writeValue(createdAt)
        parcel.writeValue(updatedAt)
        parcel.writeByte(if (isActive) 1 else 0)
        parcel.writeString(priority.name)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Note> {
        override fun createFromParcel(parcel: Parcel): Note = Note(parcel)
        override fun newArray(size: Int): Array<Note?> = arrayOfNulls(size)
    }
}
