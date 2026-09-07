package com.anivers.anime

import android.app.Application
import com.google.firebase.FirebaseApp

class AniVerseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try { FirebaseApp.initializeApp(this) } catch (_: Exception) {}
    }
}
