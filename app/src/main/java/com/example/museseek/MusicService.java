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

import java.io.IOException;
import java.util.ArrayList;

public class MusicService extends Service
        implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    private MediaPlayer mediaPlayer = new MediaPlayer();

    private final IBinder mBinder = new ServiceBinder();

    private ArrayList<String> mSongs = new ArrayList<>();
    private static int currentSongPosition = 0;

    final int NOTIFICATION_ID = 1;

    private boolean mIsInitialized = false;
    private boolean mIsPlaying = false;

    private ImageButton mPlayBtn;
    private ImageButton mNotifPlayBtn;

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
                    mSongs = intent.getStringArrayListExtra("songs_url");
                    try {
                        mediaPlayer.setDataSource(mSongs.get(currentSongPosition));
                        mediaPlayer.prepareAsync();
                        createNotification();
                        mIsInitialized = true;
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
                            if (mNotifPlayBtn != null) {
                                mNotifPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_play_arrow_grey_24));
                                //need to rebuild the notification for changing the image
                            }
                        }
                    } else {
                        if (play()) {
                            if (mPlayBtn != null) {
                                mPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));
                            }
                            if (mNotifPlayBtn != null) {
                                mNotifPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_grey_24));
                            }
                        }
                    }
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
                    stopSelf();
                    stopForeground(true);
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
            mediaPlayer.setDataSource(mSongs.get(currentSongPosition));
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
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        String channelID = null;
        CharSequence channelName = "MuSeek_Channel";
        channelID = "oron_music_channel_id";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel notificationChannel = new NotificationChannel(channelID, channelName,
                    NotificationManager.IMPORTANCE_HIGH);
            //notificationChannel.enableLights(false);
            //notificationChannel.enableVibration(false);

            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelID);
        builder.setSmallIcon(R.drawable.ic_round_music_note_grey_50).
                setPriority(Notification.PRIORITY_MAX);

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);

        Intent playIntent = new Intent(this, MusicService.class);
        playIntent.putExtra("action", "play");
        PendingIntent playPendingIntent = PendingIntent.getService(this,
                0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.notif_play_btn, playPendingIntent);

        Intent previousIntent = new Intent(this, MusicService.class);
        previousIntent.putExtra("action", "previous");
        PendingIntent previousPendingIntent = PendingIntent.getService(this,
                1, previousIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.notif_previous_btn, previousPendingIntent);

        Intent nextIntent = new Intent(this, MusicService.class);
        nextIntent.putExtra("action", "next");
        PendingIntent nextPendingIntent = PendingIntent.getService(this,
                2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.notif_next_btn, nextPendingIntent);

        Intent closeIntent = new Intent(this, MusicService.class);
        closeIntent.putExtra("action", "close");
        PendingIntent closePendingIntent = PendingIntent.getService(this,
                3, closeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.notif_close_btn, closePendingIntent);

        builder.setCustomContentView(remoteViews);

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR;

        startForeground(NOTIFICATION_ID, notification);
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

    public void setNotifPlayBtn(ImageButton notifPlayBtn) {
        this.mNotifPlayBtn = notifPlayBtn;
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
