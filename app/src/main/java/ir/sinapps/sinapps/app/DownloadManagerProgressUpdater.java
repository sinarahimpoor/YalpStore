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

import android.util.Pair;

public class DownloadManagerProgressUpdater extends RepeatingTask {

    private long downloadId;
    private DownloadManagerAdapter dm;

    public DownloadManagerProgressUpdater(long downloadId, DownloadManagerAdapter dm) {
        this.downloadId = downloadId;
        this.dm = dm;
    }

    @Override
    protected void payload() {
        final Pair<Integer, Integer> progress = dm.getProgress(downloadId);
        if (null == progress) {
            return;
        }
        DownloadState state = DownloadState.get(downloadId);
        if (null == state) {
            return;
        }
        state.setProgress(downloadId, progress.first, progress.second);
    }

    @Override
    protected boolean shouldRunAgain() {
        DownloadState state = DownloadState.get(downloadId);
        return null != state && !state.isEverythingFinished();
    }
}
