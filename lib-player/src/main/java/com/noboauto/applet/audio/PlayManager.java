package com.noboauto.applet.audio;

import static com.google.android.exoplayer2.Player.STATE_BUFFERING;
import static com.google.android.exoplayer2.Player.STATE_ENDED;
import static com.google.android.exoplayer2.Player.STATE_IDLE;
import static com.google.android.exoplayer2.Player.STATE_READY;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayManager {

    private static final String TAG = "PlayManager";

    public static final int UPDATE_PROGRESS_MSG = 1;
    public static final int REMOVE_PROGRESS_MSG = 2;
    public static final int updateDelay = 500;
    private static IPlayerListener eventListener;

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {

            switch (msg.what) {
                case UPDATE_PROGRESS_MSG:

                    updateProgress();
                    msg = Message.obtain();
                    msg.what = UPDATE_PROGRESS_MSG;
                    handler.sendMessageDelayed(msg, updateDelay);
                    break;
                case REMOVE_PROGRESS_MSG:
                    handler.removeMessages(UPDATE_PROGRESS_MSG);
                    break;
            }

        }
    };

    private void updateProgress() {
        if (player != null && player.isPlaying()) {
            long currentPosition = player.getCurrentPosition();
            if (eventListener != null && currentPosition > 0 && mCurrState != IPlayerListener.State.PLAYING) {
                eventListener.playStateChange(IPlayerListener.State.PLAYING);
                mCurrState = IPlayerListener.State.PLAYING;
            }
            long duration = player.getDuration();
            eventListener.updateProgress(duration, currentPosition);
        }
    }

    private PlayManager() {

    }

    public static void initPlayer(Context context) {
        // 创建带宽
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter.Builder(context).build();
        // 创建轨道选择工厂 视频每一这的画面如何渲染,实现默认的实现类
        RenderersFactory videoTrackSelectionFactory = new DefaultRenderersFactory(context);
        // 创建轨道选择实例 视频的音视频轨道如何加载,使用默认的轨道选择器
        TrackSelector trackSelector = new DefaultTrackSelector(context);
        // 创建播放器实例
        player = new ExoPlayer.Builder(context, videoTrackSelectionFactory)
                .setBandwidthMeter(bandwidthMeter)
                .setTrackSelector(trackSelector)
                .build();

        // 添加一个监听器来接收来自播放器的事件.

        player.addListener(new Player.Listener() {
            @Override
            public void onIsLoadingChanged(boolean isLoading) {
                Log.d(TAG, "onIsLoadingChanged: " + isLoading);

            }

            @Override
            public void onPlaybackStateChanged(int state) {
                Log.d(TAG, "onPlaybackStateChanged: " + state);
                if (eventListener != null) {
                    switch (state) {
                        case STATE_IDLE:
                        case STATE_ENDED:
                            eventListener.playStateChange(IPlayerListener.State.IDLE);
                            mCurrState = IPlayerListener.State.IDLE;
                            break;
                        case STATE_BUFFERING:
                            eventListener.playStateChange(IPlayerListener.State.BUFFERING);
                            mCurrState = IPlayerListener.State.BUFFERING;

                            break;
                        case STATE_READY:
                            eventListener.playStateChange(IPlayerListener.State.READY);
                            mCurrState = IPlayerListener.State.READY;
                            break;


                    }
                }
            }

            @Override
            public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
                Log.d(TAG, "onPlayWhenReadyChanged: " + playWhenReady);
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Log.d(TAG, "onIsPlayingChanged: " + isPlaying);
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.d(TAG, "onPlayerError: " + error);
                if (eventListener != null) {
                    eventListener.playError(error.getMessage());
                }
            }
        });

    }

    public static PlayManager get() {
        return Holder.INSTANCE;
    }

    public void fastForward(long millions) {
        long pos = player.getCurrentPosition() + millions;

        if (pos > player.getDuration()) {
            pos = player.getCurrentPosition();
        }
        player.seekTo(pos);
    }

    public void rewind(Long millions) {
        Long pos = player.getCurrentPosition() - millions;

        if (pos < 0) {
            pos = 0L;
        }

        player.seekTo(pos);
    }

    private static class Holder {
        private static PlayManager INSTANCE = new PlayManager();
    }

    // 创建播放器实例
    private static ExoPlayer player;
    private List<MediaItem> mediaItems = new ArrayList<>();
    private int playCurrPos = -1;

    public void play(MediaItem mediaItem) {
        mediaItems = Collections.singletonList(mediaItem);
        play();
    }

    public void addMediaSource(List<MediaItem> mediaItems) {
        if (mediaItems == null) {
            return;
        }
        this.mediaItems.clear();
        this.mediaItems.addAll(mediaItems);
    }

    public void addMediaItems(MediaItem mediaItem) {
        if (mediaItem == null) {
            return;
        }
        this.mediaItems.add(mediaItem);
    }

    public void play(String uri) {
        if (!player.isPlaying() && player.getPlaybackState() == STATE_READY) {
            player.play();
        }
        MediaItem mediaItem = new MediaItem.Builder().setUri(uri).build();
        mediaItems = Collections.singletonList(mediaItem);
        play();
    }

    public void play() {
        if (mediaItems.isEmpty()) {
            throw new NullPointerException("mediaItems is empty");
        }
        if (player.isPlaying()) {
            return;
        }
        if (!player.isPlaying() && player.getPlaybackState() == STATE_READY) {
            player.play();
            handler.sendEmptyMessage(UPDATE_PROGRESS_MSG);
            return;
        }
        if (playCurrPos == -1) {
            playCurrPos = 0;
        }

        player.setMediaItems(mediaItems);
        player.prepare();
        player.play();

        handler.sendEmptyMessage(UPDATE_PROGRESS_MSG);
    }

    public void pause() {
        if (player.isPlaying()) {
            player.pause();
            eventListener.playStateChange(IPlayerListener.State.PAUSED);
            mCurrState = IPlayerListener.State.PAUSED;
        }
        handler.removeMessages(UPDATE_PROGRESS_MSG);
    }

    public void stop() {

        player.stop();
        if (eventListener != null && mCurrState != IPlayerListener.State.IDLE) {
            eventListener.playStateChange(IPlayerListener.State.IDLE);
        }
        handler.sendEmptyMessage(REMOVE_PROGRESS_MSG);
    }

    public void seekTo(long seek) {

        if (player != null) {
            player.seekTo(seek);
        }

    }

    String mp3Url = "https://api.frdic.com/api/v3/media/mp3/ebc3fc81-fa2f-4aa1" +
            "-b1cd-042f7dde8377?mediatype=audio&agent=%2Feusoft_ting_en_android%2F9.2.1%2F%2Fcar%2F%2F&token=QYN%20mp3_db6aeb75d9e31b9a916b223323a154c0da1c5d144fdc2c42fd810254a256951a";

    public void nextSong() {
        if (player.hasNext()) {
            player.next();
        }

    }

    public void previousSong() {
        if (player.hasPrevious()) {
            player.previous();
        }
    }

    static IPlayerListener.State mCurrState = IPlayerListener.State.IDLE;

    public IPlayerListener.State getCurrentState() {
        return mCurrState;
    }

    private void checkThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("Player is accessed on the wrong thread");
        }
    }

    public void setPlayListener(IPlayerListener eventListener) {
        this.eventListener = eventListener;
    }

    public void destory() {
        if (player != null) {
            player.release();
        }
    }
}
