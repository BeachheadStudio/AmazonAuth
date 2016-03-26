package com.singlemalt.amazon.auth.amazonauth;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager;
import com.amazon.identity.auth.device.authorization.api.AuthorizationListener;
import com.singlemalt.amazon.auth.amazonauth.listeners.LoginListener;
import com.singlemalt.amazon.auth.amazonauth.listeners.TokenListener;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by kmiller on 3/25/16.
 */
public class AuthService implements AuthorizationListener {
    private static final String TAG = AuthService.class.getSimpleName();

    private static final String[] STATUS_VALUES = new String[]
            { "Working", "Success", "Failure", "Cancel"};

    public static final String[] APP_AUTH_SCOPES = new String[]{ "profile" };

    private class AuthRunner implements Runnable {
        private final String TAG = AuthRunner.class.getSimpleName();
        private AmazonAuthorizationManager manager;
        private AuthorizationListener serviceListener;

        public AuthRunner(AuthorizationListener serviceListener) {
            this.serviceListener = serviceListener;
        }

        @Override
        public void run() {
            TokenListener tokenListener = new TokenListener(serviceListener);
            LoginListener loginListener = new LoginListener(serviceListener, tokenListener, manager);

            Log.d(TAG, "Starting");
            manager = new AmazonAuthorizationManager(context, Bundle.EMPTY);
            manager.authorize(APP_AUTH_SCOPES, Bundle.EMPTY, loginListener);
        }
    }

    private Context context;

    // enums are a pain in the JNI, so using a string
    private String status;

    public AuthService(Context context) {
        Log.d(TAG, "Starting");

        this.context = context;
        this.status = STATUS_VALUES[0];

        new Thread(new AuthRunner(this)).start();
    }

    @Override
    public void onCancel(Bundle bundle) {
        Log.d(TAG, "onCancel");

        this.status = STATUS_VALUES[3];
    }

    @Override
    public void onSuccess(Bundle bundle) {
        Log.d(TAG, "onSuccess");

        this.status = STATUS_VALUES[1];
    }

    @Override
    public void onError(AuthError authError) {
        Log.d(TAG, String.format("onError %s", authError.toString()));

        this.status = STATUS_VALUES[2];
    }

    public String getStatus() {
        return status;
    }
}
