package com.singlemalt.amazon.auth.amazonauth;

import com.amazon.ags.api.AGResponseCallback;
import com.amazon.ags.api.AmazonGamesCallback;
import com.amazon.ags.api.AmazonGamesClient;
import com.amazon.ags.api.AmazonGamesFeature;
import com.amazon.ags.api.AmazonGamesStatus;
import com.amazon.ags.api.player.Player;
import com.amazon.ags.api.player.RequestPlayerResponse;
import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager;
import com.amazon.identity.auth.device.authorization.api.AuthorizationListener;
import com.amazon.identity.auth.device.authorization.api.AuthzConstants;
import com.amazon.insights.core.util.StringUtil;
import com.singlemalt.amazon.auth.amazonauth.listeners.LoginListener;
import com.singlemalt.amazon.auth.amazonauth.listeners.TokenListener;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.singlemalt.amazon.auth.amazonauth.runners.AuthRunner;
import com.singlemalt.amazon.auth.amazonauth.runners.GCRunner;
import com.singlemalt.amazon.auth.amazonauth.runners.ServerAuthRunner;
import com.unity3d.player.UnityPlayer;

import java.util.EnumSet;
import java.util.concurrent.Executors;

/**
 * Created by kmiller on 3/25/16.
 */
public class AuthServiceActivity extends Activity implements AmazonGamesCallback,
        AGResponseCallback<RequestPlayerResponse> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(AuthInstance.TAG, "AuthServiceActivity Starting");

        super.onCreate(savedInstanceState);

        if(savedInstanceState != null && !savedInstanceState.equals(Bundle.EMPTY)
                && savedInstanceState.getBoolean("onResume")) {
            this.runOnUiThread(new GCRunner(this));
        } else {
            this.runOnUiThread(new GCRunner(this));

            Executors.newSingleThreadExecutor().execute(new AuthRunner(this));
        }
    }

    // START GameCircle callbacks
    @Override
    public void onServiceReady(AmazonGamesClient amazonGamesClient) {
        Log.d(AuthInstance.TAG, "GameCircle onServiceReady");
        AuthInstance.getInstance().setAgClient(amazonGamesClient);

        if(AuthInstance.getInstance().getAgClient().getPlayerClient().isSignedIn()) {
            AuthInstance.getInstance().getAgClient().getPlayerClient()
                    .getLocalPlayer((Object[]) null).setCallback(this);
        } else {
            AuthInstance.getInstance().setGcStatus(AuthInstance.Status.Cancel);

            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startActivity(UnityPlayer.currentActivity.getIntent());
                }
            });

            this.finish();
        }
    }

    @Override
    public void onServiceNotReady(AmazonGamesStatus amazonGamesStatus) {
        Log.d(AuthInstance.TAG, "GameCircle onServiceNotReady: " + amazonGamesStatus.name());

        AuthInstance.getInstance().setGcStatus(AuthInstance.Status.Failure);
        AuthInstance.getInstance().setFailureError(amazonGamesStatus.name());
        AuthInstance.getInstance().checkStatus();

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startActivity(UnityPlayer.currentActivity.getIntent());
            }
        });

        this.finish();
    }

    @Override
    public void onComplete(RequestPlayerResponse requestPlayerResponse) {
        Log.d(AuthInstance.TAG, "GameCircle get player onComplete");

        if(requestPlayerResponse.isError()) {
            Log.e(AuthInstance.TAG, "GameCircle get player error: " + requestPlayerResponse.getError().toString());
            AuthInstance.getInstance().setGcStatus(AuthInstance.Status.Failure);
            AuthInstance.getInstance().setFailureError(requestPlayerResponse.getError().toString());
        } else {
            Log.d(AuthInstance.TAG, "GameCircle get player success: " + requestPlayerResponse.getPlayer().getAlias());
            AuthInstance.getInstance().setGcStatus(AuthInstance.Status.Success);
            AuthInstance.getInstance().setPlayerName(requestPlayerResponse.getPlayer().getAlias());
        }

        AuthInstance.getInstance().checkStatus();

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startActivity(UnityPlayer.currentActivity.getIntent());
            }
        });

        this.finish();
    }
    // END GameCircle callbacks
}
