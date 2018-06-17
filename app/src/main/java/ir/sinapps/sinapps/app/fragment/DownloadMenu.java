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

package ir.sinapps.sinapps.app.fragment;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;

import ir.sinapps.sinapps.app.BlackWhiteListManager;
import ir.sinapps.sinapps.app.BuildConfig;
import ir.sinapps.sinapps.app.ManualDownloadActivity;
import ir.sinapps.sinapps.app.R;
import ir.sinapps.sinapps.app.VersionIgnoreManager;
import ir.sinapps.sinapps.app.YalpStoreActivity;
import ir.sinapps.sinapps.app.YalpStoreApplication;
import ir.sinapps.sinapps.app.model.App;
import ir.sinapps.sinapps.app.task.CheckShellTask;
import ir.sinapps.sinapps.app.task.ConvertToNormalTask;
import ir.sinapps.sinapps.app.task.ConvertToSystemTask;
import ir.sinapps.sinapps.app.task.CopyApkTask;
import ir.sinapps.sinapps.app.task.SystemRemountTask;
import ir.sinapps.sinapps.app.task.playstore.WishlistToggleTask;
import ir.sinapps.sinapps.app.view.FlagDialogBuilder;

public class DownloadMenu extends Abstract {

    public DownloadMenu(YalpStoreActivity activity, App app) {
        super(activity, app);
    }

    @Override
    public void draw() {
        final View more = activity.findViewById(R.id.more);
        if (null == more || more instanceof ViewStub) {
            return;
        }
        activity.registerForContextMenu(more);
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                more.showContextMenu();
            }
        });
    }

    public void inflate(Menu menu) {
        MenuInflater inflater = activity.getMenuInflater();
        inflater.inflate(R.menu.menu_download, menu);
        onCreateOptionsMenu(menu);
    }

    public void onCreateOptionsMenu(Menu menu) {
        if (!app.isInstalled()) {
            show(menu, R.id.action_wishlist_add, !YalpStoreApplication.wishlist.contains(app.getPackageName()));
            show(menu, R.id.action_wishlist_remove, YalpStoreApplication.wishlist.contains(app.getPackageName()));
        } else {
            BlackWhiteListManager manager = new BlackWhiteListManager(activity);
            boolean isContained = manager.contains(app.getPackageName());
            if (manager.isBlack()) {
                show(menu, R.id.action_ignore, true);
            } else {
                show(menu, R.id.action_whitelist, true);
            }
            setChecked(menu, R.id.action_ignore, isContained);
            setChecked(menu, R.id.action_whitelist, isContained);
            if (app.getInstalledVersionCode() > 0 && app.getVersionCode() > app.getInstalledVersionCode()) {
                show(menu, R.id.action_ignore_this, true);
                setChecked(menu, R.id.action_ignore_this, !new VersionIgnoreManager(activity).isUpdatable(app.getPackageName(), app.getVersionCode()));
            }
            if (isConvertible(app)) {
                show(menu, R.id.action_make_system, !app.isSystem());
                show(menu, R.id.action_make_normal, app.isSystem());
            }
            show(menu, R.id.action_flag, app.isInPlayStore());
        }
        show(menu, R.id.action_manual, true);
        show(menu, R.id.action_get_local_apk, app.isInstalled());
    }

    private void setChecked(Menu menu, int itemId, boolean checked) {
        MenuItem item = menu.findItem(itemId);
        if (null != item) {
            item.setChecked(checked);
        }
    }

    private void show(Menu menu, int itemId, boolean show) {
        MenuItem item = menu.findItem(itemId);
        if (null != item) {
            item.setVisible(show);
        }
    }

    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_ignore:
            case R.id.action_whitelist:
                BlackWhiteListManager manager = new BlackWhiteListManager(activity);
                if (item.isChecked()) {
                    manager.remove(app.getPackageName());
                } else {
                    manager.add(app.getPackageName());
                }
                item.setChecked(!item.isChecked());
                return true;
            case R.id.action_manual:
                activity.startActivity(new Intent(activity, ManualDownloadActivity.class));
                return true;
            case R.id.action_get_local_apk:
                new CopyApkTask(activity).execute(app);
                return true;
            case R.id.action_make_system:
                checkAndExecute(new ConvertToSystemTask(activity, app));
                return true;
            case R.id.action_make_normal:
                checkAndExecute(new ConvertToNormalTask(activity, app));
                return true;
            case R.id.action_flag:
                new FlagDialogBuilder().setActivity(activity).setApp(app).build().show();
                return true;
            case R.id.action_wishlist_add:
            case R.id.action_wishlist_remove:
                WishlistToggleTask task = new WishlistToggleTask();
                task.setContext(activity);
                task.setPackageName(app.getPackageName());
                task.execute();
                return true;
            case R.id.action_ignore_this:
                if (item.isChecked()) {
                    new VersionIgnoreManager(activity).remove(app.getPackageName(), app.getVersionCode());
                } else {
                    new VersionIgnoreManager(activity).add(app.getPackageName(), app.getVersionCode());
                }
                item.setChecked(!item.isChecked());
                return true;
        }
        return false;
    }

    private void checkAndExecute(SystemRemountTask primaryTask) {
        CheckShellTask checkShellTask = new CheckShellTask(activity);
        checkShellTask.setPrimaryTask(primaryTask);
        checkShellTask.execute();
    }

    private boolean isConvertible(App app) {
        return isInstalled(app)
            && !app.getPackageName().equals(BuildConfig.APPLICATION_ID)
            && null != app.getPackageInfo().applicationInfo
            && null != app.getPackageInfo().applicationInfo.sourceDir
            && !app.getPackageInfo().applicationInfo.sourceDir.endsWith("pkg.apk")
        ;
    }

    private boolean isInstalled(App app) {
        try {
            activity.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
