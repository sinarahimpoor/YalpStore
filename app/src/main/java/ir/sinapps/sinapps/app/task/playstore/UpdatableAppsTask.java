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

package ir.sinapps.sinapps.app.task.playstore;

import android.app.Activity;
import android.text.TextUtils;

import com.github.yeriomin.playstoreapi.GooglePlayAPI;
import ir.sinapps.sinapps.app.BlackWhiteListManager;
import ir.sinapps.sinapps.app.ContextUtil;
import ir.sinapps.sinapps.app.PlayStoreApiAuthenticator;
import ir.sinapps.sinapps.app.PreferenceUtil;
import ir.sinapps.sinapps.app.R;
import ir.sinapps.sinapps.app.VersionIgnoreManager;
import ir.sinapps.sinapps.app.model.App;
import ir.sinapps.sinapps.app.task.InstalledAppsTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UpdatableAppsTask extends RemoteAppListTask {

    protected List<App> updatableApps = new ArrayList<>();

    @Override
    protected List<App> getResult(GooglePlayAPI api, String... packageNames) throws IOException {
        api.toc();
        Map<String, App> installedApps = getInstalledApps();
        for (App appFromMarket: getAppsFromPlayStore(api, filterBlacklistedApps(installedApps).keySet())) {
            String packageName = appFromMarket.getPackageName();
            if (TextUtils.isEmpty(packageName) || !installedApps.containsKey(packageName)) {
                continue;
            }
            App installedApp = installedApps.get(packageName);
            appFromMarket = addInstalledAppInfo(appFromMarket, installedApp);
            if (installedApp.getVersionCode() < appFromMarket.getVersionCode()) {
                updatableApps.add(appFromMarket);
            }
        }
        return updatableApps;
    }

    @Override
    protected void onPostExecute(List<App> result) {
        super.onPostExecute(result);
        Collections.sort(updatableApps);
    }

    @Override
    protected void processIOException(IOException e) {
        super.processIOException(e);
        if (noNetwork(e) && context instanceof Activity) {
            ContextUtil.toast(context, R.string.error_no_network);
        }
    }

    @Override
    protected List<App> getRemoteAppList(GooglePlayAPI api, List<String> packageNames) throws IOException {
        List<App> appList = super.getRemoteAppList(api, packageNames);
        VersionIgnoreManager versionIgnoreManager = new VersionIgnoreManager(context);
        for (App app: appList.toArray(new App[appList.size()])) {
            if (!versionIgnoreManager.isUpdatable(app.getPackageName(), app.getVersionCode())) {
                appList.remove(app);
            }
        }
        return appList;
    }

    private App addInstalledAppInfo(App appFromMarket, App installedApp) {
        if (null != installedApp) {
            appFromMarket.setPackageInfo(installedApp.getPackageInfo());
            appFromMarket.setVersionName(installedApp.getVersionName());
            appFromMarket.setDisplayName(installedApp.getDisplayName());
            appFromMarket.setSystem(installedApp.isSystem());
            appFromMarket.setInstalled(true);
        }
        return appFromMarket;
    }

    private Map<String, App> getInstalledApps() {
        InstalledAppsTask task = new InstalledAppsTask();
        task.setContext(context);
        task.setIncludeSystemApps(true);
        return task.getInstalledApps(false);
    }

    protected List<App> getAppsFromPlayStore(GooglePlayAPI api, Collection<String> packageNames) throws IOException {
        List<App> appsFromPlayStore = new ArrayList<>();
        boolean builtInAccount = PreferenceUtil.getBoolean(context, PlayStoreApiAuthenticator.PREFERENCE_APP_PROVIDED_EMAIL);
        for (App app: getRemoteAppList(api, new ArrayList<>(packageNames))) {
            if (!builtInAccount || app.isFree()) {
                appsFromPlayStore.add(app);
            }
        }
        return appsFromPlayStore;
    }

    private Map<String, App> filterBlacklistedApps(Map<String, App> apps) {
        Set<String> packageNames = new HashSet<>(apps.keySet());
        if (PreferenceUtil.getDefaultSharedPreferences(context).getString(PreferenceUtil.PREFERENCE_UPDATE_LIST_WHITE_OR_BLACK, PreferenceUtil.LIST_BLACK).equals(PreferenceUtil.LIST_BLACK)) {
            packageNames.removeAll(new BlackWhiteListManager(context).get());
        } else {
            packageNames.retainAll(new BlackWhiteListManager(context).get());
        }
        Map<String, App> result = new HashMap<>();
        for (App app: apps.values()) {
            if (packageNames.contains(app.getPackageName())) {
                result.put(app.getPackageName(), app);
            }
        }
        return result;
    }
}
