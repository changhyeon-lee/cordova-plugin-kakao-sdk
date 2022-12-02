package com.raccoondev85.plugin.kakao;

import android.util.Log;

import com.kakao.sdk.auth.AuthCodeClient;
import com.kakao.sdk.common.KakaoSdk;
import com.kakao.sdk.user.UserApiClient;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class KakaoCordovaSDK extends CordovaPlugin {

    private static final String LOG_TAG = "KakaoCordovaSDK";

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        Log.v(LOG_TAG, "kakao : initialize");
        super.initialize(cordova, webView);
        try {
            KakaoResources.initResources(cordova.getActivity().getApplication());
            KakaoSdk.init(this.cordova.getContext(), KakaoResources.KAKAO_APP_KEY);
        } catch (Exception e) {

        }

    }

    public boolean execute(final String action, final JSONArray options, final CallbackContext callbackContext)
            throws JSONException {
        Log.v(LOG_TAG, "kakao : execute " + action);
        cordova.setActivityResultCallback(this);

        if (action.equals("login")) {
            this.login(callbackContext, options);
            return true;
        }
        else if (action.equals("logout")) {
            this.logout(callbackContext);
            return true;
        }
        return false;
    }

    private void login(final CallbackContext callbackContext, final JSONArray options) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                UserApiClient.getInstance().loginWithKakaoTalk(
                        cordova.getContext(),
                        AuthCodeClient.DEFAULT_REQUEST_CODE,
                        null,
                        null,
                        (token, error) -> {

                            // 에러 발생 시
                            if (error != null) {
                                Log.e(LOG_TAG, "로그인 실패", error);
                                JSONObject jsonError = new JSONObject();
                                try {
                                    jsonError.put("error", error.getMessage());
                                    callbackContext.error(jsonError);
                                } catch (JSONException e) {
                                    callbackContext.error(e.getMessage());
                                }
                            }
                            // 토큰이 존재하는 경우
                            else if (token != null) {

                                Log.i(LOG_TAG, "로그인 성공 ${token.accessToken}: " + token.getAccessToken());

                                UserApiClient.getInstance().me((user, meError) -> {

                                    // 에러 발생 시
                                    if (meError != null) {
                                        Log.e(LOG_TAG, "사용자 정보 요청 실패", meError);
                                        JSONObject jsonError = new JSONObject();
                                        try {
                                            jsonError.put("error", meError.getMessage());
                                            callbackContext.error(jsonError);
                                        } catch (JSONException e) {
                                            callbackContext.error(e.getMessage());
                                        }
                                    }
                                    // 사용자 정보가 존재하는 경우
                                    else if (user != null) {

                                        List<String> scopes = new ArrayList<>();

                                        // 카카오 계정 정보가 존재하는 경우
                                        if(user.getKakaoAccount() != null) {
                                            if (Boolean.TRUE.equals(user.getKakaoAccount().getEmailNeedsAgreement()))
                                                scopes.add("account_email");
                                            if (Boolean.TRUE.equals(user.getKakaoAccount().getBirthdayNeedsAgreement()))
                                                scopes.add("birthday");
                                            if (Boolean.TRUE.equals(user.getKakaoAccount().getBirthyearNeedsAgreement()))
                                                scopes.add("birthyear");
                                            if (Boolean.TRUE.equals(user.getKakaoAccount().getGenderNeedsAgreement()))
                                                scopes.add("gender");
                                            if (Boolean.TRUE.equals(user.getKakaoAccount().getPhoneNumberNeedsAgreement()))
                                                scopes.add("phone_number");
                                            if (Boolean.TRUE.equals(user.getKakaoAccount().getProfileNeedsAgreement()))
                                                scopes.add("profile");
                                            if (Boolean.TRUE.equals(user.getKakaoAccount().getAgeRangeNeedsAgreement()))
                                                scopes.add("age_range");
                                            if (Boolean.TRUE.equals(user.getKakaoAccount().getCiNeedsAgreement()))
                                                scopes.add("account_ci");
                                        }

                                        // 추가된 계정 정보가 존재하지 않는 경우
                                        if (scopes.size() == 0) {

                                            JSONObject jsonError = new JSONObject();
                                            try {
                                                jsonError.put("accessToken", token.getAccessToken());
                                                callbackContext.success(jsonError);
                                            } catch (JSONException e) {
                                                callbackContext.error(e.getMessage());
                                            }
                                        }
                                        // 추가된 계정 정보가 존재하는 경우
                                        else {
                                            Log.d(LOG_TAG, "사용자에게 추가 동의를 받아야 합니다.");

                                            //scope 목록을 전달하여 카카오 로그인 요청
                                            UserApiClient.getInstance().loginWithNewScopes(
                                                    cordova.getContext(),
                                                    scopes,
                                                    null,
                                                    (newToken, newError) -> {
                                                        if (newError != null) {
                                                            Log.e(LOG_TAG, "사용자 추가 동의 실패", newError);
                                                            JSONObject jsonError = new JSONObject();
                                                            try {
                                                                jsonError.put("error", newError.getMessage());
                                                                callbackContext.error(jsonError);
                                                            } catch (JSONException e) {
                                                                callbackContext.error(e.getMessage());
                                                            }
                                                        } else {
                                                            Log.d(LOG_TAG, "allowed scopes: ${token!!.scopes}");

                                                            // 사용자 정보 재요청
                                                            UserApiClient.getInstance().me((newUser, newMeError) -> {
                                                                if (newMeError != null) {
                                                                    Log.e(LOG_TAG, "사용자 정보 요청 실패", newMeError);
                                                                    JSONObject jsonError = new JSONObject();
                                                                    try {
                                                                        jsonError.put("error", newMeError.getMessage());
                                                                        callbackContext.error(jsonError);
                                                                    } catch (JSONException e) {
                                                                        callbackContext.error(e.getMessage());
                                                                    }
                                                                }
                                                                else if (newUser != null) {
                                                                    Log.i(LOG_TAG, "사용자 정보 요청 성공");

                                                                    JSONObject jsonError = new JSONObject();
                                                                    try {
                                                                        jsonError.put("accessToken", newToken.getAccessToken());
                                                                        callbackContext.success(jsonError);
                                                                    } catch (JSONException e) {
                                                                        callbackContext.error(e.getMessage());
                                                                    }
                                                                }

                                                                return null;
                                                            });
                                                        }

                                                        return null;
                                                    }
                                            );
                                        }
                                    }

                                    return null;
                                });
                            }

                            return null;
                        }
                );
           }
        });

    }

    private void logout(final CallbackContext callbackContext) {

        UserApiClient.getInstance().logout((error) -> {

            // 에러 발생 시
            if (error != null) {
                Log.e(LOG_TAG, "로그인 실패", error);
                JSONObject jsonError = new JSONObject();
                try {
                    jsonError.put("error", error.getMessage());
                    callbackContext.error(jsonError);
                } catch (JSONException e) {
                    callbackContext.error(e.getMessage());
                }
            }
            // 성공 시
            else {
                Log.v(LOG_TAG, "kakao : onCompleteLogout");
                callbackContext.success("true");
            }

            return null;
        });
    }
}
