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
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.github.yeriomin.playstoreapi.AndroidAppDeliveryData;
import ir.sinapps.sinapps.app.BuildConfig;
import ir.sinapps.sinapps.app.ContextUtil;
import ir.sinapps.sinapps.app.DownloadManagerAbstract;
import ir.sinapps.sinapps.app.DownloadState;
import ir.sinapps.sinapps.app.Downloader;
import ir.sinapps.sinapps.app.ManualDownloadActivity;
import ir.sinapps.sinapps.app.Paths;
import ir.sinapps.sinapps.app.PlayStoreApiAuthenticator;
import ir.sinapps.sinapps.app.PreferenceUtil;
import ir.sinapps.sinapps.app.R;
import ir.sinapps.sinapps.app.SinAppsModelActivity;
import ir.sinapps.sinapps.app.SinAppsPermissionManager;
import ir.sinapps.sinapps.app.model.App;
import ir.sinapps.sinapps.app.selfupdate.UpdaterFactory;
import ir.sinapps.sinapps.app.task.playstore.DownloadLinkTask;
import ir.sinapps.sinapps.app.task.playstore.PurchaseTask;
import ir.tapsell.sdk.Tapsell;
import ir.tapsell.sdk.TapsellAd;
import ir.tapsell.sdk.TapsellAdRequestListener;
import ir.tapsell.sdk.TapsellAdRequestOptions;
import ir.tapsell.sdk.TapsellAdShowListener;
import ir.tapsell.sdk.TapsellRewardListener;
import ir.tapsell.sdk.TapsellShowOptions;

import java.io.File;

import static ir.sinapps.sinapps.app.DownloadState.TriggeredBy.DOWNLOAD_BUTTON;
import static ir.sinapps.sinapps.app.DownloadState.TriggeredBy.MANUAL_DOWNLOAD_BUTTON;

public class ButtonDownload extends Button {

    public ButtonDownload(SinAppsModelActivity activity, App app) {
        super(activity, app);
    }

    @Override
    protected View getButton() {
        return activity.findViewById(R.id.download);
    }

    @Override
    public boolean shouldBeVisible() {
        File apk = Paths.getApkPath(activity, app.getPackageName(), app.getVersionCode());
        return (!apk.exists()
                || apk.length() != app.getSize()
                || !DownloadState.get(app.getPackageName()).isEverythingSuccessful()
            )
            && (app.isFree() || !PreferenceUtil.getBoolean(activity, PlayStoreApiAuthenticator.PREFERENCE_APP_PROVIDED_EMAIL))
            && (app.isInPlayStore() || app.getPackageName().equals(BuildConfig.APPLICATION_ID))
            && (getInstalledVersionCode() != app.getVersionCode() || activity instanceof ManualDownloadActivity)
        ;
    }

    @Override
    protected void onButtonClick(View v) {
        checkAndDownload();
    }

    public void checkAndDownload() {
        SinAppsPermissionManager permissionManager = new SinAppsPermissionManager(activity);
        if (app.getVersionCode() == 0 && !(activity instanceof ManualDownloadActivity)) {
            activity.startActivity(new Intent(activity, ManualDownloadActivity.class));
        } else if (permissionManager.checkPermission()) {
            Log.i(getClass().getSimpleName(), "Write permission granted");

            final TapsellShowOptions showOptions = new TapsellShowOptions();
            showOptions.setBackDisabled(false);
            showOptions.setImmersiveMode(true);
            showOptions.setRotationMode(TapsellShowOptions.ROTATION_UNLOCKED);
            showOptions.setShowDialog(true);
            showOptions.setWarnBackPressedDialogMessage("سلام دوست من بک نزن");
            showOptions.setWarnBackPressedDialogMessageTextColor(Color.RED);
            showOptions.setWarnBackPressedDialogAssetTypefaceFileName("IranNastaliq.ttf");
            showOptions.setWarnBackPressedDialogPositiveButtonText("ادامه بده");
            showOptions.setWarnBackPressedDialogNegativeButtonText("ولم کن بزن بیرون");
            showOptions.setWarnBackPressedDialogPositiveButtonBackgroundResId(R.drawable.button_background);
            showOptions.setWarnBackPressedDialogNegativeButtonBackgroundResId(R.drawable.button_background);
            showOptions.setWarnBackPressedDialogPositiveButtonTextColor(Color.RED);
            showOptions.setWarnBackPressedDialogNegativeButtonTextColor(Color.GREEN);
            showOptions.setWarnBackPressedDialogBackgroundResId(R.drawable.dialog_background);

            Tapsell.requestAd(activity.getBaseContext(), "5b16957cfd8cec0001d4d11e", new TapsellAdRequestOptions(TapsellAdRequestOptions.CACHE_TYPE_STREAMED), new TapsellAdRequestListener() {
                @Override
                public void onError (String error) {
                }

                @Override
                public void onAdAvailable (TapsellAd ad) {
                    ad.show(activity.getBaseContext(), showOptions, new TapsellAdShowListener() {

                        @Override
                        public void onOpened (TapsellAd ad) {
                            Log.e(getClass().getSimpleName(), "on ad opened");
                        }
                        @Override
                        public void onClosed (TapsellAd ad) {
                            Log.e(getClass().getSimpleName(), "on ad opened");
                        }
                    });
                }

                @Override
                public void onNoAdAvailable () {
                }

                @Override
                public void onNoNetwork () {
                }

                @Override
                public void onExpiring (TapsellAd ad) {
                }
            });


            Tapsell.setRewardListener(new TapsellRewardListener() {
                @Override
                public void onAdShowFinished(TapsellAd ad, boolean completed) {
                    // store user reward if ad.isRewardedAd() and completed is true
                    Log.i("onAdShowFinished", String.valueOf(completed));
                    if(completed){

                        download();
                    }

                }
            });

            //download();
            View buttonCancel = activity.findViewById(R.id.cancel);
            if (null != buttonCancel) {
                buttonCancel.setVisibility(View.VISIBLE);
            }
        } else {
            permissionManager.requestPermission();
        }
    }

    @Override
    public void draw() {
        super.draw();
        DownloadState state = DownloadState.get(app.getPackageName());
        if (Paths.getApkPath(activity, app.getPackageName(), app.getVersionCode()).exists()
            && !state.isEverythingSuccessful()
        ) {
            disable(R.string.details_downloading);
        }
        if (null != button) {
            button.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    DownloadLinkTask task = new DownloadLinkTask();
                    task.setApp(app);
                    task.setContext(activity);
                    task.execute();
                    return true;
                }
            });
        }
    }

    public void download() {
        if (app.getPackageName().equals(BuildConfig.APPLICATION_ID)) {
            new Downloader(activity).download(
                app,
                AndroidAppDeliveryData.newBuilder().setDownloadUrl(UpdaterFactory.get(activity).getUrlString(app.getVersionCode())).build()
            );
        } else {
            boolean writePermission = new SinAppsPermissionManager(activity).checkPermission();
            if (writePermission && prepareDownloadsDir()) {
                getPurchaseTask().execute();
            } else {
                ContextUtil.toast(this.activity.getApplicationContext(), R.string.error_downloads_directory_not_writable);
            }
        }
    }

    private boolean prepareDownloadsDir() {
        File dir = Paths.getYalpPath(activity);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.exists() && dir.isDirectory() && dir.canWrite();
    }

    private LocalPurchaseTask getPurchaseTask() {
        LocalPurchaseTask purchaseTask = new LocalPurchaseTask();
        purchaseTask.setFragment(this);
        purchaseTask.setApp(app);
        purchaseTask.setContext(activity);
        purchaseTask.setTriggeredBy(activity instanceof ManualDownloadActivity ? MANUAL_DOWNLOAD_BUTTON : DOWNLOAD_BUTTON);
        purchaseTask.setProgressIndicator(activity.findViewById(R.id.progress));
        return purchaseTask;
    }

    private int getInstalledVersionCode() {
        try {
            return activity.getPackageManager().getPackageInfo(app.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    static class LocalPurchaseTask extends PurchaseTask {

        private ButtonDownload fragment;

        public LocalPurchaseTask setFragment(ButtonDownload fragment) {
            this.fragment = fragment;
            return this;
        }

        @Override
        public LocalPurchaseTask clone() {
            LocalPurchaseTask task = new LocalPurchaseTask();
            task.setTriggeredBy(triggeredBy);
            task.setApp(app);
            task.setErrorView(errorView);
            task.setContext(context);
            task.setProgressIndicator(progressIndicator);
            task.setFragment(fragment);
            return task;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            fragment.disable(R.string.details_downloading);
        }

        @Override
        protected void onPostExecute(AndroidAppDeliveryData deliveryData) {
            super.onPostExecute(deliveryData);
            if (!success()) {
                fragment.draw();
                String restriction = DownloadManagerAbstract.getRestrictionString(context, app.getRestriction());
                if (!TextUtils.isEmpty(restriction)) {
                    ContextUtil.toastLong(context, restriction);
                    Log.i(getClass().getSimpleName(), "No download link returned, app restriction is " + app.getRestriction());
                }
            }
        }
    }

}
