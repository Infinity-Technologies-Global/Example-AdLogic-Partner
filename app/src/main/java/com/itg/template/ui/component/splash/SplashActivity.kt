package com.itg.template.ui.component.splash

import android.os.CountDownTimer
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.ads.module.admob.Admob
import com.ads.module.admob.AppOpenManager
import com.ads.module.ads.ERainAd
import com.ads.module.funtion.AdCallback
import com.itg.template.R
import com.itg.template.ads.AdRemoteConfig
import com.itg.template.ads.AdsManager.loadNativeLanguage
import com.itg.template.ads.AdsManager.loadNativeLanguageClick
import com.itg.template.ads.RemoteConfigUtils
import com.itg.template.ads.inter_splash
import com.itg.template.ads.open_resume
import com.itg.template.app.AppConstants
import com.itg.template.databinding.ActivitySplashBinding
import com.itg.template.ui.bases.BaseActivity
import com.itg.template.ui.bases.ConsentHandler
import com.itg.template.ui.bases.ext.goneView
import com.itg.template.ui.bases.ext.isNetwork
import com.itg.template.ui.bases.ext.visibleView
import com.itg.template.utils.Routes
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SplashActivity : BaseActivity<ActivitySplashBinding>(), RemoteConfigUtils.Listener {

    private var getConfigSuccess = false
    private var showLanguageNextTime = true
    private var simpleExoPlayer: ExoPlayer? = null
    private lateinit var consentHandler: ConsentHandler
    private fun shouldShowLanguageNextTime() = showLanguageNextTime

    private val uriVideo = "asset:///video_splash.mp4".toUri()
    private val isHasVideo = true

    override fun getLayoutActivity() = R.layout.activity_splash

    override fun initViews() {
        super.initViews()

        RemoteConfigUtils.init(this, this)
        consentHandler = ConsentHandler(
            activity = this,
            appSharedPref = appSharedPref,
            trackingSuffix = 1,
            onConsentFlowCompleted = { loadingRemoteConfig() }
        )
        if (appSharedPref.isConfirmConsent.not() && appSharedPref.isUserGlobal.not() && isNetwork()) {
            consentHandler.requestConsent()
        } else {
            loadingRemoteConfig()
        }
        initVideoSplash()

    }

    private fun initVideoSplash() {
        if (isHasVideo) {
            mBinding.imgSplash.goneView()
            mBinding.playerView.visibleView()
            simpleExoPlayer = ExoPlayer.Builder(this).build()
            mBinding.playerView.player = simpleExoPlayer
            val mediaItem: MediaItem = MediaItem.fromUri(uriVideo)
            simpleExoPlayer?.addMediaItem(mediaItem)
            simpleExoPlayer?.prepare()
            simpleExoPlayer?.volume = 0f
            simpleExoPlayer?.repeatMode = ExoPlayer.REPEAT_MODE_ALL
            simpleExoPlayer?.playWhenReady = true
            simpleExoPlayer?.play()
        } else {
            mBinding.imgSplash.visibleView()
            mBinding.playerView.goneView()
        }
    }

    private fun loadingRemoteConfig() {
        object : CountDownTimer(AppConstants.DEFAULT_TIME_SPLASH, 100) {
            override fun onTick(millisUntilFinished: Long) {
                if (getConfigSuccess && millisUntilFinished < AppConstants.DEFAULT_LIMIT_TIME_SPLASH) {
                    checkRemoteConfigResult()
                    cancel()
                }
            }

            override fun onFinish() {
                if (!getConfigSuccess) {
                    checkRemoteConfigResult()
                }
            }
        }.start()
    }


    private fun checkRemoteConfigResult() {
        AdRemoteConfig.initialize(this, RemoteConfigUtils.getAdRemoteConfig())
        loadNativeLanguage(this, appSharedPref.firstLanguage, R.layout.layout_native_language)
        loadNativeLanguageClick(this, appSharedPref.firstLanguage, R.layout.layout_native_language_click)

        if (AdRemoteConfig.inter_splash.isEnable && isNetwork(this@SplashActivity)) {
            Admob.getInstance().setOpenActivityAfterShowInterAds(false)
            ERainAd.getInstance().loadSplashInterstitialAds(
                this,
                AdRemoteConfig.inter_splash.id,
                30000,
                5000,
                object : AdCallback() {
                    override fun onNextAction() {
                        super.onNextAction()
                        moveActivity()
                    }
                })
        } else {
            moveActivity()
        }

        if (AdRemoteConfig.open_resume.isEnable) {
            AppOpenManager.getInstance().setAppResumeAdId(AdRemoteConfig.open_resume.id)
            AppOpenManager.getInstance().enableAppResume()
        } else {
            AppOpenManager.getInstance().disableAppResume()
        }
    }

    private fun moveActivity() {
        stopVideo()
        if (shouldShowLanguageNextTime() || appSharedPref.firstOnBoarding)
            Routes.startLanguageActivity(this, null)
        else
            Routes.startMainActivity(this)
        finish()
    }

    private fun stopVideo() {
        simpleExoPlayer?.let { player ->
            player.pause()
            player.playWhenReady = false
        }
    }

    override fun onPause() {
        super.onPause()
        stopVideo()
    }

    override fun onResume() {
        super.onResume()
        ERainAd.getInstance()
            .onCheckShowSplashWhenFail(this@SplashActivity, object : AdCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    moveActivity()
                }
            }, 1000)
    }

    override fun loadSuccess() {
        getConfigSuccess = true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::consentHandler.isInitialized) {
            consentHandler.clear()
        }
        stopVideo()
        simpleExoPlayer?.release()
        simpleExoPlayer = null
        mBinding.playerView.player = null
    }
}