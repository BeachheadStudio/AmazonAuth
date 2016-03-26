package com.singlemalt.amazon.auth.amazonauth.listeners;

import android.os.Bundle;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.shared.APIListener;

/**
 * Created by kmiller on 3/25/16.
 */
public class TokenListener implements APIListener {
    @Override
    public void onSuccess(Bundle bundle) {

    }

    @Override
    public void onError(AuthError authError) {

    }
}
