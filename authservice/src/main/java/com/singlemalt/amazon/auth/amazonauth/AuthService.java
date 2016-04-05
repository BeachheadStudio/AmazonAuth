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
import com.singlemalt.amazon.auth.amazonauth.listener.LoginListener;
import com.singlemalt.amazon.auth.amazonauth.listener.TokenListener;

import android.os.Bundle;
import android.util.Log;

import com.unity3d.player.UnityPlayer;

import java.util.EnumSet;
import java.util.concurrent.Executors;

/**
 * Created by kmiller on 3/25/16.
 */
public class AuthService implements AuthorizationListener, AmazonGamesCallback,
        AGResponseCallback<RequestPlayerResponse> {

    private static final String TAG = AuthService.class.getSimpleName();

    public enum Status {
        Working,
        Success,
        Failure,
        Cancel
    }

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

    private Status loginStatus;
    private Status gcStatus;

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

        this.loginStatus = Status.Working;
        this.gcStatus = Status.Working;

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
    }

    @Override
    public void onCancel(Bundle bundle) {
        Log.d(TAG, "login onCancel");

        loginStatus = Status.Cancel;

        checkStatus();
    }

    @Override
    public void onSuccess(Bundle bundle) {
        Log.d(TAG, "login onSuccess");

        loginStatus = Status.Success;
        oauthToken = bundle.getString(AuthzConstants.BUNDLE_KEY.TOKEN.val);

        checkStatus();
    }

    @Override
    public void onError(AuthError authError) {
        Log.d(TAG, String.format("login onError %s", authError.toString()));

        loginStatus = Status.Failure;
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
            gcStatus = Status.Cancel;
            checkStatus();
        }
    }

    @Override
    public void onServiceNotReady(AmazonGamesStatus amazonGamesStatus) {
        Log.d(TAG, "gc onServiceNotReady: " + amazonGamesStatus.name());

        gcStatus = Status.Failure;
        failureError = amazonGamesStatus.name();
        checkStatus();
    }

    @Override
    public void onComplete(RequestPlayerResponse requestPlayerResponse) {
        Log.d(TAG, "get player onComplete");

        if(requestPlayerResponse.isError()) {
            Log.e(TAG, "get player error: " +requestPlayerResponse.getError().toString());
            gcStatus = Status.Failure;
        } else {
            Log.d(TAG, "get player success: " + requestPlayerResponse.getPlayer().getAlias());
            player = requestPlayerResponse.getPlayer();
            gcStatus = Status.Success;
        }

        checkStatus();
    }

    public void onPause() {
        Log.d(TAG, "onPause");

        if(agClient != null) {
            agClient.release();
        }
    }

    public void onResume() {
        Log.d(TAG, "onResume");

        Executors.newSingleThreadExecutor().execute(new GCRunner(this));
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
                UnityPlayer.UnitySendMessage("AuthGameObject", "PlayerChange", "true");
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

        if(loginStatus.equals(Status.Working) || gcStatus.equals(Status.Working)) {
            Log.d(TAG, "not ready to update unity");
            return;
        }

        if(loginStatus.equals(Status.Success) && gcStatus.equals(Status.Success)) {
            anonymous = false;

            Log.d(TAG, "login success");
            UnityPlayer.UnitySendMessage("AuthGameObject", "LoginResult", Status.Success.toString());
        } else if(loginStatus.equals(Status.Cancel)) {
            anonymous = true;
            player = null;

            Log.d(TAG, "login cancelled");
            UnityPlayer.UnitySendMessage("AuthGameObject", "LoginResult", Status.Cancel.toString());
        } else if(loginStatus.equals(Status.Failure) || gcStatus.equals(Status.Failure)) {
            anonymous = true;
            player = null;

            Log.d(TAG, "login failed");
            UnityPlayer.UnitySendMessage("AuthGameObject", "LoginResult", Status.Failure.toString());
        }
    }

}
