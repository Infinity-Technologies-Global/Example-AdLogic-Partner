package com.itg.template.ui.component.main

import android.app.Dialog
import android.os.Handler
import android.os.Looper
import com.hjq.permissions.dsl.xxPermissions
import com.hjq.permissions.permission.PermissionLists
import com.itg.template.BuildConfig
import com.itg.template.R
import com.itg.template.ads.AdRemoteConfig
import com.itg.template.ads.AdsManager
import com.itg.template.ads.RemoteConfigUtils
import com.itg.template.ads.banner_home
import com.itg.template.data.model.ForceUpdateConfig
import com.itg.template.databinding.ActivityMainBinding
import com.itg.template.ui.bases.BaseActivity
import com.itg.template.ui.bases.ConsentHandler
import com.itg.template.ui.bases.ext.click
import com.itg.template.ui.component.main.dialog.ForceUpdateDialog
import com.itg.template.ui.component.main.dialog.NoInternetDialog
import com.itg.template.utils.ConnectionLiveData
import com.itg.template.utils.ITGTrackingHelper
import com.itg.template.utils.Routes
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    override val bannerConfig = Pair(AdRemoteConfig.banner_home, true)

    private lateinit var consentHandler: ConsentHandler
    private val delayHandler = Handler(Looper.getMainLooper())
    private var delayRunnable: Runnable? = null
    private lateinit var noInternetDialog: NoInternetDialog
    private lateinit var forceUpdateDialog: ForceUpdateDialog
    private var forceUpdateDialogHandle: Dialog? = null
    private var cachedForceUpdateConfig: ForceUpdateConfig? = null
    override fun getLayoutActivity(): Int = R.layout.activity_main


    override fun initViews() {
        super.initViews()
        noInternetDialog = NoInternetDialog(this)
        forceUpdateDialog = ForceUpdateDialog(this)
        checkInternet()
        initConsentHandler()
        checkConsentStatus()
        loadInterstitial()
        maybeShowForceUpdateDialog()
    }

    private fun initConsentHandler() {
        consentHandler = ConsentHandler(
            activity = this,
            appSharedPref = appSharedPref,
            trackingSuffix = 2,
            onConsentFlowCompleted = { Timber.d("Consent flow completed") },
            onConsentSuccess = { canPersonalized ->
                if (canPersonalized) {
                    Routes.startSplashActivity(this)
                    finish()
                }
            },
            onNotUsingAdConsent = {
                appSharedPref.isUserGlobal = true
            }
        )
    }

    private fun checkConsentStatus() {
        if (appSharedPref.isConfirmConsent.not() && appSharedPref.isUserGlobal.not()) {
            delayShowConsentDialog()
        }
    }

    private fun checkInternet() {
        ConnectionLiveData(this).observe(this) { isNetwork ->
            if (isNetwork) {
                Timber.d("network on")
                noInternetDialog.dismiss()
            } else {
                Timber.d("network off")
                noInternetDialog.show()
            }
        }
    }


    override fun onClickViews() {
        super.onClickViews()
        mBinding.buttonHello.click {
            ITGTrackingHelper.logEventClick(this::class.java.simpleName, "buttonHello", null)
        }

        mBinding.buttonShowInter.click {
            showInterstitial()
        }
        mBinding.buttonRequestPermission.click {
            requestPermission()
        }
        mBinding.buttonShowSetting.click {
            Routes.startSettingActivity(this)
        }
        mBinding.buttonShowForceUpdate.click {
            val fallback = ForceUpdateConfig(
                title = getString(R.string.app_name),
                description = getString(R.string.force_update_message),
                storeLink = ""
            )
            val config = cachedForceUpdateConfig
                ?: RemoteConfigUtils.getForceUpdateConfig()
                ?: fallback
            forceUpdateDialog.show(config)
            cachedForceUpdateConfig = config
        }
    }

    private fun requestPermission() {
        xxPermissions {
            permissions(PermissionLists.getPostNotificationsPermission())
            onDoNotAskAgain { permissions, userResult ->
                Timber.tag("Permission").d("Do not ask again ")
                // Show a dialog to the user explaining why the permission is needed
                // and guide them to the app settings.
                // userResult.onResult(true) will take the user to settings.
            }
            onShouldShowRationale { shouldShowRationaleList, onUserResult ->
                Timber.tag("Permission").d("Should show rationale")
                // Show a dialog explaining why you need the permission.
                // onUserResult.onResult(true) will proceed with the permission request.
            }
            onResult { allGranted, grantedList, deniedList ->
                // Handle the result
            }
        }
    }


    private fun loadInterstitial() {
        if (!AdRemoteConfig.isInitialized()) {
            AdRemoteConfig.initializeFromAssets(this)
        }
        AdsManager.loadInterOnboarding(this)
    }

    private fun showInterstitial() {
        AdsManager.showInterOnboarding(this) {
            Timber.d("Interstitial shown or skipped")
            AdsManager.loadInterOnboarding(this)
        }
    }

    private fun delayShowConsentDialog() {
        if (!RemoteConfigUtils.getOnShowDialogConsent()) {
            return
        }
        delayRunnable = Runnable {
            consentHandler.requestConsent()
        }
        delayHandler.postDelayed(delayRunnable!!, 5000L)
    }

    private fun maybeShowForceUpdateDialog() {
        val config = RemoteConfigUtils.getForceUpdateConfig() ?: return
        if (config.storeLink.isBlank()) return
        val needsUpdate = BuildConfig.VERSION_CODE < config.minVersionCode
        if (needsUpdate || config.force) {
            cachedForceUpdateConfig = config
            forceUpdateDialog.show(config)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::consentHandler.isInitialized) {
            consentHandler.clear()
        }
        delayRunnable?.let {
            delayHandler.removeCallbacks(it)
        }
        noInternetDialog.dismiss()
        forceUpdateDialog.dismiss()
    }
}
