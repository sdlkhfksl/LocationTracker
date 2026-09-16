package com.ljs.locationtracker;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

public class LocationForcegroundService extends Service {

    private boolean notifyShown = false;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showNotify();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @SuppressLint("NewApi")
    public void showNotify() {
        if (ltmService.getNotificationEnable() != 1) {
            return;
        }
        Notification notification = Utils.buildQuietNotification(getApplicationContext());
        if (notification != null) {
            startForeground(Utils.NOTIFY_ID, notification);
            notifyShown = true;
        }
    }

    public void updateNotification(int batteryLevel, double latitude, double longitude, int reportCount, long timeSinceLastReport) {
        // 省电：不频繁刷新通知内容，仅保证前台保活通知存在
        if (!notifyShown) {
            showNotify();
        }
    }

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        LocationForcegroundService getService() {
            return LocationForcegroundService.this;
        }
    }
}
