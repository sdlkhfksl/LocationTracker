<p align="right">
  <b>中文</b> | <a href="./README.en.md">英文</a>
</p>

# LocationTracker

Android 位置上报 App（V2.1.6，minSdk 14）。填 HA 根地址后自动生成 Webhook ID，经 HTTP POST 推送到 Home Assistant（推荐 `ha_loca_device`）。

业务源码：`app/src/main/java/com/ljs/locationtracker/`  
HA 插件：`ha_loca_device/`（HACS 集成）

开源免费；仅从 GitHub 仓库下载。

## 调用链

```
MainActivity 保存配置
  → Utils.buildWebhookUrl（根地址 + 本机 UUID → /api/webhook/<id>）
  → SQLite config 表落盘
  → 启动 ltmService / LocationForcegroundService

ltmService
  GPS/Network 稀疏采样（默认 600s）
  → 定时 reportRunnable（本地初始间隔 10~10800s，默认 60）
  → 组装 JSON POST → HA Webhook
  → 成功响应 update_interval → 同步本地间隔
  → 电量 ≤10% 暂停上报

BootBroadcastReceiver
  BOOT_COMPLETED / SCREEN_* / 自定义 start
  → 拉起 ltmService 保活

HA ha_loca_device
  webhook 收包 → coordinator 更新 device_tracker / sensor
  → 响应 {"ok":true,"update_interval":N}
```

## 目录

| 路径 | 作用 |
| ---- | ---- |
| `app/.../MainActivity.java` | UI：监控 / 配置 / 日志 |
| `app/.../ltmService.java` | 定位采样、定时上报、间隔同步、低电量保护 |
| `app/.../LocationForcegroundService.java` | 前台保活服务 |
| `app/.../Utils.java` | Webhook ID、URL 拼接、静默通知 |
| `app/.../ConfigSyncService.java` | 可选远程配置同步 |
| `app/.../BootBroadcastReceiver.java` | 开机 / 屏状态 / 保活拉起 |
| `app/.../DataBaseOpenHelper.java` | SQLite 配置持久化 |
| `app/.../DeviceOptimizationHelper.java` | 各品牌后台优化指导 |
| `app/.../HonorKeepAliveHelper.java` | 华为/荣耀保活 |
| `ha_loca_device/` | HA Webhook 集成（device_tracker + sensor） |
| `ui/` | 手机 / 平板界面预览 HTML |
| `build_scripts.sh` / `.bat` | 打包脚本 |

---

## 快速使用

1. 安装 [APK](./app/build/outputs/apk/release/) 或自行编译
2. 授予定位 / 网络 / 后台相关权限
3. 配置面板填 HA 根地址（如 `https://ha.example.com`），记下自动生成的 Webhook ID
4. 本地初始间隔：10~10800 秒（默认 60）；成功上报后以 HA 返回为准
5. 点击「开始定位」

界面预览：[手机](https://haoxiang.eu.org/ui/MOBILE_UI_PREVIEW) | [平板](https://haoxiang.eu.org/ui/TABLET_UI_PREVIEW)

---

## 上报规则

| 项 | 值 |
| -- | -- |
| GPS 采样 | 稀疏，默认 600s（`Utils.getGpsSampleIntervalSeconds`） |
| 上报间隔 | 本地初始 10~10800s，默认 60；成功后同步 HA `update_interval` |
| 低电量 | ≤10% 暂停上报 |
| 去重 | 位置未变化不重复上报 |
| 通知 | 前台保活静默通知，不频繁刷新内容 |
| 地址 | 须公网域名/IP，不能用内网 |

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

| 字段 | 类型 | 必需 |
| ---- | ---- | ---- |
| `latitude` / `longitude` | number | 是 |
| `timestamp` | number | 是 |
| `altitude` / `gps_accuracy` / `battery` / `speed` / `bearing` | number | 否 |
| `provider` | string | 否 |
| `screen_off` / `power_save_mode` / `immediate_report` | boolean | 否 |

成功响应：`{"ok":true,"update_interval":60}`

---

## 权限

| 权限 | 说明 |
| ---- | ---- |
| `ACCESS_FINE/COARSE_LOCATION` | 定位 |
| `ACCESS_BACKGROUND_LOCATION` | 后台定位（Android 10+） |
| `INTERNET` / `ACCESS_NETWORK_STATE` | 网络 |
| `WAKE_LOCK` | 唤醒 |
| `RECEIVE_BOOT_COMPLETED` | 开机自启 |
| `FOREGROUND_SERVICE` | 前台服务（Android 8+） |
| `POST_NOTIFICATIONS` | 通知（Android 13+） |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | 忽略电池优化 |
| `SYSTEM_ALERT_WINDOW` | 悬浮窗（华为/荣耀） |

华为 / 小米 / OPPO / vivo / 三星通常需手动开自启动、后台保活、忽略电池优化；App 内「优化设置」有指导。

---

## 兼容性

| 能力 | 4.0–4.3 | 4.4 | 5.0+ | 10+ |
| ---- | ------- | --- | ---- | --- |
| 基础定位 | ✅ | ✅ | ✅ | ✅ |
| 后台定位 | ✅ | ✅ | ✅ | 需权限 |
| 前台服务 | ❌ | ❌ | ✅ | ✅ |
| 透明状态栏 | ❌ | 半透明 | 全透明 | 全透明 |

布局：`layout` + `sw400dp` / `sw600dp` / `sw720dp` / `sw800dp` / `sw900dp`

---

## ha_loca_device（推荐）

路径：`ha_loca_device/`。App **主动推送**到 HA 公网 Webhook；一设备一入口。

**安装**

1. HACS → 自定义仓库 → 本仓库（Integration）→ 安装 `ha_loca_device`
2. 或复制到 `config/custom_components/ha_loca_device`
3. 重启 HA → 添加集成：设备名、Webhook ID、上报间隔（10~10800，默认 60）

**对接**

| 项 | 说明 |
| -- | ---- |
| App 地址 | `https://your-ha` → 自动拼 `/api/webhook/<id>` |
| HA | 填同一 Webhook ID + 间隔 |
| 实体 | `device_tracker` + 相关 sensor |

调试：

```bash
curl -X POST "https://你的HA/api/webhook/<id>" \
  -H "Content-Type: application/json" \
  -d '{"latitude":31.23,"longitude":121.47,"battery":80,"gps_accuracy":10}'
```

明细见 [`ha_loca_device/README.md`](./ha_loca_device/README.md)。

### 备选：原生 Webhook 自动化

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

## 打包

```bash
# 推荐
./build_scripts.sh          # Linux/Mac
build_scripts.bat           # Windows

# 或
./gradlew assembleRelease
./gradlew assembleDebug
```

手动打包前：`cp local.properties.example local.properties`，填 SDK 路径与签名。

| 版本 | 包名 | 说明 |
| ---- | ---- | ---- |
| Release | `com.hx.locationtracker` | 签名、混淆，正式用 |
| Debug | `com.hx.locationtracker.debug` | 可同机安装调试 |

可选 `local.properties`：`CONFIG_URL` / `HEARTBEAT_URL` / `WEBHOOK_URL`（有默认值）。

---

## 更新日志

### 对接 ha_loca_device

- App 填 HA 根地址，自动生成 Webhook ID 并拼 `/api/webhook/<id>`
- 上报间隔以 HA 为准，响应 `update_interval` 自动同步
- 通知改为保活静默，不频繁刷新
- 定位改为稀疏采样 + 定时上报，息屏不再重绑 GPS
- 新增仓库集成 `ha_loca_device`

### v2.1.6（2024-07-19）

- 白天持续上报；夜间静止暂停
- 静止/运动判定与 WorkManager 重启问题修复
- 前后台切换时刷新状态；权限日志去刷屏
- 引导弹框顺序优化；静默通知开关

### v2.1.5

- 移除 MQTT；UI / 输入校验 / 防抖 / 崩溃日志

### v2.1.4

- 透明状态栏（5.0+ 全透明，4.4 半透明）

### v2.1.3

- 主题兼容崩溃修复；数据去重；低电量保护；开机自启与保活

### v2.0.0 / v1.0.0

- 低电量 / WakeLock / SQL 注入修复；初始 HTTP Webhook 上报

## 已知问题

- 部分输入错误仅写日志区，未全部 Toast
- 暂无完整夜间/高对比度多主题自动适配

## 许可证

MIT，见 LICENSE。
