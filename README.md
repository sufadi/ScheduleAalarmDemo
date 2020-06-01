定时器demo
7/100
保存草稿
发布文章
su749520

## 1. 定时器 UI效果
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200601203923609.gif)
## 2. Demo下载地址
#### 2.1 GitHub
https://github.com/sufadi/ScheduleAalarmDemo

#### 2.2 CSDN 下载
https://download.csdn.net/download/su749520/12486117

## 3. 运用实践
https://blog.csdn.net/su749520/article/details/106446177

## 4. 技术简要
#### 4.1 时间选择器
主要使用TimePick进行定时时间选择，并且需要同时支持12/24小时制，时间设置后在 onTimeSet中取出数值（注意由于要支持12/24小时制，故保存的时间值为1天的秒数（toSecondOfDay）），保存到数据库，并开始调用Alarm定时接口进行闹钟设置。

```
    private Dialog creatTimePickerDialog(final Context context, final int dialogId) {
        LocalTime mInitTime = null;
        if(dialogId == DIALOG_START_TIME) {
            mInitTime = getSchduleStartLocalTime();
        } else {
            mInitTime = getSchduleEndLocalTime();
        }
        Log.d(TAG, "creatTimePickerDialog mInitTime: " + mInitTime.toSecondOfDay()
                + " : " + mInitTime.toString());

        boolean is24HourFormat = android.text.format.DateFormat.is24HourFormat(context);
        return new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() {
            LocalTime mTempTime = dialogId == DIALOG_START_TIME ? getSchduleEndLocalTime() : getSchduleStartLocalTime();

            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                LocalTime mCurLocalTime = LocalTime.of(hourOfDay, minute);
                if (mCurLocalTime.toSecondOfDay() == mTempTime.toSecondOfDay()) {
                    Toast.makeText(context,getResources().getString(R.string.normal_settings_time_dialog_tip),Toast.LENGTH_SHORT).show();
                } else {
                    if (dialogId == DIALOG_START_TIME) {
                        setSchduleStartTime(mCurLocalTime.toSecondOfDay());
                    } else if (dialogId == DIALOG_END_TIME) {
                        setSchduleEndTime(mCurLocalTime.toSecondOfDay());
                    }
                    updateTimeSummaryUI();
                    Log.d(TAG, "onTimeSet dialogId:" + dialogId + ", SecondOfDay" + mCurLocalTime.toSecondOfDay());
                    startService(new Intent(mContext, AlarmReceviceService.class));
                }
            }
        }, mInitTime.getHour(), mInitTime.getMinute(), is24HourFormat);
    }
```

#### 4.3 AlarmManager API 使用
##### 4.3.1 根据当前时间，计算下一个闹钟时间戳
这里先看判断当前的时间戳是否满足定时开始和结束的实际段内

```
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
```
如果当前时间大于定时开始时间，则设置下一个定时为定时结束时刻，如果当前时间还未大于定时开始时间，则设置下一个定时为定时开始时间
```
    private void scheduleNextCustomTimeListener() {
       ....
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

        mAlarmManager.setExact(AlarmManager.RTC, millis, TAG, mCustomTimeListener, null);
    }
```


##### 4.3.2 重复调用Alarm API接口，达到每天重复闹钟效果
为了保证定时的准度我们使用setExact接口并设置AlarmManager.RTC唤醒，防止手机进入IDLE闹钟不生效，且Android的API接口的设置重复闹钟不起作用，故一般我们调用setExact接口，当接受到定时后，再重复设置一次setExact
```
        mAlarmManager.setExact(AlarmManager.RTC, millis, TAG, mCustomTimeListener, null);

    private final AlarmManager.OnAlarmListener mCustomTimeListener = new AlarmManager.OnAlarmListener() {
        @Override
        public void onAlarm() {
            synchronized (mLock) {
                Log.d(TAG, "OnAlarmListener");
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
```
#### 5 服务处于后台可能会出现alarm事件被拦截
现在的Android系统对后台应用及其服务管控很严格，待机一段时间会禁止后台应用活动。主要是根据应用退到后台的进程优先级变化进行是否拦截限制。故例如闹钟 应用主要是进行一个常驻消息通知进行保持系统adj值为可感知类型，一般adj为200内的进程，可以让应用或服务正常保活。
1. 定时器 UI效果
在这里插入图片描述

2. Demo下载地址
2.1 GitHub
https://github.com/sufadi/ScheduleAalarmDemo

2.2 CSDN 下载
https://download.csdn.net/download/su749520/12486117

3. 运用实践
https://blog.csdn.net/su749520/article/details/106446177

4. 技术简要
4.1 时间选择器
主要使用TimePick进行定时时间选择，并且需要同时支持12/24小时制，时间设置后在 onTimeSet中取出数值（注意由于要支持12/24小时制，故保存的时间值为1天的秒数（toSecondOfDay）），保存到数据库，并开始调用Alarm定时接口进行闹钟设置。

    private Dialog creatTimePickerDialog(final Context context, final int dialogId) {
        LocalTime mInitTime = null;
        if(dialogId == DIALOG_START_TIME) {
            mInitTime = getSchduleStartLocalTime();
        } else {
            mInitTime = getSchduleEndLocalTime();
        }
        Log.d(TAG, "creatTimePickerDialog mInitTime: " + mInitTime.toSecondOfDay()
                + " : " + mInitTime.toString());

        boolean is24HourFormat = android.text.format.DateFormat.is24HourFormat(context);
        return new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() {
            LocalTime mTempTime = dialogId == DIALOG_START_TIME ? getSchduleEndLocalTime() : getSchduleStartLocalTime();

            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                LocalTime mCurLocalTime = LocalTime.of(hourOfDay, minute);
                if (mCurLocalTime.toSecondOfDay() == mTempTime.toSecondOfDay()) {
                    Toast.makeText(context,getResources().getString(R.string.normal_settings_time_dialog_tip),Toast.LENGTH_SHORT).show();
                } else {
                    if (dialogId == DIALOG_START_TIME) {
                        setSchduleStartTime(mCurLocalTime.toSecondOfDay());
                    } else if (dialogId == DIALOG_END_TIME) {
                        setSchduleEndTime(mCurLocalTime.toSecondOfDay());
                    }
                    updateTimeSummaryUI();
                    Log.d(TAG, "onTimeSet dialogId:" + dialogId + ", SecondOfDay" + mCurLocalTime.toSecondOfDay());
                    startService(new Intent(mContext, AlarmReceviceService.class));
                }
            }
        }, mInitTime.getHour(), mInitTime.getMinute(), is24HourFormat);
    }
4.3 AlarmManager API 使用
4.3.1 根据当前时间，计算下一个闹钟时间戳
这里先看判断当前的时间戳是否满足定时开始和结束的实际段内

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
如果当前时间大于定时开始时间，则设置下一个定时为定时结束时刻，如果当前时间还未大于定时开始时间，则设置下一个定时为定时开始时间

    private void scheduleNextCustomTimeListener() {
       ....
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

        mAlarmManager.setExact(AlarmManager.RTC, millis, TAG, mCustomTimeListener, null);
    }
4.3.2 重复调用Alarm API接口，达到每天重复闹钟效果
为了保证定时的准度我们使用setExact接口并设置AlarmManager.RTC唤醒，防止手机进入IDLE闹钟不生效，且Android的API接口的设置重复闹钟不起作用，故一般我们调用setExact接口，当接受到定时后，再重复设置一次setExact

        mAlarmManager.setExact(AlarmManager.RTC, millis, TAG, mCustomTimeListener, null);

    private final AlarmManager.OnAlarmListener mCustomTimeListener = new AlarmManager.OnAlarmListener() {
        @Override
        public void onAlarm() {
            synchronized (mLock) {
                Log.d(TAG, "OnAlarmListener");
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
5 服务处于后台可能会出现alarm事件被拦截
现在的Android系统对后台应用及其服务管控很严格，待机一段时间会禁止后台应用活动。主要是根据应用退到后台的进程优先级变化进行是否拦截限制。故例如闹钟 应用主要是进行一个常驻消息通知进行保持系统adj值为可感知类型，一般adj为200内的进程，可以让应用或服务正常保活。
