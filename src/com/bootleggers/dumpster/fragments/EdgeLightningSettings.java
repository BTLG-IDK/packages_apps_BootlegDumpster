/*
 * Copyright (C) 2014 TeamEos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bootleggers.dumpster.fragments;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import androidx.preference.ListPreference;
import androidx.preference.SwitchPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;

import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;

import com.bootleggers.support.preferences.colorpicker.ColorPickerPreference;
import com.bootleggers.support.preferences.CustomSeekBarPreference;

public class EdgeLightningSettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {

    private static String KEY_DURATION = "ambient_notification_light_duration";
    private static String KEY_REPEATS = "ambient_notification_light_repeats";
    private static String KEY_TIMEOUT = "ambient_notification_light_timeout";
    private static String KEY_COLOR_MODE = "ambient_notification_color_mode";
    private static String KEY_COLOR = "ambient_notification_light_color";

    private CustomSeekBarPreference mDurationPref;
    private CustomSeekBarPreference mRepeatsPref;
    private ListPreference mTimeoutPref;
    private ListPreference mColorModePref;
    private ColorPickerPreference mColorPref;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.bootleg_dumpster_frag_edge_lightning);
        final ContentResolver resolver = getContentResolver();
        final int accentColor = getAccentColor();

        mDurationPref = (CustomSeekBarPreference) findPreference(KEY_DURATION);
        int value = Settings.System.getIntForUser(resolver,
                KEY_DURATION, 2, UserHandle.USER_CURRENT);
        mDurationPref.setValue(value);
        mDurationPref.setOnPreferenceChangeListener(this);

        mRepeatsPref = (CustomSeekBarPreference) findPreference(KEY_REPEATS);
        int repeats = Settings.System.getIntForUser(resolver,
                KEY_REPEATS, 0, UserHandle.USER_CURRENT);
        mRepeatsPref.setValue(repeats);
        mRepeatsPref.setOnPreferenceChangeListener(this);

        mTimeoutPref = (ListPreference) findPreference(KEY_TIMEOUT);
        value = Settings.System.getIntForUser(resolver,
                KEY_TIMEOUT, accentColor, UserHandle.USER_CURRENT);
        mTimeoutPref.setValue(Integer.toString(value));
        mTimeoutPref.setSummary(mTimeoutPref.getEntry());
        mTimeoutPref.setOnPreferenceChangeListener(this);
        updateTimeoutEnablement(repeats);

        mColorPref = (ColorPickerPreference) findPreference(KEY_COLOR);
        value = Settings.System.getIntForUser(resolver,
                KEY_COLOR, accentColor, UserHandle.USER_CURRENT);
        mColorPref.setDefaultColor(accentColor);
        String colorHex = ColorPickerPreference.convertToRGB(value);
        if (value == accentColor) {
            mColorPref.setSummary(R.string.default_string);
        } else {
            mColorPref.setSummary(colorHex);
        }
        mColorPref.setNewPreviewColor(value);
        mColorPref.setOnPreferenceChangeListener(this);

        mColorModePref = (ListPreference) findPreference(KEY_COLOR_MODE);
        value = Settings.System.getIntForUser(resolver,
                KEY_COLOR_MODE, 0, UserHandle.USER_CURRENT);
        mColorModePref.setValue(Integer.toString(value));
        mColorModePref.setSummary(mColorModePref.getEntry());
        mColorModePref.setOnPreferenceChangeListener(this);
        mColorPref.setEnabled(value == 3);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mDurationPref) {
            int value = (Integer) newValue;
            Settings.System.putIntForUser(resolver,
                    KEY_DURATION, value, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mRepeatsPref) {
            int value = (Integer) newValue;
            Settings.System.putIntForUser(resolver,
                    KEY_REPEATS, value, UserHandle.USER_CURRENT);
            updateTimeoutEnablement(value);
            return true;
        } else if (preference == mTimeoutPref) {
            int value = Integer.valueOf((String) newValue);
            int index = mTimeoutPref.findIndexOfValue((String) newValue);
            mTimeoutPref.setSummary(mTimeoutPref.getEntries()[index]);
            Settings.System.putIntForUser(resolver,
                    KEY_TIMEOUT, value, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mColorModePref) {
            int value = Integer.valueOf((String) newValue);
            int index = mColorModePref.findIndexOfValue((String) newValue);
            mColorModePref.setSummary(mColorModePref.getEntries()[index]);
            Settings.System.putIntForUser(resolver,
                    KEY_COLOR_MODE, value, UserHandle.USER_CURRENT);
            mColorPref.setEnabled(value == 3);
            return true;
        } else if (preference == mColorPref) {
            int accentColor = getAccentColor();
            String colorHex = ColorPickerPreference.convertToRGB(value);
                    Integer.valueOf(String.valueOf(newValue)));
            if (hex.equals("#3980ff")) {
                preference.setSummary(R.string.default_string);
            } else {
                preference.setSummary(hex);
            }
            int color = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putIntForUser(resolver,
                    KEY_COLOR, color, UserHandle.USER_CURRENT);
            return true;
        }
        return false;
    }

    private int getAccentColor() {
        final TypedValue value = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.colorAccent, value, true);
        return value.data;
    }

    private void updateTimeoutEnablement(int repeats) {
        if (repeats == 0) {
            int value = Settings.System.getIntForUser(getContentResolver(),
                    KEY_TIMEOUT, 0, UserHandle.USER_CURRENT);
            mTimeoutPref.setValue(Integer.toString(value));
            mTimeoutPref.setSummary(mTimeoutPref.getEntry());
            mTimeoutPref.setEnabled(true);
        } else {
            mTimeoutPref.setSummary(R.string.set_to_zero);
            mTimeoutPref.setEnabled(false);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.BOOTLEG;
    }
}
