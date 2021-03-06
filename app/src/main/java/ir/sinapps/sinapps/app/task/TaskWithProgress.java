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

package ir.sinapps.sinapps.app.task;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.view.View;

import ir.sinapps.sinapps.app.ContextUtil;
import ir.sinapps.sinapps.app.ThemeManager;

abstract public class TaskWithProgress<T> extends AsyncTask<String, Void, T> {

    protected Context context;
    protected ProgressDialog progressDialog;
    protected View progressIndicator;

    public View getProgressIndicator() {
        return progressIndicator;
    }

    public void setProgressIndicator(View progressIndicator) {
        this.progressIndicator = progressIndicator;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void prepareDialog(int messageId, int titleId) {
        ProgressDialog dialog = new ProgressDialog(context, new ThemeManager().getDialogThemeId(context));
        dialog.setTitle(context.getString(titleId));
        dialog.setMessage(context.getString(messageId));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        this.progressDialog = dialog;
    }

    @Override
    protected void onPreExecute() {
        if (null != this.progressDialog && ContextUtil.isAlive(context)) {
            this.progressDialog.show();
        }
        if (null != progressIndicator) {
            progressIndicator.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onPostExecute(T result) {
        if (null != this.progressDialog && ContextUtil.isAlive(context) && progressDialog.isShowing()) {
            this.progressDialog.dismiss();
        }
        if (null != progressIndicator) {
            progressIndicator.setVisibility(View.GONE);
        }
    }

    public AsyncTask<String, Void, T> executeOnExecutorIfPossible() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            return this.execute();
        } else {
            return this.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
}
