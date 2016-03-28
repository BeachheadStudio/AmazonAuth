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
import com.singlemalt.amazon.auth.amazonauth.listeners.LoginListener;
import com.singlemalt.amazon.auth.amazonauth.listeners.TokenListener;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.unity3d.player.UnityPlayer;

import java.util.EnumSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by kmiller on 3/25/16.
 */
public class AuthService implements AuthorizationListener, AmazonGamesCallback,
        AGResponseCallback<RequestPlayerResponse> {

    private static final String TAG = AuthService.class.getSimpleName();

    private static final String[] STATUS_VALUES = new String[]
            { "Working", "Success", "Failure", "Cancel"};

    public static final String[] APP_AUTH_SCOPES = new String[]{ "profile" };

    private class AuthRunner implements Runnable {
        private final String TAG = AuthRunner.class.getSimpleName();
        private AmazonAuthorizationManager manager;
        private AuthService authService;

        public AuthRunner(AuthService authService) {
            this.authService = authService;
        }

        @Override
        public void run() {
            Log.d(TAG, "Starting Amazon login");
            manager = new AmazonAuthorizationManager(
                    UnityPlayer.currentActivity.getApplicationContext(), Bundle.EMPTY);

            TokenListener tokenListener = new TokenListener(authService);
            LoginListener loginListener = new LoginListener(authService, tokenListener, manager);

            manager.authorize(APP_AUTH_SCOPES, Bundle.EMPTY, loginListener);
        }
    }

    private class GCRunner implements Runnable {
        private AuthService authService;

        public GCRunner(AuthService authService) {
            this.authService = authService;
        }

        @Override
        public void run() {
            Log.d(TAG, "Starting GameCircle login");
            AmazonGamesClient.initialize(UnityPlayer.currentActivity, authService, features);
        }
    }

    // singleton methods
    private static AuthService instance;

    public static AuthService getInstance() {
        if(instance == null) {
            instance = new AuthService();
        }

        return instance;
    }

    // game circle client
    private AmazonGamesClient agClient;

    // enums are a pain in the JNI, so using a string
    private String loginStatus;
    private String gcStatus;

    // Amazon parameters
    private EnumSet<AmazonGamesFeature> features;
    private Player player;
    private String playerId = null;
    private String oauthToken = "";
    private String failureError;
    private boolean anonymous = true;

    private AuthService() { }

    public void init(boolean achievements, boolean leaderboards, boolean whisperSync) {
        Log.d(TAG, "Starting");

        this.loginStatus = STATUS_VALUES[0];
        this.gcStatus = STATUS_VALUES[0];

        // AmazonGamesClient
        features = EnumSet.noneOf(AmazonGamesFeature.class);

        if(achievements) {
            features.add(AmazonGamesFeature.Achievements);
        }

        if(leaderboards) {
            features.add(AmazonGamesFeature.Leaderboards);
        }

        if(whisperSync) {
            features.add(AmazonGamesFeature.Whispersync);
        }

        Executors.newSingleThreadExecutor().execute(new AuthRunner(this));
        Executors.newSingleThreadExecutor().execute(new GCRunner(this));
    }

    @Override
    public void onCancel(Bundle bundle) {
        Log.d(TAG, "login onCancel");

        loginStatus = STATUS_VALUES[3];

        checkStatus();
    }

    @Override
    public void onSuccess(Bundle bundle) {
        Log.d(TAG, "login onSuccess");

        loginStatus = STATUS_VALUES[1];
        oauthToken = bundle.getString(AuthzConstants.BUNDLE_KEY.TOKEN.val);

        checkStatus();
    }

    @Override
    public void onError(AuthError authError) {
        Log.d(TAG, String.format("login onError %s", authError.toString()));

        loginStatus = STATUS_VALUES[2];
        failureError = authError.getMessage();

        checkStatus();
    }

    @Override
    public void onServiceReady(AmazonGamesClient amazonGamesClient) {
        Log.d(TAG, "gc onServiceReady");
        agClient = amazonGamesClient;

        if(agClient.getPlayerClient().isSignedIn()) {
            agClient.getPlayerClient().getLocalPlayer((Object[]) null).setCallback(this);
        } else {
            gcStatus = STATUS_VALUES[3];
            checkStatus();
        }
        checkStatus();
    }

    @Override
    public void onServiceNotReady(AmazonGamesStatus amazonGamesStatus) {
        Log.d(TAG, "gc onServiceNotReady: " + amazonGamesStatus.name());

        gcStatus = STATUS_VALUES[2];
        failureError = amazonGamesStatus.name();
        checkStatus();
    }

    @Override
    public void onComplete(RequestPlayerResponse requestPlayerResponse) {
        Log.d(TAG, "get player onComplete");

        if(requestPlayerResponse.isError()) {
            Log.e(TAG, "get player error: " +requestPlayerResponse.getError().toString());
            gcStatus = STATUS_VALUES[2];
        } else {
            Log.d(TAG, "get player success: " + requestPlayerResponse.getPlayer().getAlias());
            player = requestPlayerResponse.getPlayer();
            gcStatus = STATUS_VALUES[1];
        }

        checkStatus();
    }

    public void onPause() {
        if(agClient != null) {
            agClient.release();
        }
    }

    public void onResume() {
        if(agClient != null) {
            agClient = null;
        }

        AmazonGamesClient.initialize(UnityPlayer.currentActivity, this, features);
    }

    public String getPlayerName() {
        if(anonymous) {
            return "";
        } else {
            return player.getAlias();
        }
    }

    public void setPlayerId(String playerId) {
        if(this.playerId != null) {
            if(!this.playerId.equals(playerId)) {
                this.playerId = playerId;
                UnityPlayer.UnitySendMessage("Main Camera", "PlayerChange", "true");
            }
        } else {
            this.playerId = playerId;
        }
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getOauthToken() {
        return oauthToken;
    }

    public String getFailureError() {
        return failureError;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    private void checkStatus() {
        Log.d(TAG, String.format("checkStatus: loginStatus %s, gcStatus %s", loginStatus, gcStatus));

        if(loginStatus.equals(STATUS_VALUES[0]) || gcStatus.equals(STATUS_VALUES[0])) {
            Log.d(TAG, "not ready to update unity");
            return;
        }

        if(loginStatus.equals(STATUS_VALUES[1]) && gcStatus.equals(STATUS_VALUES[1])) {
            anonymous = false;

            Log.d(TAG, "login success");
            UnityPlayer.UnitySendMessage("Main Camera", "LoginResult", STATUS_VALUES[1]);
        } else if(loginStatus.equals(STATUS_VALUES[3])) {
            anonymous = true;
            player = null;

            Log.d(TAG, "login cancelled");
            UnityPlayer.UnitySendMessage("Main Camera", "LoginResult", STATUS_VALUES[3]);
        } else if(loginStatus.equals(STATUS_VALUES[2]) || gcStatus.equals(STATUS_VALUES[2])) {
            anonymous = true;
            player = null;

            Log.d(TAG, "login failed");
            UnityPlayer.UnitySendMessage("Main Camera", "LoginResult", STATUS_VALUES[2]);
        }
    }

}
