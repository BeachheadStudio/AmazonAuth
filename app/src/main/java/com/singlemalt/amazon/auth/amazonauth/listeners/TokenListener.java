package com.singlemalt.amazon.auth.amazonauth.listeners;

import android.os.Bundle;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.authorization.api.AuthorizationListener;
import com.amazon.identity.auth.device.shared.APIListener;

import android.util.Log;

/**
 * Created by kmiller on 3/25/16.
 */
public class TokenListener implements APIListener {
    private static final String TAG = TokenListener.class.getSimpleName();

    private AuthorizationListener serviceListener;

    public TokenListener(AuthorizationListener serviceListener) {
        Log.d(TAG, "init");

        this.serviceListener = serviceListener;
    }

    @Override
    public void onSuccess(Bundle bundle) {
        Log.d(TAG, "onSuccess");

        // call service onError
        serviceListener.onSuccess(bundle);
    }

    @Override
    public void onError(AuthError authError) {
        Log.d(TAG, String.format("onError %s", authError.toString()));

        // call service onError
        serviceListener.onError(authError);
    }
}
