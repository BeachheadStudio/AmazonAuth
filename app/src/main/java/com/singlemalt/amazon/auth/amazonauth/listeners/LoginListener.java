package com.singlemalt.amazon.auth.amazonauth.listeners;

import android.os.Bundle;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.authorization.api.AuthorizationListener;

/**
 * Created by kmiller on 3/25/16.
 */
public class LoginListener implements AuthorizationListener {

    @Override
    public void onCancel(Bundle bundle) {

    }

    @Override
    public void onSuccess(Bundle bundle) {

    }

    @Override
    public void onError(AuthError authError) {

    }
}
