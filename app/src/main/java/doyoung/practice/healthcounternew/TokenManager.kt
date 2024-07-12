package doyoung.practice.healthcounternew

/*
object TokenManager {
    var accessToken: String? = null
    var refreshToken: String? = null
}
 */

import android.content.Context
import android.content.SharedPreferences

object TokenManager {
    private const val PREFS_NAME = "token_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"

    var accessToken: String? = null
        private set

    var refreshToken: String? = null
        private set

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    fun saveTokens(context: Context, accessToken: String?, refreshToken: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            apply()
        }
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    fun clearTokens(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            apply()
        }
        accessToken = null
        refreshToken = null
    }
}