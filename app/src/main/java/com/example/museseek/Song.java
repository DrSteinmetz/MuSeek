package com.example.museseek;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class Song implements Serializable {
    private String mName;
    private String mArtist;
    private String mSongURL;
    private String mPhotoPath;
    private boolean mIsPhotoFromURL;

    public Song(String name, String artist, String songURL, String photoPath) {
        this.mName = name;
        this.mArtist = artist;
        this.mSongURL = songURL;
        this.mPhotoPath = photoPath;
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

    public boolean isPhotoFromURL() {
        return mIsPhotoFromURL;
    }

    public void setIsPhotoFromURL(boolean photoFromURL) {
        mIsPhotoFromURL = photoFromURL;
    }
}
