package com.js.tvremote.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.UserMessagingPlatform
import androidx.compose.runtime.mutableStateOf

object AdIds {
    const val BANNER = "ca-app-pub-4459526948931070/4889277827"
    const val INTERSTITIAL = "ca-app-pub-4459526948931070/5000958464"
}

object AdManager {
    private var interstitialAd: InterstitialAd? = null
    private var adsInitialized = false
    val adsReady = mutableStateOf(false)

    fun init(activity: Activity) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation.requestConsentInfoUpdate(
            activity,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    initializeAdsIfAllowed(activity, consentInformation)
                }
            },
            {
                // If consent info cannot be refreshed, keep the app usable.
                initializeAdsIfAllowed(activity, consentInformation)
            }
        )
    }

    private fun initializeAdsIfAllowed(context: Context, consentInformation: ConsentInformation) {
        if (adsInitialized || !consentInformation.canRequestAds()) return
        adsInitialized = true
        MobileAds.initialize(context) {
            adsReady.value = true
            loadInterstitial(context)
        }
    }

    private fun loadInterstitial(context: Context) {
        InterstitialAd.load(
            context,
            AdIds.INTERSTITIAL,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            // The UMP SDK refreshes consent state as needed.
        }
    }

    fun maybeShowInterstitial(activity: Activity) {
        val ad = interstitialAd ?: return
        ad.show(activity)
        interstitialAd = null
        loadInterstitial(activity)
    }
}
