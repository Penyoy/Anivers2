package com.anivers.anime.data.ads

import android.app.Activity
import android.content.Context
import com.anivers.anime.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdsManager {
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    fun load(context: Context, onLoaded: (() -> Unit)? = null, onFailed: ((String) -> Unit)? = null) {
        if (isLoading || rewardedAd != null) return
        isLoading = true
        val adUnit = try { context.getString(R.string.admob_rewarded_unit) } catch (_: Exception) { "ca-app-pub-3940256099942544/5224354917" }
        val unit = adUnit.ifBlank { "ca-app-pub-3940256099942544/5224354917" }
        val req = AdRequest.Builder().build()
        RewardedAd.load(context, unit, req, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(err: LoadAdError) {
                isLoading = false
                rewardedAd = null
                onFailed?.invoke(err.message)
            }
            override fun onAdLoaded(ad: RewardedAd) {
                isLoading = false
                rewardedAd = ad
                onLoaded?.invoke()
            }
        })
    }

    fun isReady(): Boolean = rewardedAd != null

    fun show(activity: Activity, onRewarded: () -> Unit, onClosed: (() -> Unit)? = null, onFailed: ((String) -> Unit)? = null) {
        val ad = rewardedAd
        if (ad == null) {
            onFailed?.invoke("Ad not ready, loading...")
            load(activity, onLoaded = { show(activity, onRewarded, onClosed, onFailed) }, onFailed = onFailed)
            return
        }
        ad.show(activity) { _ ->
            rewardedAd = null
            onRewarded()
            // preload next
            load(activity)
        }
        // optional: handle fullScreen callbacks for closed without reward
        ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                onClosed?.invoke()
                load(activity)
            }
            override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) {
                rewardedAd = null
                onFailed?.invoke(e.message)
                load(activity)
            }
        }
    }

    fun preload(context: Context) { load(context) }
}
