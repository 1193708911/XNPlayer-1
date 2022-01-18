package com.noboauto.demo;

import android.app.Application;

import com.noboauto.applet.audio.PlayManager;


public class App extends Application {


    @Override
    public void onCreate() {
        super.onCreate();

        init();
    }

    private void init() {
        PlayManager.initPlayer(this);
    }
}
