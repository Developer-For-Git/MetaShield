package com.metashield.app.data.model

import com.google.gson.annotations.SerializedName

data class UpdateInfo(
    @SerializedName("version")     val version: String,
    @SerializedName("date")        val date: String,
    @SerializedName("title")       val title: String,
    @SerializedName("changes")     val changes: List<String>,
    @SerializedName("download_url") val downloadUrl: String
)
