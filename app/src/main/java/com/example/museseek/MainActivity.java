package com.example.museseek;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
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

    private File mFile;
    private Uri mSelectedImage;

    private SongAdapter mSongAdapter;
    private List<Song> mSongs = new ArrayList<>();

    private ImageButton playBtn;
    private ImageButton nextBtn;
    private ImageButton prevBtn;

    private final String SONG_PATH = "songs";

    private boolean mIsDarkMode;

    private final int CAMERA_REQUEST = 1;
    private final int GALLERY_REQUEST = 2;
    private final int WRITE_PERMISSION_REQUEST = 7;

    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSharedPreferences = getSharedPreferences("user_pref", MODE_PRIVATE);


        /**<-------Initializing dark\light mode------->**/
        mIsDarkMode = mSharedPreferences.getBoolean("is_dark_mode", false);
        if (mIsDarkMode) {
            findViewById(R.id.main_layout).setBackgroundColor(getColor(R.color.colorGrey));
        } else {
            findViewById(R.id.main_layout).setBackgroundColor(getColor(R.color.colorAccent));
        }


        checkIfFirstUseOfApp();


        /**<-------Initializing control bar------->**/
        playBtn = findViewById(R.id.play_btn);
        nextBtn = findViewById(R.id.next_btn);
        prevBtn = findViewById(R.id.previous_btn);

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**<-------Changing Play/Pause button------->**/
                if (mService.isPlaying()) {
                    playBtn.setImageDrawable(getDrawable(R.drawable.ic_round_play_arrow_white_100));
                } else {
                    playBtn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));
                }

                /**<-------Start the music------->**/
                Intent intent = new Intent(MainActivity.this, MusicService.class);
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
                    playBtn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));

                    Intent intent = new Intent(MainActivity.this, MusicService.class);
                    intent.putExtra("action", "next");
                    startService(intent);
                }
            }
        });

        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService.isInitialized()) {
                    playBtn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));

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

        mSongAdapter = new SongAdapter(mSongs, this);

        mSongAdapter.setListener(new SongAdapter.SongListener() {
            @Override
            public void onSongClicked(int position, View view) {
                if (mService.isPlaying()) {
                    playBtn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));
                }

                if (MusicService.getCurrentSongPosition() != position) {
                    MusicService.setCurrentSongPosition(position);

                    Log.d("onSongClicked(init)", "position: " + position);
                    Log.d("onSongClicked(init)", "mSongs.size(): " + mSongs.size());
                    Intent musicIntent = new Intent(MainActivity.this, MusicService.class);
                    if (!mService.isInitialized()) {
                        initializeService();
                    }
                    musicIntent.putExtra("action", "initial");
                    startService(musicIntent);
                } else if (!mService.isInitialized()) {
                    MusicService.setCurrentSongPosition(position);

                    Log.d("onSongClicked(notInit)", "position: " + position);
                    Log.d("onSongClicked(notInit)", "mSongs.size(): " + mSongs.size());
                    Intent musicIntent = new Intent(MainActivity.this, MusicService.class);
                    initializeService();
                    musicIntent.putExtra("action", "initial");
                    startService(musicIntent);
                }
                Log.d("onSongClicked", "position: " + position);
                Log.d("onSongClicked", "mSongs.size(): " + mSongs.size());

                Intent intent = new Intent(MainActivity.this, SongPageActivity.class);
                intent.putExtra("photo_path", mSongs.get(position).getPhotoPath());
                intent.putExtra("name", mSongs.get(position).getName());
                intent.putExtra("artist", mSongs.get(position).getArtist());
                intent.putExtra("is_dark", mIsDarkMode);
                startActivity(intent);
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

                if (from == MusicService.getCurrentSongPosition()) {
                    MusicService.setCurrentSongPosition(to);
                }

                mSongAdapter.notifyItemMoved(from, to);
                Song song = mSongs.remove(from);
                mSongs.add(to, song);

                if (mService != null) {
                    mService.setSongList(mSongs);
                }

                return false;
            }

            @Override
            public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == ItemTouchHelper.RIGHT) {
                    //TODO: Action on right swipe OR delete of there's no action
                    mSongAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                } else if (direction == ItemTouchHelper.LEFT) {
                    showSongDeletionDialog(viewHolder.getAdapterPosition());
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        recyclerView.setAdapter(mSongAdapter);
    }

    /**<-------Initializing MusicService connection methods------->**/
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((MusicService.ServiceBinder) service).getService();

            /**<-------Setting the play/pause button according to the Music service------->**/
            if (mService != null) {
                if (mService.isPlaying()) {
                    playBtn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));
                } else {
                    playBtn.setImageDrawable(getDrawable(R.drawable.ic_round_play_arrow_white_100));
                }
            } else {
                playBtn.setImageDrawable(getDrawable(R.drawable.ic_round_play_arrow_white_100));
            }

            initializeService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
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
        if (mIsBound) {
            unbindService(serviceConnection);
            mIsBound = false;
        }
    }


    /**<-------Initializing Menu------->**/
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
            /**<-------Requesting user permissions------->**/
            if (Build.VERSION.SDK_INT >= 23) {
                int hasWritePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (hasWritePermission != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            WRITE_PERMISSION_REQUEST);
                }
            }
            showSongAddingDialog();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_REQUEST) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Can't take picture", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("onActivityResult", "RESULT_OK = -1, resultCode = " + resultCode);
        //Result canceled every time taking a pic with the camera...
        Log.d("onActivityResult", "CAMERA_REQUEST = 1, requestCode = " + requestCode);
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            mSelectedImage = Uri.parse(mFile.getAbsolutePath());
            Log.d("onActivityResult", "CAMERA_REQUEST\n" + mSelectedImage.toString());
        } else if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK) {
            mSelectedImage = data.getData();
            Log.d("onActivityResult", "GALLERY_REQUEST");
        }
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

            Song song_3 = new Song("One More Cup Of Coffee", "Bob Dylan",
                    "http://www.syntax.org.il/xtra/bob.m4a",
                    "https://www.needsomefun.net/wp-content/uploads/2015/09/one_more_cup_of_cofee.jpg");
            song_2.setIsPhotoFromURL(true);
            Song song_4 = new Song("Sara", "Bob Dylan",
                    "http://www.syntax.org.il/xtra/bob1.m4a",
                    "https://images.rapgenius.com/f9fa75848596b53395fe7f6ccd25844c.619x414x1.jpg");
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

            saveSongsToFile(); //TODO: Remove to onPause when done developing!!!!!!!!
        } else {
            readSongsFromFile();
        }
    }

    private void initializeService() {
        /**<-------Passing on an instance of the play button so the
         *           service will be able to change the button too------->**/
        mService.setMainPlayBtn(playBtn);
        /**<-------Initializing song list------->**/
        mService.setSongList(mSongs);
        /**<-------Passing on an instance of the song adapter so the
         *           service will be able to change the UI too------->**/
        mService.setSongAdapter(mSongAdapter);
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

        final ImageButton btn_cancel = view.findViewById(R.id.btn_cancel);
        final ImageButton btn_confirm = view.findViewById(R.id.btn_confirm);

        final AlertDialog alertDialog = builder.create();

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSongAdapter.notifyItemChanged(songPosition);
                alertDialog.dismiss();
            }
        });

        btn_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**<-------If the song that the user intend to delete is playing right now
                 * than move on to the next song and delete the current song. If the song the user
                 * intend to delete is the current song but it isn't playing than move on to the
                 *                      next song and pause it.------->*/
                if (songPosition == MusicService.getCurrentSongPosition()) {
                    MusicService.setCurrentSongPosition(MusicService.getCurrentSongPosition() - 1);
                    if (mService.isPlaying()) {
                        nextBtn.performClick();
                    } else {
                        nextBtn.performClick();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                playBtn.performClick();
                            }
                        }, 250);
                    }
                }

                mSongs.remove(songPosition);
                mSongAdapter.notifyItemRemoved(songPosition);
                if (mService != null) {
                    mService.setSongList(mSongs);
                }
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

        final EditText name_et = view.findViewById(R.id.song_name_et);
        final EditText artist_et = view.findViewById(R.id.song_artist_et);
        final EditText url_et = view.findViewById(R.id.song_url_et);
        final ImageButton btn_gallery = view.findViewById(R.id.btn_gallery);
        final ImageButton btn_camera = view.findViewById(R.id.btn_camera);
        final ImageButton btn_cancel = view.findViewById(R.id.btn_cancel);
        final ImageButton btn_confirm = view.findViewById(R.id.btn_confirm);

        final AlertDialog alertDialog = builder.create();

        btn_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(Intent.createChooser(intent, "Select Image"), GALLERY_REQUEST);
            }
        });

        btn_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFile = new File(Environment.getExternalStorageDirectory(),
                        "song" + (mSongs != null ? mSongs.size() : 0) + ".jpg");
                mSelectedImage = FileProvider.getUriForFile(MainActivity.this,
                        "com.example.museseek.provider", mFile);
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mSelectedImage);
                startActivityForResult(intent, CAMERA_REQUEST);
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        btn_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**<-------Add a song to the list------->*/
                String songName = name_et.getText().toString();
                String songArtist = artist_et.getText().toString();
                String songURL = url_et.getText().toString();
                String photoUri = mSelectedImage != null ? mSelectedImage.toString() : "";

                /**<-------Checking if the user entered all the details------->*/
                if (songName.trim().length() < 1 || songArtist.trim().length() < 1
                        || songURL.trim().length() < 1) {
                    Toast.makeText(MainActivity.this, R.string.dialog_add_error, Toast.LENGTH_SHORT).show();
                } else {
                    Song song = new Song(songName, songArtist, songURL, photoUri);

                    mSongs.add(song);
                    mSongAdapter.notifyItemInserted(mSongs.size() - 1);
                    if (mService != null) {
                        mService.setSongList(mSongs);
                    }

                    mSelectedImage = null;
                    alertDialog.dismiss();
                }
            }
        });

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        alertDialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();

        doBindService();
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

        doUnbindService();
    }
}