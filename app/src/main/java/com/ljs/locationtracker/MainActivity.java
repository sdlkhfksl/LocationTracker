package com.ljs.locationtracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

import android.app.ActivityManager;

import com.ljs.locationtracker.DeviceOptimizationHelper;
import com.ljs.locationtracker.DeviceOptimizationHelper.DeviceBrand;

import android.content.res.Configuration;
import android.os.PowerManager;

import android.app.AlertDialog;
import android.content.DialogInterface;

public class MainActivity extends AppCompatActivity {
    private EditText txtWebhookUrl, txtTime;
    private Button btnStart, btnStatusTab, btnConfigTab, btnCopyLog, btnDeviceOptimization;
    private TextView lblHaconfig, tvConnectionStatus, tvLocationStatus, tvBatteryLevel, tvReportCount;
    private ScrollView statusPanel;
    private ScrollView configPanel;
    private Switch sw_notification;
    private RecyclerView rvLogs;
    private LogAdapter logAdapter;
    private BroadcastReceiver statusReceiver;
    private boolean serviceStartedSuccessfully = false;
    String TAG="LJSTAG";
    
    // 广播动作常量
    public static final String ACTION_STATUS_UPDATE = "com.ljs.locationtracker.STATUS_UPDATE";
    public static final String ACTION_LOG_UPDATE = "com.ljs.locationtracker.LOG_UPDATE";
    public static final String EXTRA_CONNECTION_STATUS = "connection_status";
    public static final String EXTRA_LOCATION_STATUS = "location_status";
    public static final String EXTRA_BATTERY_LEVEL = "battery_level";
    public static final String EXTRA_REPORT_COUNT = "report_count";
    public static final String EXTRA_LOG_MESSAGE = "log_message";
    public static final String EXTRA_LOG_TYPE = "log_type";
    
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        
        // 在attachBaseContext中强制设置兼容主题，确保在Activity创建的最早阶段就生效
        try {
            Log.d(TAG, "MainActivity attachBaseContext - 强制设置兼容主题");
            setTheme(R.style.Theme_AppCompat_DayNight_NoActionBar);
        } catch (Exception e) {
            Log.e(TAG, "MainActivity attachBaseContext设置主题失败", e);
            try {
                setTheme(R.style.Theme_AppCompat_NoActionBar);
            } catch (Exception ex) {
                Log.e(TAG, "MainActivity attachBaseContext设置基础主题也失败", ex);
                try {
                    setTheme(R.style.Theme_AppCompat);
                } catch (Exception exc) {
                    Log.e(TAG, "MainActivity attachBaseContext所有主题设置都失败", exc);
                }
            }
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 强制设置兼容主题，确保在省电模式下也能正常工作
        forceSetCompatibleTheme();
        
        // 额外的安全措施：通过Application检查主题兼容性
        LocationTrackerApplication.ensureCompatibleTheme(this);
        
        try {
            Log.d(TAG, "MainActivity onCreate开始");
            setContentView(R.layout.activity_main);
            Log.d(TAG, "setContentView完成");
            
            // 设置透明状态栏（兼容Android 4.4及以上）
            setupTransparentStatusBar();
            
            // 初始化UI组件
            initViews();
            
            // 设置RecyclerView
            setupRecyclerView();
            
            // 注册状态广播接收器
            registerStatusReceiver();
            
            // 加载配置
            loadConfiguration();
            
            // 设置事件监听器
            setupEventListeners();
            
            // 初始化状态显示
            updateStatusDisplay();
            
            // 设置应用标题
            setAppTitle();
            
            // 检查logAdapter是否成功初始化
            if (logAdapter == null) {
                Log.e(TAG, "logAdapter初始化失败");
                Toast.makeText(this, "日志系统初始化失败", Toast.LENGTH_LONG).show();
                return;
            }
            
            // 添加基本启动日志
            logAdapter.addLog("=== APP启动 ===", "INFO");
            logAdapter.addLog("📱 手机位置上报应用已启动", "SUCCESS");
            logAdapter.addLog("=== 初始化完成 ===", "SUCCESS");
            
            // 处理从通知启动的情况
            handleNotificationIntent();
            
            // 检查权限
            checkAndRequestPermissions();
            
            // 检查并弹出设备优化建议
            new Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (PermissionGuideDialog.shouldShowDeviceOptimization(MainActivity.this)) {
                        DeviceOptimizationHelper.DeviceBrand brand = DeviceOptimizationHelper.detectDeviceBrand();
                        PermissionGuideDialog.showDeviceOptimizationDialog(MainActivity.this, brand);
                    }
                }
            }, 2000);
        
            Log.d(TAG, "MainActivity onCreate完成");
            
        } catch (Exception e) {
            Log.e(TAG, "MainActivity onCreate执行失败", e);
            LocationTrackerApplication.logError("MainActivity onCreate执行失败", e);
            Toast.makeText(this, "应用初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 强制设置兼容主题，确保在任何情况下都不会出现主题兼容性问题
     */
    private void forceSetCompatibleTheme() {
        try {
            // 智能主题设置：先获取系统状态，再设置最合适的主题
            setThemeIntelligently();
        } catch (Exception e) {
            Log.e(TAG, "MainActivity智能主题设置失败，使用兜底方案", e);
            // 兜底方案：强制设置最兼容的主题
            setFallbackTheme();
        }
    }
    
    /**
     * MainActivity智能主题设置：先获取系统主题和状态，再设置最合适的App主题
     */
    private void setThemeIntelligently() {
        try {
            Log.d(TAG, "MainActivity开始智能主题设置");
            
            // 1. 获取系统当前主题信息
            String systemTheme = getSystemThemeInfo();
            Log.d(TAG, "MainActivity系统主题信息: " + systemTheme);
            
            // 2. 检查系统状态（省电模式、夜间模式等）
            boolean isPowerSaveMode = isPowerSaveModeEnabled();
            boolean isNightMode = isNightModeEnabled();
            boolean isHighContrast = isHighContrastEnabled();
            
            Log.d(TAG, String.format("MainActivity系统状态 - 省电模式: %s, 夜间模式: %s, 高对比度: %s", 
                isPowerSaveMode, isNightMode, isHighContrast));
            
            // 3. 根据系统状态选择最合适的主题
            int selectedTheme = selectOptimalTheme(isPowerSaveMode, isNightMode, isHighContrast);
            
            // 4. 应用选定的主题
            setTheme(selectedTheme);
            
            Log.d(TAG, "MainActivity智能主题设置完成，使用主题ID: " + selectedTheme);
            
        } catch (Exception e) {
            Log.e(TAG, "MainActivity智能主题设置过程中发生异常", e);
            setFallbackTheme();
        }
    }
    
    /**
     * 获取MainActivity系统主题信息
     */
    private String getSystemThemeInfo() {
        try {
            android.content.res.Resources.Theme systemTheme = getTheme();
            if (systemTheme != null) {
                return systemTheme.toString();
            }
            
            // 获取系统配置信息
            android.content.res.Configuration config = getResources().getConfiguration();
            StringBuilder info = new StringBuilder();
            info.append("API Level: ").append(android.os.Build.VERSION.SDK_INT);
            info.append(", UI Mode: ").append(config.uiMode);
            info.append(", Screen Layout: ").append(config.screenLayout);
            info.append(", Orientation: ").append(config.orientation);
            
            return info.toString();
            
        } catch (Exception e) {
            Log.e(TAG, "MainActivity获取系统主题信息失败", e);
            return "获取失败";
        }
    }
    
    /**
     * 检查MainActivity是否启用省电模式
     */
    private boolean isPowerSaveModeEnabled() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                android.os.PowerManager pm = (android.os.PowerManager) getSystemService(android.content.Context.POWER_SERVICE);
                return pm != null && pm.isPowerSaveMode();
            }
        } catch (Exception e) {
            Log.e(TAG, "MainActivity检查省电模式失败", e);
        }
        return false;
    }
    
    /**
     * 检查MainActivity是否启用夜间模式
     */
    private boolean isNightModeEnabled() {
        try {
            android.content.res.Configuration config = getResources().getConfiguration();
            int nightMode = config.uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        } catch (Exception e) {
            Log.e(TAG, "MainActivity检查夜间模式失败", e);
        }
        return false;
    }
    
    /**
     * 检查MainActivity是否启用高对比度模式
     */
    private boolean isHighContrastEnabled() {
        try {
            android.content.res.Configuration config = getResources().getConfiguration();
            int uiModeType = config.uiMode & android.content.res.Configuration.UI_MODE_TYPE_MASK;
            return uiModeType != android.content.res.Configuration.UI_MODE_TYPE_NORMAL;
        } catch (Exception e) {
            Log.e(TAG, "MainActivity检查高对比度模式失败", e);
        }
        return false;
    }
    
    /**
     * MainActivity根据系统状态选择最优主题
     */
    private int selectOptimalTheme(boolean isPowerSaveMode, boolean isNightMode, boolean isHighContrast) {
        try {
            // 优先级1：省电模式 - 使用最基础的AppCompat主题
            if (isPowerSaveMode) {
                Log.d(TAG, "MainActivity省电模式检测，使用最基础AppCompat主题");
                return R.style.Theme_AppCompat;
            }
            
            // 优先级2：夜间模式 - 使用DayNight主题
            if (isNightMode) {
                Log.d(TAG, "MainActivity夜间模式检测，使用DayNight主题");
                return R.style.Theme_AppCompat_DayNight_NoActionBar;
            }
            
            // 优先级3：高对比度模式 - 使用基础AppCompat主题
            if (isHighContrast) {
                Log.d(TAG, "MainActivity高对比度模式检测，使用基础AppCompat主题");
                return R.style.Theme_AppCompat_NoActionBar;
            }
            
            // 优先级4：正常模式 - 使用标准DayNight主题
            Log.d(TAG, "MainActivity正常模式，使用标准DayNight主题");
            return R.style.Theme_AppCompat_DayNight_NoActionBar;
            
        } catch (Exception e) {
            Log.e(TAG, "MainActivity选择主题失败，使用兜底主题", e);
            return R.style.Theme_AppCompat;
        }
    }
    
    /**
     * MainActivity兜底主题设置
     */
    private void setFallbackTheme() {
        try {
            Log.w(TAG, "MainActivity使用兜底主题设置");
            
            // 尝试设置最兼容的主题
            setTheme(R.style.Theme_AppCompat_DayNight_NoActionBar);
            Log.d(TAG, "MainActivity成功设置Theme_AppCompat_DayNight_NoActionBar主题");
        } catch (Exception e) {
            Log.e(TAG, "MainActivity设置Theme_AppCompat_DayNight_NoActionBar主题失败", e);
            try {
                // 如果失败，尝试基础AppCompat主题
                setTheme(R.style.Theme_AppCompat_NoActionBar);
                Log.d(TAG, "MainActivity成功设置Theme_AppCompat_NoActionBar主题");
            } catch (Exception ex) {
                Log.e(TAG, "MainActivity设置Theme_AppCompat_NoActionBar主题失败", ex);
                try {
                    // 最后尝试最基础的AppCompat主题
                    setTheme(R.style.Theme_AppCompat);
                    Log.d(TAG, "MainActivity成功设置Theme_AppCompat主题");
                } catch (Exception exc) {
                    Log.e(TAG, "MainActivity所有主题设置都失败，应用可能崩溃", exc);
                    // 这里不抛出异常，让应用继续运行
                }
            }
        }
    }
    
    private void initViews() {
        try {
            txtWebhookUrl = (EditText) findViewById(R.id.txtUrl);
            txtTime = (EditText) findViewById(R.id.txtTime);
            lblHaconfig = (TextView) findViewById(R.id.lblHaconfig);
            sw_notification = (Switch) findViewById(R.id.sw_notification);
            btnStart = (Button) findViewById(R.id.btnStart);
            
            // 新增UI元素
            btnStatusTab = (Button) findViewById(R.id.btn_status_tab);
            btnConfigTab = (Button) findViewById(R.id.btn_config_tab);
            btnCopyLog = (Button) findViewById(R.id.btn_copy_log);
            btnDeviceOptimization = (Button) findViewById(R.id.btn_device_optimization);
            
            // 添加崩溃日志管理按钮
            Button btnCrashLogs = (Button) findViewById(R.id.btn_crash_logs);
            if (btnCrashLogs != null) {
                btnCrashLogs.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showCrashLogsDialog();
                    }
                });
            }
            statusPanel = (ScrollView) findViewById(R.id.status_panel);
            configPanel = (ScrollView) findViewById(R.id.config_panel);
            tvConnectionStatus = (TextView) findViewById(R.id.tv_connection_status);
            tvLocationStatus = (TextView) findViewById(R.id.tv_location_status);
            tvBatteryLevel = (TextView) findViewById(R.id.tv_battery_level);
            tvReportCount = (TextView) findViewById(R.id.tv_report_count);
            rvLogs = (RecyclerView) findViewById(R.id.recycler_view_logs);
            
            // 检查关键UI元素是否找到
            if (rvLogs == null) {
                Log.e(TAG, "RecyclerView未找到，可能导致崩溃");
                Toast.makeText(this, "UI初始化失败：RecyclerView未找到", Toast.LENGTH_LONG).show();
                return;
            }
            
            // 检查其他关键UI元素
            if (txtWebhookUrl == null || txtTime == null || btnStart == null) {
                Log.e(TAG, "关键UI元素未找到");
                Toast.makeText(this, "UI初始化失败：关键元素未找到", Toast.LENGTH_LONG).show();
                return;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "initViews执行失败", e);
            LocationTrackerApplication.logError("UI初始化失败", e);
            Toast.makeText(this, "UI初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void setupRecyclerView() {
        try {
            if (rvLogs == null) {
                Log.e(TAG, "RecyclerView为null，无法初始化");
                return;
            }
            
            logAdapter = new LogAdapter();
            rvLogs.setLayoutManager(new LinearLayoutManager(this));
            rvLogs.setAdapter(logAdapter);
            
            // 添加触摸反馈
            rvLogs.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, android.view.MotionEvent event) {
                    switch (event.getAction()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            v.setAlpha(0.9f);
                            break;
                        case android.view.MotionEvent.ACTION_UP:
                        case android.view.MotionEvent.ACTION_CANCEL:
                            v.setAlpha(1.0f);
                            break;
                    }
                    return false; // 不拦截事件，让RecyclerView正常处理
                }
            });
            
            // 优化滚动性能
            rvLogs.setHasFixedSize(true);
            rvLogs.setItemViewCacheSize(20);
            
            // 添加初始日志
            logAdapter.addLog("RecyclerView初始化完成", "INFO");
            
        } catch (Exception e) {
            Log.e(TAG, "setupRecyclerView执行失败", e);
            LocationTrackerApplication.logError("RecyclerView初始化失败", e);
            Toast.makeText(this, "RecyclerView初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void registerStatusReceiver() {
        statusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ACTION_STATUS_UPDATE.equals(action)) {
                    // 更新状态信息
                    String connectionStatus = intent.getStringExtra(EXTRA_CONNECTION_STATUS);
                    String locationStatus = intent.getStringExtra(EXTRA_LOCATION_STATUS);
                    String batteryLevel = intent.getStringExtra(EXTRA_BATTERY_LEVEL);
                    String reportCount = intent.getStringExtra(EXTRA_REPORT_COUNT);
                    
                    if (connectionStatus != null && tvConnectionStatus != null) {
                        tvConnectionStatus.setText(connectionStatus);
                    }
                    if (locationStatus != null && tvLocationStatus != null) {
                        tvLocationStatus.setText(locationStatus);
                    }
                    if (batteryLevel != null && tvBatteryLevel != null) {
                        tvBatteryLevel.setText(batteryLevel);
                    }
                    if (reportCount != null && tvReportCount != null) {
                        tvReportCount.setText(reportCount);
                    }
                } else if (ACTION_LOG_UPDATE.equals(action)) {
                    // 添加日志条目
                    String logMessage = intent.getStringExtra(EXTRA_LOG_MESSAGE);
                    String logType = intent.getStringExtra(EXTRA_LOG_TYPE);
                    
                    if (logMessage != null && logAdapter != null) {
                        logAdapter.addLog(logMessage, logType != null ? logType : "INFO");
                        
                        // 检测服务启动成功的标志
                        if (logMessage.contains("[应用服务]服务启动成功")) {
                            serviceStartedSuccessfully = true;
                        }
                    }
                }
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_STATUS_UPDATE);
        filter.addAction(ACTION_LOG_UPDATE);
        registerReceiver(statusReceiver, filter);
    }
    
    private void loadConfiguration() {
        try {
            Log.d(TAG, "开始加载配置...");
        DataBaseOpenHelper dataBaseOpenHelper = new DataBaseOpenHelper(this);
        SQLiteDatabase db = dataBaseOpenHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from " + Contant.TABLENAME, null);
            
            Log.d(TAG, "数据库记录数: " + cursor.getCount());
        
        // 如果数据库为空，插入默认配置
        if(cursor.getCount() == 0) {
                Log.d(TAG, "数据库为空，插入默认配置");
            String sql = "INSERT INTO " + Contant.TABLENAME + "(url, time, notification_enable) VALUES(?, ?, ?)";
            db.execSQL(sql, new Object[]{BuildConfig.WEBHOOK_URL, 3600, 0});
            cursor = db.rawQuery("select * from " + Contant.TABLENAME, null);
                Log.d(TAG, "插入默认配置后记录数: " + cursor.getCount());
        }
        
        while (cursor.moveToNext()) {
                String url = cursor.getString(0);
                int time = cursor.getInt(5);
                int notification = cursor.getInt(8);
                
                Log.d(TAG, "从数据库读取 - URL: " + url + ", Time: " + time + ", Notification: " + notification);
                
                if (txtWebhookUrl != null) {
                    Utils.syncWebhookIdFromUrl(this, url);
                    String normalized = Utils.buildWebhookUrl(this, url);
                    txtWebhookUrl.setText(normalized);
                    Log.d(TAG, "设置Webhook URL: " + normalized);
                } else {
                    Log.e(TAG, "txtWebhookUrl为null");
                }
                
                if (txtTime != null) {
                    Log.d(TAG, "设置Time: " + time);
                    txtTime.setText(time + "");
                } else {
                    Log.e(TAG, "txtTime为null");
                }
                
                if (sw_notification != null) {
                    Log.d(TAG, "设置Notification: " + (notification == 1));
                    if(notification == 1)
                sw_notification.setChecked(true);
            else
                sw_notification.setChecked(false);
                } else {
                    Log.e(TAG, "sw_notification为null");
                }
            }
        
        cursor.close();
        db.close();
            Log.d(TAG, "配置加载完成");
            
        } catch (Exception e) {
            Log.e(TAG, "loadConfiguration执行失败", e);
            Toast.makeText(this, "配置加载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (logAdapter != null) {
                logAdapter.addLog("配置加载失败: " + e.getMessage(), "ERROR");
            }
        }
    }
    
    private void setupEventListeners() {
        try {
            // Tab切换事件
            if (btnStatusTab != null) {
                btnStatusTab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switchToStatusTab();
                    }
                });
            }
            
            if (btnConfigTab != null) {
                btnConfigTab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switchToConfigTab();
                    }
                });
            }
            
            // 开始按钮事件
            if (btnStart != null) {
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                        startLocationService();
                    }
                });
            }
            
            // 复制日志按钮事件
            if (btnCopyLog != null) {
                btnCopyLog.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        copyLogToClipboard();
                    }
                });
            }
            
            // 设备优化按钮事件
            if (btnDeviceOptimization != null) {
                btnDeviceOptimization.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showDeviceOptimizationDialog();
                    }
                });
            }
            
            // 通知开关监听器
            if (sw_notification != null) {
                sw_notification.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                        try {
                            if (logAdapter != null) {
                                logAdapter.addLog("🔔 通知开关: " + (isChecked ? "开启" : "关闭"), "INFO");
                            }
                            // 实时保存开关状态到数据库
                            saveNotificationSetting(isChecked);
                        } catch (Exception e) {
                            Log.e(TAG, "通知开关状态变化处理失败", e);
                        }
                    }
                });
            }
            
        } catch (Exception e) {
            Log.e(TAG, "setupEventListeners执行失败", e);
            Toast.makeText(this, "事件监听器设置失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 保存通知设置到数据库
     */
    private void saveNotificationSetting(boolean enabled) {
        try {
            DataBaseOpenHelper dataBaseOpenHelper = new DataBaseOpenHelper(this);
            SQLiteDatabase db = dataBaseOpenHelper.getWritableDatabase();
            try {
                String sql = "UPDATE " + Contant.TABLENAME + " SET notification_enable=?";
                db.execSQL(sql, new Object[]{enabled ? 1 : 0});
                Log.d(TAG, "通知设置已保存: " + (enabled ? "开启" : "关闭"));
                // 同步到服务变量，确保立即生效
                ltmService.setNotificationEnable(enabled ? 1 : 0);
            } finally {
                db.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "保存通知设置失败", e);
        }
    }
    
    private void switchToStatusTab() {
        try {
            if (statusPanel == null || configPanel == null || btnStatusTab == null || btnConfigTab == null) {
                Log.e(TAG, "UI元素为null，无法切换Tab");
                return;
            }
            // 添加淡入淡出动画
            statusPanel.setAlpha(0f);
            statusPanel.setVisibility(View.VISIBLE);
            statusPanel.animate().alpha(1f).setDuration(300).start();
            configPanel.setVisibility(View.GONE);
            // 更新按钮状态
            btnStatusTab.setSelected(true);
            btnConfigTab.setSelected(false);
            // 按钮动画：只做scaleY，动画期间禁用，前后刷新状态
            btnStatusTab.setEnabled(false);
            btnStatusTab.animate()
                .scaleY(1.05f)
                .setDuration(120)
                .withStartAction(new Runnable() {
                    @Override
                    public void run() {
                        btnStatusTab.refreshDrawableState();
                    }
                })
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        btnStatusTab.animate()
                            .scaleY(1.0f)
                            .setDuration(80)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    btnStatusTab.setEnabled(true);
                                    btnStatusTab.refreshDrawableState();
                                }
                            })
                            .start();
                    }
                })
                .start();
            btnConfigTab.animate()
                .scaleY(1.0f)
                .setDuration(120)
                .start();
            // 添加触觉反馈（兼容Android 4.0及以上）
            try {
                android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    // 使用兼容的方法，避免过时API警告
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        // Android 8.0+ 使用新的震动方法
                        android.os.VibrationEffect effect = android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE);
                        vibrator.vibrate(effect);
                    } else {
                        // Android 4.0-7.1 使用旧方法
                        vibrator.vibrate(50);
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "触觉反馈失败", e);
            }
            Log.d(TAG, "切换到监控页面");
        } catch (Exception e) {
            Log.e(TAG, "switchToStatusTab执行失败", e);
        }
    }
    
    private void switchToConfigTab() {
        try {
            if (statusPanel == null || configPanel == null || btnStatusTab == null || btnConfigTab == null) {
                Log.e(TAG, "UI元素为null，无法切换Tab");
                return;
            }
            // 添加淡入淡出动画
            configPanel.setAlpha(0f);
            configPanel.setVisibility(View.VISIBLE);
            configPanel.animate().alpha(1f).setDuration(300).start();
            statusPanel.setVisibility(View.GONE);
            // 更新按钮状态
            btnStatusTab.setSelected(false);
            btnConfigTab.setSelected(true);
            // 按钮动画：只做scaleY，动画期间禁用，前后刷新状态
            btnConfigTab.setEnabled(false);
            btnConfigTab.animate()
                .scaleY(1.05f)
                .setDuration(120)
                .withStartAction(new Runnable() {
                    @Override
                    public void run() {
                        btnConfigTab.refreshDrawableState();
                    }
                })
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        btnConfigTab.animate()
                            .scaleY(1.0f)
                            .setDuration(80)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    btnConfigTab.setEnabled(true);
                                    btnConfigTab.refreshDrawableState();
                                }
                            })
                            .start();
                    }
                })
                .start();
            btnStatusTab.animate()
                .scaleY(1.0f)
                .setDuration(120)
                .start();
            // 添加触觉反馈（兼容Android 4.0及以上）
            try {
                android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    // 使用兼容的方法，避免过时API警告
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        // Android 8.0+ 使用新的震动方法
                        android.os.VibrationEffect effect = android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE);
                        vibrator.vibrate(effect);
                    } else {
                        // Android 4.0-7.1 使用旧方法
                        vibrator.vibrate(50);
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "触觉反馈失败", e);
            }
            Log.d(TAG, "切换到配置页面");
        } catch (Exception e) {
            Log.e(TAG, "switchToConfigTab执行失败", e);
        }
    }
    
    private void updateStatusDisplay() {
        try {
            // 初始状态显示
            if (tvConnectionStatus != null) {
                tvConnectionStatus.setText(getString(R.string.disconnected));
            }
            if (tvLocationStatus != null) {
                tvLocationStatus.setText(getString(R.string.stopped));
            }
            if (tvBatteryLevel != null) {
                tvBatteryLevel.setText("--");
            }
            if (tvReportCount != null) {
                tvReportCount.setText("0");
            }
        } catch (Exception e) {
            Log.e(TAG, "更新状态显示失败", e);
        }
    }
    
    private void startLocationService() {
        try {
            // 隐藏键盘
            hideKeyboard();
            
            logAdapter.addLog("开始启动位置服务...", "INFO");
            
                // 输入验证
                String webhookInput = txtWebhookUrl.getText().toString().trim();
                String timeStr = txtTime.getText().toString().trim();
                if (timeStr.equals("")) {
                    timeStr = "3600";
                    if (txtTime != null) {
                        txtTime.setText(timeStr);
                    }
                }

                if (webhookInput.equals("")) {
                    Toast.makeText(MainActivity.this, getString(R.string.fill_required), Toast.LENGTH_LONG).show();
                    return;
                }
                if (!webhookInput.startsWith("http://") && !webhookInput.startsWith("https://")) {
                    Toast.makeText(MainActivity.this, getString(R.string.invalid_url), Toast.LENGTH_LONG).show();
                    return;
                }

                String webhookUrl = Utils.buildWebhookUrl(this, webhookInput);
                String webhookId = Utils.getOrCreateWebhookId(this);
                if (txtWebhookUrl != null) {
                    txtWebhookUrl.setText(webhookUrl);
                }
                
                int time;
                try {
                    time = Integer.parseInt(timeStr);
                    if (time < 10 || time > 10800) {
                        time = 3600;
                    }
                } catch (NumberFormatException e) {
                    time = 3600;
                }
            
            logAdapter.addLog("配置验证通过", "SUCCESS");
            logAdapter.addLog("保存配置到数据库...", "INFO");
            
            DataBaseOpenHelper dataBaseOpenHelper = new DataBaseOpenHelper(this);
                SQLiteDatabase db = dataBaseOpenHelper.getWritableDatabase();
                try {
                    String sql = "UPDATE " + Contant.TABLENAME + " SET url=?, time=?, notification_enable=?";
                    db.execSQL(sql, new Object[]{webhookUrl, time, sw_notification.isChecked() ? 1 : 0});
                logAdapter.addLog("配置已保存到数据库", "SUCCESS");
                
                ltmService.HOST = webhookUrl;
                ltmService.setTimeInterval(time);
                ltmService.setNotificationEnable(sw_notification.isChecked() ? 1 : 0);
                
                logAdapter.addLog("📡 Webhook URL: " + webhookUrl, "INFO");
                logAdapter.addLog("🔑 Webhook ID: " + webhookId, "INFO");
                logAdapter.addLog("⏱️ 初始间隔: " + time + "秒（可被 HA 配置覆盖）", "INFO");
                logAdapter.addLog("=== 配置修改成功 ===", "SUCCESS");
                
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, getString(R.string.save_failed) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "保存配置失败", e);
                logAdapter.addLog("保存配置失败: " + e.getMessage(), "ERROR");
                logAdapter.addLog("错误详情: " + e.toString(), "ERROR");
                return;
            } finally {
                db.close();
            }
            
            logAdapter.addLog("启动位置服务...", "INFO");
                    ltmService.setFromMain(true);
                    Intent intent = new Intent(MainActivity.this, ltmService.class);
                    startService(intent);
                    Toast.makeText(MainActivity.this, getString(R.string.service_started), Toast.LENGTH_SHORT).show();
                    
                    logAdapter.addLog("位置服务启动中...", "INFO");
                    
                    // 切换到状态面板
                    switchToStatusTab();
                    
                    // 添加按钮状态反馈
                    btnStart.setEnabled(false);
                    btnStart.setText("启动中...");
                    
                    // 3秒后恢复按钮状态
                    new Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            btnStart.setEnabled(true);
                            btnStart.setText(getString(R.string.start_location));
                        }
                    }, 3000);
                    
        } catch (Exception e) {
            Log.e(TAG, "startLocationService执行失败", e);
            logAdapter.addLog("启动位置服务失败: " + e.getMessage(), "ERROR");
            Toast.makeText(this, "启动服务失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销广播接收器
        if (statusReceiver != null) {
            try {
                unregisterReceiver(statusReceiver);
            } catch (Exception e) {
                Log.e(TAG, "注销状态广播接收器失败", e);
            }
        }
    }

    /**
     * 检查并申请必要权限
     */
    private void checkAndRequestPermissions() {
        // Android 6.0 (API 23) 及以上才需要运行时权限申请
        if (Build.VERSION.SDK_INT >= 23) {
            logAdapter.addLog("Android版本 >= 23，需要运行时权限", "INFO");
            
            String[] perms = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.WAKE_LOCK
            };
            
            // Android 8.0 (API 26) 及以上才需要前台服务权限
            if (Build.VERSION.SDK_INT >= 26) {
                logAdapter.addLog("Android版本 >= 26，需要前台服务权限", "INFO");
                perms = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.INTERNET,
                    Manifest.permission.WAKE_LOCK,
                    Manifest.permission.FOREGROUND_SERVICE
                };
            }
            
            // Android 13+ (API 33) 需要通知权限
            if (Build.VERSION.SDK_INT >= 33) {
                logAdapter.addLog("Android版本 >= 33，需要通知权限", "INFO");
                String[] newPerms = new String[perms.length + 1];
                System.arraycopy(perms, 0, newPerms, 0, perms.length);
                newPerms[perms.length] = Manifest.permission.POST_NOTIFICATIONS;
                perms = newPerms;
            }
            
            final String[] permissions = perms;
            
            boolean needRequest = false;
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    needRequest = true;
                    logAdapter.addLog("缺少权限: " + permission, "WARNING");
                    break;
                }
            }
            
            if (needRequest) {
                logAdapter.addLog("开始申请权限...", "INFO");
                
                // 显示权限申请说明对话框
                PermissionGuideDialog.showPermissionRequestDialog(this);
                
                // 延迟1秒后申请权限，让用户先看到说明
                new Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
                    }
                }, 1000);
            } else {
                logAdapter.addLog("所有权限已授予", "SUCCESS");
                logAdapter.addLog("=== 自动启动位置服务 ===", "INFO");
                
                // 延迟2秒后自动启动位置服务，确保UI完全初始化
                new Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        logAdapter.addLog("权限已授予，开始自动启动位置服务...", "INFO");
                        autoStartLocationService();
                    }
                }, 2000);
            }
        } else {
            logAdapter.addLog("Android版本 < 23，无需运行时权限申请", "INFO");
            logAdapter.addLog("权限已就绪", "SUCCESS");
        }
        // Android 4.0-5.1 在安装时已经申请了权限，无需运行时申请
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted) {
                Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_LONG).show();
                logAdapter.addLog("权限申请失败，部分功能可能无法使用", "ERROR");
                
                // 显示权限被拒绝的引导对话框
                PermissionGuideDialog.showPermissionDeniedDialog(this);
            } else {
                logAdapter.addLog("权限申请成功", "SUCCESS");
                logAdapter.addLog("=== 自动启动位置服务 ===", "INFO");
                
                // 延迟2秒后自动启动位置服务，确保UI完全初始化
                new Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        logAdapter.addLog("权限已授予，开始自动启动位置服务...", "INFO");
                        autoStartLocationService();
                    }
                }, 2000);
            }
        }
    }

    private void copyLogToClipboard() {
        try {
            if (logAdapter == null) {
                Toast.makeText(this, "日志系统未初始化", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String logText = logAdapter.getAllLogsText();
            if (logText.isEmpty()) {
                Toast.makeText(this, "暂无日志可复制", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 复制到剪贴板
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("位置上报日志", logText);
            clipboard.setPrimaryClip(clip);
            
            // 显示成功提示
            Toast.makeText(this, "✅ 日志已复制到剪贴板", Toast.LENGTH_SHORT).show();
            
            // 添加按钮动画反馈
            if (btnCopyLog != null) {
                btnCopyLog.setScaleX(0.9f);
                btnCopyLog.setScaleY(0.9f);
                btnCopyLog.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
            }
            
            // 添加日志记录
            logAdapter.addLog("📋 日志已复制到剪贴板", "SUCCESS");
            logAdapter.addLog("日志条数: " + logAdapter.getLogCount() + " 条", "INFO");
            
            // 添加触觉反馈（兼容Android 4.0及以上）
            try {
                android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    // 使用兼容的方法，避免过时API警告
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        // Android 8.0+ 使用新的震动方法
                        android.os.VibrationEffect effect = android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE);
                        vibrator.vibrate(effect);
                    } else {
                        // Android 4.0-7.1 使用旧方法
                        vibrator.vibrate(50);
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "触觉反馈失败", e);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "复制日志失败", e);
            LocationTrackerApplication.logError("复制日志失败", e);
            Toast.makeText(this, "复制失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 自动启动定位服务
     */
    private void autoStartLocationService() {
        try {
            logAdapter.addLog("检查配置参数...", "INFO");
            
            String webhookInput = txtWebhookUrl.getText().toString().trim();
            String timeStr = txtTime.getText().toString().trim();
            if (timeStr.equals("")) {
                timeStr = "3600";
            }
            if (webhookInput.equals("") || (!webhookInput.startsWith("http://") && !webhookInput.startsWith("https://"))) {
                logAdapter.addLog("配置验证失败：请填写有效的 HA 地址", "ERROR");
                return;
            }
            String webhookUrl = Utils.buildWebhookUrl(this, webhookInput);
            String webhookId = Utils.getOrCreateWebhookId(this);
            if (txtWebhookUrl != null) {
                txtWebhookUrl.setText(webhookUrl);
            }
            
            int time;
            try {
                time = Integer.parseInt(timeStr);
                if (time < 10 || time > 10800) {
                    time = 3600;
                }
            } catch (NumberFormatException e) {
                time = 3600;
            }
            
            logAdapter.addLog("配置验证通过，开始自动启动定位服务", "SUCCESS");
            
            logAdapter.addLog("保存配置到数据库...", "INFO");
            DataBaseOpenHelper dataBaseOpenHelper = new DataBaseOpenHelper(this);
            SQLiteDatabase db = dataBaseOpenHelper.getWritableDatabase();
            try {
                String sql = "UPDATE " + Contant.TABLENAME + " SET url=?, time=?, notification_enable=?";
                db.execSQL(sql, new Object[]{webhookUrl, time, sw_notification.isChecked() ? 1 : 0});
                logAdapter.addLog("配置保存成功", "SUCCESS");
                
                ltmService.HOST = webhookUrl;
                ltmService.setMode(time);
                ltmService.setNotificationEnable(sw_notification.isChecked() ? 1 : 0);
                
                logAdapter.addLog("Webhook URL: " + webhookUrl, "INFO");
                logAdapter.addLog("Webhook ID: " + webhookId, "INFO");
                logAdapter.addLog("通知开关: " + (sw_notification.isChecked() ? "开启" : "关闭"), "INFO");
                
            } catch (Exception e) {
                Log.e(TAG, "保存配置失败", e);
                logAdapter.addLog("保存配置失败: " + e.getMessage(), "ERROR");
                logAdapter.addLog("错误详情: " + e.toString(), "ERROR");
                return;
            } finally {
                db.close();
            }
            
            // 启动位置服务
            logAdapter.addLog("启动位置服务...", "INFO");
            ltmService.setFromMain(true);
            Intent intent = new Intent(MainActivity.this, ltmService.class);
            startService(intent);
            Toast.makeText(MainActivity.this, getString(R.string.service_started), Toast.LENGTH_SHORT).show();
            
            logAdapter.addLog("位置服务启动中...", "INFO");
            
            // 切换到状态面板
            switchToStatusTab();
            
            // 延迟检查服务是否正常启动
            new Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!serviceStartedSuccessfully) {
                        logAdapter.addLog("检查服务启动状态...", "INFO");
                        logAdapter.addLog("如果上方没有服务启动日志，请检查：", "WARNING");
                        logAdapter.addLog("1. GPS是否已开启", "WARNING");
                        logAdapter.addLog("2. 定位权限是否已授予", "WARNING");
                        logAdapter.addLog("3. 网络连接是否正常", "WARNING");
                    }
                }
            }, 3000);
            
            // 延迟5秒后立即上报一次位置
            new Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    logAdapter.addLog("=== 立即上报一次位置 ===", "INFO");
                    logAdapter.addLog("触发立即位置上报...", "INFO");
                    
                    // 发送立即上报的广播
                    Intent immediateReportIntent = new Intent("com.ljs.locationtracker.IMMEDIATE_REPORT");
                    sendBroadcast(immediateReportIntent);
                    
                    logAdapter.addLog("立即上报广播已发送", "SUCCESS");
                }
            }, 5000);
            
        } catch (Exception e) {
            Log.e(TAG, "自动启动服务失败", e);
            logAdapter.addLog("自动启动服务失败: " + e.getMessage(), "ERROR");
            logAdapter.addLog("错误详情: " + e.toString(), "ERROR");
            logAdapter.addLog("堆栈跟踪: " + Log.getStackTraceString(e), "ERROR");
        }
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ltmService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取设备名称
     */
    private String getDeviceName() {
        try {
            String manufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER : "";
            String model = Build.MODEL != null ? Build.MODEL : "";
            String brand = Build.BRAND != null ? Build.BRAND : "";
            
            // 获取系统版本信息
            String androidVersion = "Android " + Build.VERSION.RELEASE;
            String sdkVersion = "API " + Build.VERSION.SDK_INT;
            String buildNumber = Build.DISPLAY != null ? Build.DISPLAY : "";
            
            // 移除重复的设备信息日志显示，只保留到系统日志
            Log.d(TAG, "设备信息 - Manufacturer: " + manufacturer + ", Brand: " + brand + ", Model: " + model + ", Android: " + androidVersion + ", SDK: " + sdkVersion);
            
            // 清理设备名称
            String cleanBrand = brand != null ? brand.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9\u4e00-\u9fa5]", "") : "";
            String cleanModel = model != null ? model.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9\u4e00-\u9fa5]", "") : "";
            String cleanManufacturer = manufacturer != null ? manufacturer.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9\u4e00-\u9fa5]", "") : "";
            
            // 按优先级选择设备名称：品牌 > 型号 > 制造商
            String deviceName = null;
            
            // 1. 优先使用品牌
            if (cleanBrand != null && !cleanBrand.isEmpty() && !cleanBrand.equals("unknown")) {
                deviceName = cleanBrand;
                Log.d(TAG, "使用品牌作为设备名称: " + deviceName);
            }
            // 2. 如果品牌无效，使用型号
            else if (cleanModel != null && !cleanModel.isEmpty() && !cleanModel.equals("unknown")) {
                deviceName = cleanModel;
                Log.d(TAG, "使用型号作为设备名称: " + deviceName);
            }
            // 3. 如果型号无效，使用制造商
            else if (cleanManufacturer != null && !cleanManufacturer.isEmpty() && !cleanManufacturer.equals("unknown")) {
                deviceName = cleanManufacturer;
                Log.d(TAG, "使用制造商作为设备名称: " + deviceName);
            }
            
            if (deviceName != null && !deviceName.isEmpty()) {
                String deviceTitle = deviceName + "位置上报";
                Log.d(TAG, "设置设备标题: " + deviceTitle);
                return deviceTitle;
            } else {
                Log.d(TAG, "无法获取设备名称，使用默认标题");
                return "位置上报";
            }
        } catch (Exception e) {
            Log.e(TAG, "获取设备名称失败", e);
            if (logAdapter != null) {
                logAdapter.addLog("获取设备名称失败: " + e.getMessage(), "ERROR");
            }
            return "位置上报";
        }
    }
    
    /**
     * 设置应用标题
     */
    private void setAppTitle() {
        try {
            String deviceTitle = getDeviceName();
            TextView titleTextView = findViewById(R.id.tv_app_title);
            if (titleTextView != null) {
                titleTextView.setText(deviceTitle);
            }
        } catch (Exception e) {
            Log.e(TAG, "设置应用标题失败", e);
        }
    }

    /**
     * 检测设备品牌并显示优化建议
     */
    private void detectDeviceAndShowTips() {
        try {
            Log.d(TAG, "开始检测设备品牌...");
            
            DeviceOptimizationHelper.DeviceBrand brand = DeviceOptimizationHelper.detectDeviceBrand();
            String androidVersion = "Android " + Build.VERSION.RELEASE;
            String sdkVersion = "API " + Build.VERSION.SDK_INT;
            
            String deviceInfo = String.format("📱 %s %s (%s) - %s %s", 
                Build.MANUFACTURER != null ? Build.MANUFACTURER : "未知",
                Build.MODEL != null ? Build.MODEL : "未知", 
                brand.getDisplayName(),
                androidVersion,
                sdkVersion);
            
            // 显示设备信息，使用标题+内容的格式
            if (logAdapter != null) {
                logAdapter.addLog("=== 设备信息 ===", "INFO");
                logAdapter.addLog(deviceInfo, "INFO");
            } else {
                Log.w(TAG, "logAdapter为null，无法显示设备信息");
            }
            
            // 显示设备优化对话框（延迟3秒，避免与权限申请对话框冲突）
            new Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    PermissionGuideDialog.showDeviceOptimizationDialog(MainActivity.this, brand);
                }
            }, 3000);
            
            // 应用设备优化策略
            DeviceOptimizationHelper.applyDeviceOptimization(this);
            
        } catch (Exception e) {
            Log.e(TAG, "设备检测失败", e);
            if (logAdapter != null) {
                logAdapter.addLog("❌ 设备检测失败: " + e.getMessage(), "ERROR");
            }
            // 即使设备检测失败，也不应该导致应用崩溃
        }
    }
    
    /**
     * 显示设备优化对话框
     */
    private void showDeviceOptimizationDialog() {
        try {
            DeviceOptimizationHelper.DeviceBrand brand = DeviceOptimizationHelper.detectDeviceBrand();
            PermissionGuideDialog.showDeviceOptimizationDialog(this, brand);
        } catch (Exception e) {
            Log.e(TAG, "显示设备优化对话框失败", e);
            Toast.makeText(this, "显示设备优化对话框失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleNotificationIntent() {
        try {
            // 检查是否从通知启动
            Intent intent = getIntent();
            if (intent != null) {
                // 如果是从通知启动，自动切换到状态页面并添加日志
                logAdapter.addLog("从通知启动应用", "INFO");
                logAdapter.addLog("🔄 自动切换到状态页面", "INFO");
                
                // 延迟切换到状态页面，确保UI完全初始化
                new Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        switchToStatusTab();
                        logAdapter.addLog("✅ 已切换到状态页面", "SUCCESS");
                    }
                }, 1000);
            }
        } catch (Exception e) {
            Log.e(TAG, "处理通知Intent失败", e);
            logAdapter.addLog("处理通知Intent失败: " + e.getMessage(), "ERROR");
        }
    }


    
    /**
     * 显示崩溃日志管理对话框
     */
    private void showCrashLogsDialog() {
        try {
            showCrashLogsDialogWithData();
        } catch (Exception e) {
            Log.e(TAG, "显示崩溃日志对话框失败", e);
            LocationTrackerApplication.logError("显示崩溃日志对话框失败", e);
            Toast.makeText(this, "显示崩溃日志管理失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 显示崩溃日志管理对话框（带数据）
     */
    private void showCrashLogsDialogWithData() {
        int logCount = LocationTrackerApplication.getCrashLogCount();
        long logSize = LocationTrackerApplication.getCrashLogSize();
        String logDirectory = LocationTrackerApplication.getCrashLogDirectory();
        String recentCrashes = LocationTrackerApplication.getRecentCrashLogsSummary();
        
        String details = String.format(
            "崩溃日志管理\n\n" +
            "存储位置:\n" +
            "%s\n\n" +
            "当前状态:\n" +
            "• 文件数量: %d 个\n" +
            "• 占用空间: %d MB\n\n" +
            "最近3个崩溃:\n" +
            "%s\n" +
            "自动清理策略:\n" +
            "• 保留时间: 7天\n" +
            "• 最大文件数: 20个\n" +
            "• 清理时机: 应用启动后5秒\n" +
            "• 保护机制: 最近1分钟内的文件不会被清理",
            logDirectory, logCount, logSize, recentCrashes
        );
        
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("崩溃日志管理")
            .setMessage(details)
            .setPositiveButton("清理日志", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        // 使用立即清理方法
                        LocationTrackerApplication.cleanAllCrashLogs();
                        Toast.makeText(MainActivity.this, "崩溃日志已清理", Toast.LENGTH_SHORT).show();
                        if (logAdapter != null) {
                            logAdapter.addLog("✅ 崩溃日志已清理", "SUCCESS");
                        }
                        
                        // 延迟500毫秒后重新显示对话框，更新数据
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                showCrashLogsDialogWithData();
                            }
                        }, 500);
                        
                    } catch (Exception e) {
                        Log.e(TAG, "清理崩溃日志失败", e);
                        LocationTrackerApplication.logError("清理崩溃日志失败", e);
                        Toast.makeText(MainActivity.this, "清理失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("复制日志", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        // 复制到剪贴板
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText("崩溃日志详情", details);
                        clipboard.setPrimaryClip(clip);
                        
                        Toast.makeText(MainActivity.this, "崩溃日志详情已复制到剪贴板", Toast.LENGTH_SHORT).show();
                        
                        if (logAdapter != null) {
                            logAdapter.addLog("✅ 崩溃日志详情已复制到剪贴板", "SUCCESS");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "复制崩溃日志详情失败", e);
                        LocationTrackerApplication.logError("复制崩溃日志详情失败", e);
                        Toast.makeText(MainActivity.this, "复制失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNeutralButton("取消", null)
            .create();
            
        dialog.show();
    }

    /**
     * 隐藏键盘
     */
    private void hideKeyboard() {
        try {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                View currentFocus = getCurrentFocus();
                if (currentFocus != null) {
                    imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "隐藏键盘失败", e);
        }
    }

    /**
     * 设置透明状态栏（兼容Android 4.4及以上）
     */
    private void setupTransparentStatusBar() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Android 5.0+ 完全透明
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                );
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(Color.TRANSPARENT);
                Log.d(TAG, "设置Android 5.0+透明状态栏");
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // Android 4.4-4.4W 半透明
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                Log.d(TAG, "设置Android 4.4半透明状态栏");
            } else {
                // Android 4.0-4.3 不支持，自动忽略
                Log.d(TAG, "Android 4.0-4.3不支持透明状态栏，自动忽略");
            }
        } catch (Exception e) {
            Log.e(TAG, "设置透明状态栏失败", e);
            // 失败时不影响应用正常运行
        }
    }
}