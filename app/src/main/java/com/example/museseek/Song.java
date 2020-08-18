package com.example.museseek;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Objects;

public class Song implements Serializable {
    private long mId;
    private String mName;
    private String mArtist;
    private String mSongURL;
    private String mPhotoPath;

    public Song(String name, String artist, String songURL, String photoPath) {
        mId = System.nanoTime();
        this.mName = name;
        this.mArtist = artist;
        this.mSongURL = songURL;
        this.mPhotoPath = photoPath;
    }

    public long getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getArtist() {
        return mArtist;
    }

    public void setArtist(String artist) {
        this.mArtist = artist;
    }

    public String getSongURL() {
        return mSongURL;
    }

    public void setSongURL(String songURL) {
        this.mSongURL = songURL;
    }

    public String getPhotoPath() {
        return mPhotoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.mPhotoPath = photoPath;
    }

    @NonNull
    @Override
    public String toString() {
        return "Name: " + mName +
                "\nArtist: " + mArtist;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Song song = (Song) obj;
        return (mId == song.getId());
    }
}
