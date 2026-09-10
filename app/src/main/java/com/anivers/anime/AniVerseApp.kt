package com.anivers.anime

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp

class AniVerseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try { FirebaseApp.initializeApp(this) } catch (_: Exception) {}
        try { MobileAds.initialize(this) {} } catch (_: Exception) {}
    }
}
