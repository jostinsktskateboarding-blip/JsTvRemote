package com.js.tvremote.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.js.tvremote.ads.AdManager
import com.js.tvremote.ads.AdIds

@Composable
fun BannerAdView() {
    if (!AdManager.adsReady.value) return
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdIds.BANNER
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
