package com.bootleggers.dumpster.fragments;

import com.android.internal.logging.nano.MetricsProto;

import android.os.Bundle;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.content.ContentResolver;
import android.content.res.Resources;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.DeviceConfig;
import com.android.settings.R;

import java.util.Locale;
import android.text.TextUtils;
import android.view.View;

import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.util.bootleg.BootlegUtils;
import com.android.settings.Utils;
import android.util.Log;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class StatusBarSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String COMBINED_STATUSBAR_ICONS = "show_combined_status_bar_signal_icons";
    private static final String CONFIG_RESOURCE_NAME = "flag_combined_status_bar_signal_icons";
    private static final String SYSTEMUI_PACKAGE = "com.android.systemui";
    private static final String STATUS_BAR_SHOW_BATTERY_PERCENT = "status_bar_show_battery_percent";
    private static final String STATUS_BAR_BATTERY_STYLE = "status_bar_battery_style";

    private static final int BATTERY_STYLE_PORTRAIT = 0;
    private static final int BATTERY_STYLE_TEXT = 4;
    private static final int BATTERY_STYLE_HIDDEN = 5;
    private static final int BATTERY_PERCENT_HIDDEN = 0;
    private static final int BATTERY_PERCENT_SHOW = 2;

    private SwitchPreference mCombinedIcons;
    private ListPreference mBatteryPercent;
    private ListPreference mBatteryStyle;
    private int mBatteryPercentValue;
    private int mBatteryPercentValuePrev;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.bootleg_dumpster_frag_status_bar);

        ContentResolver resolver = getActivity().getContentResolver();
        PreferenceScreen prefSet = getPreferenceScreen();

        mCombinedIcons = (SwitchPreference)
                findPreference(COMBINED_STATUSBAR_ICONS);
        Resources sysUIRes = null;
        boolean def = false;
        int resId = 0;
        try {
            sysUIRes = getActivity().getPackageManager()
                    .getResourcesForApplication(SYSTEMUI_PACKAGE);
        } catch (Exception ignored) {
            // If you don't have system UI you have bigger issues
        }
        if (sysUIRes != null) {
            resId = sysUIRes.getIdentifier(
                    CONFIG_RESOURCE_NAME, "bool", SYSTEMUI_PACKAGE);
            if (resId != 0) def = sysUIRes.getBoolean(resId);
        }
        boolean enabled = Settings.Secure.getInt(resolver,
                COMBINED_STATUSBAR_ICONS, def ? 1 : 0) == 1;
        mCombinedIcons.setChecked(enabled);
        mCombinedIcons.setOnPreferenceChangeListener(this);

        int batterystyle = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STYLE, BATTERY_STYLE_PORTRAIT, UserHandle.USER_CURRENT);

        mBatteryStyle = (ListPreference) findPreference(STATUS_BAR_BATTERY_STYLE);
        mBatteryStyle.setValue(String.valueOf(batterystyle));
        mBatteryStyle.setSummary(mBatteryStyle.getEntry());
        mBatteryStyle.setOnPreferenceChangeListener(this);

        mBatteryPercentValue = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, 0, UserHandle.USER_CURRENT);
        mBatteryPercentValuePrev = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT + "_prev", -1, UserHandle.USER_CURRENT);

        mBatteryPercent = (ListPreference) findPreference(STATUS_BAR_SHOW_BATTERY_PERCENT);
        mBatteryPercent.setValue(String.valueOf(mBatteryPercentValue));
        mBatteryPercent.setSummary(mBatteryPercent.getEntry());
        mBatteryPercent.setOnPreferenceChangeListener(this);

        updateBatteryOptions(batterystyle, mBatteryPercentValue);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mCombinedIcons) {
            boolean enabled = (boolean) newValue;
            Settings.Secure.putInt(resolver,
                    COMBINED_STATUSBAR_ICONS, enabled ? 1 : 0);
            return true;
        } else if (preference == mBatteryStyle) {
            int batterystyle = Integer.parseInt((String) newValue);
            updateBatteryOptions(batterystyle, mBatteryPercentValue);
            int index = mBatteryStyle.findIndexOfValue((String) newValue);
            mBatteryStyle.setSummary(mBatteryStyle.getEntries()[index]);
            return true;
        } else if (preference == mBatteryPercent) {
            mBatteryPercentValue = Integer.parseInt((String) newValue);
            Settings.System.putIntForUser(resolver,
                    Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, mBatteryPercentValue,
                    UserHandle.USER_CURRENT);
            int index = mBatteryPercent.findIndexOfValue((String) newValue);
            mBatteryPercent.setSummary(mBatteryPercent.getEntries()[index]);
            return true;
        }
        return false;
    }

    private void updateBatteryOptions(int batterystyle, int batterypercent) {
        ContentResolver resolver = getActivity().getContentResolver();
        switch (batterystyle) {
            case BATTERY_STYLE_TEXT:
            handleTextPercentage(BATTERY_PERCENT_SHOW);
            break;
            case BATTERY_STYLE_HIDDEN:
            handleTextPercentage(BATTERY_PERCENT_HIDDEN);
            break;
            default:
            mBatteryPercent.setEnabled(true);
            if (mBatteryPercentValuePrev != -1) {
                Settings.System.putIntForUser(resolver,
                    Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT,
                    mBatteryPercentValuePrev, UserHandle.USER_CURRENT);
                Settings.System.putIntForUser(resolver,
                    Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT + "_prev",
                    -1, UserHandle.USER_CURRENT);
                mBatteryPercentValue = mBatteryPercentValuePrev;
                mBatteryPercentValuePrev = -1;
                int index = mBatteryPercent.findIndexOfValue(String.valueOf(mBatteryPercentValue));
                mBatteryPercent.setSummary(mBatteryPercent.getEntries()[index]);
            }

            Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_STYLE, batterystyle,
                UserHandle.USER_CURRENT);
            break;
        }
    }

    private void handleTextPercentage(int batterypercent) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (mBatteryPercentValuePrev == -1) {
            mBatteryPercentValuePrev = mBatteryPercentValue;
            Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT + "_prev",
                mBatteryPercentValue, UserHandle.USER_CURRENT);
        }

        Settings.System.putIntForUser(resolver,
            Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT,
            batterypercent, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
            Settings.System.STATUS_BAR_BATTERY_STYLE, BATTERY_STYLE_TEXT,
            UserHandle.USER_CURRENT);
        int index = mBatteryPercent.findIndexOfValue(String.valueOf(batterypercent));
        mBatteryPercent.setSummary(mBatteryPercent.getEntries()[index]);
        mBatteryPercent.setEnabled(false);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.BOOTLEG;
    }

}
