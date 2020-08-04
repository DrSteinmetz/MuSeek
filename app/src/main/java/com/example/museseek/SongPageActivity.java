package com.example.museseek;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
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

    ImageView photoIv;
    TextView nameTv;
    TextView artistTv;

    private ImageButton play_btn;
    private ImageButton next_btn;
    private ImageButton prev_btn;

    private boolean mIsDarkMode;

    private boolean mHasFocus;

    private final String TAG = "Life Cycle";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_page);

        String photoPath = getIntent().getStringExtra("photo_path");
        String name = getIntent().getStringExtra("name");
        String artist = getIntent().getStringExtra("artist");
        mIsDarkMode = getIntent().getBooleanExtra("is_dark", false);

        if (mIsDarkMode) {
            findViewById(R.id.song_page_layout).setBackgroundColor(getColor(R.color.colorGrey));
        } else {
            findViewById(R.id.song_page_layout).setBackgroundColor(getColor(R.color.colorAccent));
        }

        photoIv = findViewById(R.id.details_iv);
        nameTv = findViewById(R.id.details_name_tv);
        artistTv = findViewById(R.id.details_artist_tv);

        initializeSongPage(name, artist, photoPath);

        /**<-------Initializing control bar------->**/
        play_btn = findViewById(R.id.play_btn);
        next_btn = findViewById(R.id.next_btn);
        prev_btn = findViewById(R.id.previous_btn);

        play_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService.isPlaying()) {
                    play_btn.setImageDrawable(getDrawable(R.drawable.ic_round_play_arrow_white_100));

                    Intent intent = new Intent(SongPageActivity.this, MusicService.class);
                    intent.putExtra("action", "play");
                    startService(intent);
                } else {
                    play_btn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));

                    Intent intent = new Intent(SongPageActivity.this, MusicService.class);
                    if (!mService.isInitialized()) {
                        initializeService();
                        /**<-------Initializing Music Service------->**/
                        intent.putExtra("action", "initial");
                    } else {
                        intent.putExtra("action", "play");
                    }
                    startService(intent);
                }
            }
        });

        next_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService.isInitialized()) {
                    play_btn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));

                    int position = MusicService.getCurrentSongPosition() >= mSongs.size() - 1 ?
                            0 : MusicService.getCurrentSongPosition() + 1;

                    String photoPath = mSongs.get(position).getPhotoPath();
                    String name = mSongs.get(position).getName();
                    String artist = mSongs.get(position).getArtist();
                    initializeSongPage(name, artist, photoPath);

                    if (mHasFocus) {
                        Intent intent = new Intent(SongPageActivity.this, MusicService.class);
                        intent.putExtra("action", "next");
                        startService(intent);
                    }
                }
            }
        });

        prev_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService.isInitialized()) {
                    play_btn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));

                    int position = MusicService.getCurrentSongPosition() <= 0 ?
                            mSongs.size() - 1 : MusicService.getCurrentSongPosition() - 1;

                    String photoPath = mSongs.get(position).getPhotoPath();
                    String name = mSongs.get(position).getName();
                    String artist = mSongs.get(position).getArtist();
                    initializeSongPage(name, artist, photoPath);

                    if (mHasFocus) {
                        Intent intent = new Intent(SongPageActivity.this, MusicService.class);
                        intent.putExtra("action", "previous");
                        startService(intent);
                    }
                }
            }
        });
    }

    /**<-------Initializing MusicService connection methods------->**/
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((MusicService.ServiceBinder) service).getService();

            /**<-------Setting the play/pause button according to the Music service------->**/
            if (mService != null) {
                Log.d(TAG, "onServiceConnected: Service activated");
                mSongs = mService.getSongList();
                mService.setPagePlayBtn(play_btn);
                mService.setPageNextBtn(next_btn);
                mService.setPagePrevBtn(prev_btn);
                if (mService.isPlaying()) {
                    play_btn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));
                } else {
                    play_btn.setImageDrawable(getDrawable(R.drawable.ic_round_play_arrow_white_100));
                }
            } else {
                play_btn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));
                Log.d(TAG, "onServiceConnected: Service is NULL");
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
            bindService(new Intent(this, MusicService.class), serviceConnection, Context.BIND_AUTO_CREATE);
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
                into(photoIv);

        nameTv.setText(name);
        artistTv.setText(artist);
    }

    private void initializeService() {
        /**<-------Passing on an instance of the play button so the
         *           service will be able to change the button too------->**/
        mService.setPagePlayBtn(play_btn);
        /**<-------Initializing song list------->**/
        mService.setSongList(mSongs);
    }

    @Override
    protected void onStart() {
        super.onStart();

        doBindService();
        Log.d(TAG, "onStart");
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        mHasFocus = hasFocus;
        if (mService != null) {
            if (hasFocus) {
                mService.setPageNextBtn(null);
                mService.setPagePrevBtn(null);
            } else {
                mService.setPageNextBtn(next_btn);
                mService.setPagePrevBtn(prev_btn);
            }
        }
        Log.d(TAG, "onWindowFocusChanged: " + hasFocus);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mService != null) {
            mService.setPageNextBtn(null);
            mService.setPagePrevBtn(null);
        }

        doUnbindService();
        Log.d(TAG, "onStop");
    }
}
