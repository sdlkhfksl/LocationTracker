package com.ljs.locationtracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static final int NOTIFY_ID = 2001;
    private static final String NOTIFICATION_CHANNEL_NAME = "AMapBackgroundLocation";
    private static final String PREFS_NAME = "loca_device";
    private static final String KEY_WEBHOOK_ID = "webhook_id";
    private static final Pattern WEBHOOK_TAIL = Pattern.compile("(?i)/api/webhook/([^/?#]+)/?$");
    private static NotificationManager notificationManager = null;
    private static boolean isCreatedChannel = false;

    public static String getOrCreateWebhookId(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String id = prefs.getString(KEY_WEBHOOK_ID, null);
        if (id == null || id.trim().isEmpty()) {
            id = UUID.randomUUID().toString().replace("-", "");
            prefs.edit().putString(KEY_WEBHOOK_ID, id).apply();
        }
        return id;
    }

    public static String extractWebhookId(String url) {
        if (url == null) {
            return null;
        }
        Matcher matcher = WEBHOOK_TAIL.matcher(url.trim());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static void syncWebhookIdFromUrl(Context context, String url) {
        String id = extractWebhookId(url);
        if (id == null || id.isEmpty()) {
            return;
        }
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_WEBHOOK_ID, id).apply();
    }

    /**
     * 用户填写 HA 根地址或任意前缀，自动拼成 /api/webhook/{本机生成的id}
     */
    public static String buildWebhookUrl(Context context, String input) {
        if (input == null) {
            return null;
        }
        String raw = input.trim();
        if (raw.isEmpty()) {
            return raw;
        }
        String id = getOrCreateWebhookId(context);
        String base = raw.replaceAll("/+$", "");
        Matcher matcher = WEBHOOK_TAIL.matcher(base);
        if (matcher.find()) {
            base = base.substring(0, matcher.start());
        } else if (base.toLowerCase().endsWith("/api/webhook")) {
            base = base.substring(0, base.length() - "/api/webhook".length());
        }
        base = base.replaceAll("/+$", "");
        return base + "/api/webhook/" + id;
    }

    public static String getLocalIpAddress() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces =
                    java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface nif = interfaces.nextElement();
                if (!nif.isUp() || nif.isLoopback()) {
                    continue;
                }
                java.util.Enumeration<java.net.InetAddress> addrs = nif.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    java.net.InetAddress addr = addrs.nextElement();
                    if (addr.isLoopbackAddress() || !(addr instanceof java.net.Inet4Address)) {
                        continue;
                    }
                    String ip = addr.getHostAddress();
                    if (ip != null && !ip.startsWith("169.254.")) {
                        return ip;
                    }
                }
            }
        } catch (Exception e) {
            Log.e("Utils", "getLocalIpAddress failed", e);
        }
        return "127.0.0.1";
    }

    public static String getDeviceServiceBaseUrl(Context context) {
        return "http://" + getLocalIpAddress() + ":" + LocationHttpServer.DEFAULT_PORT;
    }

    public static int getGpsSampleIntervalSeconds() {
        // 稀疏采样，省电；精确点在 HA 请求时再取 lastKnown / 单次刷新
        return 600;
    }

    public static Notification buildQuietNotification(Context context) {
        return buildNotification(context, 0, 0, 0, 0, 0);
    }

    /**
     * 创建一个通知栏，API>=26时才有效
     * @param context
     * @param batteryLevel 电池电量
     * @param latitude 纬度
     * @param longitude 经度
     * @param reportCount 上报次数
     * @param timeSinceLastReport 距离上次上报时间（秒）
     * @return
     */
    public static Notification buildNotification(Context context, int batteryLevel, double latitude, double longitude, int reportCount, long timeSinceLastReport) {
        try {
            if (context == null) {
                Log.e("Utils", "Context为null，无法创建通知");
                return null;
            }
            Context mContext = context.getApplicationContext();
            Notification.Builder builder = null;
            Notification notification = null;
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                if (null == notificationManager) {
                    notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (notificationManager == null) {
                        Log.e("Utils", "无法获取NotificationManager");
                        return null;
                    }
                }
                String channelId = mContext.getPackageName() + ".keepalive";
                if (!isCreatedChannel) {
                    try {
                        NotificationChannel notificationChannel = new NotificationChannel(channelId,
                                "定位保活",
                                NotificationManager.IMPORTANCE_LOW);
                        notificationChannel.enableLights(false);
                        notificationChannel.enableVibration(false);
                        notificationChannel.setSound(null, null);
                        notificationChannel.setShowBadge(false);
                        if (Build.VERSION.SDK_INT >= 29) {
                            notificationChannel.setAllowBubbles(false);
                        }
                        notificationManager.createNotificationChannel(notificationChannel);
                        isCreatedChannel = true;
                    } catch (Exception e) {
                        Log.e("Utils", "创建通知渠道失败", e);
                        return null;
                    }
                }
                builder = new Notification.Builder(mContext, channelId);
            } else {
                builder = new Notification.Builder(mContext);
            }
            builder.setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(getAppName(mContext))
                    .setContentText("定位服务运行中")
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setWhen(System.currentTimeMillis());
            if (Build.VERSION.SDK_INT >= 16) {
                builder.setPriority(Notification.PRIORITY_MIN);
                notification = builder.build();
            } else {
                notification = builder.getNotification();
            }
            return notification;
        } catch (Exception e) {
            Log.e("Utils", "创建通知失败", e);
            LocationTrackerApplication.logError("创建通知失败", e);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取app的名称
     * @param context
     * @return
     */
    public static String getAppName(Context context) {
        String appName = "";
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            int labelRes = packageInfo.applicationInfo.labelRes;
            appName =  context.getResources().getString(labelRes);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return appName;
    }
}
