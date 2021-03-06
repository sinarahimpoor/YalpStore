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

import ir.sinapps.sinapps.app.view.AppBadge;

public class ListItemDownloadProgressUpdater extends DownloadProgressUpdater {

    private AppBadge appBadge;

    public ListItemDownloadProgressUpdater(String packageName, AppBadge appBadge) {
        super(packageName);
        this.appBadge = appBadge;
    }

    @Override
    protected void setProgress(int progress, int max) {
        appBadge.setProgress(progress, max);
    }

    @Override
    protected void finish() {
        appBadge.hideMoreButton();
    }
}
