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

package ir.sinapps.sinapps.app.notification;

import android.app.IntentService;
import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import ir.sinapps.sinapps.app.BuildConfig;
import ir.sinapps.sinapps.app.DownloadManagerFactory;
import ir.sinapps.sinapps.app.DownloadManagerInterface;
import ir.sinapps.sinapps.app.DownloadState;
import ir.sinapps.sinapps.app.Paths;
import ir.sinapps.sinapps.app.SinAppsApplication;

import java.util.ArrayList;
import java.util.List;

public class CancelDownloadService extends IntentService {

    static public final String DOWNLOAD_ID = "DOWNLOAD_ID";
    static public final String PACKAGE_NAME = "PACKAGE_NAME";

    private DownloadManagerInterface dm;

    public CancelDownloadService() {
        super("CancelDownloadService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground((int) (System.currentTimeMillis() % 10000), new Notification.Builder(this, BuildConfig.APPLICATION_ID).build());
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        dm = DownloadManagerFactory.get(getApplicationContext());
        long downloadId = intent.getLongExtra(DOWNLOAD_ID, 0L);
        String packageName = intent.getStringExtra(PACKAGE_NAME);
        if (downloadId == 0 && TextUtils.isEmpty(packageName)) {
            Log.w(getClass().getSimpleName(), "No download id or package name provided in the intent");
        }
        List<Long> downloadIds = new ArrayList<>();
        if (downloadId != 0) {
            downloadIds.add(downloadId);
        }
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        ((SinAppsApplication) getApplicationContext()).removePendingUpdate(packageName);
        DownloadState state = DownloadState.get(packageName);
        downloadIds.addAll(state.getDownloadIds());
        for (long id: downloadIds) {
            cancel(id);
        }
        if (null != state.getApp()) {
            Paths.getApkPath(getApplicationContext(), packageName, state.getApp().getVersionCode()).delete();
        }
        state.reset();
    }

    private void cancel(long downloadId) {
        Log.i(getClass().getSimpleName(), "Cancelling download " + downloadId);
        dm.cancel(downloadId);
    }
}
