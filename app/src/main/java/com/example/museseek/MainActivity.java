package com.example.museseek;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

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
    private List<Song> songs = new ArrayList<>();

    private final String SONG_PATH = "songs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSharedPreferences = getSharedPreferences("user_pref", MODE_PRIVATE);

        checkIfFirstUseOfApp();

        RecyclerView recyclerView = findViewById(R.id.recycler_view_layout);
        recyclerView.setHasFixedSize(true);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        final SongAdapter songAdapter = new SongAdapter(songs, this);

        songAdapter.setListener(new SongAdapter.SongListener() {
            @Override
            public void onSongClicked(int position, View view) {
            }

            @Override
            public void onSongLongClicked(int position, View view) {
            }
        });

        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT|ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == ItemTouchHelper.RIGHT) {
                    //TODO: Action on right swipe
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

    public void checkIfFirstUseOfApp() {
        if (mSharedPreferences.getBoolean("is_first_use", true)) {
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

            songs.add(song_0);
            songs.add(song_1);
            songs.add(song_2);

            mSharedPreferences.edit().putBoolean("is_first_use", false).commit();
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
        saveSongsToFile();

        super.onPause();
    }
}