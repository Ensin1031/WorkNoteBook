package com.example.worknotebook

import com.google.gson.Gson
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import kotlin.jvm.java


fun parseError(response: Response<*>): String {

    return try {
        val gson = Gson()
        val errorJson = response.errorBody()?.string()

        val error = gson.fromJson(errorJson, ErrorResponse::class.java)

        error?.detail ?: "Unknown error"

    } catch (e: Exception) {
        "Unknown error"
    }
}


object ApiRoutes {
    const val BACK_ROOT = "/api/v1/"
    const val AUTH = "${BACK_ROOT}auth/"
    const val USERS = "${BACK_ROOT}users/"
    const val NOTES = "${BACK_ROOT}notes/"
    const val MEETINGS = "${BACK_ROOT}meetings/"
}


interface ApiService {

    @GET("${ApiRoutes.BACK_ROOT}check/")
    suspend fun check(): Response<Boolean>

    @POST("${ApiRoutes.AUTH}login/")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("${ApiRoutes.AUTH}register/")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @PATCH("${ApiRoutes.USERS}{user_id}/")
    suspend fun updateUser(
        @Path("user_id") userId: Int,
        @Body request: UserUpdate
    ): Response<User>

    // NOTES
    @GET("${ApiRoutes.NOTES}{note_id}/")
    suspend fun getNote(@Path("note_id") noteId: Int): Response<Note>

    @GET(ApiRoutes.NOTES)
    suspend fun getNotes(): Response<Note>

    @POST("${ApiRoutes.NOTES}sync/")
    suspend fun syncNote(@Body request: Note): Response<Note>

    @POST(ApiRoutes.NOTES)
    suspend fun createNote(@Body request: Note): Response<Note>

    @PATCH("${ApiRoutes.NOTES}{note_id}/")
    suspend fun updateNote(@Path("note_id") noteId: Int, @Body request: Note): Response<Note>

    @DELETE("${ApiRoutes.NOTES}{note_id}/")
    suspend fun deleteNote(
        @Path("note_id") noteId: Int,
        @Query("archive") archive: Boolean = false
    ): Response<Unit>

    // MEETINGS
    @GET("${ApiRoutes.MEETINGS}{meeting_id}/")
    suspend fun getMeeting(@Path("meeting_id") meetingId: Int): Response<Meeting>

    @GET(ApiRoutes.MEETINGS)
    suspend fun getMeetings(): Response<Meeting>

    @POST(ApiRoutes.MEETINGS)
    suspend fun createMeeting(@Body request: Meeting): Response<Meeting>

    @PATCH("${ApiRoutes.MEETINGS}{meeting_id}/")
    suspend fun updateMeeting(@Path("meeting_id") meetingId: Int, @Body request: Meeting): Response<Meeting>

    @DELETE("${ApiRoutes.MEETINGS}{meeting_id}/")
    suspend fun deleteMeeting(@Path("meeting_id") meetingId: Int): Response<Unit>
}
