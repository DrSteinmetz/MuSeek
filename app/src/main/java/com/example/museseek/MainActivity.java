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
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.Snackbar;

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

    private RecyclerView mRecyclerView;
    private SongAdapter mSongAdapter;
    private List<Song> mSongs = new ArrayList<>();

    private ImageButton mPlayBtn;
    private ImageButton mNextBtn;
    private ImageButton mPrevBtn;
    private ImageView mControlBarImage;
    private TextView mControlBarName;
    private TextView mControlBarArtist;
    private ImageView mNoSongsAvailableIv;
    private Button mFinishBtn;

    private  ImageButton mDlgCameraBtn;
    private ImageView mDlgSelectedImageIv;

    private boolean mIsDarkMode;

    private final String SONG_PATH = "songs";

    private final int CAMERA_REQUEST = 1;
    private final int GALLERY_REQUEST = 2;
    private final int WRITE_PERMISSION_REQUEST = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSharedPreferences = getSharedPreferences("user_pref", MODE_PRIVATE);


        /**<-------Initializing dark\light mode------->**/
        mIsDarkMode = mSharedPreferences.getBoolean("is_dark_mode", true);
        if (mIsDarkMode) {
            findViewById(R.id.main_layout).setBackgroundColor(getColor(R.color.colorGrey));
        } else {
            findViewById(R.id.main_layout).setBackgroundColor(getColor(R.color.colorAccent));
        }


        checkIfFirstUseOfApp();


        /**<-------Initializing control bar------->**/
        mControlBarImage = findViewById(R.id.control_bar_song_image);
        mControlBarName = findViewById(R.id.control_bar_name_tv);
        mControlBarArtist = findViewById(R.id.control_bar_artist_tv);
        mPlayBtn = findViewById(R.id.play_btn);
        mNextBtn = findViewById(R.id.next_btn);
        mPrevBtn = findViewById(R.id.previous_btn);

        mNoSongsAvailableIv = findViewById(R.id.no_songs_iv);
        mFinishBtn = findViewById(R.id.finish_btn);

        mControlBarName.setSelected(true);

        Button controlBarSongPageBtn = findViewById(R.id.control_bar_song_page_btn);
        controlBarSongPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService.isInitialized() && !mSongs.isEmpty()) {
                    int position = MusicService.getCurrentSongPosition();
                    Intent intent = new Intent(MainActivity.this, SongPageActivity.class);
                    intent.putExtra("photo_path", mSongs.get(position).getPhotoPath());
                    intent.putExtra("name", mSongs.get(position).getName());
                    intent.putExtra("artist", mSongs.get(position).getArtist());
                    intent.putExtra("is_dark", mIsDarkMode);
                    startActivity(intent);
                    overridePendingTransition(R.anim.enter_main, R.anim.leave_main);
                }
            }
        });

        mPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService != null) {
                    /**<-------Changing Play/Pause button------->**/
                    if (mService.isPlaying()) {
                        mPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_play_arrow_white_100));
                    } else {
                        mPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));
                    }

                    /**<-------Start the music------->**/
                    Intent intent = new Intent(MainActivity.this, MusicService.class);
                    if (!mService.isInitialized()) {
                        initializeService();
                        intent.putExtra("action", "initial");
                    } else {
                        intent.putExtra("action", "play");
                        if (mControlBarArtist.getVisibility() == View.GONE) {
                            initializeControlBar();
                        }
                    }
                    startService(intent);
                }
            }
        });

        mNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService != null && mService.isInitialized()) {
                    mPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));

                    Intent intent = new Intent(MainActivity.this, MusicService.class);
                    intent.putExtra("action", "next");
                    startService(intent);
                }
            }
        });

        mPrevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService != null && mService.isInitialized()) {
                    mPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));

                    Intent intent = new Intent(MainActivity.this, MusicService.class);
                    intent.putExtra("action", "previous");
                    startService(intent);
                }
            }
        });

        mFinishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAffinity();
            }
        });


        /**<-------Initializing the RecyclerView------->**/
        mRecyclerView = findViewById(R.id.recycler_view_layout);
        mRecyclerView.setHasFixedSize(true);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mSongAdapter = new SongAdapter(mSongs, this);

        mSongAdapter.setListener(new SongAdapter.SongListener() {
            @Override
            public void onSongClicked(int position, View view) {
                if (mService.isPlaying()) {
                    mPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));
                }

                if (MusicService.getCurrentSongPosition() != position) {
                    MusicService.setCurrentSongPosition(position);

                    Intent musicIntent = new Intent(MainActivity.this, MusicService.class);
                    if (!mService.isInitialized()) {
                        initializeService();
                    }
                    musicIntent.putExtra("action", "initial");
                    startService(musicIntent);
                } else if (!mService.isInitialized()) {
                    MusicService.setCurrentSongPosition(position);

                    Intent musicIntent = new Intent(MainActivity.this, MusicService.class);
                    initializeService();
                    musicIntent.putExtra("action", "initial");
                    startService(musicIntent);
                }

                Intent intent = new Intent(MainActivity.this, SongPageActivity.class);
                intent.putExtra("photo_path", mSongs.get(position).getPhotoPath());
                intent.putExtra("name", mSongs.get(position).getName());
                intent.putExtra("artist", mSongs.get(position).getArtist());
                intent.putExtra("is_dark", mIsDarkMode);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_main, R.anim.leave_main);
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
                final int currentSongPosition = MusicService.getCurrentSongPosition();

                if (from == currentSongPosition) {
                    MusicService.setCurrentSongPosition(to);
                } else if (from < currentSongPosition && currentSongPosition <= to ) {
                    MusicService.setCurrentSongPosition(currentSongPosition - 1);
                } else if (to <= currentSongPosition && currentSongPosition < from) {
                    MusicService.setCurrentSongPosition(currentSongPosition + 1);
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
                    showEditSongDialog(viewHolder.getAdapterPosition());
                } else if (direction == ItemTouchHelper.LEFT) {
                    showSongDeletionDialog(viewHolder.getAdapterPosition());
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(mRecyclerView);

        mRecyclerView.setAdapter(mSongAdapter);
    }

    /**<-------Sets MusicService connection methods------->**/
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((MusicService.ServiceBinder) service).getService();

            /**<-------Sets the control bar according to the Music service------->**/
            if (mService != null) {
                if (mService.isPlaying()) {
                    mPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_pause_white_100));
                    initializeControlBar();
                } else {
                    mPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_play_arrow_white_100));
                }
            } else {
                mPlayBtn.setImageDrawable(getDrawable(R.drawable.ic_round_play_arrow_white_100));
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


    /**<-------Sets Menu------->**/
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
            showSongAddingDialog();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == WRITE_PERMISSION_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mDlgCameraBtn.setVisibility(View.VISIBLE);
            } else {
                mDlgCameraBtn.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_REQUEST) {
                if (mDlgSelectedImageIv != null) {
                    Glide.with(MainActivity.this)
                            .load(mSelectedImage)
                            .error(R.drawable.ic_default_song_pic)
                            .into(mDlgSelectedImageIv);

                    mDlgSelectedImageIv.setVisibility(View.VISIBLE);
                }
            } else if (requestCode == GALLERY_REQUEST) {
                if (data != null) {
                    mSelectedImage = data.getData();

                    if (mDlgSelectedImageIv != null) {
                        Glide.with(MainActivity.this)
                                .load(mSelectedImage)
                                .error(R.drawable.ic_default_song_pic)
                                .into(mDlgSelectedImageIv);

                        mDlgSelectedImageIv.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    }

    /**<-------Initialization methods------->*/
    public void checkIfFirstUseOfApp() {
        if (mSharedPreferences.getBoolean("is_first_use", true)) {
            /**<-------If it's the first time the user opens the app,
             *         initialize the app with these 3 songs------->**/

            Song song_0 = new Song("One More Cup Of Coffee", "Bob Dylan",
                    "http://www.syntax.org.il/xtra/bob.m4a",
                    "https://www.needsomefun.net/wp-content/uploads/2015/09/one_more_cup_of_cofee.jpg");

            Song song_1 = new Song("Sara", "Bob Dylan",
                    "http://www.syntax.org.il/xtra/bob1.m4a",
                    "https://images.rapgenius.com/f9fa75848596b53395fe7f6ccd25844c.619x414x1.jpg");

            Song song_2 = new Song("The Man In Me", "Bob Dylan",
                    "http://www.syntax.org.il/xtra/bob2.mp3",
                    "https://i.ytimg.com/vi/94jPU2gc1E0/maxresdefault.jpg");

            Song song_3 = new Song("Guaranteed", "Eddie Vedder",
                    "https://www.mboxdrive.com/Eddie%20Vedder%20-%20Guaranteed.mp3",
                    "https://www.musicmaniarecords.be/media/coverart-big/17951-into-the-wild.jpg");

            Song song_4 = new Song("Closing Time", "Semisonic",
                    "https://www.mboxdrive.com/Semisonic%20-%20Closing%20Time.mp3",
                    "https://img.discogs.com/_Tm33mfoOsUd2SCwAy_QTq0DppE=/fit-in/600x524/filters:strip_icc():format(jpeg):mode_rgb():quality(90)/discogs-images/R-1146898-1580504265-6407.jpeg.jpg");

            Song song_5 = new Song("Stay", "Thirty Seconds to Mars",
                    "https://www.mboxdrive.com/Thirty%20Seconds%20To%20Mars%20-%20Stay.mp3",
                    "https://a4-images.myspacecdn.com/images04/1/3881ba327864444b96011406b381fac3/600x600.jpg");

            mSongs.add(song_0);
            mSongs.add(song_1);
            mSongs.add(song_2);
            mSongs.add(song_3);
            mSongs.add(song_4);
            mSongs.add(song_5);

            mSharedPreferences.edit().putBoolean("is_first_use", false).commit();
        } else {
            readSongsFromFile();
        }
    }

    private void initializeService() {
        /**<-------Passing on an instance of the buttons so the
         *     service will be able to change the buttons as well------->**/
        mService.setMainPlayBtn(mPlayBtn);
        mService.setMainControlBarImage(mControlBarImage);
        mService.setMainControlBarName(mControlBarName);
        mService.setMainControlBarArtist(mControlBarArtist);
        mService.setNoSongsAvailableIv(mNoSongsAvailableIv);
        mService.setMainFinishBtn(mFinishBtn);
        /**<-------Initializing song list------->**/
        mService.setSongList(mSongs);
        /**<-------Passing on an instance of the song adapter so the
         *           service will be able to change the UI too------->**/
        mService.setSongAdapter(mSongAdapter);
    }

    private void initializeControlBar() {
        Song song = mSongs.get(MusicService.getCurrentSongPosition());

        RequestOptions options = new RequestOptions()
                .circleCrop()
                .placeholder(R.drawable.ic_default_song_pic)
                .error(R.drawable.ic_default_song_pic);

        Glide.with(MainActivity.this)
                .asBitmap()
                .load(song.getPhotoPath())
                .apply(options)
                .into(mControlBarImage);
        mControlBarName.setText(song.getName());
        mControlBarName.setSelected(true);
        mControlBarArtist.setVisibility(View.VISIBLE);
        mControlBarArtist.setText(song.getArtist());
        mControlBarArtist.setSelected(true);
    }

    /**<-------File handling methods------->*/
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

    /**<-------Dialogs------->*/
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
                        mNextBtn.performClick();
                    } else {
                        mNextBtn.performClick();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mPlayBtn.performClick();
                            }
                        }, 250);
                    }
                }

                final Song song = mSongs.remove(songPosition);
                mSongAdapter.notifyItemRemoved(songPosition);
                if (mService != null) {
                    mService.setSongList(mSongs);
                }
                alertDialog.dismiss();

                /**<-------Popping up SnackBar for the 'UNDO' option------->**/
                Snackbar.make(mRecyclerView, R.string.dlg_dlt_undo_tv, Snackbar.LENGTH_LONG).
                        setTextColor(getResources().getColor(R.color.colorPurple1, null)).
                        setActionTextColor(getResources().getColor(R.color.colorAccent, null)).
                        setBackgroundTint(getResources().getColor(R.color.colorPrimaryDark, null)).
                        setAction(R.string.dlg_dlt_undo_btn, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mSongs.add(songPosition, song);
                                mSongAdapter.notifyItemInserted(songPosition);
                            }
                        }).show();
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

        final TextView title_tv = view.findViewById(R.id.title_tv);
        title_tv.setText(R.string.dlg_add_title_tv);

        final EditText nameEt = view.findViewById(R.id.song_name_et);
        final EditText artistEt = view.findViewById(R.id.song_artist_et);
        final EditText urlEt = view.findViewById(R.id.song_url_et);
        final ImageButton galleryBtn = view.findViewById(R.id.btn_gallery);
        mDlgCameraBtn = view.findViewById(R.id.btn_camera);
        final ImageButton cancelBtn = view.findViewById(R.id.btn_cancel);
        final ImageButton confirmBtn = view.findViewById(R.id.btn_confirm);
        mDlgSelectedImageIv = view.findViewById(R.id.selected_image_iv);
        mDlgSelectedImageIv.setClipToOutline(true);
        mDlgSelectedImageIv.setVisibility(View.GONE);

        /**<-------Requesting user permissions------->**/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int hasWritePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWritePermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        WRITE_PERMISSION_REQUEST);
            }
        }

        final AlertDialog alertDialog = builder.create();

        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(Intent.createChooser(intent,
                        getResources().getString(R.string.dlg_add_gallery_pic)), GALLERY_REQUEST);
            }
        });

        mDlgCameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                        "museek" + System.nanoTime() + "pic.jpg");
                mSelectedImage = FileProvider.getUriForFile(MainActivity.this,
                        "com.example.museseek.provider", mFile);
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mSelectedImage);
                startActivityForResult(intent, CAMERA_REQUEST);
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**<-------Add a song to the list------->*/
                String songName = nameEt.getText().toString();
                String songArtist = artistEt.getText().toString();
                String songURL = urlEt.getText().toString();
                String photoUri = mSelectedImage != null ? mSelectedImage.toString() : null;

                /**<-------Checks if the user entered all the details------->*/
                if (songName.trim().length() < 1 || songArtist.trim().length() < 1
                        || songURL.trim().length() < 1) {
                    if (songName.trim().length() < 1) {
                        nameEt.setError(getString(R.string.dlg_add_name_error));
                    } else { nameEt.setError(null); }

                    if (songArtist.trim().length() < 1) {
                        artistEt.setError(getString(R.string.dlg_add_artist_error));
                    } else { artistEt.setError(null); }

                    if (songURL.trim().length() < 1) {
                        urlEt.setError(getString(R.string.dlg_add_url_error));
                    } else { urlEt.setError(null); }
                } else {
                    nameEt.setError(null);
                    artistEt.setError(null);
                    urlEt.setError(null);

                    /**<-------Checks if the user entered a valid URL------->*/
                    if (Patterns.WEB_URL.matcher(songURL).matches()) {
                        Song song = new Song(songName, songArtist, songURL, photoUri);

                        mSongs.add(song);
                        mSongAdapter.notifyItemInserted(mSongs.size() - 1);
                        if (mService != null) {
                            mService.setSongList(mSongs);
                        }

                        mSelectedImage = null;
                        alertDialog.dismiss();
                    } else {
                        nameEt.setError(null);
                        artistEt.setError(null);
                        urlEt.setError(getString(R.string.dlg_add_bad_url));
                    }
                }
            }
        });

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        alertDialog.show();
    }

    void showEditSongDialog(final int songPosition) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogTheme);
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_add_song,
                (RelativeLayout) findViewById(R.id.layoutDialogContainer));

        builder.setView(view);
        builder.setCancelable(false);

        final TextView title_tv = view.findViewById(R.id.title_tv);
        title_tv.setText(R.string.dlg_edit_title_tv);

        final EditText name_et = view.findViewById(R.id.song_name_et);
        final EditText artist_et = view.findViewById(R.id.song_artist_et);
        final EditText url_et = view.findViewById(R.id.song_url_et);
        final ImageButton btn_gallery = view.findViewById(R.id.btn_gallery);
        mDlgCameraBtn = view.findViewById(R.id.btn_camera);
        final ImageButton btn_cancel = view.findViewById(R.id.btn_cancel);
        final ImageButton btn_confirm = view.findViewById(R.id.btn_confirm);
        mDlgSelectedImageIv = view.findViewById(R.id.selected_image_iv);
        mDlgSelectedImageIv.setClipToOutline(true);
        mDlgSelectedImageIv.setVisibility(View.VISIBLE);

        /**<-------Requesting user permissions------->**/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int hasWritePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWritePermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        WRITE_PERMISSION_REQUEST);
            }
        }

        final Song song = mSongs.get(songPosition);
        name_et.setText(song.getName());
        artist_et.setText(song.getArtist());
        url_et.setText(song.getSongURL());
        Glide.with(this)
                .load(song.getPhotoPath())
                .error(R.drawable.ic_default_song_pic)
                .into(mDlgSelectedImageIv);

        final AlertDialog alertDialog = builder.create();

        btn_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(Intent.createChooser(intent,
                        getResources().getString(R.string.dlg_add_gallery_pic)), GALLERY_REQUEST);
            }
        });

        mDlgCameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFile = new File(Environment.getExternalStorageDirectory(),
                        "museek" + System.nanoTime() + "pic.jpg");
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
                mSongAdapter.notifyItemChanged(songPosition);
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
                String photoUri = mSelectedImage != null
                        ? mSelectedImage.toString()
                        : song.getPhotoPath();

                /**<-------Checks if the user entered all the details------->*/
                if (songName.trim().length() < 1 || songArtist.trim().length() < 1
                        || songURL.trim().length() < 1) {
                    Toast.makeText(MainActivity.this, R.string.dlg_add_name_error, Toast.LENGTH_SHORT).show();
                } else {
                    /**<-------Checks if the user entered a valid URL------->*/
                    if (Patterns.WEB_URL.matcher(songURL).matches()) {
                        song.setName(songName);
                        song.setArtist(songArtist);
                        song.setSongURL(songURL);
                        song.setPhotoPath(photoUri);

                        mSongAdapter.notifyItemChanged(songPosition);
                        if (mService != null) {
                            mService.setSongList(mSongs);
                        }

                        mSelectedImage = null;
                        alertDialog.dismiss();
                    } else {
                        Toast.makeText(MainActivity.this, R.string.dlg_add_bad_url, Toast.LENGTH_SHORT).show();
                    }
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

        if (mService != null) {
            mService.setMainPlayBtn(null);
            mService.setMainControlBarImage(null);
            mService.setMainControlBarName(null);
            mService.setMainControlBarArtist(null);
            mService.setNoSongsAvailableIv(null);
            mService.setMainFinishBtn(null);
        }

        doUnbindService();
    }
}