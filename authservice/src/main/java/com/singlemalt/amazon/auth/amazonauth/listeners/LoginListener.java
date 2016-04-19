package com.singlemalt.amazon.auth.amazonauth.listeners;

import android.os.Bundle;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager;
import com.amazon.identity.auth.device.authorization.api.AuthorizationListener;
import com.amazon.identity.auth.device.authorization.api.AuthzConstants;
import com.amazon.identity.auth.device.shared.APIListener;
import com.amazon.insights.core.util.StringUtil;
import com.google.gson.Gson;
import com.singlemalt.amazon.auth.amazonauth.AuthInstance;
import com.singlemalt.amazon.auth.amazonauth.AuthServiceActivity;

import android.util.Log;

/**
 * Created by kmiller on 3/25/16.
 */
public class LoginListener implements AuthorizationListener {
    private AmazonAuthorizationManager manager;
    private ProfileListener profileListener;

    public LoginListener(ProfileListener profileListener, AmazonAuthorizationManager manager) {
        Log.d(AuthInstance.TAG, "LoginListener init");

        this.profileListener = profileListener;
        this.manager = manager;
    }

    @Override
    public void onCancel(Bundle bundle) {
        Log.d(AuthInstance.TAG, "LoginListener onCancel");
        bundle.putBoolean("isToken", false);

        // call service onCancel
        AuthInstance.getInstance().onCancel(bundle);
    }

    @Override
    public void onSuccess(Bundle bundle) {
        Log.d(AuthInstance.TAG, "LoginListener onSuccess");
        manager.getProfile(profileListener);
    }

    @Override
    public void onError(AuthError authError) {
        Log.d(AuthInstance.TAG, String.format("LoginListener onError %s", authError.toString()));

        // call service onError
        AuthInstance.getInstance().onError(authError);
    }
}
