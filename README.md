**Language / Ngôn ngữ / भाषा:** [English](README.md) | [Tiếng Việt](README.vi.md) | [हिन्दी](README.hi.md)

# Ads Integration Guide — Base Project (EN)

This document is the **mandatory reference standard** for partner teams integrating ads into Infinity products. Any ad-related change must follow the architecture, load/show flow, and gating rules defined in this base project.

---

## Purpose and Scope

### 1. Shared base for all apps

`Example-AdLogic-Partner` is designed as the **template/base** for all Android apps in the ecosystem. Partners should fork or clone this base to keep:

- The same Ads package structure (`AdRemoteConfig`, `RemoteConfigUtils`, `AdsManager`, `AdExtension`).
- The same config source flow (assets + Firebase Remote Config).
- The same LiveData observation pattern for native ad rendering.
- The same QA entry through DevSetting on Language screen.

Goal: reduce app-to-app drift, improve maintainability, simplify audits, and centralize technical support.

### 2. Load/show logic is the optimized baseline

The current base flow — early init in `GlobalApp`, config sync in `Splash`, next-screen preload, centralized gating in `AdsManager`, and organic handling via `ERainAd.shouldDisplay...` — is the result of iterative optimization for:

- Correct load timing
- Reduced UI jank
- Safe fallback when offline / purchased
- Cohort-based display control

**Partners must not change the core flow** (for example: calling SDK directly and bypassing `AdsManager`, removing organic gating, or changing load/show order) unless approved by Infinity technical team.

### 3. Existing ad-enabled screens must follow current implementation

The following screens are already implemented and must preserve load/show behavior, preload points, and gating conditions:

| Screen | Placements / behavior |
| --- | --- |
| Splash | `inter_splash`, preload `native_language`, `open_resume` config |
| Language | Native language/click, preload onboarding page 1, DevSetting (`tvTitle`) |
| Onboarding | Native page 1 & 4, native full, `inter_onboarding`, uninstall widget |
| Welcome / Resume | `native_welcome`, `inter_welcome`, `ResumeAdsEntryRule` |
| Banner (Home + screens extending `BaseActivityWithBanner`) | Normal / collapsible banner, reload by config |

When customizing UI, only adjust layout/container. Do **not** remove `isEnable`, purchase, network, or `shouldDisplay...` checks.

### 4. Custom app screens must follow load/show rules

For any **new custom screen** (not already present in base), partners must follow the same rule set:

1. Add placement keys to `ad_config.json` / `ad_config_debug.json` and add matching properties in `AdRemoteConfig`.
2. Add load methods in `AdsManager` (native via `loadNativeInternal`, interstitial via `load` + `show` pattern).
3. In Activity/Fragment: load in `initViews` (optional short `postDelayed`), observe LiveData, call `populateNativeAdView` when ad exists, hide container on `null`.
4. For sensitive placements (onboarding-like, welcome, widget...): attach the relevant `ERainAd.getInstance().shouldDisplay...` gate or align with Infinity before shipping.
5. For banners: extend `BaseActivityWithBanner`, configure `BannerConfig`, do not load banner outside `AdsManager.loadBanner`.

Detailed UI/Ads reference (CTA size, Done button delay, native placement by page):
[Infinity UI Documentation — Language & Onboarding](https://interim-pink-4gmxxkfh.edgeone.app/).

---

## 1. Ads Initialization and Config

### 1.1 Config sources
- Debug: read `ad_config_debug.json`.
- Release: read `ad_config.json`, then optionally override from Firebase Remote Config (`ad_remote_config`).

### 1.2 Initialization points
- `GlobalApp.onCreate()`:
  - `AdRemoteConfig.initializeFromAssets(this)`
  - `ERainAd.getInstance().init(...)`
- `SplashActivity.checkRemoteConfigResult()`:
  - `AdRemoteConfig.initialize(this, RemoteConfigUtils.getAdRemoteConfig())` to apply latest remote config.

### 1.3 `initAds()` integration in `GlobalApp` (recommended standard)

In the current base, the core integration is implemented in `GlobalApp.initAds()`. Partners should keep this pattern when creating new apps:

1. Select `environment` by build type (`ERainAdConfig.ENVIRONMENT_DEVELOP` / `ERainAdConfig.ENVIRONMENT_PRODUCTION`).
2. Create `mERainAdConfig = ERainAdConfig(this, environment)`.
3. Set required config fields before calling `ERainAd.init(...)`:
   - `adjustConfig`
   - `facebookClientToken`
   - `adjustTokenTiktok`
   - `intervalInterstitialAd`
   - `idAdResume`
4. Call `ERainAd.getInstance().init(this, mERainAdConfig)`.
5. Apply extra resume/inter rules:
   - `Admob.getInstance().setDisableAdResumeWhenClickAds(true)`
   - `Admob.getInstance().setOpenActivityAfterShowInterAds(true)`
   - `AppOpenManager.getInstance().disableAppResumeWithActivity(...)` for excluded screens.

Reference snippet:
```kotlin
private fun initAds() {
    val environment =
        if (BuildConfig.DEBUG) ERainAdConfig.ENVIRONMENT_DEVELOP else ERainAdConfig.ENVIRONMENT_PRODUCTION
    mERainAdConfig = ERainAdConfig(this, environment)

    mERainAdConfig.adjustConfig = AdjustConfig(true, resources.getString(R.string.adjust_token))
    mERainAdConfig.facebookClientToken = resources.getString(R.string.facebook_client_token)
    mERainAdConfig.adjustTokenTiktok = resources.getString(R.string.event_token)
    mERainAdConfig.intervalInterstitialAd = 35
    mERainAdConfig.idAdResume = ""

    ERainAd.getInstance().init(this, mERainAdConfig)
}
```

> Note: `initAdRemoteConfig()` should still run before `initAds()`, and remote config is still synced in `SplashActivity` via `RemoteConfigUtils.init(...)` + `AdRemoteConfig.initialize(...)`.

### 1.4 DevSetting entry for Ads QA
- `LanguageActivity`: `mBinding.tvTitle.setOnAdminAdToggleListener()`
- QA can check: sdk versions, mediation, config id, ad id, reset organic.

> **Mandatory in `app/build.gradle`:** to make DevConfig UI show version info correctly, partners must declare all 3 `buildConfigField` lines below (in both `debug` and `release`):
>
> ```gradle
> buildConfigField "String", "ERAIN_STUDIO_VERSION", "\"$erain_studio_version\""
> buildConfigField "String", "PLAY_SERVICES_ADS_VERSION", "\"$play_services_ads_version\""
> buildConfigField "String", "GDPR_MODULE_VERSION", "\"$module_update_gdpr_version\""
> ```

## 2. Load/Show Ads by placement

### 2.1 Splash
- Inter Splash:
  - Condition: `AdRemoteConfig.inter_splash.isEnable == true` and network available.
  - API: `ERainAd.getInstance().loadSplashInterstitialAds(...)`.
  - On successful load (`onAdLoaded`), preload `native_language`.
- Open Resume:
  - Enabled/disabled by `ResumeAdsEntryRule.shouldEnableOpenResume()`.

### 2.2 Language
- Native language:
  - Preload from Splash: `AdsManager.loadNativeLanguage(...)`
  - Click variant: `AdsManager.loadNativeLanguageClick(...)`
- Early preload for onboarding page 1:
  - `AdsManager.loadNativeOnboarding1(...)`

### 2.3 Onboarding
- `AdsManager.loadNativeOnboarding4(...)`
- `AdsManager.loadNativeOnboardingFull(...)`
- `AdsManager.loadInterOnboarding(...)`, then show via `AdsManager.showInterOnboarding(...)` at onboarding completion.

### 2.4 Welcome / Resume
- Native welcome:
  - `AdsManager.loadNativeWelcome(...)` gated by `shouldDisplayNativeWelcomeBack`.
- Inter welcome:
  - `AdsManager.loadInterWelcome(...)`, `AdsManager.showInterWelcome(...)`.
  - Welcome flow is triggered by `AppLifecycleObserver` when `ResumeAdsEntryRule.shouldShowWelcomeOnResume()` and `shouldDisplayInterWelcomeBack` allow it.

### 2.5 Banner (normal / collapsible)
- Use `BaseActivityWithBanner`.
- `AdsManager.loadBanner(..., isCollapse = false)` => normal banner.
- `AdsManager.loadBanner(..., isCollapse = true)` => collapsible banner (SDK expand/collapse behavior).
- Reload interval follows `reloadIntervalSeconds`.

## 3. Global conditions for loading ads

In `AdsManager`, an ad loads only when all conditions pass:
- `adUnitConfig.isEnable == true`
- `!AppPurchase.getInstance().isPurchased(...)`
- Network available
- If organic gate exists: `shouldDisplay... == true`

If any condition fails, native LiveData emits `null` so UI hides the ad container.

## 4. `shouldDisplay*` variables (Ads / Organic)
- `shouldDisplayNativeOnboardingNormal1`
  - Used in `AdsManager.loadNativeOnboarding1(...)`
- `shouldDisplayNativeOnboardingNormal2`
  - Used in `AdsManager.loadNativeOnboarding4(...)`
- `shouldDisplayNativeOnboardingFull1`
  - Used in `AdsManager.loadNativeOnboardingFull(...)`
  - Also used in `OnBoardingActivity.initOnboardingItems()` to decide inserting full native page
- `shouldDisplayInterOnboarding`
  - Used in both `loadInterOnboarding(...)` and `showInterOnboarding(...)`
- `shouldDisplayNativeWelcomeBack`
  - Used in `loadNativeWelcome(...)`
- `shouldDisplayInterWelcomeBack`
  - Used in `AppLifecycleObserver` (gate for welcome flow)
  - Also part of `loadInterWelcome(...)` condition
- `shouldDisplayWidgetUninstall`
  - Used in `OnBoardingActivity.applyUninstallWidgetShortcutsFromRemoteConfig()` to enable/disable uninstall widget

## 5. Organic mechanism

Organic here is user classification from Ads SDK / growth logic used to:
- Reduce frequency or disable sensitive ad slots for certain users
- Balance retention, UX, and revenue
- Run cohort-based rules without rewriting each screen

How it works in this app:
- The app does not compute organic locally; it reads `ERainAd.getInstance().shouldDisplay...` flags.
- When organic/cohort rules change, these flags change and directly affect load/show behavior per slot.
- `reset organic` in DevSetting helps QA restore clean test state and re-verify all ad slots + uninstall widget behavior.

## 6. Load/show examples (quick reference)

### 6.1 Inter Splash
```kotlin
if (AdRemoteConfig.inter_splash.isEnable && isNetwork(this)) {
    ERainAd.getInstance().loadSplashInterstitialAds(
        this, AdRemoteConfig.inter_splash.id, 30000, 5000, object : AdCallback() {
            override fun onNextAction() { moveActivity() }
        }
    )
} else moveActivity()
```

### 6.2 Native (via AdsManager)
```kotlin
AdsManager.loadNativeOnboarding1(this, appSharedPref.firstOnBoarding, R.layout.layout_native_onboarding)
AdsManager.nativeOnboarding1AdLive.observe(this) { ad ->
    if (ad == null) hideAd() else showAd(ad)
}
```

### 6.3 Inter (Onboarding)
```kotlin
AdsManager.loadInterOnboarding(this)
AdsManager.showInterOnboarding(this) {
    goNextScreen()
}
```

### 6.4 Normal banner
```kotlin
override val bannerConfig = BannerConfig(
    adUnitConfig = AdRemoteConfig.banner_home,
    isCollapse = false
)
```

### 6.5 Collapsible banner (expand/collapse)
```kotlin
override val bannerConfig = BannerConfig(
    adUnitConfig = AdRemoteConfig.banner_home,
    isCollapse = true
)
```

## 7. Additional reference

- [Infinity UI Documentation — Language & Onboarding](https://interim-pink-4gmxxkfh.edgeone.app/) — UI, Remote Config, and ad-unit display conditions (placement/method names should be cross-checked against this document).
