package com.noboauto.applet.audio;

public interface IPlayerListener {

    enum State {
        /**
         * 空闲状态
         */
        IDLE,
        /**
         * 当前播放位置需要缓冲
         */
        BUFFERING,
        /**
         * 当前播放位置可以播放
         */
        READY,
        /**
         * 播放中
         */
        PLAYING,
        /**
         * 已暂停
         */
        PAUSED,

    }

    void updateProgress(long duration, long progress);

    void playStateChange(State state);

    void playError(String error);
}
