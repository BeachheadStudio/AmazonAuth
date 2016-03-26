package com.singlemalt.amazon.auth.amazonauth.listeners;

import android.os.Bundle;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager;
import com.amazon.identity.auth.device.authorization.api.AuthorizationListener;
import com.amazon.identity.auth.device.shared.APIListener;
import com.singlemalt.amazon.auth.amazonauth.AuthService;

import android.util.Log;

/**
 * Created by kmiller on 3/25/16.
 */
public class LoginListener implements AuthorizationListener {
    private static final String TAG = LoginListener.class.getSimpleName();

    private AmazonAuthorizationManager manager;
    private AuthorizationListener serviceListener;
    private APIListener tokenListener;

    public LoginListener(AuthorizationListener serviceListener, APIListener tokenListener,
                         AmazonAuthorizationManager manager) {
        Log.d(TAG, "init");

        this.serviceListener = serviceListener;
        this.tokenListener = tokenListener;
        this.manager = manager;
    }

    @Override
    public void onCancel(Bundle bundle) {
        Log.d(TAG, "onCancel");

        // call service onCancel
        serviceListener.onCancel(bundle);
    }

    @Override
    public void onSuccess(Bundle bundle) {
        Log.d(TAG, "onSuccess");

        // call manager, start token process
        manager.getToken(AuthService.APP_AUTH_SCOPES, tokenListener);
    }

    @Override
    public void onError(AuthError authError) {
        Log.d(TAG, String.format("onError %s", authError.toString()));

        // call service onError
        serviceListener.onError(authError);
    }
}
