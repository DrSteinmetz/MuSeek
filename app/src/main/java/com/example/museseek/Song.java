package com.example.museseek;

import java.io.Serializable;

public class Song implements Serializable {
    private String mName;
    private String mArtist;
    private String mSongLink;
    private String mPhotoPath;
    private boolean isPhotoFromURL;

    public Song(String name, String artist, String songLink, String photoPath) {
        this.mName = name;
        this.mArtist = artist;
        this.mSongLink = songLink;
        this.mPhotoPath = photoPath;
    }

    public String getmName() {
        return mName;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    public String getmArtist() {
        return mArtist;
    }

    public void setmArtist(String mArtist) {
        this.mArtist = mArtist;
    }

    public String getmSongLink() {
        return mSongLink;
    }

    public void setmSongLink(String mSongLink) {
        this.mSongLink = mSongLink;
    }

    public String getmPhotoPath() {
        return mPhotoPath;
    }

    public void setmPhotoPath(String mPhotoPath) {
        this.mPhotoPath = mPhotoPath;
    }

    public boolean isPhotoFromURL() {
        return isPhotoFromURL;
    }

    public void setIsPhotoFromURL(boolean photoFromURL) {
        isPhotoFromURL = photoFromURL;
    }
}
