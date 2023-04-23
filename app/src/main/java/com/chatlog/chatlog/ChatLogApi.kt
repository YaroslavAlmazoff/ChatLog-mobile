package com.chatlog.chatlog

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
        @Part("isFile") isFile: RequestBody,
        @Part file: MultipartBody.Part?,
        @Part videoFile: MultipartBody.Part?,
        @Part("audio") audioFile: RequestBody,
        @Header("Authorization") token: String,
    ): String
}