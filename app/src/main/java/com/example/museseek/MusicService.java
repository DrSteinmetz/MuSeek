package com.example.museseek;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.NotificationTarget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service
        implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    private MediaPlayer mediaPlayer = new MediaPlayer();

    private final IBinder mBinder = new ServiceBinder();

    private List<Song> mSongs = new ArrayList<>();
    private static int currentSongPosition = 0;

    NotificationManager mNotificationManager;
    RemoteViews mRemoteViews;
    Notification mNotification;
    final int NOTIFICATION_ID = 1;

    private boolean mIsInitialized = false;
    private boolean mIsPlaying = false;

    private ImageButton mPlayBtn;
    private SongAdapter songAdapter;

    public MusicService() {
    }

    public class ServiceBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.reset();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getStringExtra("action");

        Log.d("Action:", action);

        if (mediaPlayer != null) {
            switch (action) {
                case "initial":
                    try {
                        if (!mIsInitialized) {
                            mediaPlayer.setDataSource(mSongs.get(currentSongPosition).getSongURL());
                            mediaPlayer.prepareAsync();
                            createNotification();
                            mIsInitialized = true;
                        } else {
                            mediaPlayer.reset();
                            mediaPlayer.setDataSource(mSongs.get(currentSongPosition).getSongURL());
                            mediaPlayer.prepareAsync();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case "play":
                    if (mediaPlayer.isPlaying()) {
                        if (pause()) {
                            if (mPlayBtn != null) {
                                mPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_play_arrow_white_100));
                            }
                            mRemoteViews.setImageViewResource(R.id.notif_play_btn,
                                    R.drawable.ic_round_play_arrow_grey_24);
                        }
                    } else {
                        if (play()) {
                            if (mPlayBtn != null) {
                                mPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));
                            }
                            mRemoteViews.setImageViewResource(R.id.notif_play_btn,
                                    R.drawable.ic_round_pause_grey_24);
                        }
                    }
                    mNotificationManager.notify(NOTIFICATION_ID, mNotification);
                    break;

                case "next":
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                    moveSong(true);
                    break;

                case "previous":
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                    moveSong(false);
                    break;

                case "close":
                    stopForeground(true);
                    stopSelf();
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        moveSong(true);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            mIsPlaying = true;

            Song song = mSongs.get(currentSongPosition);
            mRemoteViews.setImageViewResource(R.id.notif_play_btn,
                    R.drawable.ic_round_pause_grey_24);
            mRemoteViews.setTextViewText(R.id.notif_song_name_tv,
                    song.getName());
            mRemoteViews.setTextViewText(R.id.notif_song_artist_tv,
                    song.getArtist());
            NotificationTarget notificationTarget = new NotificationTarget(
                    MusicService.this,
                    R.id.notif_song_image,
                    mRemoteViews,
                    mNotification,
                    NOTIFICATION_ID
            );
            Glide.with(MusicService.this).asBitmap().load(song.getPhotoPath()).
                    circleCrop().into(notificationTarget);

            mNotificationManager.notify(NOTIFICATION_ID, mNotification);

            songAdapter.notifyDataSetChanged();
        }
    }

    private void moveSong(boolean isNext) {
        if (isNext) {
            if (currentSongPosition < mSongs.size() - 1) {
                currentSongPosition++;
            } else {
                currentSongPosition = 0;
            }
        } else {
            if (currentSongPosition <= 0) {
                currentSongPosition = mSongs.size() - 1;
            } else {
                currentSongPosition--;
            }
        }

        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(mSongs.get(currentSongPosition).getSongURL());
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mIsPlaying = true;
    }

    private boolean play() {
        mIsPlaying = false;
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mIsPlaying = true;
            }
            mediaPlayer.start();
            mIsPlaying = true;
        }
        return mIsPlaying;
    }

    private boolean pause() {
        mIsPlaying = true;
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                mIsPlaying = false;
            }
        }
        return !mIsPlaying;
    }

    private void createNotification() {
        /**<-------Initializing notification------->**/
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        String channelID = null;
        CharSequence channelName = "MuSeek_Channel";
        channelID = "oron_music_channel_id";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel notificationChannel = new NotificationChannel(channelID, channelName,
                    NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);

            mNotificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelID);
        builder.setSmallIcon(R.drawable.ic_round_music_note_grey_50).
                setPriority(Notification.PRIORITY_MAX);

        mRemoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);

        Intent playIntent = new Intent(this, MusicService.class);
        playIntent.putExtra("action", "play");
        PendingIntent playPendingIntent = PendingIntent.getService(this,
                0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.notif_play_btn, playPendingIntent);

        Intent previousIntent = new Intent(this, MusicService.class);
        previousIntent.putExtra("action", "previous");
        PendingIntent previousPendingIntent = PendingIntent.getService(this,
                1, previousIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.notif_previous_btn, previousPendingIntent);

        Intent nextIntent = new Intent(this, MusicService.class);
        nextIntent.putExtra("action", "next");
        PendingIntent nextPendingIntent = PendingIntent.getService(this,
                2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.notif_next_btn, nextPendingIntent);

        Intent closeIntent = new Intent(this, MusicService.class);
        closeIntent.putExtra("action", "close");
        PendingIntent closePendingIntent = PendingIntent.getService(this,
                3, closeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.notif_close_btn, closePendingIntent);

        builder.setCustomBigContentView(mRemoteViews);

        mNotification = builder.build();
        mNotification.flags |= Notification.FLAG_NO_CLEAR;

        startForeground(NOTIFICATION_ID, mNotification);
    }

    public static int getCurrentSongPosition() {
        return currentSongPosition;
    }

    public static void setCurrentSongPosition(int currentSongPosition) {
        MusicService.currentSongPosition = currentSongPosition;
    }

    public boolean isInitialized() {
        return mIsInitialized;
    }

    public boolean isPlaying() {
        return mIsPlaying;
    }

    public void setPlayBtn(ImageButton playBtn) {
        this.mPlayBtn = playBtn;
    }

    public List<Song> getSongList() {
        return mSongs;
    }

    public void setSongList(List<Song> songList) {
        this.mSongs = songList;
    }

    public SongAdapter getSongAdapter() {
        return songAdapter;
    }

    public void setSongAdapter(SongAdapter songAdapter) {
        this.songAdapter = songAdapter;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
        }

        mIsPlaying = mIsInitialized = false;
    }
}
