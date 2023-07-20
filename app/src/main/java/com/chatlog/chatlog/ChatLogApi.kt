package com.chatlog.chatlog

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Response
import retrofit2.http.*
import java.io.File

interface ChatLogApi {
    @Multipart
    @POST("editprofile")
    suspend fun uploadImage(
        @Part("name") name: String,
        @Part("surname") surname: String,
        @Part("age") age: String,
        @Part("email") email: String,
        @Part("aboutMe") aboutMe: String,
        @Part file: MultipartBody.Part?,
        @Part file2: MultipartBody.Part?,
        @Header("Authorization") token: String,
    ): Response<String>

    @Multipart
    @POST("createuserpost")
    suspend fun sendPost(
        @Part("title") title: RequestBody,
        @Part("date") date: RequestBody,
        @Part file: List<MultipartBody.Part?>,
        @Header("Authorization") token: String,
    ): String

    @Multipart
    @POST("new-messages/{id}")
    suspend fun sendMessage(
        @Path("id") id: String,
        @Part("message") message: RequestBody,
        @Part("date") date: RequestBody,
        @Part("isFile") isFile: RequestBody,
        @Part file: MultipartBody.Part?,
        @Part videoFile: MultipartBody.Part?,
        @Part audioFile: MultipartBody.Part?,
        @Header("Authorization") token: String,
    ): String

    @Multipart
    @POST("new-chatmessages-mobile/{id}")
    suspend fun sendChatMessage(
        @Path("id") id: String,
        @Part("message") message: RequestBody,
        @Part("date") date: RequestBody,
        @Part("isFile") isFile: RequestBody,
        @Part file: MultipartBody.Part?,
        @Part videoFile: MultipartBody.Part?,
        @Part audioFile: MultipartBody.Part?,
        @Header("Authorization") token: String,
    ): String

    @Multipart
    @POST("uploadbg-mobile/{id}")
    suspend fun sendRoomBg(
        @Path("id") id: String,
        @Part file: MultipartBody.Part?,
    ): String

    @GET("last-message-mobile/{id}")
    suspend fun getLastMessage(@Path("id") id: String): Message

    @Multipart
    @POST("createchatroom")
    suspend fun createDiscussion(
        @Part("title") title: String,
        @Part file: MultipartBody.Part?,
        @Header("Authorization") token: String,
    ): String

    @Multipart
    @POST("edit-discussion/{id}")
    suspend fun saveDiscussion(
        @Path("id") id: String,
        @Part("title") title: String,
        @Part file: MultipartBody.Part?,
    ): String

    @Multipart
    @POST("cloud/upload-mobile")
    suspend fun uploadFile(
        @Part file: List<MultipartBody.Part?>,
        @Part("mobile") mobile: Boolean,
        @Part("folder") folder: String,
        @Header("Authorization") token: String
    ): String
}