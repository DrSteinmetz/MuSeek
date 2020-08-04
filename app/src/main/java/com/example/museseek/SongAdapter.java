package com.example.museseek;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
    private List<Song> songs;

    private Context context;

    private boolean firstRun = true;

    public SongAdapter(List<Song> songs, Context context) {
        this.songs = songs;
        this.context = context;
    }

    private SongListener listener;

    interface SongListener {
        void onSongClicked(int position, View view);
        void onSongLongClicked(int position, View view);
    }

    public void setListener(SongListener listener) {
        this.listener = listener;
    }

    public class SongViewHolder extends RecyclerView.ViewHolder {
        ImageView photoIv;
        TextView nameTv;
        TextView artistTv;
        CardView cardLayout;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);

            photoIv = itemView.findViewById(R.id.photo_iv);
            nameTv = itemView.findViewById(R.id.name_tv);
            artistTv = itemView.findViewById(R.id.artist_tv);
            cardLayout = itemView.findViewById(R.id.card_layout);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onSongClicked(getAdapterPosition(), v);
                    }
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (listener != null) {
                        listener.onSongLongClicked(getAdapterPosition(), v);
                    }
                    return false;
                }
            });
        }
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_cell_layout,
                parent, false);
        SongViewHolder songViewHolder = new SongViewHolder(view);
        return songViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);

        if (song.getPhotoPath() != null && song.getPhotoPath().trim().length() > 0) {
            RequestOptions options = new RequestOptions().
                    circleCrop().
                    placeholder(R.mipmap.ic_launcher_round).
                    error(R.mipmap.ic_launcher_round);

            Glide.with(context).
                    load(song.getPhotoPath()).
                    apply(options).
                    into(holder.photoIv);
        }

        holder.nameTv.setText(song.getName());
        holder.artistTv.setText(song.getArtist());


        if (position == MusicService.getCurrentSongPosition() && !firstRun) {
            holder.cardLayout.setCardBackgroundColor(context.getResources().getColor(R.color.colorPurple2, null));
        } else {
            holder.cardLayout.setCardBackgroundColor(context.getResources().getColor(R.color.colorPrimary, null));
        }

        firstRun = false;
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }
}
