package com.sudaiwu.schedulalarm;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import static com.sudaiwu.schedulalarm.MainActivity.TAG;

public class AlarmReceviceService extends Service {

    public static final LocalTime DEFAULT_CUSTOM_START_TIME = LocalTime.of(23, 0);
    public static final LocalTime DEFAULT_CUSTOM_END_TIME = LocalTime.of(7, 0);

    private final static String ID_CHANNEL = "alarm channel id";
    private final static String TAG_NOTIFICATION = "alarm notice tag";
    private final static String TAG_NOTIFICATION_CHANNEL = "alarm notice channel";
    private final static int ID_NOTIFICATION_CHANNEL = 0x99;

    private boolean isRegisterTimeChangerRecevier = false;

    private ShareUtil mShareUtil;
    private AlarmManager mAlarmManager;
    private NotificationManager mNoMan;

    final Object mLock = new Object();
    private boolean isEnable;
    private LocalTime mCustomAutoStartMilliseconds = DEFAULT_CUSTOM_START_TIME;
    private LocalTime mCustomAutoEndMilliseconds = DEFAULT_CUSTOM_END_TIME;

    private Handler mHandle = new H(this);

    private static class H extends Handler {
        WeakReference<AlarmReceviceService> mService;

        public H(AlarmReceviceService activity) {
            mService = new WeakReference<AlarmReceviceService>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(mService.get().getApplicationContext(), (String)msg.obj, Toast.LENGTH_LONG).show();
            Log.d(TAG, (String)msg.obj);
        }

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNoMan = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mShareUtil = new ShareUtil(this);
        updateConfigurationLocked();
        showNotification();
    }

    private void showNotification() {
        NotificationChannel mChannel = new NotificationChannel(ID_CHANNEL,
                TAG_NOTIFICATION_CHANNEL, NotificationManager.IMPORTANCE_HIGH);

        mNoMan.createNotificationChannel(mChannel);
        String bigText = getString(R.string.normal_settings_time_title);
        NotificationCompat.BigTextStyle bigxtstyle =
                new NotificationCompat.BigTextStyle();
        bigxtstyle.bigText(bigText);

        final NotificationCompat.Builder nb = new NotificationCompat.Builder(this, ID_CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setStyle(bigxtstyle)
                .setContentTitle(getString(R.string.normal_settings_time_title))
                .setContentText(bigText)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true);

        mNoMan.notify(TAG_NOTIFICATION, ID_NOTIFICATION_CHANNEL, nb.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void updateConfigurationLocked() {
        if (isEnable) {
            registerTimeChangerEvent();
            scheduleNextCustomTimeListener();
        } else {
            unregisterTimeChangeEvent();
            cancelCustomAlarm();
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return super.onStartCommand(intent, flags, startId);
        }
        isEnable = mShareUtil.getBoolean(MainActivity.KEY_SCHDULE_SWITCH, false);
        int startTime = mShareUtil.getInt(MainActivity.KEY_SCHDULE_START_TIME, DEFAULT_CUSTOM_START_TIME.toSecondOfDay());
        int endTime = mShareUtil.getInt(MainActivity.KEY_SCHDULE_END_TIME, DEFAULT_CUSTOM_END_TIME.toSecondOfDay());

        mCustomAutoStartMilliseconds = LocalTime.ofSecondOfDay(startTime);
        mCustomAutoEndMilliseconds = LocalTime.ofSecondOfDay(endTime);
        updateConfigurationLocked();

        return super.onStartCommand(intent, flags, startId);
    }

    private void scheduleNextCustomTimeListener() {
        if (!isEnable) {
            cancelCustomAlarm();
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        final boolean active = computeCustomPowerSaveMode();
        final  LocalDateTime next = active
                ? getDateTimeAfter(mCustomAutoEndMilliseconds, now)
                : getDateTimeAfter(mCustomAutoStartMilliseconds, now);
        final long millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Log.d(TAG, "scheduleNextCustomTimeListener millis:" + millis
                + ",next:" + next.toString()
                + ",mCustomAutoStartMilliseconds: " + mCustomAutoStartMilliseconds.toString()
                + ", mCustomAutoEndMilliseconds: " + mCustomAutoEndMilliseconds
                + ", now:" + now.toString()
        );

        if(active) {
            showInfo();
        }

        mAlarmManager.setExact(AlarmManager.RTC, millis, TAG, mCustomTimeListener, null);
    }

    private final AlarmManager.OnAlarmListener mCustomTimeListener = new AlarmManager.OnAlarmListener() {
        @Override
        public void onAlarm() {
            synchronized (mLock) {
                Log.d(TAG, "OnAlarmListener");
                updateCustomTimeLocked();
            }
        }
    };

    private final BroadcastReceiver mOnTimeChangerHandle = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (mLock) {
                Log.d(TAG, "Time change");
                updateCustomTimeLocked();
            }
        }
    };

    private void updateCustomTimeLocked() {
        if (isEnable) {
            showInfo();
        }

        scheduleNextCustomTimeListener();
    }

    private void showInfo() {
        final boolean active = computeCustomPowerSaveMode();
        LocalDateTime now = LocalDateTime.now();
        final  LocalDateTime next = active
                ? getDateTimeAfter(mCustomAutoEndMilliseconds, now)
                : getDateTimeAfter(mCustomAutoStartMilliseconds, now);

        // 下一次定时到来时间
        Message msg = new Message();
        msg.obj = getString(R.string.next_alarm_time) + next.toString();
        mHandle.sendMessage(msg);
    }

    private void cancelCustomAlarm() {
        mAlarmManager.cancel(mCustomTimeListener);
    }

    private boolean computeCustomPowerSaveMode() {
        return isTimeBetween(LocalTime.now()
                ,mCustomAutoStartMilliseconds
                ,mCustomAutoEndMilliseconds
        );
    }

    public static boolean isTimeBetween(LocalTime reference, LocalTime start, LocalTime end) {
        //    ////////E----+-----S////////
        if ((reference.isBefore(start) && reference.isAfter(end)
              //    -----+----S//////////E------
              || (reference.isBefore(end) && reference.isBefore(start) && start.isBefore(end))
              //    ---------S//////////E---+---
              || (reference.isAfter(end) && reference.isAfter(start)) && start.isBefore(end))) {
            return false;
        } else {
            return true;
        }
    }

    private LocalDateTime getDateTimeAfter(LocalTime localTime, LocalDateTime compareTime) {
        final LocalDateTime idt = LocalDateTime.of(compareTime.toLocalDate(), localTime);
        // Check if the local time has passed, if so return the same time tomorrow
        return idt.isBefore(compareTime) ? idt.plusDays(1) : idt;
    }

    private synchronized void registerTimeChangerEvent() {
        isRegisterTimeChangerRecevier = true;
        final IntentFilter intentFilter = new IntentFilter(Intent.ACTION_TIME_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(mOnTimeChangerHandle, intentFilter);
    }

    private synchronized void unregisterTimeChangeEvent() {
        if (isRegisterTimeChangerRecevier) {
            return;
        }
        isRegisterTimeChangerRecevier = false;
        try {
            unregisterReceiver(mOnTimeChangerHandle);
        } catch (Exception e) {
            // we ignore this exception if the receviver is unregister already
        }
    }

}
