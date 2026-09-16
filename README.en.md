<p align="right">
  <a href="./README.md">中文</a> | <b>English</b>
</p>

# LocationTracker

Android location reporting app (V2.1.6, minSdk 14). Enter the HA base URL; the app auto-generates a Webhook ID and POSTs location to Home Assistant (recommended: `ha_loca_device`).

App source: `app/src/main/java/com/ljs/locationtracker/`  
HA integration: `ha_loca_device/` (HACS)

Open source and free; download only from the GitHub repo.

## Call chain

```
MainActivity saves config
  → Utils.buildWebhookUrl (base URL + device UUID → /api/webhook/<id>)
  → persist to SQLite config table
  → start ltmService / LocationForcegroundService

ltmService
  sparse GPS/Network sampling (default 600s)
  → timed reportRunnable (local interval 10~10800s, default 60)
  → build JSON POST → HA Webhook
  → on success, sync local interval from update_interval
  → pause reporting when battery ≤10%

BootBroadcastReceiver
  BOOT_COMPLETED / SCREEN_* / custom start
  → restart ltmService for keep-alive

HA ha_loca_device
  webhook receive → coordinator updates device_tracker / sensor
  → respond {"ok":true,"update_interval":N}
```

## Layout

| Path | Role |
| ---- | ---- |
| `app/.../MainActivity.java` | UI: monitor / config / logs |
| `app/.../ltmService.java` | sampling, timed report, interval sync, low-battery guard |
| `app/.../LocationForcegroundService.java` | foreground keep-alive |
| `app/.../Utils.java` | Webhook ID, URL build, quiet notification |
| `app/.../ConfigSyncService.java` | optional remote config sync |
| `app/.../BootBroadcastReceiver.java` | boot / screen / keep-alive restart |
| `app/.../DataBaseOpenHelper.java` | SQLite config persistence |
| `app/.../DeviceOptimizationHelper.java` | OEM background optimization tips |
| `app/.../HonorKeepAliveHelper.java` | Huawei/Honor keep-alive |
| `ha_loca_device/` | HA webhook integration (device_tracker + sensor) |
| `ui/` | phone / tablet UI preview HTML |
| `build_scripts.sh` / `.bat` | build scripts |

---

## Quick start

1. Install the [APK](./app/build/outputs/apk/release/) or build from source
2. Grant location / network / background permissions
3. In config, enter HA base URL (e.g. `https://ha.example.com`); note the auto-generated Webhook ID
4. Local initial interval: 10~10800s (default 60); after a successful report, HA response wins
5. Tap Start Location

UI preview: [Phone](https://haoxiang.eu.org/ui/MOBILE_UI_PREVIEW) | [Tablet](https://haoxiang.eu.org/ui/TABLET_UI_PREVIEW)

---

## Reporting rules

| Item | Value |
| ---- | ----- |
| GPS sampling | sparse, default 600s (`Utils.getGpsSampleIntervalSeconds`) |
| Report interval | local 10~10800s, default 60; then sync HA `update_interval` |
| Low battery | pause when ≤10% |
| Dedup | skip if location unchanged |
| Notification | quiet foreground keep-alive; no frequent content refresh |
| URL | public domain/IP only; no private LAN |

**JSON**

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

| Field | Type | Required |
| ----- | ---- | -------- |
| `latitude` / `longitude` | number | yes |
| `timestamp` | number | yes |
| `altitude` / `gps_accuracy` / `battery` / `speed` / `bearing` | number | no |
| `provider` | string | no |
| `screen_off` / `power_save_mode` / `immediate_report` | boolean | no |

Success response: `{"ok":true,"update_interval":60}`

---

## Permissions

| Permission | Purpose |
| ---------- | ------- |
| `ACCESS_FINE/COARSE_LOCATION` | location |
| `ACCESS_BACKGROUND_LOCATION` | background location (Android 10+) |
| `INTERNET` / `ACCESS_NETWORK_STATE` | network |
| `WAKE_LOCK` | wake lock |
| `RECEIVE_BOOT_COMPLETED` | auto-start on boot |
| `FOREGROUND_SERVICE` | foreground service (Android 8+) |
| `POST_NOTIFICATIONS` | notifications (Android 13+) |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | ignore battery optimizations |
| `SYSTEM_ALERT_WINDOW` | overlay (Huawei/Honor) |

Huawei / Xiaomi / OPPO / vivo / Samsung usually need manual auto-start, background keep-alive, and battery whitelist; see in-app Optimization Settings.

---

## Compatibility

| Feature | 4.0–4.3 | 4.4 | 5.0+ | 10+ |
| ------- | ------- | --- | ---- | --- |
| Basic location | ✅ | ✅ | ✅ | ✅ |
| Background location | ✅ | ✅ | ✅ | needs permission |
| Foreground service | ❌ | ❌ | ✅ | ✅ |
| Transparent status bar | ❌ | translucent | full | full |

Layouts: `layout` + `sw400dp` / `sw600dp` / `sw720dp` / `sw800dp` / `sw900dp`

---

## ha_loca_device (recommended)

Path: `ha_loca_device/`. App **pushes** to a public HA webhook; one device, one endpoint.

**Install**

1. HACS → Custom repositories → this repo (Integration) → install `ha_loca_device`
2. Or copy to `config/custom_components/ha_loca_device`
3. Restart HA → Add integration: device name, Webhook ID, interval (10~10800, default 60)

**Wire-up**

| Item | Notes |
| ---- | ----- |
| App URL | `https://your-ha` → auto `/api/webhook/<id>` |
| HA | same Webhook ID + interval |
| Entities | `device_tracker` + related sensors |

Debug:

```bash
curl -X POST "https://your-ha/api/webhook/<id>" \
  -H "Content-Type: application/json" \
  -d '{"latitude":31.23,"longitude":121.47,"battery":80,"gps_accuracy":10}'
```

Details: [`ha_loca_device/README.md`](./ha_loca_device/README.md).

### Alternative: native webhook automation

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

---

## Build

```bash
# recommended
./build_scripts.sh          # Linux/Mac
build_scripts.bat           # Windows

# or
./gradlew assembleRelease
./gradlew assembleDebug
```

Before manual build: `cp local.properties.example local.properties`, set SDK path and signing.

| Variant | Package | Notes |
| ------- | ------- | ----- |
| Release | `com.hx.locationtracker` | signed, obfuscated |
| Debug | `com.hx.locationtracker.debug` | side-by-side install OK |

Optional `local.properties`: `CONFIG_URL` / `HEARTBEAT_URL` / `WEBHOOK_URL` (defaults exist).

---

## Changelog

### ha_loca_device integration

- App takes HA base URL, auto Webhook ID → `/api/webhook/<id>`
- Interval driven by HA; sync from `update_interval` response
- Quiet keep-alive notification; no frequent refresh
- Sparse GPS sampling + timed report; no GPS rebind on screen off
- Added `ha_loca_device` in-repo

### v2.1.6 (2024-07-19)

- Daytime continuous report; pause when stationary at night
- Motion detection / WorkManager restart fixes
- Refresh status on foreground; less noisy permission logs
- Onboarding dialog order; quiet notification toggle

### v2.1.5

- Removed MQTT; UI / validation / debounce / crash logs

### v2.1.4

- Transparent status bar (full on 5.0+, translucent on 4.4)

### v2.1.3

- Theme crash fix; dedup; low-battery guard; boot / keep-alive

### v2.0.0 / v1.0.0

- Low battery / WakeLock / SQL injection fixes; initial HTTP webhook

## Known issues

- Some input errors only appear in the log panel, not always as Toast
- No full night / high-contrast multi-theme auto adaptation yet

## License

MIT; see LICENSE.
