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
        @Part("name") name: RequestBody,
        @Part("surname") surname: RequestBody,
        @Part("age") age: RequestBody,
        @Part("email") email: RequestBody,
        @Part("aboutMe") aboutMe: RequestBody,
        @Part("city") city: RequestBody,
        @Part("country") country: RequestBody,
        @Part file: MultipartBody.Part?,
        @Part file2: MultipartBody.Part?,
        @Header("Authorization") token: String,
    ): Response<String>

    @Multipart
    @POST("public/edit/{id}")
    suspend fun updatePublic(
        @Path("id") id: String,
        @Part("name") name: RequestBody,
        @Part("description") description: RequestBody,
        @Part avatar: MultipartBody.Part?,
        @Part banner: MultipartBody.Part?,
        @Part("avatar") isAvatar: RequestBody,
        @Part("banner") isBanner: RequestBody,
        @Header("Authorization") token: String,
    ): String

    @Multipart
    @POST("createuserpost")
    suspend fun sendPost(
        @Part("title") title: RequestBody,
        @Part("date") date: RequestBody,
        @Part file: List<MultipartBody.Part?>,
        @Header("Authorization") token: String,
    ): String

    @Multipart
    @POST("public/createpost/{id}")
    suspend fun sendPublicPost(
        @Path("id") id: String,
        @Part("title") title: RequestBody,
        @Part("text") text: RequestBody,
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
        @Part("fileLink") fileLink: RequestBody?,
        @Header("Authorization") token: String,
    ): String

    @Multipart
    @POST("new-chat-messages/{id}")
    suspend fun sendChatMessage(
        @Path("id") id: String,
        @Part("message") message: RequestBody,
        @Part("date") date: RequestBody,
        @Part("isFile") isFile: RequestBody,
        @Part file: MultipartBody.Part?,
        @Part videoFile: MultipartBody.Part?,
        @Part audioFile: MultipartBody.Part?,
        @Part("fileLink") fileLink: RequestBody?,
        @Header("Authorization") token: String,
    ): String

    @Multipart
    @POST("uploadbg-mobile/{id}")
    suspend fun sendRoomBg(
        @Path("id") id: String,
        @Part file: MultipartBody.Part?,
        @Header("Authorization") token: String
    ): String

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
        @Header("Authorization") token: String,
    ): String

    @Multipart
    @POST("cloud/upload-mobile")
    suspend fun uploadFile(
        @Part file: List<MultipartBody.Part?>,
        @Part("mobile") mobile: Boolean,
        @Part("folderId") folderId: RequestBody,
        @Part("folderName") folderName: RequestBody,
        @Part("names") names: StringArray,
        @Header("Authorization") token: String
    ): String

    @Multipart
    @POST("public/create")
    suspend fun createPublic(
        @Part("name") name: RequestBody,
        @Part("description") description: RequestBody,
        @Part avatar: MultipartBody.Part?,
        @Part banner: MultipartBody.Part?,
        @Header("Authorization") token: String,
    ): String
}