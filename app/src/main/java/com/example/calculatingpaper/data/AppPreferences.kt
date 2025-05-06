package com.example.calculatingpaper.data
import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_OPENED_TYPE = "last_opened_type"
        private const val KEY_LAST_OPENED_ID = "last_opened_id"
        private const val KEY_DECIMAL_PRECISION = "decimal_precision"
        const val TYPE_NOTE = "note"
        const val TYPE_FOLDER = "folder"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ID = "user_id"

        private const val KEY_REALTIME_SYNC_ENABLED = "realtime_sync_enabled"

        var decimalPrecision: Int = 10
            private set
    }

    init {
        decimalPrecision = sharedPreferences.getInt(KEY_DECIMAL_PRECISION, 10)
    }


    fun saveDecimalPrecision(precision: Int) {
        decimalPrecision = precision.coerceAtLeast(0)
        sharedPreferences.edit()
            .putInt(KEY_DECIMAL_PRECISION, decimalPrecision)
            .apply()
    }
    fun saveLastOpenedItem(type: String, id: Long) {
        sharedPreferences.edit()
            .putString(KEY_LAST_OPENED_TYPE, type)
            .putLong(KEY_LAST_OPENED_ID, id)
            .apply()
    }
    fun getLastOpenedItem(): Pair<String?, Long> {
        val type = sharedPreferences.getString(KEY_LAST_OPENED_TYPE, null)
        val id = sharedPreferences.getLong(KEY_LAST_OPENED_ID, -1L)
        return Pair(type, id)
    }
    fun clearLastOpenedItem() {
        sharedPreferences.edit()
            .remove(KEY_LAST_OPENED_TYPE)
            .remove(KEY_LAST_OPENED_ID)
            .apply()
    }
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    fun getUserEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }
    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }
    fun saveLoginInfo(isLoggedIn: Boolean, email: String?, userId: String?) {
        sharedPreferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_ID, userId)
            .apply()
    }
    fun clearLoginInfo() {
        sharedPreferences.edit()
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_ID)
            .remove(KEY_REALTIME_SYNC_ENABLED)
            .apply()
    }

    fun isRealtimeSyncEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_REALTIME_SYNC_ENABLED, false)
    }

    fun saveRealtimeSyncEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_REALTIME_SYNC_ENABLED, enabled)
            .apply()
    }

}
