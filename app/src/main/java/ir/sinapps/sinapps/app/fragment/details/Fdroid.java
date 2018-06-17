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

package ir.sinapps.sinapps.app.fragment.details;

import android.view.View;
import android.widget.TextView;

import ir.sinapps.sinapps.app.R;
import ir.sinapps.sinapps.app.SinAppsModelActivity;
import ir.sinapps.sinapps.app.SinAppsApplication;
import ir.sinapps.sinapps.app.fragment.Abstract;
import ir.sinapps.sinapps.app.model.App;
import ir.sinapps.sinapps.app.view.UriOnClickListener;

public class Fdroid extends Abstract {

    private static final String FDROID_LINK = "https://f-droid.org/packages/";

    @Override
    public void draw() {
        TextView view = activity.findViewById(R.id.to_fdroid);
        if (!SinAppsApplication.fdroidPackageNames.contains(app.getPackageName())) {
            if (null != view) {
                view.setVisibility(View.GONE);
            }
            return;
        }
        view.setVisibility(View.VISIBLE);
        view.setOnClickListener(new UriOnClickListener(activity, FDROID_LINK + app.getPackageName()));
    }

    public Fdroid(SinAppsModelActivity activity, App app) {
        super(activity, app);
    }
}
