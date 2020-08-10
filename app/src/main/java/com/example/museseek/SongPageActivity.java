package com.example.museseek;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

public class SongPageActivity extends AppCompatActivity {
    private MusicService mService;
    private boolean mIsBound = false;

    private List<Song> mSongs = new ArrayList<>();
    private MediaPlayer mMediaPlayer;

    private ImageView mPhotoIv;
    private TextView mNameTv;
    private TextView mArtistTv;

    private String mPhotoPath;
    private String mName;
    private String mArtist;

    private ImageButton mPlayBtn;
    private ImageButton mNextBtn;
    private ImageButton mPrevBtn;
    private Button mServiceNextBtn;
    private Button mServicePrevBtn;

    private SeekBar mSongSeekBar;
    private TextView mSongTimerStart;
    private TextView mSongTimerEnd;

    private Runnable mRunnable;

    private boolean mIsDarkMode;
    private boolean mIsRestarted;

    private final String TAG = "SongPage";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_page);

        mPhotoPath = getIntent().getStringExtra("photo_path");
        mName = getIntent().getStringExtra("name");
        mArtist = getIntent().getStringExtra("artist");
        mIsDarkMode = getIntent().getBooleanExtra("is_dark", false);

        if (mIsDarkMode) {
            findViewById(R.id.song_page_layout).setBackgroundColor(getColor(R.color.colorGrey));
        } else {
            findViewById(R.id.song_page_layout).setBackgroundColor(getColor(R.color.colorAccent));
        }

        mSongSeekBar = findViewById(R.id.details_duration_sb);
        mSongTimerStart = findViewById(R.id.details_start_duration_tv);
        mSongTimerEnd = findViewById(R.id.details_end_duration_tv);

        mPhotoIv = findViewById(R.id.details_iv);
        mNameTv = findViewById(R.id.details_name_tv);
        mArtistTv = findViewById(R.id.details_artist_tv);

        /**<-------Initializing control bar------->**/
        mPlayBtn = findViewById(R.id.play_btn);

        mNextBtn = findViewById(R.id.next_btn);
        mServiceNextBtn = findViewById(R.id.service_next_btn);

        mPrevBtn = findViewById(R.id.previous_btn);
        mServicePrevBtn = findViewById(R.id.service_previous_btn);


        mPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService.isPlaying()) {
                    mPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_play_arrow_white_100));
                } else {
                    mPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));
                }

                Intent intent = new Intent(SongPageActivity.this, MusicService.class);
                if (!mService.isInitialized()) {
                    initializeService();
                    intent.putExtra("action", "initial");
                } else {
                    intent.putExtra("action", "play");
                }
                startService(intent);
            }
        });


        mNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService.isInitialized()) {
                    mPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));

                    Intent intent = new Intent(SongPageActivity.this, MusicService.class);
                    intent.putExtra("action", "next");
                    startService(intent);
                }
            }
        });
        mServiceNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService.isInitialized()) {
                    mPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));

                    int position = MusicService.getCurrentSongPosition();

                    Log.d(TAG, "onNextClick: " + position);

                    String photoPath = mSongs.get(position).getPhotoPath();
                    String name = mSongs.get(position).getName();
                    String artist = mSongs.get(position).getArtist();
                    initializeSongPage(name, artist, photoPath);
                }
            }
        });


        mPrevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService.isInitialized()) {
                    mPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));

                    Intent intent = new Intent(SongPageActivity.this, MusicService.class);
                    intent.putExtra("action", "previous");
                    startService(intent);
                }
            }
        });
        mServicePrevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService.isInitialized()) {
                    mPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));

                    int position = MusicService.getCurrentSongPosition();

                    String photoPath = mSongs.get(position).getPhotoPath();
                    String name = mSongs.get(position).getName();
                    String artist = mSongs.get(position).getArtist();
                    initializeSongPage(name, artist, photoPath);
                }
            }
        });
    }

    /**<-------Initializing MusicService connection methods------->**/
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mService = ((MusicService.ServiceBinder) service).getService();

            /**<-------Setting the play/pause button according to the Music service------->**/
            Log.d(TAG, "onServiceConnected: Service activated");
            mSongs = mService.getSongList();
            mService.setPagePlayBtn(mPlayBtn);
            mService.setPageNextBtn(mServiceNextBtn);
            mService.setPagePrevBtn(mServicePrevBtn);
            mService.setPageSeekBar(mSongSeekBar);
            mService.setPageTimerStartTv(mSongTimerStart);
            mService.setPageTimerEndTv(mSongTimerEnd);

            initializeSongPage(mName, mArtist, mPhotoPath);

            mMediaPlayer = mService.getMediaPlayer();

            if (mMediaPlayer.isPlaying()) {
                mPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));
            } else {
                mPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_play_arrow_white_100));
            }

            if (!mIsRestarted && mService != null) {
                Log.d(TAG, "onServiceConnected: Initializing seekBar");

                mSongSeekBar.setMax(mMediaPlayer.getDuration());
                mSongSeekBar.setProgress(0);
                mRunnable = mService.getRunnable();
                mRunnable.run();

                mSongSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (mMediaPlayer != null && !fromUser) {
                            mSongTimerStart.setText(milliSecondsToTimer(mMediaPlayer.getCurrentPosition()));
                            mSongTimerEnd.setText(milliSecondsToTimer(mMediaPlayer.getDuration() - mMediaPlayer.getCurrentPosition()));
                        }

                        if (fromUser) {
                            mSongTimerStart.setText(milliSecondsToTimer(progress));
                            mSongTimerEnd.setText(milliSecondsToTimer(mMediaPlayer.getDuration() - progress));
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        if (mMediaPlayer != null) {
                            mMediaPlayer.seekTo(seekBar.getProgress());
                        }
                    }
                });
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            mService = null;
        }
    };

    /**<-------Sets the MusicService Binding methods------->**/
    private void doBindService() {
        if (!mIsBound) {
            bindService(new Intent(this, MusicService.class), serviceConnection,
                    Context.BIND_AUTO_CREATE);
            mIsBound = true;
        }
    }

    private void doUnbindService() {
        Log.d(TAG, "doUnbindService: " + mIsBound);
        if (mIsBound) {
            unbindService(serviceConnection);
            mIsBound = false;
        }
    }

    private void initializeSongPage(String name, String artist, String photoPath) {
        RequestOptions options = new RequestOptions().
                placeholder(R.mipmap.ic_launcher_round).
                error(R.mipmap.ic_launcher_round);

        Glide.with(this).
                load(photoPath).
                apply(options).
                into(mPhotoIv);

        mNameTv.setText(name);
        mArtistTv.setText(artist);
    }

    private void initializeService() {
        /**<-------Passing on an instance of the play button so the
         *           service will be able to change the button too------->**/
        mService.setPagePlayBtn(mPlayBtn);
        /**<-------Initializing song list------->**/
        mService.setSongList(mSongs);
    }

    private String milliSecondsToTimer(long milliseconds){
        String finalTimerString = "";
        String secondsString = "";
        String minutesString = "";

        /**<-------Convert total duration into time------->**/
        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
        /**<-------Add hours if there------->**/
        if (hours > 0) {
            finalTimerString = hours + ":";
        }

        /**<-------Prepending 0 to seconds if it is one digit------->**/
        secondsString = (seconds < 10 ? "0" + seconds : "" + seconds);
        /**<-------Prepending 0 to minutes if it is one digit------->**/
        minutesString = (minutes < 10 ? "0" + minutes : "" + minutes);

        finalTimerString = finalTimerString + minutesString + ":" + secondsString;

        /**<-------Return timer string------->**/
        return finalTimerString;
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        mIsRestarted = true;

        Log.d(TAG, "onRestart");
    }

    @Override
    protected void onStart() {
        super.onStart();

        doBindService();

        Log.d(TAG, "onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mService != null) {
            mService.setPageNextBtn(null);
            mService.setPagePrevBtn(null);
            mService.setPagePlayBtn(null);
            mService.setPageSeekBar(null);
            mService.setPageTimerStartTv(null);
            mService.setPageTimerEndTv(null);
        }

        doUnbindService();
        Log.d(TAG, "onStop");
    }
}
