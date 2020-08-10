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
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.NotificationTarget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service
        implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    private MediaPlayer mMediaPlayer = new MediaPlayer();

    private final IBinder mBinder = new ServiceBinder();

    private List<Song> mSongs = new ArrayList<>();
    private static int currentSongPosition = 0;

    NotificationManager mNotificationManager;
    RemoteViews mRemoteViews;
    Notification mNotification;
    final int NOTIFICATION_ID = 1;

    private boolean mIsInitialized = false;
    private boolean mIsPlaying = false;


    private ImageButton mainPlayBtn;
    private SongAdapter songAdapter;

    private ImageButton pagePlayBtn;
    private Button pageNextBtn;
    private Button pagePrevBtn;
    private SeekBar pageSeekBar;
    private TextView pageTimerStartTv;
    private TextView pageTimerEndTv;
    private Handler mHandler = new Handler();
    private Runnable mRunnable;


    private final String TAG = "MusicService";

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

        mRunnable = new Runnable() {
            @Override
            public void run() {
                if (pageSeekBar != null && mMediaPlayer.getDuration() > 0) {
                    pageSeekBar.setProgress(mMediaPlayer.getCurrentPosition(), true);
                }
                mHandler.postDelayed(mRunnable, 1000);
            }
        };

        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.reset();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getStringExtra("action");

        Log.d(TAG, "onStartCommand - Action: " + action);
        Log.d(TAG, "onStartCommand - currentSong: " + mSongs.get(currentSongPosition));

        if (mMediaPlayer != null) {
            switch (action) {
                case "initial":
                    try {
                        if (!mIsInitialized) {
                            Log.d(TAG, "onStartCommand: mSongs.size(): " + mSongs.size());
                            mMediaPlayer.setDataSource(mSongs.get(currentSongPosition).getSongURL());
                            mMediaPlayer.prepareAsync();
//                            mMediaPlayer.prepare();
                            createNotification();
                            mIsInitialized = true;
                        } else {
                            if (mMediaPlayer.isPlaying()) {
                                mMediaPlayer.stop();
                            }
                            mMediaPlayer.reset();

                            mMediaPlayer.setDataSource(mSongs.get(currentSongPosition).getSongURL());
                            mMediaPlayer.prepareAsync();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case "play":
                    if (mMediaPlayer.isPlaying()) {
                        if (pause()) {
                            /**<-------Performing UI changes------->**/
                            if (mainPlayBtn != null) {
                                mainPlayBtn.setImageDrawable(getDrawable(
                                        R.drawable.ic_round_play_arrow_white_100));
                            }
                            if (pagePlayBtn != null) {
                                pagePlayBtn.setImageDrawable(getDrawable(
                                        R.drawable.ic_round_play_arrow_white_100));
                            }
                            /**<-------Performing notification changes------->**/
                            mRemoteViews.setImageViewResource(R.id.notif_play_btn,
                                    R.drawable.ic_round_play_arrow_grey_24);
                        }
                    } else {
                        if (play()) {
                            /**<-------Performing UI changes------->**/
                            if (mainPlayBtn != null) {
                                mainPlayBtn.setImageDrawable(getDrawable(
                                        R.drawable.ic_round_pause_white_100));
                            }
                            if (pagePlayBtn != null) {
                                pagePlayBtn.setImageDrawable(getDrawable(
                                        R.drawable.ic_round_pause_white_100));
                            }
                            /**<-------Performing notification changes------->**/
                            mRemoteViews.setImageViewResource(R.id.notif_play_btn,
                                    R.drawable.ic_round_pause_grey_24);
                        }
                    }
                    mNotificationManager.notify(NOTIFICATION_ID, mNotification);
                    break;

                case "next":
                    moveSong(true);
                    break;

                case "previous":
                    moveSong(false);
                    break;

                case "close":
                    stopForeground(true);
                    stopSelf();
                    mNotificationManager.cancel(NOTIFICATION_ID);
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        moveSong(true);
        Log.d(TAG, "onCompletion");
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "onPrepared");
        if (mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }

        if (mMediaPlayer != null) {
            Log.d(TAG, "onPrepared: Duration: " + mMediaPlayer.getDuration());
            mMediaPlayer.start();
            mIsPlaying = true;

            Song song = mSongs.get(currentSongPosition);

            /**<-------Performing notification changes------->**/
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


            /**<-------Performing UI changes------->**/
            songAdapter.notifyDataSetChanged();

            if (pageSeekBar != null) {
                pageSeekBar.setMax(mMediaPlayer.getDuration());
                pageSeekBar.setProgress(0);
                mRunnable.run();

                pageSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (mMediaPlayer != null && !fromUser) {
                            pageTimerStartTv.setText(milliSecondsToTimer(mMediaPlayer.getCurrentPosition()));
                            pageTimerEndTv.setText(milliSecondsToTimer(
                                    mMediaPlayer.getDuration() - mMediaPlayer.getCurrentPosition()));
                        }

                        if (fromUser) {
                            pageTimerStartTv.setText(milliSecondsToTimer(progress));
                            pageTimerEndTv.setText(milliSecondsToTimer(mMediaPlayer.getDuration() - progress));
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
    }

    private void moveSong(boolean isNext) {
        if (isNext) {
            if (currentSongPosition < mSongs.size() - 1) {
                currentSongPosition++;
            } else {
                currentSongPosition = 0;
            }

            if (pageNextBtn != null) {
                pageNextBtn.performClick();
            }
        } else {
            if (currentSongPosition <= 0) {
                currentSongPosition = mSongs.size() - 1;
            } else {
                currentSongPosition--;
            }

            if (pagePrevBtn != null) {
                pagePrevBtn.performClick();
            }
        }

        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
        mMediaPlayer.reset();
        try {
            mMediaPlayer.setDataSource(mSongs.get(currentSongPosition).getSongURL());
            mMediaPlayer.prepareAsync();
//            mMediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mIsPlaying = true;
    }

    private boolean play() {
        mIsPlaying = false;
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mIsPlaying = true;
            }
            mMediaPlayer.start();
            mIsPlaying = true;
        }
        return mIsPlaying;
    }

    private boolean pause() {
        mIsPlaying = true;
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
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
            notificationChannel.setSound(null, null);

            mNotificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelID);
        builder.setSmallIcon(R.drawable.ic_round_music_note_grey_50).setOnlyAlertOnce(true).
                setPriority(Notification.PRIORITY_MAX).setContentTitle("MuSeek");

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

        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this,
                4, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(activityPendingIntent);

        builder.setCustomBigContentView(mRemoteViews);

        mNotification = builder.build();
        mNotification.flags |= Notification.FLAG_NO_CLEAR;

        startForeground(NOTIFICATION_ID, mNotification);
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

    public static int getCurrentSongPosition() {
        return currentSongPosition;
    }

    public static void setCurrentSongPosition(int currentSongPosition) {
        if (currentSongPosition >= 0) {
            MusicService.currentSongPosition = currentSongPosition;
        }
    }

    public boolean isInitialized() {
        return mIsInitialized;
    }

    public boolean isPlaying() {
        return mIsPlaying;
    }

    public MediaPlayer getMediaPlayer() {
        return mMediaPlayer;
    }

    public void setMainPlayBtn(ImageButton mainPlayBtn) {
        this.mainPlayBtn = mainPlayBtn;
    }

    public void setPagePlayBtn(ImageButton pagePlayBtn) {
        this.pagePlayBtn = pagePlayBtn;
    }

    public void setPageNextBtn(Button pageNextBtn) {
        this.pageNextBtn = pageNextBtn;
    }

    public void setPagePrevBtn(Button pagePrevBtn) {
        this.pagePrevBtn = pagePrevBtn;
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

    public void setPageSeekBar(SeekBar pageSeekBar) {
        this.pageSeekBar = pageSeekBar;
    }

    public void setPageTimerStartTv(TextView pageTimerStartTv) {
        this.pageTimerStartTv = pageTimerStartTv;
    }

    public void setPageTimerEndTv(TextView pageTimerEndTv) {
        this.pageTimerEndTv = pageTimerEndTv;
    }

    public Runnable getRunnable() {
        return mRunnable;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
        }

        mIsPlaying = mIsInitialized = false;

        Log.d(TAG, "onDestroy");
    }
}
