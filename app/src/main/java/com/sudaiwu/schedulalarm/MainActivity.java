package com.sudaiwu.schedulalarm;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.DateFormat;
import java.time.LocalTime;
import java.util.Calendar;

import java.util.TimeZone;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = "shz_debug";

    private static final int DIALOG_START_TIME = 0;
    private static final int DIALOG_END_TIME = 1;

    public static final String KEY_SCHDULE_SWITCH = "key_schdule_switch";
    public static final String KEY_SCHDULE_START_TIME = "key_schdule_start_time";
    public static final String KEY_SCHDULE_END_TIME = "key_schdule_end_time";


    private Context mContext;
    private ShareUtil mShareUtil;

    private Dialog mStartDialog;
    private Dialog mEndDialog;

    private Switch normal_settings_switch;
    private LinearLayout normal_settings_schedule;
    private LinearLayout normal_settings_schedule_start;
    private LinearLayout normal_settings_schedule_end;
    private TextView normal_settings_schedule_start_tv;
    private TextView normal_settings_schedule_end_tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initVaulue();
        initListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void initViews() {
        normal_settings_switch = findViewById(R.id.normal_settings_switch);
        normal_settings_schedule = findViewById(R.id.normal_settings_schedule);
        normal_settings_schedule_start = findViewById(R.id.normal_settings_schedule_start);
        normal_settings_schedule_end = findViewById(R.id.normal_settings_schedule_end);
        normal_settings_schedule_start_tv = findViewById(R.id.normal_settings_schedule_start_tv);
        normal_settings_schedule_end_tv = findViewById(R.id.normal_settings_schedule_end_tv);
    }

    private void initVaulue() {
        mContext = this;
        mShareUtil = new ShareUtil(this);
    }

    private void initListeners() {
        normal_settings_schedule.setOnClickListener(this);
        normal_settings_schedule_start.setOnClickListener(this);
        normal_settings_schedule_end.setOnClickListener(this);
    }

    private void updateUI() {
        updateEnableStatueUI();
        updateTimeSummaryUI();
    }

    private void updateEnableStatueUI() {
        boolean isEnable = mShareUtil.getBoolean(KEY_SCHDULE_SWITCH, false);
        normal_settings_switch.setChecked(isEnable);
        normal_settings_schedule_start.setEnabled(isEnable);
        normal_settings_schedule_end.setEnabled(isEnable);
        Log.d(TAG, "updateEnableStatueUI isEnable:" + isEnable);
    }

    private void updateTimeSummaryUI() {
        String startTime = getFormattedTimeString(LocalTime.ofSecondOfDay(getSchduleStartTime()));
        String endTime = getFormattedTimeString(LocalTime.ofSecondOfDay(getSchduleEndTime()));
        normal_settings_schedule_start_tv.setText(startTime);
        normal_settings_schedule_end_tv.setText(endTime);
        Log.d(TAG, "updateTimeSummaryUI startTime:" + startTime + ", endTime:" + endTime);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.normal_settings_schedule:
                normal_settings_switch.toggle();
                setSchduleSwitchSettings(normal_settings_switch.isChecked());
                updateEnableStatueUI();
                startService(new Intent(mContext, AlarmReceviceService.class));
                break;
            case R.id.normal_settings_schedule_start:
                mStartDialog = creatTimePickerDialog(mContext, DIALOG_START_TIME);
                mStartDialog.show();
                break;
            case R.id.normal_settings_schedule_end:
                mEndDialog = creatTimePickerDialog(mContext, DIALOG_END_TIME);
                mEndDialog.show();
                break;
        }
    }

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


    private void setSchduleSwitchSettings(boolean isEnable) {
        mShareUtil.setShare(KEY_SCHDULE_SWITCH, isEnable);
    }

    private void setSchduleStartTime(int secondOfDay) {
        mShareUtil.setShare(KEY_SCHDULE_START_TIME, secondOfDay);
    }

    private int getSchduleStartTime(){
        return mShareUtil.getInt(KEY_SCHDULE_START_TIME, AlarmReceviceService.DEFAULT_CUSTOM_START_TIME.toSecondOfDay());
    }

    private LocalTime getSchduleStartLocalTime() {
        return LocalTime.ofSecondOfDay(getSchduleStartTime());
    }

    private void setSchduleEndTime(int secondOfDay) {
        mShareUtil.setShare(KEY_SCHDULE_END_TIME, secondOfDay);
    }

    private int getSchduleEndTime(){
        return mShareUtil.getInt(KEY_SCHDULE_END_TIME, AlarmReceviceService.DEFAULT_CUSTOM_END_TIME.toSecondOfDay());
    }

    private LocalTime getSchduleEndLocalTime(){
        return LocalTime.ofSecondOfDay(getSchduleEndTime());
    }

    private String getFormattedTimeString(LocalTime localTime) {
        DateFormat mTimeFormatter = android.text.format.DateFormat.getTimeFormat(this);
        mTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        Calendar c = Calendar.getInstance();
        c.setTimeZone(mTimeFormatter.getTimeZone());
        c.set(Calendar.HOUR_OF_DAY, localTime.getHour());
        c.set(Calendar.MINUTE, localTime.getMinute());
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return mTimeFormatter.format(c.getTime());
    }
}
