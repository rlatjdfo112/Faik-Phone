package com.boxfox.faikphone.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.boxfox.faikphone.R;
import com.boxfox.faikphone.data.PhoneStatus;
import com.boxfox.faikphone.utils.DensityConverter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.realm.Realm;

/**
 * Created by BeINone on 2017-04-04.
 */

public class FakeStatusBarService extends Service {

    private PhoneStatus phoneStatus;
    private WindowManager mWindowManager;
    private View mFakeStatusBar;
    private TextView mTimeTV, mMobileCarrierTV;

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);
        phoneStatus = Realm.getDefaultInstance().where(PhoneStatus.class).findFirst();
        if (phoneStatus.useStatusBar()) {
            showFakeStatusBar();
        } else {
            hideFakeStatusBar();
        }

        registerReceiver(mPreferenceChangedReceiver, new IntentFilter(getString(R.string.preferences_changed_broadcast)));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFakeStatusBar != null && mFakeStatusBar.isShown())
            mWindowManager.removeViewImmediate(mFakeStatusBar);
        if (mTimeChangedReceiver != null) {
            try {
                unregisterReceiver(mTimeChangedReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void showFakeStatusBar() {
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        mFakeStatusBar = layoutInflater.inflate(R.layout.view_fake_status_bar, null);

        mTimeTV = (TextView) mFakeStatusBar.findViewById(R.id.tv_time);
        mMobileCarrierTV = (TextView) mFakeStatusBar.findViewById(R.id.tv_mobile_carrier);
        mMobileCarrierTV.setText(phoneStatus.getMobileCarrier());
        setCurrentTime();

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                (int) DensityConverter.dpToPx(this, 26),
                // Allows the view to be on top of the StatusBar
                WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                // Keeps the button presses from going to the background window
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        // pass touch event to real status bar behind this view
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        // Enables the notification to receive touch events
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        // Draws over status bar
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.CENTER;

        mFakeStatusBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                mWindowManager.addView(mFakeStatusBar, params);
            } else {
                Toast.makeText(this, "권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        } else {
            mWindowManager.addView(mFakeStatusBar, params);
        }

        registerReceiver(mTimeChangedReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    private void hideFakeStatusBar() {
        if (mFakeStatusBar != null && mFakeStatusBar.isShown())
            mWindowManager.removeViewImmediate(mFakeStatusBar);
    }

    private void setCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm", Locale.KOREA);
        String currentTime = sdf.format(new Date());
        mTimeTV.setText(currentTime);
    }

    private BroadcastReceiver mTimeChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_TIME_TICK)) {
                setCurrentTime();
            }
        }
    };

    private BroadcastReceiver mPreferenceChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(getString(R.string.preferences_changed_broadcast))) {
                if (phoneStatus.mode() && phoneStatus.useStatusBar()) {
                    if (mFakeStatusBar != null && mFakeStatusBar.isShown())
                        showFakeStatusBar();
                    else
                        mMobileCarrierTV.setText(phoneStatus.getMobileCarrier());
                } else {
                    hideFakeStatusBar();
                }
            }
        }
    };
}
