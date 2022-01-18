package com.noboauto.demo;


import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.noboauto.applet.audio.IPlayerListener;
import com.noboauto.applet.audio.PlayManager;
import com.noboauto.applet.audio.TimeUtil;
import com.noboauto.applet.video.media.XNBasicStd;
import com.noboauto.applet.video.media.XNMediaExo;
import com.noboauto.applet.video.vd.XnvdStd;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView mTimer;
    private SeekBar mSeekbar;
    private TextView mEndTime;
    private Button mPlay;
    private Button mPause;
    private Button mStop;
    private Button mNext;
    private Button mPrevious;
    private Button mFastforward;
    private Button mRewind;
    private TextView mStatus;
    private XNBasicStd mXnBasicStd;
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        PlayManager.get().setPlayListener(new IPlayerListener() {
            @Override
            public void updateProgress(long duration, long progress) {
                Log.d(TAG, "updateProgress:duration= " + TimeUtil.asTime(duration) + "  progress=" + TimeUtil.asTime(progress));
                mTimer.setText(TimeUtil.asTime(progress));
                mEndTime.setText(TimeUtil.asTime(duration));
                mSeekbar.setMax((int) duration);
                mSeekbar.setProgress((int) progress);
            }

            @Override
            public void playStateChange(State state) {
                Log.d(TAG, "playStateChange: " + System.currentTimeMillis() + "  state=" + state);
                mStatus.setText(state.toString());
            }

            @Override
            public void playError(String error) {

            }
        });


        initVideo();
    }

    private void initVideo() {
        mXnBasicStd.setUp("http://vfx.mtime.cn/Video/2019/02/04/mp4/190204084208765161.mp4"
                , "开始行动", XnvdStd.SCREEN_NORMAL, XNMediaExo.class);
        Glide.with(this)
                .load("")
                .into(mXnBasicStd.posterImageView);
    }


    private void initView() {
        mXnBasicStd = findViewById(R.id.std_video);
        mTimer = findViewById(R.id.timer);
        mSeekbar = findViewById(R.id.seekbar);
        mEndTime = findViewById(R.id.endTime);
        mStatus = findViewById(R.id.status);

        mPlay = findViewById(R.id.play);
        mPause = findViewById(R.id.pause);
        mStop = findViewById(R.id.stop);
        mNext = findViewById(R.id.next);
        mPrevious = findViewById(R.id.previous);
        mFastforward = findViewById(R.id.fastforward);
        mRewind = findViewById(R.id.rewind);

        mPlay.setOnClickListener(this);
        mPause.setOnClickListener(this);
        mStop.setOnClickListener(this);
        mNext.setOnClickListener(this);
        mPrevious.setOnClickListener(this);
        mFastforward.setOnClickListener(this);
        mRewind.setOnClickListener(this);
        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onStopTrackingTouch: " + seekBar.getProgress());
                PlayManager.get().seekTo(seekBar.getProgress());
            }
        });

    }

    String mp3Url = "http://repo.bfw.wiki/bfwrepo/sound/5c89fd22dea6948307.mp3";
    String mp3Url1 = "https://api.frdic.com/api/v3/media/mp3/ebc3fc81-fa2f-4aa1" +
            "-b1cd-042f7dde8377?mediatype=audio&agent=%2Feusoft_ting_en_android%2F9.2.1%2F%2Fcar%2F%2F&token=QYN%20mp3_db6aeb75d9e31b9a916b223323a154c0da1c5d144fdc2c42fd810254a256951a";

    private void initData() {
//        PlayManager.get().addMediaItems(new MediaItem.Builder().setUri(mp3Url).build());
//        PlayManager.get().addMediaItems(new MediaItem.Builder().setUri(mp3Url1).build());
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play:
                PlayManager.get().play(mp3Url);
                break;
            case R.id.pause:
                PlayManager.get().pause();
                break;
            case R.id.stop:
                PlayManager.get().stop();
                break;
            case R.id.next:
                PlayManager.get().nextSong();
                break;
            case R.id.previous:
                PlayManager.get().previousSong();
                break;
            case R.id.fastforward:

                PlayManager.get().fastForward(5000L);
                break;
            case R.id.rewind:
                PlayManager.get().rewind(5000L);
                break;
        }
    }
}