package com.anivers.anime.utils

object Constants {
    const val BASE_URL = "https://apps.animekita.org/"
    const val API_PREFIX = "api/v1.2.5/"
    // token hanya untuk episode/data.php, jangan untuk series.php
    const val EPISODE_TOKEN = "75504c37d8b97486a1211245f2fb21691ac11a791180d26ca2b0b2e85eaffb814185e2250e111b9abeee15ccb1eba55c2b880e335e124c02d517509ee377939d87053383ee0243354230a7a71eb88ffd71752be22f8f2d234884788f70c776ddf2beccfa"
    const val UA_DART = "Dart/3.9 (dart:io)"
    const val UA_FLUTTER = "Flutter/2.5.3"
    const val FALLBACK_COVER = "https://via.placeholder.com/300x400?text=No+Cover"

    // Premium gateway placeholder (ganti dengan URL asli gateway kamu)
    const val GATEWAY_BASE = "https://api.anivers.placeholder/"
    const val PREMIUM_1M_PRICE = 10000
    const val PREMIUM_3M_PRICE = 25000
    const val PREMIUM_5M_PRICE = 50000
    const val PREMIUM_1M_DAYS = 30L
    const val PREMIUM_3M_DAYS = 90L
    const val PREMIUM_5M_DAYS = 150L

    // Keys: 1 kunci = 1 anime permanen, tumpuk max 6
    const val MAX_KEYS = 6
    const val AD_REWARD_KEYS = 1
    const val AD_DURATION_SEC = 30L

    // AdMob IDs (dari user)
    const val ADMOB_APP_ID = "ca-app-pub-2178230808163908~5116416643"
    const val ADMOB_REWARDED_UNIT = "ca-app-pub-2178230808163908/2191704555"
}
