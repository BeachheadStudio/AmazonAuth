package com.singlemalt.amazon.auth.amazonauth.listener;

import android.os.Bundle;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager;
import com.amazon.identity.auth.device.authorization.api.AuthorizationListener;
import com.amazon.identity.auth.device.authorization.api.AuthzConstants;
import com.amazon.identity.auth.device.shared.APIListener;
import com.amazon.insights.core.util.StringUtil;
import com.singlemalt.amazon.auth.amazonauth.AuthService;

import android.util.Log;

/**
 * Created by kmiller on 3/25/16.
 */
public class LoginListener implements AuthorizationListener {
    private static final String TAG = LoginListener.class.getSimpleName();

    private AmazonAuthorizationManager manager;
    private AuthService authService;
    private APIListener tokenListener;

    public LoginListener(AuthService authService, APIListener tokenListener,
                         AmazonAuthorizationManager manager) {
        Log.d(TAG, "init");

        this.authService = authService;
        this.tokenListener = tokenListener;
        this.manager = manager;
    }

    @Override
    public void onCancel(Bundle bundle) {
        Log.d(TAG, "onCancel");

        // call service onCancel
        authService.onCancel(bundle);
    }

    @Override
    public void onSuccess(Bundle bundle) {
        Log.d(TAG, "onSuccess");
        Bundle profileBundle = bundle.getBundle(AuthzConstants.BUNDLE_KEY.PROFILE.val);
        String name = profileBundle != null ? profileBundle.getString(AuthzConstants.PROFILE_KEY.NAME.val) : null;
        String email = profileBundle != null ? profileBundle.getString(AuthzConstants.PROFILE_KEY.EMAIL.val) : null;
        String playerId = profileBundle != null ? profileBundle.getString(AuthzConstants.PROFILE_KEY.USER_ID.val) : null;

        // set playerId
        if(StringUtil.isNullOrEmpty(playerId)) {
            Log.e(TAG, "The playerId from Amazon is null! Name: "+name+" email: "+email);
        } else {
            Log.d(TAG, "setting playerId to: "+playerId);
            authService.setPlayerId(playerId);
        }

        // call manager, start token process
        manager.getToken(AuthService.APP_AUTH_SCOPES, tokenListener);
    }

    @Override
    public void onError(AuthError authError) {
        Log.d(TAG, String.format("onError %s", authError.toString()));

        // call service onError
        authService.onError(authError);
    }
}
