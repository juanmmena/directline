package com.directline.core.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.directline.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InterstitialAdManager @Inject constructor(
  @param:ApplicationContext private val context: Context,
) {
  companion object {
    const val DEFAULT_LAUNCH_INTERVAL = 3
  }

  private val mainHandler = Handler(Looper.getMainLooper())
  private val launchCounter = AtomicInteger(0)

  @Volatile
  private var interstitialAd: InterstitialAd? = null

  @Volatile
  private var isLoading: Boolean = false

  fun preload(adUnitId: String = BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID) {
    if (interstitialAd != null || isLoading) return
    isLoading = true

    mainHandler.post {
      val request = AdRequest.Builder().build()
      InterstitialAd.load(
        context,
        adUnitId,
        request,
        object : InterstitialAdLoadCallback() {
          override fun onAdLoaded(ad: InterstitialAd) {
            interstitialAd = ad
            isLoading = false
          }

          override fun onAdFailedToLoad(error: LoadAdError) {
            interstitialAd = null
            isLoading = false
          }
        },
      )
    }
  }

  /**
   * Shows a preloaded interstitial every [interval] calls when available, then invokes [onComplete].
   * Always invokes [onComplete] immediately when offline or no ad is ready so core flows never block.
   */
  fun maybeShowOnAction(
    activity: Activity?,
    interval: Int = DEFAULT_LAUNCH_INTERVAL,
    onComplete: () -> Unit,
  ) {
    val count = launchCounter.incrementAndGet()
    if (activity == null || count % interval != 0) {
      if (interstitialAd == null) preload()
      onComplete()
      return
    }

    val ad = interstitialAd
    if (ad == null) {
      preload()
      onComplete()
      return
    }

    interstitialAd = null
    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
      override fun onAdDismissedFullScreenContent() {
        preload()
        onComplete()
      }

      override fun onAdFailedToShowFullScreenContent(adError: AdError) {
        preload()
        onComplete()
      }
    }
    ad.show(activity)
  }
}
