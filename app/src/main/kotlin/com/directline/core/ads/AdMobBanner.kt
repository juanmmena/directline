package com.directline.core.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.directline.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdMobBanner(
  modifier: Modifier = Modifier,
  adUnitId: String = BuildConfig.ADMOB_BANNER_AD_UNIT_ID,
) {
  if (LocalInspectionMode.current) return

  val context = LocalContext.current

  BoxWithConstraints(
    modifier = modifier.fillMaxWidth(),
    contentAlignment = Alignment.Center,
  ) {
    val adWidthDp = maxWidth.value.toInt().coerceAtLeast(320)
    val adView = remember(adUnitId, adWidthDp) {
      AdView(context).apply {
        this.adUnitId = adUnitId
        setAdSize(
          AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp),
        )
        loadAd(AdRequest.Builder().build())
      }
    }

    DisposableEffect(adView) {
      onDispose { adView.destroy() }
    }

    AndroidView(
      factory = { adView },
      modifier = Modifier.fillMaxWidth(),
    )
  }
}
