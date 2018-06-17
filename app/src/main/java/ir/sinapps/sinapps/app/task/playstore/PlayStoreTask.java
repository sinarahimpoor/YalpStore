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
import android.util.Log;
import android.widget.TextView;

import com.github.yeriomin.playstoreapi.AuthException;
import ir.sinapps.sinapps.app.ContextUtil;
import ir.sinapps.sinapps.app.CredentialsEmptyException;
import ir.sinapps.sinapps.app.FirstLaunchChecker;
import ir.sinapps.sinapps.app.PlayStoreApiAuthenticator;
import ir.sinapps.sinapps.app.PreferenceUtil;
import ir.sinapps.sinapps.app.R;
import ir.sinapps.sinapps.app.task.TaskWithProgress;
import ir.sinapps.sinapps.app.view.AccountTypeDialogBuilder;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLHandshakeException;

abstract public class PlayStoreTask<T> extends TaskWithProgress<T> {

    protected Throwable exception;
    protected TextView errorView;

    public void setErrorView(TextView errorView) {
        this.errorView = errorView;
    }

    public Throwable getException() {
        return exception;
    }

    public boolean success() {
        return null == exception;
    }

    @Override
    protected void onPostExecute(T result) {
        super.onPostExecute(result);
        if (exception != null) {
            processException(exception);
        }
    }

    protected void processException(Throwable e) {
        Log.d(getClass().getSimpleName(), e.getClass().getName() + " caught during a google api request: " + e.getMessage());
        if (e instanceof AuthException) {
            processAuthException((AuthException) e);
        } else if (e instanceof IOException) {
            processIOException((IOException) e);
        } else {
            Log.e(getClass().getSimpleName(), "Unknown exception " + e.getClass().getName() + " " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected void processIOException(IOException e) {
        String message;
        if (noNetwork(e)) {
            message = this.context.getString(R.string.error_no_network);
        } else {
            message = TextUtils.isEmpty(e.getMessage())
                ? this.context.getString(R.string.error_network_other, e.getClass().getName())
                : e.getMessage()
            ;
        }
        if (null != this.errorView) {
            this.errorView.setText(message);
        } else {
            ContextUtil.toastLong(this.context, message);
        }
    }

    protected void processAuthException(AuthException e) {
        if (e instanceof CredentialsEmptyException) {
            Log.i(getClass().getSimpleName(), "Credentials empty");
            if (new FirstLaunchChecker(context).isFirstLogin() && ContextUtil.isAlive(context)) {
                Log.i(getClass().getSimpleName(), "First launch, so using built-in account");
                logInWithPredefinedAccount();
                ContextUtil.toast(context, R.string.first_login_message);
                return;
            }
        } else if (e.getCode() == 401 && PreferenceUtil.getBoolean(context, PlayStoreApiAuthenticator.PREFERENCE_APP_PROVIDED_EMAIL)) {
            Log.i(getClass().getSimpleName(), "Token is stale");
            refreshToken();
            return;
        } else {
            ContextUtil.toast(this.context, R.string.error_incorrect_password);
            new PlayStoreApiAuthenticator(context).logout();
        }
        if (ContextUtil.isAlive(context)) {
            AccountTypeDialogBuilder builder = new AccountTypeDialogBuilder((Activity) context);
            builder.setCaller(this);
            builder.show();
        } else {
            Log.e(getClass().getSimpleName(), "AuthException happened and the provided context is not ui capable");
        }
    }

    private void refreshToken() {
        RefreshTokenTask task = new RefreshTokenTask();
        task.setCaller(this);
        task.setContext(context);
        task.execute();
    }


    public void logInWithPredefinedAccount() {
        LoginTask task = new LoginTask();
        task.setCaller(this);
        task.setContext(context);
        task.prepareDialog(R.string.dialog_message_logging_in_predefined, R.string.dialog_title_logging_in);
        task.execute();
    }

    static public boolean noNetwork(Throwable e) {
        return e instanceof UnknownHostException
            || e instanceof SSLHandshakeException
            || e instanceof ConnectException
            || e instanceof SocketException
            || e instanceof SocketTimeoutException
            || (null != e && null != e.getCause() && noNetwork(e.getCause()))
        ;
    }
}
