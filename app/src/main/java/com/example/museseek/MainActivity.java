package com.example.museseek;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences mSharedPreferences;

    private NotificationManager notificationManager;
    private final int NOTIFICATION_ID = 1;

    private List<Song> songs = new ArrayList<>();

    private final String SONG_PATH = "songs";

    private boolean isDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSharedPreferences = getSharedPreferences("user_pref", MODE_PRIVATE);

        /**<-------Initializing dark\light mode------->**/
        isDarkMode = mSharedPreferences.getBoolean("is_dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        checkIfFirstUseOfApp();


        /**<-------Initializing notification------->**/
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        String channelID = null;
        if (Build.VERSION.SDK_INT >= 26) {
            channelID = "music_channel_id";
            CharSequence channelName = "MuSeek_Channel";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(channelID, channelName, importance);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);

            notificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, channelID);
        builder.setSmallIcon(R.drawable.ic_round_music_note_white_100).
                setPriority(Notification.PRIORITY_MAX);
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);
        Intent intent = new Intent(MainActivity.this, SongPageActivity.class);
        intent.putExtra("name", "notification");
        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.notif_play_btn, pendingIntent);

        builder.setContent(remoteViews);
        //builder.setCustomBigContentView(remoteViews);

        Notification notification = builder.build();
        notification.defaults = Notification.DEFAULT_VIBRATE;
        notification.flags |= Notification.FLAG_NO_CLEAR;
        notificationManager.notify(NOTIFICATION_ID, notification);


        /**<-------Initializing the RecyclerView------->**/
        RecyclerView recyclerView = findViewById(R.id.recycler_view_layout);
        recyclerView.setHasFixedSize(true);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        final SongAdapter songAdapter = new SongAdapter(songs, this);

        songAdapter.setListener(new SongAdapter.SongListener() {
            @Override
            public void onSongClicked(int position, View view) {
                Intent intent = new Intent(MainActivity.this, SongPageActivity.class);
                intent.putExtra("photo_path", songs.get(position).getmPhotoPath());
                intent.putExtra("name", songs.get(position).getmName());
                intent.putExtra("artist", songs.get(position).getmArtist());
                intent.putExtra("is_url", songs.get(position).isPhotoFromURL());
                startActivity(intent);
            }

            @Override
            public void onSongLongClicked(int position, View view) {
            }
        });

        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                final int from = viewHolder.getAdapterPosition();
                final int to = target.getAdapterPosition();

                Song song = songs.remove(from);
                songs.add(to, song);

                songAdapter.notifyItemMoved(from, to);

                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == ItemTouchHelper.RIGHT) {
                    //TODO: Action on right swipe
                    songAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                } else if (direction == ItemTouchHelper.LEFT) {
                    songs.remove(viewHolder.getAdapterPosition());
                    songAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                    //TODO: Add a confirmation dialog on song delete
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        recyclerView.setAdapter(songAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.dark_mode_op) {
            if (isDarkMode) {
                isDarkMode = false;
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                isDarkMode = true;
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void checkIfFirstUseOfApp() {
        if (mSharedPreferences.getBoolean("is_first_use", true)) {
            /**<-------If it's the first time the user opens the app,
             *         initialize the app with these 3 songs------->**/

            Song song_0 = new Song("One More Cup Of Coffee", "Bob Dylan",
                    "http://www.syntax.org.il/xtra/bob.m4a",
                    "https://www.needsomefun.net/wp-content/uploads/2015/09/one_more_cup_of_cofee.jpg");
            song_0.setIsPhotoFromURL(true);

            Song song_1 = new Song("Sara", "Bob Dylan",
                    "http://www.syntax.org.il/xtra/bob1.m4a",
                    "https://images.rapgenius.com/f9fa75848596b53395fe7f6ccd25844c.619x414x1.jpg");
            song_1.setIsPhotoFromURL(true);

            Song song_2 = new Song("The Man In Me", "Bob Dylan",
                    "http://www.syntax.org.il/xtra/bob2.mp3",
                    "https://i.ytimg.com/vi/94jPU2gc1E0/maxresdefault.jpg");
            song_2.setIsPhotoFromURL(true);

            Song song_3 = new Song("The Man In Me", "Bob Dylan",
                    "http://www.syntax.org.il/xtra/bob2.mp3",
                    "https://i.ytimg.com/vi/94jPU2gc1E0/maxresdefault.jpg");
            song_2.setIsPhotoFromURL(true);
            Song song_4 = new Song("The Man In Me", "Bob Dylan",
                    "http://www.syntax.org.il/xtra/bob2.mp3",
                    "https://i.ytimg.com/vi/94jPU2gc1E0/maxresdefault.jpg");
            song_2.setIsPhotoFromURL(true);
            Song song_5 = new Song("The Man In Me", "Bob Dylan",
                    "http://www.syntax.org.il/xtra/bob2.mp3",
                    "https://i.ytimg.com/vi/94jPU2gc1E0/maxresdefault.jpg");
            song_2.setIsPhotoFromURL(true);

            songs.add(song_0);
            songs.add(song_1);
            songs.add(song_2);
            songs.add(song_3);
            songs.add(song_4);
            songs.add(song_5);

            mSharedPreferences.edit().putBoolean("is_first_use", false).commit();

            saveSongsToFile(); //Remove to onPause when done developing!!!!!!!!
        } else {
            readSongsFromFile();
        }
    }

    private void readSongsFromFile() {
        try {
            FileInputStream fileInputStream = openFileInput(SONG_PATH);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            songs = (ArrayList<Song>) objectInputStream.readObject();
            objectInputStream.close();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }

    private void saveSongsToFile() {
        try {
            FileOutputStream fileOutputStream = openFileOutput(SONG_PATH, MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(songs);
            objectOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mSharedPreferences.edit().putBoolean("is_dark_mode", isDarkMode).commit();
        saveSongsToFile();
    }
}