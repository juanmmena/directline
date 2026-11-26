package com.directline

import android.app.Application
import com.directline.core.ads.InterstitialAdManager
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DirectlineApplication : Application() {

  @Inject
  lateinit var interstitialAdManager: InterstitialAdManager

  private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onCreate() {
    super.onCreate()
    appScope.launch {
      MobileAds.initialize(this@DirectlineApplication) {
        interstitialAdManager.preload()
      }
    }
  }
}
