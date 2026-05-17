package com.metashield.app.data.repository

import com.google.gson.Gson
import com.metashield.app.data.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val UPDATE_URL =
    "https://raw.githubusercontent.com/Developer-For-Git/MetaShield/refs/heads/main/updates.json"

@Singleton
class UpdateRepository @Inject constructor(
    private val gson: Gson
) {
    /**
     * Fetches the remote updates.json and returns [UpdateInfo] if a newer version exists,
     * or null if the app is already up-to-date or the check fails silently.
     *
     * @param currentVersion The running app versionName (e.g. "1.9.1")
     */
    suspend fun checkForUpdate(currentVersion: String): UpdateInfo? =
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(UPDATE_URL).openConnection() as HttpURLConnection
                connection.apply {
                    connectTimeout = 8_000
                    readTimeout    = 8_000
                    requestMethod  = "GET"
                }

                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null

                val json = BufferedReader(InputStreamReader(connection.inputStream))
                    .use { it.readText() }

                val info = gson.fromJson(json, UpdateInfo::class.java)
                    ?: return@withContext null

                // Compare versions — return only when remote is strictly newer
                if (isNewerVersion(info.version, currentVersion)) info else null
            } catch (_: Exception) {
                // Network error, malformed JSON, etc. — fail silently
                null
            }
        }

    /**
     * Returns true when [remote] is strictly greater than [current].
     * Compares each dot-separated segment numerically.
     * Examples: "1.9.1" vs "1.9.0" → true | "1.9.1" vs "2.0.0" → false
     */
    private fun isNewerVersion(remote: String, current: String): Boolean {
        val r = remote.trim().split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.trim().split(".").map { it.toIntOrNull() ?: 0 }
        val len = maxOf(r.size, c.size)
        for (i in 0 until len) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv > cv) return true
            if (rv < cv) return false
        }
        return false // equal
    }
}
