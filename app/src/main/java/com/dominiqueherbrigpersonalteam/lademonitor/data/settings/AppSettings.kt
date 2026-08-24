package com.dominiqueherbrigpersonalteam.lademonitor.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.dominiqueherbrigpersonalteam.lademonitor.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Port of the iOS `AppSettings`: holds the (non-secret) server address and the app mode.
 * Backed by [SharedPreferences] for synchronous reads (the [com.dominiqueherbrigpersonalteam.lademonitor.data.remote.ApiClient]
 * needs the URL synchronously) and mirrored into [StateFlow]s so Compose recomposes on change.
 */
object AppSettings {

    private const val PREFS = "lademonitor_settings"
    private const val KEY_SERVER_URL = "serverURL"
    private const val KEY_APP_MODE = "appMode"

    private lateinit var prefs: SharedPreferences

    private val _serverUrlString = MutableStateFlow("")
    val serverUrlString: StateFlow<String> = _serverUrlString

    private val _appMode = MutableStateFlow(AppMode.UNDECIDED)
    val appMode: StateFlow<AppMode> = _appMode

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _serverUrlString.value = prefs.getString(KEY_SERVER_URL, "") ?: ""
        _appMode.value = AppMode.from(prefs.getString(KEY_APP_MODE, null))
    }

    fun setServerUrlString(value: String) {
        _serverUrlString.value = value
        prefs.edit().putString(KEY_SERVER_URL, value).apply()
    }

    fun setAppMode(mode: AppMode) {
        _appMode.value = mode
        prefs.edit().putString(KEY_APP_MODE, mode.raw).apply()
        if (mode == AppMode.LOCAL_ONLY) {
            // Drop any credentials when switching to local-only, so a later switch back to server
            // always requires a fresh sign-in (matches the iOS behaviour).
            SessionManager.invalidateSession()
        }
    }

    /**
     * The base URL (no trailing slash). If the user typed a bare domain, `https://` is prepended;
     * an explicit `http://` is preserved. Returns null when nothing is configured.
     */
    fun serverUrl(): String? {
        var raw = _serverUrlString.value.trim()
        if (raw.isEmpty()) return null
        if (!raw.startsWith("http://") && !raw.startsWith("https://")) raw = "https://$raw"
        if (raw.endsWith("/")) raw = raw.dropLast(1)
        return raw
    }

    val isConfigured: Boolean get() = serverUrl() != null

    /** Whether views may load data: always in local-only mode; in server mode only when configured. */
    val isReadyForDataAccess: Boolean
        get() = _appMode.value == AppMode.LOCAL_ONLY || isConfigured
}
