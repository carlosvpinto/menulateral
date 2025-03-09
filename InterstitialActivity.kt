package com.carlosv.dolaraldia

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.ActivityInterstitialBinding
import com.carlosv.menulateral.databinding.ActivityMainBinding

//import com.google.android.gms.ads.FullScreenContentCallback
//import com.google.android.gms.ads.LoadAdError
//import com.google.android.gms.ads.interstitial.InterstitialAd
//import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class InterstitialActivity : AppCompatActivity() {

//    private lateinit var binding: ActivityInterstitialBinding
//    private var interstitial: InterstitialAd? = null
//    private var count = 0
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityInterstitialBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//        initAds()
//        initListeners()
//        binding.btnInterstitial.setOnClickListener {
//            count += 1
//            checkCounter()
//        }
//    }
//
//    private fun initListeners() {
//        interstitial?.fullScreenContentCallback = object : FullScreenContentCallback() {
//            override fun onAdDismissedFullScreenContent() {
//            }
//
//            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
//
//            }
//
//            override fun onAdShowedFullScreenContent() {
//                interstitial = null
//            }
//        }
//    }
//
//    private fun initAds() {
//        var adRequest = AdRequest.Builder().build()
//        InterstitialAd.load(
//            this,
//            "ca-app-pub-3940256099942544/1033173712",
//            adRequest,
//            object : InterstitialAdLoadCallback() {
//                override fun onAdLoaded(interstitialAd: InterstitialAd) {
//                    interstitial = interstitialAd
//                }
//
//                override fun onAdFailedToLoad(p0: LoadAdError) {
//                    interstitial = null
//                }
//            })
//    }
//
//    private fun checkCounter() {
//        if (count == 5) {
//            showAds()
//            count = 0
//            initAds()
//        }
//    }
//
//    private fun showAds() {
//        interstitial?.show(this)
//    }
}