package com.singlemalt.amazon.auth.amazonauth.listener;

import android.os.Bundle;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.shared.APIListener;
import com.singlemalt.amazon.auth.amazonauth.AuthService;

import android.util.Log;

/**
 * Created by kmiller on 3/25/16.
 */
public class TokenListener implements APIListener {
    private static final String TAG = TokenListener.class.getSimpleName();

    private AuthService authService;

    public TokenListener(AuthService authService) {
        Log.d(TAG, "init");

        this.authService = authService;
    }

    @Override
    public void onSuccess(Bundle bundle) {
        Log.d(TAG, "onSuccess");

        // call service onError
        authService.onSuccess(bundle);
    }

    @Override
    public void onError(AuthError authError) {
        Log.d(TAG, String.format("onError %s", authError.toString()));

        // call service onError
        authService.onError(authError);
    }
}
