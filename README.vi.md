![Ad Loading and Display Flow](img.png)

**Language / Ngôn ngữ / भाषा:** [English](README.md) | [Tiếng Việt](README.vi.md) | [हिन्दी](README.hi.md)

# Hướng dẫn Infinity Ads và Preferences


📊 **[Xem sơ đồ Load và Show Ad](ad_flow_diagram.md)** - Splash, Language, Onboarding

Dự án này đi kèm với cấu hình quảng cáo debug cục bộ và một entry point shared preferences duy nhất để giữ trạng thái onboarding đồng bộ giữa các màn hình. Mục tiêu của tài liệu này là giải thích cách làm việc với `ad_config_debug.json` và `appSharedPref` mà không cần phải tìm hiểu sâu vào codebase.

## Cấu hình Quảng cáo Debug (`ad_config_debug.json`)

- Vị trí: `app/src/main/assets/ad_config_debug.json`.
- Phạm vi: chỉ được load trong các bản debug. Các bản release đọc `ad_config.json` hoặc payload trả về bởi Firebase Remote Config.
- Người dùng: `AdRemoteConfig` expose các getter định kiểu mạnh cho mỗi key và `RemoteConfigUtils` sử dụng file để build map mặc định được đẩy vào Firebase Remote Config (để bạn luôn có fallback an toàn khi offline).

### Chỉnh sửa File

1. Giữ cấu trúc cấp cao nhất là một map trong đó tên mỗi entry là cùng một key được sử dụng trong app (`inter_splash`, `native_language_1`, v.v.).
2. Đối với mỗi entry, cung cấp:
   - `id`: placement id của AdMob (hoặc mediated).
   - `isEnable`: toggle để bật/tắt nhanh cho mỗi placement.
   - Tùy chọn `reloadIntervalSeconds` cho banner.
3. Tuân thủ JSON hợp lệ. Adapter Moshi trong `AdRemoteConfig` sẽ lỗi ngay lập tức khi thiếu hoặc sai field.

Ví dụ:
```
{
  "banner_splash": {
    "id": "ca-app-pub-xxx/yyy",
    "isEnable": true,
    "reloadIntervalSeconds": 30
  }
}
```

### Khởi tạo và Làm mới

- Module quảng cáo tự khởi động bằng cách chạy `AdRemoteConfig.initialize(context)` rất sớm (thường bên trong `GlobalApp`).
- Khi `BuildConfig.DEBUG` là true, trình khởi tạo luôn đọc `ad_config_debug.json`. Không cần gọi remote.
- `RemoteConfigUtils.init(context, listener)` load cùng các giá trị mặc định vào Firebase Remote Config và sau đó fetch các override trực tiếp khi có sẵn.
- Nếu bạn thay đổi file JSON khi app đang chạy, hãy gọi `AdRemoteConfig.reset()` theo sau là `AdRemoteConfig.initializeFromAssets(context)` để reload nó mà không cần khởi động lại process.

### Lấy Overrides từ Remote

- Thêm một chuỗi JSON có tên `ad_remote_config` trong Firebase Remote Config phản ánh cùng cấu trúc. Phương thức tiện ích `RemoteConfigUtils.getAdRemoteConfig()` trả về chuỗi thô khi `fetchAndActivate()` hoàn tất.
- Để áp dụng remote overrides tại runtime:
  1. Gọi `RemoteConfigUtils.getAdRemoteConfig()`.
  2. Khi chuỗi không trống, gọi `AdRemoteConfig.initializeFromJson(jsonString)`.
  3. Tất cả các caller sử dụng `AdRemoteConfig.getInstance()` sẽ bắt đầu phục vụ các placement mới ngay lập tức.

### Khắc phục sự cố

- Nếu một key bị thiếu, `AdRemoteConfig` sẽ log cảnh báo với Timber và trả về một placeholder bị disable để UI có thể bỏ qua việc render slot quảng cáo một cách nhẹ nhàng.
- Đảm bảo mỗi key placement mới có một property tương ứng trong `AdRemoteConfig` để giữ an toàn compile-time.

## Layout Quảng cáo Ngôn ngữ (`ad_language_layout.json`)

- Vị trí: `app/src/main/assets/ad_language_layout.json`.
- Mục đích: định nghĩa typography, padding, và màu sắc cho layout quảng cáo native được sử dụng trên luồng language/onboarding.
- Luồng fallback: `RemoteConfigUtils.getAdLanguageLayout()` load asset này trước và chỉ thay thế nó bằng giá trị Remote Config (`ad_language_layout`) khi tồn tại payload không trống.

### Tùy chỉnh Cục bộ

1. Chỉnh sửa file JSON trong assets để điều chỉnh kích thước, màu sắc, hoặc các key giãn cách như `headlineTextSize`, `contentPadding`, hoặc `callToActionBackgroundColor`.
2. Hot reload bằng cách gọi `AdRemoteConfig.reset()` + `AdRemoteConfig.initializeFromAssets(context)` hoặc đơn giản là cài lại bản debug.

### Override từ Remote Config

1. Tạo một tham số Remote Config có tên `ad_language_layout` với cùng cấu trúc JSON.
2. Sau khi `RemoteConfigUtils.fetchAndActivate()` hoàn tất, chuỗi JSON mới tự động override asset khi `RemoteConfigUtils.getAdLanguageLayout()` được gọi.
3. Giữ schema giống hệt để tránh crash ở tầng view.

## Quy tắc Hiển thị Quảng cáo Onboarding

Luồng onboarding chỉ render các placement quảng cáo tác động cao khi cả Firebase Remote Config và các method gating phía SDK đồng ý rằng slot nên hiển thị. Sử dụng snippet bên dưới để tham khảo:

```
if (ERainAd.getInstance().shouldDisplayNativeOnboardingFull1) {
    AdsManager.loadNativeOnboardingFull(
        this,
        appSharedPref.firstOnBoarding,
        R.layout.layout_native_onboarding_full
    )
}
```

- `native_onboarding_full_1`: yêu cầu `AdRemoteConfig.native_onboarding_fullscreen_1_3.isEnable == true` và `ERainAd.getInstance().shouldDisplayNativeOnboardingFull1 == true`. Loại bỏ bất kỳ kiểm tra cờ `organic` cũ nào.
- `native_onboarding_full_2`: yêu cầu `AdRemoteConfig.native_onboarding_fullscreen_2_3.isEnable == true` và `ERainAd.getInstance().shouldDisplayNativeOnboardingFull2 == true`. Loại bỏ `organic`.
- `inter_onboarding`: yêu cầu `AdRemoteConfig.inter_onboarding.isEnable == true` (hoặc key tương đương được định nghĩa bởi config của bạn) và `ERainAd.getInstance().shouldDisplayInterOnboarding() == true`. Loại bỏ `organic`.
- Build gỡ cài đặt widget: không hiện widget khi `ERainAd.getInstance().shouldDisplayWidgetUninstall() == false`, bất kể cấu hình remote. Thay thế bất kỳ bộ lọc `organic` cũ nào bằng điều kiện này.

## Shared Preferences (`appSharedPref`)

`appSharedPref` là dependency duy nhất được inject vào mỗi `BaseActivity` và `BaseDialog` để cung cấp trạng thái màn hình. Nó được hỗ trợ bởi `AppSharedPreferencesApp`, bao bọc Android `SharedPreferences` với các property định kiểu mạnh.

### Các Cờ đã Lưu

- `languageCode`: ngôn ngữ được chọn cuối cùng, mặc định là `en`.
- `firstLanguage`: true cho đến khi người dùng hoàn thành chọn ngôn ngữ.
- `firstOnBoarding`: true cho đến khi onboarding hoàn tất.
- `isConfirmConsent`: đánh dấu rằng sự đồng ý GDPR đã được thu thập.
- `isUserGlobal`: cho biết người dùng đang ở khu vực yêu cầu GDPR.

### Cách dùng Điển hình

- Các Activity gọi `appSharedPref` trong các quyết định điều hướng. Ví dụ, `SplashActivity` mở dialog đồng ý chỉ khi `isConfirmConsent` là false và `isUserGlobal` là true.
- `LanguageActivity` cập nhật `languageCode` và lật `firstOnBoarding` để kiểm soát xem onboarding có xuất hiện lại hay không.

Ví dụ mẫu truy cập:
```
appSharedPref.firstOnBoarding = false
appSharedPref.languageCode = isoCode
if (appSharedPref.isConfirmConsent.not()) {
    showConsentDialog()
}
```

### Best Practices

- Tránh đọc `SharedPreferences` trực tiếp. Inject `AppSharedPref` thay vào đó để các instrumentation test có thể thay thế nó bằng một fake trong bộ nhớ.
- Giữ các key mới bên trong `AppSharedPreferencesApp` để đảm bảo một nguồn sự thật (source of truth) duy nhất và giá trị mặc định.
- Khi bạn thêm một cờ boolean mới, luôn chọn tên bắt đầu bằng `is`, `has`, hoặc `can` để giữ code tự giải thích.

Với hai phần này (cấu hình quảng cáo debug + shared preferences) đã được hiểu, bạn có thể iterate an toàn trên các placement quảng cáo và luồng onboarding mà không gặp phải regression bất ngờ.

### Làm mới `ad_config.json` Production

- Các bản release đọc `app/src/main/assets/ad_config.json` theo mặc định.
- Để ship một hotfix mà không cần gửi lại lên store, upload cùng JSON dưới key Remote Config `ad_remote_config`. Sau khi `fetchAndActivate()`, gọi:

```kotlin
RemoteConfigUtils.getAdRemoteConfig()
    .takeIf { it.isNotBlank() }
    ?.let(AdRemoteConfig::initializeFromJson)
```

Việc này rehydrate `AdRemoteConfig` tại runtime với các placement mới.

## Tổng quan Package Ads (`app/src/main/java/com/itg/template/ads`)

| File | Trách nhiệm |
| --- | --- |
| `AdRemoteConfig.kt` | Load các asset JSON/Remote Config payload và expose các getter định kiểu cho mỗi placement. |
| `AdRemoteConfigExtensions.kt` | Extension val tiện lợi cho các key định kiểu mạnh như `inter_splash`, `native_language_1`, v.v. |
| `RemoteConfigUtils.kt` | Bootstraps Firebase Remote Config, đẩy các mặc định cục bộ, expose các getter hỗ trợ (`getAdRemoteConfig()`, `getAdLanguageLayout()`, `getForceUpdateConfig()`). |
| `AdsManager.kt` | Loader/cacher tập trung cho tất cả các đối tượng quảng cáo (interstitial, native, onboarding full-screen). Theo dõi điều kiện mạng và trạng thái mua hàng. |
| `AdExtension.kt` | Các helper để populate quảng cáo Google Native vào các layout tùy chỉnh (kích thước CTA, xử lý shimmer). |

### Luồng Điển hình

1. `GlobalApp` gọi `RemoteConfigUtils.init()` và gán `ERainAd.getInstance().prepareLoadingAdsDialogLayout`.
2. `AdRemoteConfig.initialize()` chạy một lần khi khởi động. Mỗi màn hình tham chiếu các placement thông qua `AdRemoteConfig.<key>` hoặc các extension val.
3. Các UI layer không bao giờ chạm vào JSON thô. Chúng gọi `AdsManager.load...`/`show...`, hoặc kiểm tra `AdRemoteConfig.<key>.isEnable` trước khi render các container tùy chọn.

### Thêm Placement Mới

1. Mở rộng `ad_config_debug.json` / `ad_config.json` với key mới.
2. Thêm một property vào `AdRemoteConfigExtensions.kt` để các caller Kotlin có thể đọc key theo cách type-safe.
3. Quyết định nơi preload resource bên trong `AdsManager`.
4. Cập nhật mặc định Remote Config bằng cách chạy lại app một lần (giá trị được upload thông qua mặc định `RemoteConfigUtils`) hoặc chỉnh sửa thủ công trên console Remote Config.

## Dialog Bắt buộc Cập nhật (`force_update_config.json`)

- Vị trí: `app/src/main/assets/force_update_config.json`.
- Key Remote: `force_update_config` (string) trong Firebase Remote Config. Asset cục bộ được sử dụng làm giá trị mặc định khi payload remote trống.
- Data model: [`ForceUpdateConfig`](app/src/main/java/com/itg/template/data/model/ForceUpdateConfig.kt).
- Người dùng: `RemoteConfigUtils.getForceUpdateConfig()` parse JSON và `MainActivity` hiển thị dialog Auto/Manual Force Update (xem `button_show_force_update`).

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

| Trường | Mô tả |
| --- | --- |
| `icon` | URL tùy chọn hiển thị phía trên nội dung. Fallback về icon launcher. |
| `title` | Tùy chọn. Mặc định là tên app khi trống. |
| `description` | Tùy chọn. Mặc định là `force_update_message`. |
| `storeLink` | Deep link Google Play. Bắt buộc cho lời nhắc tự động. |
| `minVersionCode` | `BuildConfig.VERSION_CODE` tối thiểu yêu cầu trước khi nhắc. |
| `force` | Khi `true`, dialog không thể bị hủy. |

Sau khi `RemoteConfigUtils.init()` hoàn tất, `maybeShowForceUpdateDialog()` so sánh payload remote với `versionCode` đã cài đặt và render `dialog_force_update.xml` thông qua `ForceUpdateDialog`. Nút “Show Force Update” trong `MainActivity` sử dụng lại config đã fetch cuối cùng để QA có thể xem trước dialog ngay lập tức.

## Dialog Hệ thống dựa trên Blur

- `BlurLoadingLayout` (root cho `layout_prepare_ads.xml`, `dialog_no_internet.xml`, `dialog_force_update.xml`) capture window bên dưới và đưa nó vào [BlurView](https://github.com/Dimezis/BlurView) cho một lớp phủ blur giống iOS.
- `NoInternetDialog` và `ForceUpdateDialog` (nằm trong `ui/component/main/dialog`) là các helper tái sử dụng inflate các layout đó, xử lý callback nút, và expose `show()/dismiss()` cho tầng activity.
- Các dialog tự động snapshot activity hiện tại, vì vậy ngay cả các overlay bên thứ ba cũng kế thừa background được làm mờ mà không cần thêm code trong activity.

### Xem trước / Kích hoạt Thủ công

- `button_show_force_update` trong `activity_main.xml` kích hoạt `ForceUpdateDialog` sử dụng payload Remote Config đã cache (hoặc fallback cục bộ). Điều này hữu ích trong quá trình QA để review nội dung và thiết kế mà không cần thay đổi Remote Config.
- Mất mạng được theo dõi qua `ConnectionLiveData`; `NoInternetDialog` xuất hiện tự động bất cứ khi nào observer emit `false`, và bị hủy ngay khi kết nối được khôi phục.
