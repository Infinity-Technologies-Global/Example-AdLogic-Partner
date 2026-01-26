# Sơ đồ Load và Show Ad - Splash, Language, Onboarding

## Luồng chính

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           APP START                                     │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        SPLASH ACTIVITY                                   │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ 1. Check Consent                                                  │  │
│  │    - Not Confirmed → Request Consent Flow                        │  │
│  │    - Confirmed → Continue                                        │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                │                                         │
│                                ▼                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ 2. Load RemoteConfig                                             │  │
│  │    - From Firebase RemoteConfig                                  │  │
│  │    - Fallback: Assets (ad_config.json)                           │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                │                                         │
│                                ▼                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ 3. Initialize AdRemoteConfig                                     │  │
│  │    - Parse JSON config                                           │  │
│  │    - Setup ad unit configurations                                │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                │                                         │
│                                ▼                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ 4. Load Native Language Ad                                       │  │
│  │    - native_language_1 / native_language_2                       │  │
│  │      (dựa vào firstLanguage)                                    │  │
│  │    - native_language_1_click / native_language_2_click          │  │
│  │      (cho click event)                                           │  │
│  │    → Stored in: AdsManager.nativeLanguageAd                     │  │
│  │    → Stored in: AdsManager.nativeLanguageClickAd                │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                │                                         │
│                                ▼                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ 5. Check Inter Splash Ad                                         │  │
│  │    Condition: AdRemoteConfig.inter_splash.isEnable               │  │
│  │              && isNetwork()                                      │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                │                                         │
│                    ┌────────────┴────────────┐                           │
│                    │                        │                           │
│                    ▼                        ▼                           │
│  ┌──────────────────────────┐  ┌──────────────────────────┐            │
│  │ YES: Show Interstitial   │  │ NO: Skip Ad              │            │
│  │      Splash Ad           │  │                          │            │
│  │  - Timeout: 30s          │  │                          │            │
│  │  - Min Time: 5s          │  │                          │            │
│  │  - AdRemoteConfig        │  │                          │            │
│  │    .inter_splash.id      │  │                          │            │
│  └──────────────────────────┘  └──────────────────────────┘            │
│                    │                        │                           │
│                    └────────────┬────────────┘                           │
│                                 │                                         │
│                                 ▼                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ 6. Navigate                                                      │  │
│  │    - Should Show Language? → LanguageActivity                    │  │
│  │    - No → MainActivity                                           │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                │
                    ┌───────────┴───────────┐
                    │                       │
                    ▼                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      LANGUAGE ACTIVITY                                  │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ 1. Check From Setting                                             │  │
│  │    - Yes → Hide Ads Container                                     │  │
│  │    - No → Continue                                                │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                │                                         │
│                                ▼                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ 2. Load Native Onboarding Ads                                    │  │
│  │    (Cho màn Onboarding)                                           │  │
│  │    - native_onboarding_1_1 / native_onboarding_2_1                │  │
│  │      (dựa vào firstOnBoarding)                                   │  │
│  │    - native_onboarding_1_4 / native_onboarding_2_4                │  │
│  │    → Stored in: AdsManager.nativeOnboarding1Ad                   │  │
│  │    → Stored in: AdsManager.nativeOnboarding4Ad                    │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                │                                         │
│                                ▼                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ 3. Show Native Language Ad                                       │  │
│  │    - Check: AdsManager.nativeLanguageAd (đã load từ Splash)      │  │
│  │    - If loaded → populateNativeAdView() ngay                      │  │
│  │    - If not loaded → Wait for onLoadNativeSuccess() callback     │  │
│  │    - Container: flAds                                             │  │
│  │    - Layout: RemoteConfigUtils.getAdLanguageLayout()             │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                │                                         │
│                                ▼                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ 4. User Interaction                                               │  │
│  │    - User clicks language item                                    │  │
│  │    → Show Native Language Click Ad                                │  │
│  │      (AdsManager.nativeLanguageClickAd)                          │  │
│  │    → Delay show Done button                                       │  │
│  │    → User clicks Done                                             │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                │                                         │
│                                ▼                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ 5. Navigate                                                      │  │
│  │    - From Setting? → MainActivity                                 │  │
│  │    - No → OnBoardingActivity                                     │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     ONBOARDING ACTIVITY                                 │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ 1. Check Should Display Native Full                              │  │
│  │    Condition: NkhAd.getInstance()                                │  │
│  │              .shouldDisplayNativeOnboardingFull1 == true          │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                │                                         │
│                    ┌────────────┴────────────┐                           │
│                    │                        │                           │
│                    ▼                        ▼                           │
│  ┌──────────────────────────┐  ┌──────────────────────────┐            │
│  │ YES: Load Native         │  │ NO: Skip                 │            │
│  │      Onboarding Full     │  │                          │            │
│  │  - native_onboarding_    │  │                          │            │
│  │    fullscreen_1_3 /      │  │                          │            │
│  │    fullscreen_2_3        │  │                          │            │
│  │  → Stored in:            │  │                          │            │
│  │    AdsManager.           │  │                          │            │
│  │    nativeAdOnBoardingFull│  │                          │            │
│  └──────────────────────────┘  └──────────────────────────┘            │
│                    │                        │                           │
│                    └────────────┬────────────┘                           │
│                                 │                                         │
│                                 ▼                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ 2. Initialize Onboarding Pages                                   │  │
│  │    - Setup ViewPager2                                            │  │
│  │    - Create 4 pages                                              │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                │                                         │
│                                ▼                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ 3. Show Ads on Pages                                             │  │
│  │                                                                   │  │
│  │  ┌───────────────────────────────────────────────────────────┐  │  │
│  │  │ PAGE 1                                                     │  │  │
│  │  │ - Show Native Ad                                           │  │  │
│  │  │ - AdsManager.nativeOnboarding1Ad                           │  │  │
│  │  │ - isHasNativeOnPage1 = true                                │  │  │
│  │  └───────────────────────────────────────────────────────────┘  │  │
│  │                                │                                 │  │
│  │                                ▼                                 │  │
│  │  ┌───────────────────────────────────────────────────────────┐  │  │
│  │  │ PAGE 2                                                     │  │  │
│  │  │ - No Ad                                                    │  │  │
│  │  └───────────────────────────────────────────────────────────┘  │  │
│  │                                │                                 │  │
│  │                                ▼                                 │  │
│  │  ┌───────────────────────────────────────────────────────────┐  │  │
│  │  │ PAGE 3                                                     │  │  │
│  │  │ - Show Native Full Ad (Fullscreen)                         │  │  │
│  │  │ - AdsManager.nativeAdOnBoardingFull                        │  │  │
│  │  │ - NkhAd.getInstance().shouldDisplayNativeOnboardingFull1   │  │  │
│  │  │ - isHasNativeFull = true                                   │  │  │
│  │  └───────────────────────────────────────────────────────────┘  │  │
│  │                                │                                 │  │
│  │                                ▼                                 │  │
│  │  ┌───────────────────────────────────────────────────────────┐  │  │
│  │  │ PAGE 4                                                     │  │  │
│  │  │ - Show Native Ad                                           │  │  │
│  │  │ - AdsManager.nativeOnboarding4Ad                           │  │  │
│  │  │ - isHasNativeOnPage4 = true                                │  │  │
│  │  └───────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                │                                         │
│                                ▼                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ 4. Navigate                                                      │  │
│  │    - Last page? → Start MainActivity                             │  │
│  │    - Not last → Next page                                        │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         MAIN ACTIVITY                                    │
└─────────────────────────────────────────────────────────────────────────┘
```

## Bảng tổng hợp Ad Units

### SplashActivity - Ad Units Loaded

| Ad Unit | Config Key | Storage Location | Mục đích |
|---------|-----------|------------------|----------|
| Native Language Main | `native_language_1` / `native_language_2` | `AdsManager.nativeLanguageAd` | Show trên Language screen |
| Native Language Click | `native_language_1_click` / `native_language_2_click` | `AdsManager.nativeLanguageClickAd` | Show khi user click language |
| Interstitial Splash | `inter_splash` | Load & Show trực tiếp | Show trước khi navigate |

### LanguageActivity - Ad Units Loaded

| Ad Unit | Config Key | Storage Location | Mục đích |
|---------|-----------|------------------|----------|
| Native Onboarding 1 | `native_onboarding_1_1` / `native_onboarding_2_1` | `AdsManager.nativeOnboarding1Ad` | Show trên Onboarding page 1 |
| Native Onboarding 4 | `native_onboarding_1_4` / `native_onboarding_2_4` | `AdsManager.nativeOnboarding4Ad` | Show trên Onboarding page 4 |

### OnBoardingActivity - Ad Units Loaded

| Ad Unit | Config Key | Storage Location | Mục đích |
|---------|-----------|------------------|----------|
| Native Onboarding Full | `native_onboarding_fullscreen_1_3` / `native_onboarding_fullscreen_2_3` | `AdsManager.nativeAdOnBoardingFull` | Show fullscreen trên page 3 |

## Bảng điều kiện Show Ad

| Điều kiện | Kiểm tra | Code |
|-----------|----------|------|
| Ad Enabled | `AdUnitConfig.isEnable == true` | `config.isEnable` |
| Not Purchased | `AppPurchase.isPurchased() == false` | `AppPurchase.getInstance().isPurchased(activity)` |
| Has Network | `isNetworkAvailable() == true` | `activity.isNetworkAvailable()` |
| Config Loaded | RemoteConfig đã fetch hoặc có default | `RemoteConfigUtils.completed` hoặc từ Assets |

## Timeline Load và Show Ad

| Thời điểm | Màn hình | Hành động | Ad Unit | Trạng thái |
|-----------|----------|-----------|---------|------------|
| **T0** | SplashActivity | Load RemoteConfig | - | Loading |
| **T1** | SplashActivity | Initialize AdRemoteConfig | - | Ready |
| **T2** | SplashActivity | Load Native Language Ad | `native_language_1/2` | Loading |
| **T3** | SplashActivity | Load Native Language Click Ad | `native_language_1_click/2_click` | Loading |
| **T4** | SplashActivity | Show Interstitial Splash | `inter_splash` | Show (nếu enabled) |
| **T5** | LanguageActivity | Load Native Onboarding Ads | `native_onboarding_1_1/2_1`, `1_4/2_4` | Loading |
| **T6** | LanguageActivity | Show Native Language Ad | `nativeLanguageAd` (đã load ở T2) | Show |
| **T7** | LanguageActivity | User clicks language | `nativeLanguageClickAd` (đã load ở T3) | Show |
| **T8** | OnBoardingActivity | Load Native Onboarding Full | `native_onboarding_fullscreen_1_3/2_3` | Loading |
| **T9** | OnBoardingActivity - Page 1 | Show Native Ad | `nativeOnboarding1Ad` (đã load ở T5) | Show |
| **T10** | OnBoardingActivity - Page 2 | No Ad | - | - |
| **T11** | OnBoardingActivity - Page 3 | Show Native Full Ad | `nativeAdOnBoardingFull` (đã load ở T8) | Show |
| **T12** | OnBoardingActivity - Page 4 | Show Native Ad | `nativeOnboarding4Ad` (đã load ở T5) | Show |

## Chi tiết các bước Load và Show Ad

### 1. SplashActivity

**Load Ad:**
- Load RemoteConfig từ Firebase hoặc Assets
- Initialize AdRemoteConfig với config từ RemoteConfig
- Load Native Language Ad (cho màn Language):
  - `native_language_1` hoặc `native_language_2` (dựa vào `firstLanguage`)
  - `native_language_1_click` hoặc `native_language_2_click` (cho click event)

**Show Ad:**
- Show Interstitial Splash Ad (`inter_splash`) nếu:
  - Config enabled
  - Có network
  - Timeout: 30s, Min time: 5s
- Sau khi ad show/fail → Navigate đến Language hoặc Main

### 2. LanguageActivity

**Load Ad:**
- Load Native Onboarding Ads (cho màn Onboarding):
  - `native_onboarding_1_1` hoặc `native_onboarding_2_1` (dựa vào `firstOnBoarding`)
  - `native_onboarding_1_4` hoặc `native_onboarding_2_4`

**Show Ad:**
- Show Native Language Ad ngay khi vào màn:
  - Sử dụng `AdsManager.nativeLanguageAd` (đã load từ Splash)
  - Populate vào `flAds` container
  - Sử dụng layout config từ `RemoteConfigUtils.getAdLanguageLayout()`
  
- Show Native Language Click Ad khi user click chọn language:
  - Sử dụng `AdsManager.nativeLanguageClickAd` (đã load từ Splash)
  - Populate vào cùng container `flAds`
  - Delay show Done button sau khi click

### 3. OnBoardingActivity

**Load Ad:**
- Load Native Onboarding Full Ad (cho page 3):
  - `native_onboarding_fullscreen_1_3` hoặc `native_onboarding_fullscreen_2_3`
  - Chỉ load nếu `NkhAd.getInstance().shouldDisplayNativeOnboardingFull1 == true`

**Show Ad:**
- **Page 1**: Show Native Ad (`nativeOnboarding1Ad`) - nếu có
- **Page 2**: Không có ad
- **Page 3**: Show Native Full Ad (`nativeAdOnBoardingFull`) - fullscreen
- **Page 4**: Show Native Ad (`nativeOnboarding4Ad`) - nếu có

## Luồng Callback

```
SplashActivity
  └─> loadNativeLanguage()
      └─> AdsManager.loadNativeConfig()
          └─> onNativeAdLoaded() → nativeLanguageAd/nativeLanguageClickAd stored

LanguageActivity
  └─> showNativeLanguage()
      └─> Check AdsManager.nativeLanguageAd
          └─> If loaded → populateNativeAdView()
          └─> If not loaded → Wait for onLoadNativeSuccess()

OnBoardingActivity
  └─> loadNativeOnboardingFull()
      └─> AdsManager.loadNativeConfig()
          └─> onNativeAdLoaded() → nativeAdOnBoardingFull stored
          └─> onLoadNativeSuccess() → notifyNativeAdFullLoaded()
              └─> ViewModel updates → UI shows ad on page 3
```

## Điều kiện Show Ad

- **Ad Enabled**: `AdUnitConfig.isEnable == true`
- **Not Purchased**: `AppPurchase.getInstance().isPurchased() == false`
- **Has Network**: `isNetworkAvailable() == true`
- **Config Loaded**: RemoteConfig đã fetch thành công hoặc dùng default từ Assets

