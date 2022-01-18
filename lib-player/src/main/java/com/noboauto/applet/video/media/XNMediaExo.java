package com.noboauto.applet.video.media;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoSize;
import com.noboauto.applet.video.extension.XNMediaInterface;
import com.noboauto.applet.video.vd.Xnvd;

import cn.xnvd.R;

public class XNMediaExo extends XNMediaInterface implements Player.Listener {
    private ExoPlayer player;
    private Runnable callback;
    private String TAG = "JZMediaExo";
    private long previousSeek = 0;

    public XNMediaExo(Xnvd jzvd) {
        super(jzvd);
    }

    @Override
    public void start() {
        player.setPlayWhenReady(true);
    }

    @Override
    public void prepare() {
        Log.e(TAG, "prepare");
        Context context = jzvd.getContext();

        release();
        mMediaHandlerThread = new HandlerThread("JZVD");
        mMediaHandlerThread.start();
        mMediaHandler = new Handler(mMediaHandlerThread.getLooper());//主线程还是非主线程，就在这里
        handler = new Handler();
        mMediaHandler.post(() -> {
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

            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
                    Util.getUserAgent(context, context.getResources().getString(R.string.app_name)));

            String currUrl = jzvd.jzDataSource.getCurrentUrl().toString();
            MediaSource videoSource;
            if (currUrl.contains(".m3u8")) {
                videoSource = new DashMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(currUrl));
            } else {
                videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(currUrl));
            }
            player.addListener(this);

            Boolean isLoop = jzvd.jzDataSource.looping;
            if (isLoop) {
                player.setRepeatMode(Player.REPEAT_MODE_ONE);
            } else {
                player.setRepeatMode(Player.REPEAT_MODE_OFF);
            }
            player.prepare(videoSource);
            player.setPlayWhenReady(true);
            callback = new onBufferingUpdate();

            if (jzvd.textureView != null) {
                SurfaceTexture surfaceTexture = jzvd.textureView.getSurfaceTexture();
                if (surfaceTexture != null) {
                    player.setVideoSurface(new Surface(surfaceTexture));
                }
            }
        });

    }


    @Override
    public void pause() {
        player.setPlayWhenReady(false);
    }

    @Override
    public boolean isPlaying() {
        return player.getPlayWhenReady();
    }

    @Override
    public void seekTo(long time) {
        if (player == null) {
            return;
        }
        if (time != previousSeek) {
            player.seekTo(time);
            previousSeek = time;
            jzvd.seekToInAdvance = time;

        }
    }

    @Override
    public void release() {
        if (mMediaHandler != null && mMediaHandlerThread != null && player != null) {//不知道有没有妖孽
            HandlerThread tmpHandlerThread = mMediaHandlerThread;
            ExoPlayer tmpMediaPlayer = player;
            XNMediaInterface.SAVED_SURFACE = null;

            mMediaHandler.post(() -> {
                tmpMediaPlayer.release();//release就不能放到主线程里，界面会卡顿
                tmpHandlerThread.quit();
            });
            player = null;
        }
    }

    @Override
    public long getCurrentPosition() {
        if (player != null)
            return player.getCurrentPosition();
        else return 0;
    }

    @Override
    public long getDuration() {
        if (player != null)
            return player.getDuration();
        else return 0;
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        player.setVolume(leftVolume);
        player.setVolume(rightVolume);
    }

    @Override
    public void setSpeed(float speed) {
        PlaybackParameters playbackParameters = new PlaybackParameters(speed, 1.0F);
        player.setPlaybackParameters(playbackParameters);
    }

    @Override
    public void onRenderedFirstFrame() {
        Log.e(TAG, "onRenderedFirstFrame: 在渲染的第一帧上");
    }


    @Override
    public void onVideoSizeChanged(VideoSize videoSize) {
        handler.post(() -> jzvd.onVideoSizeChanged(videoSize.width, videoSize.height));
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        Log.e(TAG, "onLoadingChanged");
    }

    @Override
    public void onPlayerStateChanged(final boolean playWhenReady, final int playbackState) {
        Log.e(TAG, "onPlayerStateChanged" + playbackState + "/ready=" + String.valueOf(playWhenReady));
        handler.post(() -> {
            switch (playbackState) {
                case Player.STATE_IDLE: {
                }
                break;
                case Player.STATE_BUFFERING: {
                    handler.post(callback);
                }
                break;
                case Player.STATE_READY: {
                    if (playWhenReady) {
                        jzvd.onStatePlaying();
                    } else {
                    }
                }
                break;
                case Player.STATE_ENDED: {
                    jzvd.onCompletion();
                }
                break;
            }
        });
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }


    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {
        handler.post(() -> jzvd.onSeekComplete());
    }

    @Override
    public void setSurface(Surface surface) {
        if (player != null) {
            player.setVideoSurface(surface);
        } else {
            Log.e("AGVideo", "simpleExoPlayer为空");
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (SAVED_SURFACE == null) {
            SAVED_SURFACE = surface;
            prepare();
        } else {
            jzvd.textureView.setSurfaceTexture(SAVED_SURFACE);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private class onBufferingUpdate implements Runnable {
        @Override
        public void run() {
            mMediaHandler.post(() -> {
                if (player != null) {
                    final int percent = player.getBufferedPercentage();
                    handler.post(() -> jzvd.setBufferProgress(percent));
                    if (percent < 100) {
                        handler.postDelayed(callback, 300);
                    } else {
                        handler.removeCallbacks(callback);
                    }
                }
            });

        }
    }
}
