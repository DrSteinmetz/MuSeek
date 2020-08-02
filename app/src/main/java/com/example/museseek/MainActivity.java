package com.example.museseek;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
    private MusicService mService;
    private boolean mIsBound = false;

    private SharedPreferences mSharedPreferences;

    SongAdapter songAdapter;
    private List<Song> mSongs = new ArrayList<>();

    ImageButton play_btn;
    ImageButton next_btn;
    ImageButton prev_btn;

    private final String SONG_PATH = "songs";

    private boolean mIsDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        doBindService();

        mSharedPreferences = getSharedPreferences("user_pref", MODE_PRIVATE);


        /**<-------Initializing dark\light mode------->**/
        mIsDarkMode = mSharedPreferences.getBoolean("is_dark_mode", false);
        if (mIsDarkMode) {
            findViewById(R.id.main_layout).setBackgroundColor(getColor(R.color.colorGrey));
        } else {
            findViewById(R.id.main_layout).setBackgroundColor(getColor(R.color.colorAccent));
        }

        checkIfFirstUseOfApp();

        /**<-------Initializing songs urls array------->**/
        final ArrayList<String> songsURL = new ArrayList<>();
        for (Song song : mSongs) {
            songsURL.add(song.getSongURL());
        }


        /**<-------Initializing the RecyclerView------->**/
        play_btn = findViewById(R.id.play_btn);
        next_btn = findViewById(R.id.next_btn);
        prev_btn = findViewById(R.id.previous_btn);

        play_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService.isPlaying()) {
                    play_btn.setImageDrawable(getDrawable(R.drawable.ic_round_play_arrow_white_100));

                    Intent intent = new Intent(MainActivity.this, MusicService.class);
                    intent.putExtra("action", "play");
                    startService(intent);
                } else {
                    play_btn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));

                    Intent intent = new Intent(MainActivity.this, MusicService.class);
                    if (!mService.isInitialized()) {
                        View view = getLayoutInflater().inflate(R.layout.notification_layout, null);
                        ImageButton notifPlayBtn = view.findViewById(R.id.notif_play_btn);
                        mService.setPlayBtn(play_btn);
                        mService.setNotifPlayBtn(notifPlayBtn);

                        /**<-------Initializing Music Service------->**/
                        intent.putExtra("songs_url", songsURL);
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

                    Intent intent = new Intent(MainActivity.this, MusicService.class);
                    intent.putExtra("action", "next");
                    startService(intent);
                }
            }
        });

        prev_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService.isInitialized()) {
                    play_btn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));

                    Intent intent = new Intent(MainActivity.this, MusicService.class);
                    intent.putExtra("action", "previous");
                    startService(intent);
                }
            }
        });


        /**<-------Initializing the RecyclerView------->**/
        RecyclerView recyclerView = findViewById(R.id.recycler_view_layout);
        recyclerView.setHasFixedSize(true);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        songAdapter = new SongAdapter(mSongs, this);

        songAdapter.setListener(new SongAdapter.SongListener() {
            @Override
            public void onSongClicked(int position, View view) {
                Intent intent = new Intent(MainActivity.this, SongPageActivity.class);
                intent.putExtra("photo_path", mSongs.get(position).getPhotoPath());
                intent.putExtra("name", mSongs.get(position).getName());
                intent.putExtra("artist", mSongs.get(position).getArtist());
                intent.putExtra("is_url", mSongs.get(position).isPhotoFromURL());
                intent.putExtra("is_dark", mIsDarkMode);
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

                Song song = mSongs.remove(from);
                mSongs.add(to, song);

                songAdapter.notifyItemMoved(from, to);

                return false;
            }

            @Override
            public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == ItemTouchHelper.RIGHT) {
                    //TODO: Action on right swipe
                    songAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                } else if (direction == ItemTouchHelper.LEFT) {
                    showSongDeletionDialog(viewHolder.getAdapterPosition());
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        recyclerView.setAdapter(songAdapter);
    }

    /**<-------Initializing MusicService connection methods------->**/
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((MusicService.ServiceBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    private void doBindService() {
        bindService(new Intent(this, MusicService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    private void doUnbindService() {
        if (mIsBound) {
            unbindService(serviceConnection);
            mIsBound = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.dark_mode_op) {
            if (mIsDarkMode) {
                mIsDarkMode = false;
                findViewById(R.id.main_layout).setBackgroundColor(getColor(R.color.colorAccent));
            } else {
                mIsDarkMode = true;
                findViewById(R.id.main_layout).setBackgroundColor(getColor(R.color.colorGrey));
            }
            return true;
        } else if (item.getItemId() == R.id.add_song_op) {
            Toast.makeText(this, "Song Added", Toast.LENGTH_SHORT).show();
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

            mSongs.add(song_0);
            mSongs.add(song_1);
            mSongs.add(song_2);
            mSongs.add(song_3);
            mSongs.add(song_4);
            mSongs.add(song_5);

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
            mSongs = (ArrayList<Song>) objectInputStream.readObject();
            objectInputStream.close();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }

    private void saveSongsToFile() {
        try {
            FileOutputStream fileOutputStream = openFileOutput(SONG_PATH, MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(mSongs);
            objectOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void showSongDeletionDialog(final int songPosition) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogTheme);
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_song_deletion,
                (RelativeLayout) findViewById(R.id.layoutDialogContainer));

        builder.setView(view);
        builder.setCancelable(false);

        final EditText editText = view.findViewById(R.id.user_name_et);
        final ImageButton btn_cancel = view.findViewById(R.id.btn_cancel);
        final ImageButton btn_confirm = view.findViewById(R.id.btn_confirm);

        final AlertDialog alertDialog = builder.create();

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                songAdapter.notifyItemChanged(songPosition);
                alertDialog.dismiss();
            }
        });

        btn_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**<-------Saves the user's score------->*/
                mSongs.remove(songPosition);
                songAdapter.notifyItemRemoved(songPosition);
                alertDialog.dismiss();
            }
        });

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        alertDialog.show();
    }

    void showSongAddingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogTheme);
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_add_song,
                (RelativeLayout) findViewById(R.id.layoutDialogContainer));

        builder.setView(view);
        builder.setCancelable(false);

        final EditText editText = view.findViewById(R.id.user_name_et);
        final ImageButton btn_cancel = view.findViewById(R.id.btn_cancel);
        final ImageButton btn_confirm = view.findViewById(R.id.btn_confirm);

        final AlertDialog alertDialog = builder.create();

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        btn_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**<-------Saves the user's score------->*/
                alertDialog.dismiss();
            }
        });

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        alertDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        /**<-------Setting the play/pause button according to the Music service------->**/
        if (mService != null) {
            if (mService.isPlaying()) {
                Log.d("onResume", "Playing");
                play_btn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));
            } else {
                play_btn.setImageDrawable(getDrawable(R.drawable.ic_round_play_arrow_white_100));
                Log.d("onResume", "NOT Playing");
            }
        } else {
            Log.d("onResume", "Service is null");
            play_btn.setImageDrawable(getDrawable(R.drawable.ic_round_play_arrow_white_100));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!mIsBound) {
            doBindService();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mSharedPreferences.edit().putBoolean("is_dark_mode", mIsDarkMode).commit();
        saveSongsToFile();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mIsBound) {
            doUnbindService();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}