package com.example.museseek;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

public class SongPageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_page);

        String photoURL = getIntent().getStringExtra("photo_path");
        String name = getIntent().getStringExtra("name");
        String artist = getIntent().getStringExtra("artist");
        boolean isPhotoFromURL = getIntent().getBooleanExtra("is_url", false);

        ImageView photoIv = findViewById(R.id.details_iv);
        TextView nameTv = findViewById(R.id.details_name_tv);
        TextView artistTv = findViewById(R.id.details_artist_tv);

        RequestOptions options = new RequestOptions().
                placeholder(R.mipmap.ic_launcher_round).
                error(R.mipmap.ic_launcher_round);

        Glide.with(this).
                load(photoURL).
                apply(options).
                into(photoIv);

        nameTv.setText(name);
        artistTv.setText(artist);
    }
}
