package com.dominiqueherbrigpersonalteam.lademonitor

import android.app.Application
import android.content.Context
import com.dominiqueherbrigpersonalteam.lademonitor.data.local.LocalStore
import com.dominiqueherbrigpersonalteam.lademonitor.data.net.NetworkMonitor
import com.dominiqueherbrigpersonalteam.lademonitor.data.repo.SyncService
import com.dominiqueherbrigpersonalteam.lademonitor.data.session.SessionManager
import com.dominiqueherbrigpersonalteam.lademonitor.data.settings.AppSettings
import com.dominiqueherbrigpersonalteam.lademonitor.data.settings.TokenStore
import org.osmdroid.config.Configuration

class LademonitorApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Order matters: SessionManager reads the token, so TokenStore must be ready first.
        TokenStore.init(this)
        SessionManager.init()
        AppSettings.init(this)
        LocalStore.init(this)
        NetworkMonitor.init(this)
        SyncService.init(this)

        // osmdroid needs a user-agent set (OSM tile policy) and a cache location.
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
    }
}
