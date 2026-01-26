![Ad Loading and Display Flow](img.png)

# Infinity Ads And Preferences Guide


📊 **[Xem sơ đồ Load và Show Ad](ad_flow_diagram.md)** - Splash, Language, Onboarding

This project ships with a local debug ad configuration and a single shared preferences entry point to keep onboarding state aligned across screens. The goal of this document is to explain how to work with `ad_config_debug.json` and `appSharedPref` without digging through the codebase.

## Debug Ad Configuration (`ad_config_debug.json`)

- Location: `app/src/main/assets/ad_config_debug.json`.
- Scope: loaded only in debug builds. Release builds read `ad_config.json` or the payload returned by Firebase Remote Config.
- Consumers: `AdRemoteConfig` exposes strongly typed getters for every key and `RemoteConfigUtils` uses the file to build the default map pushed into Firebase Remote Config (so you always have safe fallbacks offline).

### Edit The File

1. Keep the top-level structure as a map where each entry name is the same key used inside the app (`inter_splash`, `native_language_1`, etc.).
2. For every entry provide:
   - `id`: the AdMob (or mediated) placement id.
   - `isEnable`: toggle to allow quick enable/disable per placement.
   - Optional `reloadIntervalSeconds` for banners.
3. Stick to valid JSON. The Moshi adapter in `AdRemoteConfig` fails fast when a field is missing or malformed.

Example:
```
{
  "banner_splash": {
    "id": "ca-app-pub-xxx/yyy",
    "isEnable": true,
    "reloadIntervalSeconds": 30
  }
}
```

### Initialize And Refresh

- The ads module bootstraps itself by running `AdRemoteConfig.initialize(context)` very early (usually inside `GlobalApp`).
- When `BuildConfig.DEBUG` is true, the initializer always reads `ad_config_debug.json`. No remote call is needed.
- `RemoteConfigUtils.init(context, listener)` loads the same defaults into Firebase Remote Config and then fetches live overrides when available.
- If you change the JSON file while the app is running, call `AdRemoteConfig.reset()` followed by `AdRemoteConfig.initializeFromAssets(context)` to reload it without restarting the process.

### Fetch Remote Overrides

- Add a JSON string named `ad_remote_config` in Firebase Remote Config that mirrors the same structure. The utility method `RemoteConfigUtils.getAdRemoteConfig()` returns the raw string once `fetchAndActivate()` finishes.
- To apply remote overrides at runtime:
  1. Call `RemoteConfigUtils.getAdRemoteConfig()`.
  2. When the string is not blank, invoke `AdRemoteConfig.initializeFromJson(jsonString)`.
  3. All callers using `AdRemoteConfig.getInstance()` will start serving the new placements immediately.

### Troubleshooting

- If a key is missing, `AdRemoteConfig` logs a warning with Timber and returns a disabled placeholder so the UI can skip rendering the ad slot gracefully.
- Make sure every new placement key has a matching property in `AdRemoteConfig` to keep compile-time safety.

## Ad Language Layout (`ad_language_layout.json`)

- Location: `app/src/main/assets/ad_language_layout.json`.
- Purpose: defines typography, padding, and colors for the native ad layout used on the language/onboarding flow.
- Fallback flow: `RemoteConfigUtils.getAdLanguageLayout()` loads this asset first and only replaces it with the Remote Config value (`ad_language_layout`) when a non-empty payload exists.

### Customize Locally

1. Edit the JSON file in assets to adjust sizes, colors, or spacing keys such as `headlineTextSize`, `contentPadding`, or `callToActionBackgroundColor`.
2. Hot reload by calling `AdRemoteConfig.reset()` + `AdRemoteConfig.initializeFromAssets(context)` or simply reinstalling the debug build.

### Override From Remote Config

1. Create a Remote Config parameter named `ad_language_layout` with the same JSON structure.
2. After `RemoteConfigUtils.fetchAndActivate()` completes, the new JSON string automatically overrides the asset when `RemoteConfigUtils.getAdLanguageLayout()` is invoked.
3. Keep the schema identical to prevent crashes in the view layer.

## Onboarding Ad Display Rules

The onboarding flow only renders high-impact ad placements when both Firebase Remote Config and the SDK-side gating methods agree that the slot should show. Use the snippet below as a reference:

```
if (NkhAd.getInstance().shouldDisplayNativeOnboardingFull1) {
    AdsManager.loadNativeOnboardingFull(
        this,
        appSharedPref.firstOnBoarding,
        R.layout.layout_native_onboarding_full
    )
}
```

- `native_onboarding_full_1`: require `AdRemoteConfig.native_onboarding_fullscreen_1_3.isEnable == true` and `NkhAd.getInstance().shouldDisplayNativeOnboardingFull1 == true`. Remove any legacy `organic` flag checks.
- `native_onboarding_full_2`: require `AdRemoteConfig.native_onboarding_fullscreen_2_3.isEnable == true` and `NkhAd.getInstance().shouldDisplayNativeOnboardingFull2 == true`. Remove `organic`.
- `inter_onboarding`: require `AdRemoteConfig.inter_onboarding.isEnable == true` (or the equivalent key defined by your config) and `NkhAd.getInstance().shouldDisplayInterOnboarding() == true`. Remove `organic`.
- Widget uninstall builds: do not surface the widget when `NkhAd.getInstance().shouldDisplayWidgetUninstall() == false`, regardless of remote configuration. Replace any legacy `organic` filters with this predicate.

## Shared Preferences (`appSharedPref`)

`appSharedPref` is the single dependency injected into every `BaseActivity` and `BaseDialog` to provide screen state. It is backed by `AppSharedPreferencesApp`, which wraps Android `SharedPreferences` with strongly typed properties.

### Stored Flags

- `languageCode`: last language selected, defaults to `en`.
- `firstLanguage`: true until the user finishes the language picker.
- `firstOnBoarding`: true until onboarding completes.
- `isConfirmConsent`: marks that GDPR consent is collected.
- `isUserGlobal`: indicates the user is in a GDPR-required region.

### Typical Usage

- Activities call `appSharedPref` during navigation decisions. For example, `SplashActivity` opens the consent dialog only when `isConfirmConsent` is false and `isUserGlobal` is true.
- `LanguageActivity` updates `languageCode` and flips `firstOnBoarding` to control whether onboarding appears again.

Access pattern example:
```
appSharedPref.firstOnBoarding = false
appSharedPref.languageCode = isoCode
if (appSharedPref.isConfirmConsent.not()) {
    showConsentDialog()
}
```

### Best Practices

- Avoid reading `SharedPreferences` directly. Inject `AppSharedPref` instead so instrumentation tests can replace it with an in-memory fake.
- Keep new keys inside `AppSharedPreferencesApp` to guarantee a single source of truth and default values.
- When you add a new boolean flag, always choose a name that starts with `is`, `has`, or `can` to keep the code self-explanatory.

With these two pieces (debug ad config + shared preferences) understood, you can safely iterate on ad placements and onboarding flows without unexpected regressions.

### Production `ad_config.json` Refresh

- Release builds read `app/src/main/assets/ad_config.json` by default.
- To ship a hotfix without re-submitting to the store, upload the same JSON under the Remote Config key `ad_remote_config`. After `fetchAndActivate()`, call:

```kotlin
RemoteConfigUtils.getAdRemoteConfig()
    .takeIf { it.isNotBlank() }
    ?.let(AdRemoteConfig::initializeFromJson)
```

This rehydrates `AdRemoteConfig` at runtime with the new placements.

## Ads Package Overview (`app/src/main/java/com/itg/template/ads`)

| File | Responsibility |
| --- | --- |
| `AdRemoteConfig.kt` | Loads the JSON assets/Remote Config payload and exposes typed getters per placement. |
| `AdRemoteConfigExtensions.kt` | Convenience val extensions for strongly typed keys like `inter_splash`, `native_language_1`, etc. |
| `RemoteConfigUtils.kt` | Bootstraps Firebase Remote Config, pushes local defaults, exposes helper getters (`getAdRemoteConfig()`, `getAdLanguageLayout()`, `getForceUpdateConfig()`). |
| `AdsManager.kt` | Centralized loader/cacher for all ad objects (interstitial, native, onboarding full-screen). Observes network conditions and purchase state. |
| `AdExtension.kt` | Helpers to populate Google Native ads into custom layouts (CTA size, shimmer handling). |

### Typical Flow

1. `GlobalApp` calls `RemoteConfigUtils.init()` and assigns `NkhAd.getInstance().prepareLoadingAdsDialogLayout`.
2. `AdRemoteConfig.initialize()` runs once on startup. Every screen references placements via `AdRemoteConfig.<key>` or the extension vals.
3. UI layers never touch raw JSON. They call `AdsManager.load...`/`show...`, or check `AdRemoteConfig.<key>.isEnable` before rendering optional containers.

### Adding A New Placement

1. Extend `ad_config_debug.json` / `ad_config.json` with the new key.
2. Add a property to `AdRemoteConfigExtensions.kt` so Kotlin callers can read the key in a type-safe manner.
3. Decide where to preload the resource inside `AdsManager`.
4. Update the Remote Config default by re-running the app once (the value is uploaded via `RemoteConfigUtils` defaults) or manually editing the Remote Config console.

## Force Update Dialog (`force_update_config.json`)

- Location: `app/src/main/assets/force_update_config.json`.
- Remote key: `force_update_config` (string) in Firebase Remote Config. The local asset is used as a default value when the remote payload is empty.
- Data model: [`ForceUpdateConfig`](app/src/main/java/com/itg/template/data/model/ForceUpdateConfig.kt).
- Consumers: `RemoteConfigUtils.getForceUpdateConfig()` parses the JSON and `MainActivity` shows the Auto/Manual Force Update dialog (see `button_show_force_update`).

### JSON Schema

```json
{
  "icon": "https://.../force_icon.png",
  "title": "Update Required",
  "description": "New features and fixes are waiting for you.",
  "storeLink": "https://play.google.com/store/apps/details?id=com.itg.template",
  "minVersionCode": 123,
  "force": true
}
```

| Field | Description |
| --- | --- |
| `icon` | Optional URL shown above the copy. Falls back to launcher icon. |
| `title` | Optional. Defaults to the app name when blank. |
| `description` | Optional. Defaults to `force_update_message`. |
| `storeLink` | Google Play deep link. Required for automatic prompt. |
| `minVersionCode` | Minimum `BuildConfig.VERSION_CODE` required before prompting. |
| `force` | When `true`, dialog is not cancelable. |

After `RemoteConfigUtils.init()` completes, `maybeShowForceUpdateDialog()` compares the remote payload with the installed `versionCode` and renders `dialog_force_update.xml` through `ForceUpdateDialog`. The “Show Force Update” button in `MainActivity` reuses the last fetched config so QA can preview the dialog instantly.

## Blur-Based System Dialogs

- `BlurLoadingLayout` (root for `layout_prepare_ads.xml`, `dialog_no_internet.xml`, `dialog_force_update.xml`) captures the underlying window and feeds it to [BlurView](https://github.com/Dimezis/BlurView) for an iOS-like overlay blur.
- `NoInternetDialog` and `ForceUpdateDialog` (located in `ui/component/main/dialog`) are reusable helpers that inflate those layouts, handle button callbacks, and expose `show()/dismiss()` to the activity layer.
- The dialogs automatically snapshot the current activity, so even third-party overlays inherit the blurred background without extra code in the activity.

### Preview / Manual Trigger

- `button_show_force_update` in `activity_main.xml` triggers `ForceUpdateDialog` using the cached Remote Config payload (or the local fallback). This is useful during QA to review copy and design without changing Remote Config.
- Network loss is monitored via `ConnectionLiveData`; `NoInternetDialog` appears automatically whenever the observer emits `false`, and is dismissed as soon as connectivity is restored.
