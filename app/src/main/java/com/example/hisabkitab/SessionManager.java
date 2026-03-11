package com.example.hisabkitab;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "user_session";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NAME = "name";
    private static final String KEY_UID = "uid";

    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    public SessionManager(Context context){
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveUserSession(String uid, String email, String name){
        editor.putString(KEY_UID, uid);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_NAME, name);
        editor.apply();
    }

    public String getUserEmail(){
        return prefs.getString(KEY_EMAIL, null);
    }

    public String getUserName(){
        return prefs.getString(KEY_NAME, null);
    }

    public String getUserUid(){
        return prefs.getString(KEY_UID, null);
    }

    public boolean isLoggedIn(){
        return getUserUid() != null;
    }

    public void clearSession(){
        editor.clear();
        editor.apply();
    }
}