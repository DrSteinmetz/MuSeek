package com.example.museseek;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.NotificationTarget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MusicService extends Service
        implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener {

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

    private boolean mIsShuffle = false;
    private boolean mIsRepeat = false;
    /*private ArrayList<Long> SongsForShuffle = new ArrayList<>();*/


    private SongAdapter songAdapter;

    private ImageButton mainPlayBtn;
    private ImageView mainControlBarImage;
    private TextView mainControlBarName;
    private TextView mainControlBarArtist;

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

        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.reset();

        /**<-------Initializing the runnable for the SeekBar------->**/
        mRunnable = new Runnable() {
            @Override
            public void run() {
                if (pageSeekBar != null && mMediaPlayer.getDuration() > 0) {
                    pageSeekBar.setProgress(mMediaPlayer.getCurrentPosition(), true);
                }
                mHandler.postDelayed(mRunnable, 1000);
            }
        };
    }

    @SuppressLint("UseCompatLoadingForDrawables")
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
                        mIsRepeat = false;
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

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onPrepared(MediaPlayer mp) {
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

            RequestOptions options = new RequestOptions()
                    .circleCrop()
                    .placeholder(R.drawable.ic_default_song_pic)
                    .error(R.drawable.ic_default_song_pic);

            Glide.with(MusicService.this)
                    .asBitmap()
                    .load(song.getPhotoPath() == null ? R.drawable.ic_default_song_pic : song.getPhotoPath())
                    .apply(options)
                    .into(notificationTarget);

            mNotificationManager.notify(NOTIFICATION_ID, mNotification);


            /**<-------Performing UI changes------->**/
            if (mainControlBarImage != null) {
                Glide.with(MusicService.this)
                        .asBitmap()
                        .load(song.getPhotoPath())
                        .apply(options)
                        .into(mainControlBarImage);
            }
            if (mainControlBarName != null) {
                mainControlBarName.setText(song.getName());
                mainControlBarName.setSelected(true);
            }
            if (mainControlBarArtist != null) {
                mainControlBarArtist.setVisibility(View.VISIBLE);
                mainControlBarArtist.setText(song.getArtist());
                mainControlBarArtist.setSelected(true);
            }

            if (pagePlayBtn != null) {
                pagePlayBtn.setImageDrawable(getDrawable(
                        R.drawable.ic_round_pause_white_100));
            }
            songAdapter.notifyDataSetChanged();
            initializeSeekBar();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "onError: " + what);
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.reset();
            try {
                mMediaPlayer.setDataSource(mSongs.get(currentSongPosition).getSongURL());
                mMediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private void moveSong(boolean isNext) {
        if (!mIsRepeat) {
            if (mIsShuffle) {
                shuffle();
            } else {
                if (isNext) {
                    moveToNextSong();
                } else {
                    moveToPreviousSong();
                }
            }
        }

        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
        mMediaPlayer.reset();
        try {
            mMediaPlayer.setDataSource(mSongs.get(currentSongPosition).getSongURL());
            mMediaPlayer.prepareAsync();
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
        builder.setPriority(Notification.PRIORITY_MAX).setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_museek_notif_icon)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_museek_icon));

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

    public void initializeSeekBar() {
        if (mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }

        if (pageSeekBar != null) {
            Log.d(TAG, "initializeSeekBar");

            pageSeekBar.setMax(mMediaPlayer.getDuration());
            pageTimerEndTv.setText(milliSecondsToTimer(mMediaPlayer.getDuration()));
            mRunnable.run();

            pageSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (mMediaPlayer != null && !fromUser) {
                        pageTimerStartTv.setText(milliSecondsToTimer(mMediaPlayer.getCurrentPosition()));
                    }

                    if (fromUser) {
                        pageTimerStartTv.setText(milliSecondsToTimer(progress));
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

    private String milliSecondsToTimer(long milliseconds) {
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

    private void moveToNextSong() {
        if (currentSongPosition < mSongs.size() - 1) {
            currentSongPosition++;
        } else {
            currentSongPosition = 0;
        }

        if (pageNextBtn != null) {
            pageNextBtn.performClick();
        }
    }

    private void moveToPreviousSong() {
        if (currentSongPosition <= 0) {
            currentSongPosition = mSongs.size() - 1;
        } else {
            currentSongPosition--;
        }

        if (pagePrevBtn != null) {
            pagePrevBtn.performClick();
        }
    }

    private void shuffle() {
        Random random = new Random();
        int prevSong = currentSongPosition;

        currentSongPosition = random.nextInt(mSongs.size());
        while (currentSongPosition == prevSong) {
            currentSongPosition = random.nextInt(mSongs.size());
        }

        if (pageNextBtn != null) {
            pageNextBtn.performClick();
        }
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
        if (mMediaPlayer != null) {
            return mMediaPlayer.isPlaying();
        }
        return false;
    }

    public void setMainPlayBtn(ImageButton mainPlayBtn) {
        this.mainPlayBtn = mainPlayBtn;
    }

    public boolean isShuffle() {
        return mIsShuffle;
    }

    public void setIsShuffle(boolean isShuffle) {
        this.mIsShuffle = isShuffle;
    }

    public boolean isRepeat() {
        return mIsRepeat;
    }

    public void setIsRepeat(boolean isRepeat) {
        this.mIsRepeat = isRepeat;
    }

    public void setMainControlBarImage(ImageView mainControlBarImage) {
        this.mainControlBarImage = mainControlBarImage;
    }

    public void setMainControlBarName(TextView mainControlBarName) {
        this.mainControlBarName = mainControlBarName;
    }

    public void setMainControlBarArtist(TextView mainControlBarArtist) {
        this.mainControlBarArtist = mainControlBarArtist;
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

    public void setPageSeekBar(SeekBar pageSeekBar) {
        this.pageSeekBar = pageSeekBar;
    }

    public void setPageTimerStartTv(TextView pageTimerStartTv) {
        this.pageTimerStartTv = pageTimerStartTv;
    }

    public void setPageTimerEndTv(TextView pageTimerEndTv) {
        this.pageTimerEndTv = pageTimerEndTv;
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
