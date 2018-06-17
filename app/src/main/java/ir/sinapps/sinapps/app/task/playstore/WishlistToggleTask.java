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

import com.github.yeriomin.playstoreapi.GooglePlayAPI;
import ir.sinapps.sinapps.app.PlayStoreApiAuthenticator;
import ir.sinapps.sinapps.app.PreferenceUtil;
import ir.sinapps.sinapps.app.SinAppsApplication;

import java.io.IOException;

public class WishlistToggleTask extends PlayStorePayloadTask<Boolean> implements CloneableTask {

    protected String packageName;

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public CloneableTask clone() {
        WishlistToggleTask task = new WishlistToggleTask();
        task.setPackageName(packageName);
        task.setErrorView(errorView);
        task.setProgressIndicator(progressIndicator);
        task.setContext(context);
        return task;
    }

    @Override
    protected Boolean getResult(GooglePlayAPI api, String... arguments) throws IOException {
        boolean builtInAccount = PreferenceUtil.getBoolean(context, PlayStoreApiAuthenticator.PREFERENCE_APP_PROVIDED_EMAIL);
        if (SinAppsApplication.wishlist.contains(packageName)) {
            if (!builtInAccount) {
                api.removeWishlistApp(packageName);
            }
            SinAppsApplication.wishlist.remove(packageName);
        } else {
            if (!builtInAccount) {
                api.addWishlistApp(packageName);
            }
            SinAppsApplication.wishlist.add(packageName);
        }
        SinAppsApplication.wishlist.save();
        return true;
    }
}
