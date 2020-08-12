package com.example.museseek;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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

    private ImageView mPhotoIv;
    private TextView mNameTv;
    private TextView mArtistTv;

    private String mPhotoPath;
    private String mName;
    private String mArtist;

    private ImageButton mPlayBtn;
    private Button mServiceNextBtn;
    private Button mServicePrevBtn;

    private SeekBar mSongSeekBar;
    private TextView mSongTimerStart;
    private TextView mSongTimerEnd;

    private final String TAG = "SongPage";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_page);

        mPhotoPath = getIntent().getStringExtra("photo_path");
        mName = getIntent().getStringExtra("name");
        mArtist = getIntent().getStringExtra("artist");
        boolean isDarkMode = getIntent().getBooleanExtra("is_dark", false);

        if (isDarkMode) {
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

        ImageButton nextBtn = findViewById(R.id.next_btn);
        mServiceNextBtn = findViewById(R.id.service_next_btn);

        ImageButton prevBtn = findViewById(R.id.previous_btn);
        mServicePrevBtn = findViewById(R.id.service_previous_btn);


        mPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initializePlayButton();

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


        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService.isInitialized()) {
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
                    int position = MusicService.getCurrentSongPosition();

                    Log.d(TAG, "ServiceNextOnClick" + position);

                    mName = mSongs.get(position).getName();
                    mArtist = mSongs.get(position).getArtist();
                    mPhotoPath = mSongs.get(position).getPhotoPath();
                    initializeSongPage(mName, mArtist, mPhotoPath);
                }
            }
        });


        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService.isInitialized()) {
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
                    int position = MusicService.getCurrentSongPosition();

                    Log.d(TAG, "ServicePrevOnClick" + position);

                    mName = mSongs.get(position).getName();
                    mArtist = mSongs.get(position).getArtist();
                    mPhotoPath = mSongs.get(position).getPhotoPath();
                    initializeSongPage(mName, mArtist, mPhotoPath);
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

            initializePlayButton();

            /**<-------Initializing the SeekBar------->**/
            mService.initializeSeekBar();
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
                placeholder(R.drawable.ic_default_song_pic).
                error(R.drawable.ic_default_song_pic);

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

    @SuppressLint("UseCompatLoadingForDrawables")
    private void initializePlayButton() {
        if (mService != null) {
            if (mService.isPlaying()) {
                mPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));
                Log.d(TAG, "Pause");
            } else {
                mPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_play_arrow_white_100));
                Log.d(TAG, "Play");
            }
        } else {
            mPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));
            Log.d(TAG, "Default Pause");
        }
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_song_page, R.anim.leave_song_page);
    }
}
