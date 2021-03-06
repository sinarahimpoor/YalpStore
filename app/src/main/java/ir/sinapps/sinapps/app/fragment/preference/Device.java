/*
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package ir.sinapps.sinapps.app.fragment.preference;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.Preference;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;

import com.github.yeriomin.playstoreapi.PropertiesDeviceInfoProvider;
import ir.sinapps.sinapps.app.ContextUtil;
import ir.sinapps.sinapps.app.DeviceInfoActivity;
import ir.sinapps.sinapps.app.OnListPreferenceChangeListener;
import ir.sinapps.sinapps.app.PlayStoreApiAuthenticator;
import ir.sinapps.sinapps.app.PreferenceActivity;
import ir.sinapps.sinapps.app.PreferenceUtil;
import ir.sinapps.sinapps.app.R;
import ir.sinapps.sinapps.app.SpoofDeviceManager;
import ir.sinapps.sinapps.app.Util;
import ir.sinapps.sinapps.app.SinAppsModelActivity;
import ir.sinapps.sinapps.app.bugreport.BugReportService;
import ir.sinapps.sinapps.app.view.DialogWrapper;
import ir.sinapps.sinapps.app.view.DialogWrapperAbstract;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class Device extends List {

    private static final String PREFERENCE_DEVICE_DEFINITION_REQUESTED = "PREFERENCE_DEVICE_DEFINITION_REQUESTED";

    public Device(PreferenceActivity activity) {
        super(activity);
    }

    @Override
    public void draw() {
        super.draw();
        listPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ContextUtil.toast(
                    activity.getApplicationContext(),
                    R.string.pref_device_to_pretend_to_be_notice,
                    PreferenceUtil.getDefaultSharedPreferences(activity).getString(PreferenceUtil.PREFERENCE_DOWNLOAD_DIRECTORY, "")
                );
                ((AlertDialog) listPreference.getDialog()).getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                        if (position > 0) {
                            Intent i = new Intent(activity, DeviceInfoActivity.class);
                            i.putExtra(DeviceInfoActivity.INTENT_DEVICE_NAME, (String) keyValueMap.keySet().toArray()[position]);
                            activity.startActivity(i);
                        }
                        return false;
                    }
                });
                return false;
            }
        });
    }

    @Override
    protected OnListPreferenceChangeListener getOnListPreferenceChangeListener() {
        OnListPreferenceChangeListener listener = new OnListPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!TextUtils.isEmpty((String) newValue) && !isDeviceDefinitionValid((String) newValue)) {
                    ContextUtil.toast(activity.getApplicationContext(), R.string.error_invalid_device_definition);
                    return false;
                }
                showLogOutDialog();
                return super.onPreferenceChange(preference, newValue);
            }
        };
        listener.setDefaultLabel(activity.getString(R.string.pref_device_to_pretend_to_be_default));
        return listener;
    }

    @Override
    protected Map<String, String> getKeyValueMap() {
        Map<String, String> devices = new SpoofDeviceManager(activity).getDevices();
        devices = Util.sort(devices);
        Util.addToStart(
            (LinkedHashMap<String, String>) devices,
            "",
            activity.getString(R.string.pref_device_to_pretend_to_be_default)
        );
        return devices;
    }

    private boolean isDeviceDefinitionValid(String spoofDevice) {
        PropertiesDeviceInfoProvider deviceInfoProvider = new PropertiesDeviceInfoProvider();
        deviceInfoProvider.setProperties(new SpoofDeviceManager(activity).getProperties(spoofDevice));
        deviceInfoProvider.setLocaleString(Locale.getDefault().toString());
        return deviceInfoProvider.isValid();
    }

    private DialogWrapperAbstract showRequestDialog(boolean logOut) {
        PreferenceUtil.getDefaultSharedPreferences(activity)
            .edit()
            .putBoolean(PREFERENCE_DEVICE_DEFINITION_REQUESTED, true)
            .commit()
        ;
        return new DialogWrapper(activity)
            .setMessage(R.string.dialog_message_spoof_request)
            .setTitle(R.string.dialog_title_spoof_request)
            .setPositiveButton(android.R.string.yes, new FinishingOnClickListener(logOut) {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    send();
                    ContextUtil.toastShort(activity.getApplicationContext(), activity.getString(R.string.thank_you));
                    super.onClick(dialogInterface, i);
                }
            })
            .setNegativeButton(android.R.string.no, new FinishingOnClickListener(logOut))
            .show()
        ;
    }

    private void send() {
        Intent intentBugReport = new Intent(activity.getApplicationContext(), BugReportService.class);
        intentBugReport.setAction(BugReportService.ACTION_SEND_FTP);
        intentBugReport.putExtra(BugReportService.INTENT_MESSAGE, activity.getString(R.string.sent_from_device_definition_dialog));
        activity.startService(intentBugReport);
    }

    private DialogWrapperAbstract showLogOutDialog() {
        return new DialogWrapper(activity)
            .setMessage(R.string.pref_device_to_pretend_to_be_toast)
            .setTitle(R.string.dialog_title_logout)
            .setPositiveButton(android.R.string.yes, new RequestOnClickListener(activity, true))
            .setNegativeButton(R.string.dialog_two_factor_cancel, new RequestOnClickListener(activity, false))
            .show()
        ;
    }

    private void finishAll() {
        new PlayStoreApiAuthenticator(activity.getApplicationContext()).logout();
        SinAppsModelActivity.cascadeFinish();
        activity.finish();
    }

    class RequestOnClickListener implements DialogInterface.OnClickListener {

        private boolean logOut;
        private boolean askedAlready;

        public RequestOnClickListener(Activity activity, boolean logOut) {
            askedAlready = PreferenceUtil.getBoolean(activity, PREFERENCE_DEVICE_DEFINITION_REQUESTED);
            this.logOut = logOut;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.dismiss();
            if (askedAlready) {
                if (logOut) {
                    finishAll();
                }
            } else {
                showRequestDialog(logOut);
            }
        }
    }

    class FinishingOnClickListener implements DialogInterface.OnClickListener {

        private boolean logOut;

        public FinishingOnClickListener(boolean logOut) {
            this.logOut = logOut;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.dismiss();
            if (logOut) {
                finishAll();
            }
        }
    }
}
