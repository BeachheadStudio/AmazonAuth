package com.singlemalt.amazon.auth.amazonauth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.amazon.ags.api.AmazonGamesClient;
import com.amazon.ags.api.AmazonGamesFeature;
import com.amazon.identity.auth.device.AuthError;
import com.singlemalt.amazon.auth.amazonauth.runners.ServerAuthRunner;
import com.unity3d.player.UnityPlayer;

import java.util.EnumSet;
import java.util.concurrent.Executors;

/**
 * Created by singlemalt on 4/12/2016.
 */
public class AuthInstance {
    public static final String TAG = AuthInstance.class.getSimpleName();
    public static final String[] APP_AUTH_SCOPES = new String[]{ "profile" };

    private static AuthInstance ourInstance = new AuthInstance();

    private String playerId = null;
    private String playerName;
    private String failureError;
    private String serverPlayerId;
    private String sessionToken = "";
    private String accountName;
    private String oauthToken;
    private String serverUrl;
    private boolean anonymous = true;
    private EnumSet<AmazonGamesFeature> features;

    private Status loginStatus;
    private Status gcStatus;
    private Status serverAuthStatus;

    // Amazon API client
    private AmazonGamesClient agClient;

    public enum Status {
        Working,
        Success,
        Failure,
        Cancel
    }

    public static AuthInstance getInstance() {
        return ourInstance;
    }

    private AuthInstance() {

    }

    public void init(String serverUrl, String playerId, final boolean achievements, final boolean leaderboards,
                     final boolean whisperSync) {
        Log.d(TAG, "init instance");

        this.serverUrl = serverUrl;
        this.serverPlayerId = playerId;
        this.loginStatus = Status.Working;
        this.gcStatus = Status.Working;
        this.serverAuthStatus = Status.Working;

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

        final Intent intent = new Intent(UnityPlayer.currentActivity.getApplicationContext(),
                AuthServiceActivity.class);
        intent.putExtra("onResume", false);

        UnityPlayer.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "start activity");
                UnityPlayer.currentActivity.startActivity(intent);
            }
        });
    }

    public void onPause() {
        Log.d(AuthInstance.TAG, "onPause");

        if(agClient != null) {
            //agClient.release();
        }
    }

    public void onResume() {
        Log.d(AuthInstance.TAG, "onResume");

        final Intent intent = new Intent(UnityPlayer.currentActivity.getApplicationContext(),
                AuthServiceActivity.class);
        intent.putExtra("onResume", true);

        UnityPlayer.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                Log.d(TAG, "start onResume activity");
//                UnityPlayer.currentActivity.startActivity(intent);
            }
        });
    }

    // start Login with Amazon callbacks
    public void onSuccess(Bundle bundle) {
        Log.d(AuthInstance.TAG, "onSuccess");

        loginStatus = Status.Success;
        Executors.newSingleThreadExecutor().execute(new ServerAuthRunner());
        checkStatus();
    }

    public void onError(AuthError authError) {
        Log.d(AuthInstance.TAG, String.format("login onError %s", authError.toString()));

        loginStatus = Status.Failure;
        failureError = authError.getMessage();
        Executors.newSingleThreadExecutor().execute(new ServerAuthRunner());
        checkStatus();
    }

    public void onCancel(Bundle bundle) {
        Log.d(TAG, "onCancel");
        loginStatus = Status.Cancel;
        Executors.newSingleThreadExecutor().execute(new ServerAuthRunner());
        checkStatus();
    }
    // END Login with Amazon callbacks

    public String getAuthParams() {
//        Map<String, String> authParams = new HashMap<>();
//        authParams.put("playerId", playerId);
//        authParams.put("serverPlayerId", serverPlayerId);
//        authParams.put("network", "GOOGLE");
//        authParams.put("playerName", playerName);
//        authParams.put("token", oauthToken);
//
//        return new Gson().toJson(authParams);
        return "";
    }

    public void checkStatus() {
        if (loginStatus.equals(Status.Working) || gcStatus.equals(Status.Working)
                || serverAuthStatus.equals(Status.Working)) {
            Log.d(TAG, "not ready to update unity");
        } else if (loginStatus.equals(Status.Success) && gcStatus.equals(Status.Success)
                && serverAuthStatus.equals(Status.Success)) {
            anonymous = false;
            Log.d(TAG, "login success");
            UnityPlayer.UnitySendMessage("AuthGameObject", "LoginResult", Status.Success.toString());
        } else if (serverAuthStatus.equals(Status.Success) && ((loginStatus.equals(Status.Cancel) || gcStatus.equals(Status.Cancel)))) {
            anonymous = true;

            Log.d(TAG, "login cancelled");
            UnityPlayer.UnitySendMessage("AuthGameObject", "LoginResult", Status.Cancel.toString());
        } else if (serverAuthStatus.equals(Status.Failure)) {
            anonymous = true;

            Log.e(TAG, "login failed");
            UnityPlayer.UnitySendMessage("AuthGameObject", "LoginResult", Status.Failure.toString());
        }
    }

    public void awardAchievement(String achievementId) {

    }

    public String getOauthToken() {
        return oauthToken;
    }

    public void setOauthToken(String oauthToken) {
        this.oauthToken = oauthToken;
    }

    public String getFailureError() {
        return failureError;
    }

    public void setFailureError(String failureError) {
        this.failureError = failureError;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        if(this.playerId != null) {
            if(!this.playerId.equals(playerId)) {
                Log.d(TAG, "New playerId found: " +playerId);
                UnityPlayer.UnitySendMessage("AuthGameObject", "PlayerChange", "true");
            }
        }
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getServerPlayerId() {
        return serverPlayerId;
    }

    public void setServerPlayerId(String serverPlayerId) {
        this.serverPlayerId = serverPlayerId;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setLoginStatus(Status loginStatus) {
        this.loginStatus = loginStatus;
    }

    public void setGcStatus(Status gcStatus) {
        this.gcStatus = gcStatus;
    }

    public void setServerAuthStatus(Status serverAuthStatus) {
        this.serverAuthStatus = serverAuthStatus;
    }

    public AmazonGamesClient getAgClient() {
        return agClient;
    }

    public void setAgClient(AmazonGamesClient agClient) {
        this.agClient = agClient;
    }

    public EnumSet<AmazonGamesFeature> getFeatures() {
        return features;
    }
}
