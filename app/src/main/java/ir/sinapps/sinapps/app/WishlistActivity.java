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

package ir.sinapps.sinapps.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.TextView;

import com.github.yeriomin.playstoreapi.BulkDetailsEntry;
import com.github.yeriomin.playstoreapi.GooglePlayAPI;
import ir.sinapps.sinapps.app.model.App;
import ir.sinapps.sinapps.app.model.AppBuilder;
import ir.sinapps.sinapps.app.task.playstore.CloneableTask;
import ir.sinapps.sinapps.app.task.playstore.WishlistUpdateTask;
import ir.sinapps.sinapps.app.view.ListItem;
import ir.sinapps.sinapps.app.view.SearchResultAppBadge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WishlistActivity extends AppListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
        loadApps();
    }

    @Override
    public void loadApps() {
        WishlistAppsTask task = new WishlistAppsTask(this);
        task.setProgressIndicator(findViewById(R.id.progress));
        task.setErrorView((TextView) getListView().getEmptyView());
        task.execute();
    }

    @Override
    protected ListItem buildListItem(App app) {
        SearchResultAppBadge appBadge = new SearchResultAppBadge();
        appBadge.setApp(app);
        return appBadge;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        boolean result = super.onContextItemSelected(item);
        if (item.getItemId() == R.id.action_wishlist_remove) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            DetailsActivity.app = getAppByListPosition(info.position);
            removeApp(DetailsActivity.app.getPackageName());
        }
        return result;
    }

    private static class WishlistAppsTask extends WishlistUpdateTask implements CloneableTask {

        public WishlistAppsTask(WishlistActivity activity) {
            setContext(activity);
        }

        @Override
        public CloneableTask clone() {
            WishlistAppsTask task = new WishlistAppsTask((WishlistActivity) context);
            task.setErrorView(errorView);
            task.setProgressIndicator(progressIndicator);
            task.setContext(context);
            return task;
        }

        @Override
        protected List<String> getResult(GooglePlayAPI api, String... arguments) throws IOException {
            List<String> result = super.getResult(api, arguments);
            if (!result.isEmpty()
                && apps.isEmpty()
                && PreferenceUtil.getBoolean(context, PlayStoreApiAuthenticator.PREFERENCE_APP_PROVIDED_EMAIL)
            ) {
                for (BulkDetailsEntry details: api.bulkDetails(new ArrayList<>(SinAppsApplication.wishlist)).getEntryList()) {
                    if (!details.hasDoc()) {
                        continue;
                    }
                    apps.add(AppBuilder.build(details.getDoc()));
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(List<String> result) {
            super.onPostExecute(result);
            ((WishlistActivity) context).clearApps();
            ((WishlistActivity) context).addApps(apps);
            if (success() && apps.isEmpty()) {
                errorView.setText(R.string.list_empty_search);
            }
        }
    }
}
