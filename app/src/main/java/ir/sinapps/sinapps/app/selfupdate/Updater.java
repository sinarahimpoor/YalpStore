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

package ir.sinapps.sinapps.app.selfupdate;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.TrafficStats;
import android.os.Build;

import ir.sinapps.sinapps.app.BuildConfig;
import ir.sinapps.sinapps.app.PreferenceUtil;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import info.guardianproject.netcipher.NetCipher;

abstract public class Updater {

    static private final String CACHED_VERSION_CODE = "CACHED_VERSION_CODE";
    static private final String CACHED_VERSION_CODE_CHECKED_AT = "CACHED_VERSION_CODE_CHECKED_AT";
    static private final long CACHED_VERSION_CODE_VALID_FOR = 60*60;

    protected Context context;

    public Updater(Context context) {
        this.context = context;
    }

    abstract public String getUrlString(int versionCode);

    public int getLatestVersionCode() {
        int latestVersionCode = getCachedVersionCode();
        if (latestVersionCode == 0) {
            latestVersionCode = BuildConfig.VERSION_CODE;
            while (isAvailable(latestVersionCode + 1)) {
                latestVersionCode++;
            }
            cacheVersionCode(latestVersionCode);
        }
        return latestVersionCode;
    }

    private int getCachedVersionCode() {
        SharedPreferences preferences = PreferenceUtil.getDefaultSharedPreferences(context);
        return (System.currentTimeMillis() - preferences.getLong(CACHED_VERSION_CODE_CHECKED_AT, 0)) > CACHED_VERSION_CODE_VALID_FOR
            ? 0
            : preferences.getInt(CACHED_VERSION_CODE, 0)
        ;
    }

    private void cacheVersionCode(int versionCode) {
        SharedPreferences.Editor preferences = PreferenceUtil.getDefaultSharedPreferences(context).edit();
        preferences.putInt(CACHED_VERSION_CODE, versionCode);
        preferences.putLong(CACHED_VERSION_CODE_CHECKED_AT, System.currentTimeMillis());
        preferences.commit();
    }

    private URL getUrl(int versionCode) {
        try {
            return new URL(getUrlString(versionCode));
        } catch (MalformedURLException e) {
            // Unlikely
        }
        return null;
    }

    private boolean isAvailable(int versionCode) {
        try {
            URL url = getUrl(versionCode);
            if (null == url) {
                return false;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                TrafficStats.setThreadStatsTag(Thread.currentThread().hashCode());
            }
            HttpURLConnection connection = NetCipher.getHttpURLConnection(url, true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("HEAD");
            return connection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST;
        } catch (IOException x) {
            return false;
        }
    }
}