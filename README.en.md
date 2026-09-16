<p align="right">
  <b>Chinese</b> | <a href="./README.en.md">English</a>
</p>

<br>

# LocationTracker

[![Android Version](https://img.shields.io/badge/Android-4.0+-green.svg)](https://developer.android.com/about/versions/android-4.0)
[![Version](https://img.shields.io/badge/Version-V2.1.6-blue.svg)](https://github.com/Hosiang1026/LocationTracker/releases)
[![Phone UI](https://img.shields.io/badge/Phone%20UI-Single%20Panel-brightgreen.svg)](https://haoxiang.eu.org/ui/MOBILE_UI_PREVIEW)
[![Tablet UI](https://img.shields.io/badge/Tablet%20UI-Optimized%20for%20Large%20Screen-blue.svg)](https://haoxiang.eu.org/ui/TABLET_UI_PREVIEW)

> 📱 **LocationTracker App** - Push Android location to Home Assistant via HTTP webhook (recommended: `ha_loca_device`)

## 🚨 Important Notice

> 🚨 **Anti-fraud Statement**: This app is fully open source and free. Do not trust any paid versions or services!
> 
> - 📱 **Official Channel**: Download only from the official GitHub repository
> - 💰 **Completely Free**: All features are free, no paid items
> - 🔒 **Open Source**: Code is fully open, free to view and modify
> - ⚠️ **Beware of Scams**: If you encounter any payment requests, report and block immediately
> - 📞 **Official Support**: For issues, use GitHub Issues

---

## 🚀 Quick Start

### 📥 Installation
1. Download the [APK file](./app/build/outputs/apk/release/)
2. Install on your Android device
3. Grant necessary permissions
4. Set HA base URL (e.g. `https://ha.example.com`; app auto-generates Webhook ID and appends `/api/webhook/<id>`)
5. Set local initial interval (10~10800s, default 60; after a successful push, HA `ha_loca_device` interval wins and syncs)
6. Tap "Start Location" to begin

### ⚡ Core Features
- 📍 **Real-time Location**: GPS/network location, accurate reporting
- 🔗 **HA pairing**: Base URL → auto Webhook ID, works with `ha_loca_device`
- ⏱️ **Interval sync**: Applies HA `update_interval` from successful responses
- 🔄 **Data Deduplication**: Avoid duplicate location reports
- 🔋 **Low Battery Protection**: Auto-pause reporting below 10% battery
- 🌙 **Background Keep-alive**: Auto-start on boot; quiet keep-alive notification
- 🎨 **Transparent Status Bar**: Android 4.4+ supported
- 🛡️ **Global Exception Capture**: Crashes are logged and user is notified
- 🖥️ **Multi-resolution Support**: Adapts to different screen sizes
- 🔧 **Device Optimization Guide**: Built-in brand-specific optimization tips
- 🔒 **SQL Injection Protection**: Parameterized queries for DB
- 🖱️ **Main operations have UI feedback**

## 🎨 UI Preview

> 💡 **Quick Preview**: [📱 Phone UI](https://haoxiang.eu.org/ui/MOBILE_UI_PREVIEW) | [📟 Tablet UI](https://haoxiang.eu.org/ui//TABLET_UI_PREVIEW)

### 📱 Phone Features
- **Single panel design**: Bottom TAB switch, clean UI
- **Status Monitor**: Real-time connection, location, battery, report count
- **Config Panel**: HA base URL, local initial interval, notification switch
- **Log Panel**: Real-time app status and report logs

### 📟 Tablet Features
- **Large screen optimized**: Layout and font size for tablets
- **Touch friendly**: Larger buttons and controls
- **Info density**: Efficient use of large screens
- **Portrait/Landscape**: Good UX in both orientations

## 🚀 User Guide

### First Use
1. **Install the app**: Download and install the APK file
2. **Grant permissions**: Allow location, network, auto-start, etc.
3. **Configure**: Enter HA base URL and local initial interval; note the auto Webhook ID and add `ha_loca_device` in HA
4. **Start service**: Tap "Start Location" to begin reporting
5. **Monitor status**: Switch to the monitor panel to view status and logs

### UI Operations
- **📊 Monitor Panel**: View connection, location, battery, report count
- **⚙️ Config Panel**: HA base URL, local initial interval, notification switch
- **📱 Bottom Navigation**: Switch between monitor and config panels
- **🛠️ Log Management**: Tap "🛠️ Log" to view crash logs
- **🔧 Optimization Settings**: Tap "Optimization Settings" for device-specific tips

### Daily Use
- **Auto-start**: The app will auto-start the location reporting service
- **Background running**: Service runs in the background, no manual intervention needed
- **Status monitoring**: View connection and report count in the status panel
- **Log viewing**: Check detailed logs for troubleshooting
- **Keep-alive notification**: Quiet foreground notification only; not refreshed every report

### Troubleshooting
1. **Service won't start**: Check if GPS is on and permissions are granted
2. **Data upload fails**: Check network / public HA URL; ensure Webhook ID matches `ha_loca_device`
3. **Service killed in background**: Check device optimization settings
4. **High battery usage**: Increase interval in the HA integration or enable low battery protection
5. **Theme compatibility issues**: The app has built-in theme adaptation; check crash logs if issues persist

## 📋 Permissions

### Required Permissions
- `ACCESS_FINE_LOCATION`: Precise location
- `ACCESS_COARSE_LOCATION`: Approximate location
- `ACCESS_BACKGROUND_LOCATION`: Background location (Android 10+)
- `INTERNET`: Network access
- `ACCESS_NETWORK_STATE`: Network state
- `WAKE_LOCK`: Wake lock
- `RECEIVE_BOOT_COMPLETED`: Auto-start on boot

### Optional Permissions
- `FOREGROUND_SERVICE`: Foreground service (Android 8.0+)
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Ignore battery optimizations
- `SYSTEM_ALERT_WINDOW`: Overlay (Huawei/Honor)
- `ACCESS_WIFI_STATE`: WiFi state (for network location)
- `CHANGE_WIFI_STATE`: Change WiFi state
- `ACCESS_LOCATION_EXTRA_COMMANDS`: Extra location commands
- `READ_EXTERNAL_STORAGE`: Read external storage (for crash logs)
- `WRITE_EXTERNAL_STORAGE`: Write external storage (for crash logs)
- `READ_PHONE_STATE`: Read phone state
- `WRITE_SETTINGS`: Write system settings
- `BLUETOOTH`: Bluetooth
- `BLUETOOTH_ADMIN`: Bluetooth admin

## 🔒 Security Analysis

### Fixed Security Issues
✅ **SQL Injection Protection**: All DB operations use parameterized queries
✅ **Network Security**: Uses OkHttp, supports HTTPS, with timeout and retry
✅ **Permission Management**: Complete runtime permission requests
✅ **Input Validation**: Strict user input validation
✅ **API Compatibility**: Fixed all API level compatibility issues

### Security Recommendations
- 🔐 Use HTTPS public HA URL
- 🔐 Update the app regularly
- 🔐 Use in trusted networks
- 🔐 Regularly check reported data accuracy

## ⚙️ Configuration

### Basic Configuration
1. **HA base URL**: http/https root; app auto-generates Webhook ID and builds `/api/webhook/<id>`
2. **Local initial interval**: 10~10800s; empty/out-of-range falls back to 60; after success, HA `update_interval` applies
3. **Notification switch**: Quiet keep-alive foreground notification (not refreshed every report)

### Data Format

#### Standard Location Report
```json
{
  "latitude": 37.7749,
  "longitude": -122.4194,
  "altitude": 100.5,
  "gps_accuracy": 5.0,
  "battery": 75,
  "speed": 30.0,
  "bearing": 180.0,
  "timestamp": 1640995200000,
  "provider": "gps",
  "screen_off": false,
  "power_save_mode": false
}
```

#### Immediate Report (with extra flag)
```json
{
  "latitude": 37.7749,
  "longitude": -122.4194,
  "altitude": 100.5,
  "gps_accuracy": 5.0,
  "battery": 75,
  "speed": 30.0,
  "bearing": 180.0,
  "timestamp": 1640995200000,
  "provider": "gps",
  "screen_off": false,
  "power_save_mode": false,
  "immediate_report": true
}
```

HA success response example: `{"ok":true,"update_interval":60}`

### Field Descriptions

| Field | Type | Description | Required |
|-------|------|-------------|----------|
| `latitude` | number | Latitude | ✅ |
| `longitude` | number | Longitude | ✅ |
| `altitude` | number | Altitude (meters) | ⚠️ |
| `gps_accuracy` | number | GPS accuracy (meters) | ⚠️ |
| `battery` | number | Battery percentage | ⚠️ |
| `speed` | number | Speed (m/s) | ⚠️ |
| `bearing` | number | Bearing (0-360°) | ⚠️ |
| `timestamp` | number | Timestamp (ms) | ✅ |
| `provider` | string | Location provider (gps/network/passive) | ⚠️ |
| `screen_off` | boolean | Screen off | ⚠️ |
| `power_save_mode` | boolean | Power save mode | ⚠️ |
| `immediate_report` | boolean | Immediate report | ⚠️ |

**Note**: Only `latitude` and `longitude` are always required; other fields may be missing depending on device and system state.

## 📊 Compatibility

### 🖥️ Screen Sizes
| Device Type | Screen Size | Resolution | Layout | Touch Optimized | Status Bar | Example Devices |
|-------------|------------|------------|--------|-----------------|------------|----------------|
| Small Phone | 4.0-4.7"   | 480x800-720x1280 | ✅ Compact | ✅ Standard | ✅ Transparent | Xiaomi 4, Huawei P8, Samsung S5 |
| Standard Phone | 5.0-6.0" | 720x1280-1080x1920 | ✅ Single panel | ✅ Standard | ✅ Transparent | Huawei P40, Xiaomi 12, OPPO Find X3 |
| Large Phone | 6.1-6.7"   | 1080x1920-1440x3200 | ✅ Single panel | ✅ Standard | ✅ Transparent | Huawei Mate 50 Pro, Xiaomi 13 Ultra |
| Small Tablet | 7.0-8.0"  | 1024x768-1200x1920 | ✅ Optimized | ✅ Large buttons | ✅ Transparent | Huawei M6, Xiaomi Pad 4 |
| Standard Tablet | 8.5-10.5" | 1200x1920-1600x2560 | ✅ Optimized | ✅ Large buttons | ✅ Transparent | Huawei MatePad Pro, Xiaomi Pad 5 |
| Large Tablet | 11.0-12.9" | 1600x2560-2048x2732 | ✅ Large screen | ✅ Extra large buttons | ✅ Transparent | Huawei MatePad Pro 12.6, Xiaomi Pad 6 Pro |
| Foldable | 6.7-8.0" unfolded | 1080x1920-2208x1768 | ✅ Adaptive | ✅ Smart size | ✅ Transparent | Samsung Fold, Huawei Mate X |
| Car/Big Screen | 10.0-15.0" | 1920x1080-2560x1440 | ✅ Large screen | ✅ Extra large buttons | ✅ Transparent | Android Car, Smart TV |

### 🤖 Android Versions
| Feature | 4.0-4.3 | 4.4-4.4W | 5.0+ | 10+ |
|---------|---------|----------|------|-----|
| Basic Location | ✅ | ✅ | ✅ | ✅ |
| Background Location | ✅ | ✅ | ✅ | ⚠️ Permission needed |
| Foreground Service | ❌ | ❌ | ✅ | ✅ |
| Transparent Status Bar | ❌ | ⚠️ Semi | ✅ | ✅ |
| Theme Compatibility | ✅ | ✅ | ✅ | ✅ |

### 📱 Device Brands
| Brand | Auto-start | Background Keep-alive | Battery Optimization | Optimization Guide |
|-------|------------|----------------------|---------------------|-------------------|
| Huawei/Honor | ⚠️ Needs setting | ⚠️ Needs optimization | ⚠️ Needs ignore | ✅ Built-in |
| Xiaomi/Redmi | ⚠️ Needs setting | ⚠️ Needs optimization | ⚠️ Needs ignore | ✅ Built-in |
| OPPO/OnePlus | ⚠️ Needs setting | ⚠️ Needs optimization | ⚠️ Needs ignore | ✅ Built-in |
| vivo | ⚠️ Needs setting | ⚠️ Needs optimization | ⚠️ Needs ignore | ✅ Built-in |
| Samsung | ⚠️ Needs setting | ⚠️ Needs optimization | ⚠️ Needs ignore | ✅ Built-in |

## 🏠 Home Assistant Integration

> Recommended: use the bundled `ha_loca_device` integration (HTTP webhook, one entry per device).

### Solution 1: ha_loca_device (Recommended)

#### Install
1. Copy the repo folder `ha_loca_device` to Home Assistant `custom_components/ha_loca_device`
2. Or add this repo as a HACS custom repository (Integration) and install `ha_loca_device`
3. Restart Home Assistant

#### Configure
1. Enter the HA base URL in the app and start; note the auto-generated Webhook ID
2. Add `ha_loca_device` with device name, Webhook ID, and report interval
3. The app pushes to the public webhook; interval follows HA config (synced after each successful push)

#### App pairing
1. Report URL: `https://your-ha` (app appends `/api/webhook/<id>`)
2. Works over the internet / mobile data as long as the phone can reach HA
3. Notification is keep-alive only

**Example**:
- App: `https://ha.example.com` → `https://ha.example.com/api/webhook/<auto-id>`
- HA interval: 60 seconds (changeable in integration options)

**JSON fields**: `latitude` / `longitude` required; optional `gps_accuracy`, `battery`, `altitude`, `speed`, `bearing`, `timestamp`, `provider`, `screen_off`, `power_save_mode`, `immediate_report`

> The webhook host must be a public domain or public IP, not a LAN address.

See [`ha_loca_device/README.md`](./ha_loca_device/README.md).

### Solution 2: Native webhook automation (fallback)

```yaml
automation:
  - alias: "Location Update"
    trigger:
      platform: webhook
      webhook_id: your_webhook_id
    action:
      - service: device_tracker.see
        data:
          dev_id: myphone
          latitude: "{{ trigger.json.latitude }}"
          longitude: "{{ trigger.json.longitude }}"
          gps_accuracy: "{{ trigger.json.gps_accuracy }}"
          battery: "{{ trigger.json.battery }}"
```

### Solution 3: ha_icloud_cn (Apple devices, optional)

China mainland iCloud (`icloud.com.cn`) Find My for Apple devices; unrelated to this Android app. Copy `ha_icloud_cn` into `custom_components` and restart HA. See [`ha_icloud_cn/README.md`](./ha_icloud_cn/README.md).

### Development Environment
- Android Studio 4.0+
- Android SDK API 14+
- Gradle 6.0+

### Code Standards
- Follows Android development best practices
- Well-commented
- Thoroughly tested

## 📦 Build & Install

### 🔧 Build Methods

#### Using Build Script (Recommended)
```bash
# Linux/Mac
chmod +x build_scripts.sh
./build_scripts.sh

# Windows
build_scripts.bat
```

#### Manual Build
```bash
# Build Release
./gradlew assembleRelease

# Build Debug
./gradlew assembleDebug
```

**Important**: Before building, configure `local.properties`:

1. **Copy template**:
```bash
cp local.properties.example local.properties
```

2. **Edit local.properties**:
```properties
# SDK path
sdk.dir=your local Android SDK path

# Signing config (required for Release)
KEYSTORE_PASSWORD=your keystore password
KEY_ALIAS=your key alias
KEY_PASSWORD=your key password

# Server config (optional, has defaults)
CONFIG_URL=your config server
HEARTBEAT_URL=your heartbeat URL
WEBHOOK_URL=your webhook URL
```

3. **Config notes**:
- **Signing config**: Required for Release build
- **Server config**: Optional, defaults used if not set
- **Defaults**:
  - `CONFIG_URL`: `https://www.zhangsan.com/locationtracker/api/config`
  - `HEARTBEAT_URL`: `https://www.zhangsan.com/locationtracker/api/config/heartbeat`
  - `WEBHOOK_URL`: `https://www.zhangsan.com/api/webhook/db72ebc1627e52685ca64cdb380`

### Version Info

#### Release
- **Package**: `com.hx.locationtracker`
- **Signed**: Yes, ready for release
- **Optimized**: ProGuard, resource compression, performance
- **Use**: Official release

#### Debug
- **Package**: `com.hx.locationtracker.debug`
- **Signed**: No, for testing
- **Optimized**: Debug info kept
- **Use**: Development/testing

#### Dual Install
- Both versions can be installed together (different package names)
- Release for production, Debug for testing

### 📥 Install

#### Method 1: Direct Install
1. Download APK from release
2. Install on Android device
3. Grant necessary permissions
4. Configure HA base URL and local initial interval

#### Method 2: Build from Source
1. Clone the repo
2. Open in Android Studio
3. Build APK
4. Install on device

## 🚀 Future Plans

### 📱 Device-side Enhancements

#### 🔄 Server-side Config Push (infra ready)
- **Remote config update**: Use ConfigSyncService to push config from server
- **Real-time sync**: Device auto-applies server config
- **Config versioning**: Support version control and rollback
- **Incremental update**: Only push changed items

#### 💓 Heartbeat Monitoring (infra ready)
- **Device online status**: Use HEARTBEAT_URL for real-time online/offline
- **Network quality**: Auto-detect network quality
- **Offline stats**: Track offline duration/frequency
- **Alerting**: Auto-alert on abnormal offline

#### 📊 Status Feedback
- **Device status reporting**: Regularly report health
- **Config feedback**: Report config apply status
- **Error log upload**: Auto-upload error logs
- **Performance metrics**: Collect for optimization

### 🌐 Server-side Plans

#### 🔧 Device Management Platform
- **Device registration/auth**
- **Grouping/batch ops**
- **Config templates**
- **Device monitoring**

#### 📈 Data Analytics
- **Device activity stats**
- **Config apply stats**
- **Performance analysis**
- **Usage trends**

#### 🔔 Alert System
- **Device offline alerts**
- **Config push failure alerts**
- **Abnormal behavior detection**
- **System health monitoring**

### 📋 Architecture

#### 🔐 Security
- **Device auth**: Token-based
- **Data encryption**: TLS for all comms
- **Permission control**: Fine-grained
- **Audit logs**: Full operation logs

#### ⚡ Performance
- **Connection pooling**
- **Message queue**
- **Caching**
- **Load balancing**

#### 🔄 Protocols
- **Heartbeat**: Lightweight
- **Config push**: Standardized
- **Status feedback**: Standardized
- **Error handling**: Unified

### 📅 Timeline

#### Phase 1: Core (1-2 months)
- [ ] Improve ConfigSyncService startup/calls
- [ ] Implement server device monitoring
- [ ] Improve config push
- [ ] Device auth

#### Phase 2: Management Platform (2-3 months)
- [ ] Web UI
- [ ] Device grouping
- [ ] Config templates
- [ ] Real-time monitoring

#### Phase 3: Advanced (3-4 months)
- [ ] Data analytics
- [ ] Alert system
- [ ] Performance
- [ ] Security

### 🎯 Feature Comparison

| Module | Current | Future |
|--------|---------|--------|
| **Config** | Local | Remote push |
| **Monitoring** | Basic | Real-time heartbeat |
| **Analytics** | Basic logs | Detailed stats |
| **Alerts** | Local | Remote system |
| **Permissions** | Basic | Fine-grained |
| **Security** | Basic | Enterprise |

### 💡 Principles

#### ✅ Focus
- **Lightweight**
- **Open source**
- **User-driven**
- **Incremental**

#### 🔧 Tech
- **Current infra**: ConfigSyncService, heartbeat
- **Compatibility**
- **Extensibility**
- **Stability**

## 🤝 Contributing

PRs and issues welcome!

## 📄 License

MIT License, see LICENSE.

## 📞 Contact

- GitHub Issues
- Email the maintainer

---

**Note**: Please comply with local laws when using this app.

## 📝 Changelog

### Current (ha_loca_device pairing)
- App takes HA base URL, auto-generates Webhook ID, builds `/api/webhook/<id>`
- Report interval follows HA `ha_loca_device`; syncs from response `update_interval`
- Quiet keep-alive notification; no per-report content refresh
- Sparse GPS sampling + timer reports; screen off no longer rebinds GPS
- New integrations in repo: `ha_loca_device` (Android push), `ha_icloud_cn` (CN iCloud)

### v2.1.6 (2024-07-19)
- Location reporting strategy optimized: during daytime, reporting continues regardless of stationary/moving/screen-off; at night, reporting pauses when stationary
- Improved stationary/moving detection logs: distance, speed, and staticCount are now logged for debugging
- Fixed frequent WorkManager task restarts; stationary/moving state and logs are now consistent
- UI auto-refresh: when switching between foreground/background, the status page now refreshes automatically (onResume actively requests service status)
- Permission logs are only written on first entry or when permission state changes, avoiding log spam
- Permission requests are now triggered only after all guide dialogs are closed on first entry, for better UX
- Config dialog: when clicking "Go to fill", the dialog closes immediately before switching to the config tab
- Guide dialog sequence and interaction optimized (device optimization, permission, config)
- Notification content improved: shows "Locating..." when location is not available, "Getting..." when battery is not available
- Silent notification supported: when notification is disabled in config, Android 8.0+ uses silent notification; enabling notification switches to normal immediately
- Notification content intelligently displays "Getting...", "Locating...", or actual values based on battery/location state
- Other minor UX improvements

### v2.1.5
- 🧹 **Code Cleanup**: Removed all MQTT-related code and configurations
- 🔧 **Configuration Optimization**: Simplified build configuration, removed unused dependencies
- 📝 **Documentation Update**: Updated version number and feature descriptions
- 🛠️ **ProGuard Optimization**: Updated package name reference rules
- Comprehensive UI detail optimization for better UX:
  - Added detailed hints for Webhook/interval input fields
  - Debounced button to prevent repeated "Start" clicks
  - Clear UI feedback for service start/stop, report success/failure
  - Multi-resolution adaptation (sw400dp, sw600dp, sw720dp, sw800dp, sw900dp)
- Global exception capture, crash logs saved locally, user-friendly error prompts
- Stricter input validation for Webhook URL, interval, etc.
- Other minor UX improvements and bug fixes

### v2.1.4
- ✨ **Transparent Status Bar**: Immersive status bar effect, status bar above the title becomes transparent
- ✨ **Smart Compatibility**: Automatically uses the appropriate transparency scheme for different Android versions
  - Android 5.0+ (API 21+): Fully transparent status bar
  - Android 4.4-4.4W (API 19-20): Semi-transparent status bar
  - Android 4.0-4.3 (API 14-18): Ignored, keep original
- 🛠️ **Technical Implementation**: Layout adaptation and code implementation, ensuring backward compatibility
- 🛠️ **Exception Handling**: If setting transparent status bar fails, app runs normally

### v2.1.3
- 🚨 **Important Fixes & Optimizations**: Thoroughly fixed theme compatibility crashes, ensuring stable operation on all devices
- 🛠️ **Smart Theme Mechanism**: Automatically selects the optimal AppCompat theme based on system state (power saving, night mode, high contrast, etc.)
- 🛠️ **Multi-layer Protection**: Sets compatible theme in Application, Activity's attachBaseContext and onCreate, ensuring 100% crash-free
- 🛠️ **Theme Validation**: Verifies theme after setting, fallback if failed
- ✨ **Crash Log Management Optimization**: Fixed dialog info not updating after clearing crash logs, added immediate clear function
- ✨ **Screen State Adaptive Interval**: Shortens location interval when screen is off, increasing wake-up probability
- ✨ **Data Deduplication**: Only reports when location data changes, reducing invalid reports
- ✨ **Low Battery Smart Protection**: Pauses reporting below 10% battery, auto-resumes when battery recovers
- ✨ **Enhanced WakeLock & Keep-alive**: Stronger WakeLock strategy for background operation
- ✨ **Service Auto-restart**: Auto-restarts via broadcast if killed by system
- ✨ **Boot Auto-start**: Auto-starts location reporting after system boot
- ✨ **Keep-alive Timer & Status Check**: Random keep-alive check between 60s and configured interval
- ✨ **Battery State Monitoring & Recovery**: Real-time battery and power save mode monitoring
- 🔧 **API Compatibility Fixes**: Ensures stable operation on all Android versions
- 🔧 **Network & Error Handling**: Improved exception capture and error recovery
- 🔧 **Status Broadcast & Log System**: Improved status update and log broadcast
- 🔒 **Enhanced Permission & Input Validation**: Stricter permission and input checks
- 📱 **UX & UI Improvements**: Optimized UI and user experience

### v2.1.2
- 🚨 **Important Fix**: Fixed issue where reporting interval auto-adjusts when screen state changes
- ✨ **Data Deduplication**: Only reports when location data changes
- 🔧 **API Compatibility**: Fixed API level compatibility issues
- 🔧 **Network Optimization**: Improved network requests and error handling
- 🔧 **Log System**: Optimized status broadcast and log system
- 🔒 **Security Enhancement**: Enhanced permission checks and input validation
- 📱 **UI Optimization**: Improved user experience and UI display

### v2.1.1
- ✨ Added low battery smart protection
- ✨ Enhanced WakeLock and keep-alive strategy
- ✨ Improved service auto-restart
- ✨ Improved boot auto-start
- ✨ Added keep-alive timer and status check
- ✨ Optimized battery state monitoring and recovery
- 🔧 Fixed API compatibility issues
- 🔧 Improved network requests and error handling
- 🔧 Optimized status broadcast and log system
- 🔒 Enhanced permission checks and input validation
- 📱 Improved user experience and UI display
- ⚠️ **Note**: This version has an issue where reporting interval auto-adjusts when screen state changes, fixed in v2.1.2

### v2.0.0
- ✨ Added low battery protection
- ✨ Enhanced WakeLock mechanism
- ✨ Optimized screen state monitoring
- ✨ Improved network retry mechanism
- ✨ Improved log system
- 🔒 Fixed SQL injection vulnerability
- 🔒 Enhanced input validation
- 🔒 Improved error handling

### v1.0.0
- 🎉 Initial release
- 📍 Basic location reporting
- 🌐 HTTP Webhook support
- 📱 Android 4.0+ compatibility