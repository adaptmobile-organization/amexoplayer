package com.example.adaptmobile.amexoplayer;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;

import java.util.ArrayList;

import dk.adaptmobile.amexoplayer.AMExoVideoPlayer;
import dk.adaptmobile.amexoplayer.VideoType;

public class MainActivity extends AppCompatActivity implements AMExoVideoPlayer.PlayerStateListener, AMExoVideoPlayer.PlayerErrorListener {

    private SimpleExoPlayerView exoPlayerView;
    private AMExoVideoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        exoPlayerView = (SimpleExoPlayerView) findViewById(R.id.exoPlayerView);

        player = new AMExoVideoPlayer(this);

    }

    private void initPlayer() {
        //String path = "asset:///ToodlmovieAPPLQ.mp4";
        //Uri contentUri = Uri.parse(path);

        setSingleVideo();

        player.setStateListener(this);
        player.initializePlayer();
        player.setVolume(0f);

        exoPlayerView.setPlayer(player.getPlayer());
    }

    private void setSingleVideo() {
        Uri uri = Uri.parse("https://archive.org/download/Popeye_forPresident/Popeye_forPresident_512kb.mp4");

        player.setVideo(uri, VideoType.STANDARD, true);
    }

    private void setMultipleVideos() {
        Uri uri = Uri.parse("https://archive.org/download/Popeye_forPresident/Popeye_forPresident_512kb.mp4");

        ArrayList<Uri> videos = new ArrayList<>();
        videos.add(uri);
        videos.add(uri);

        ArrayList<VideoType> videoTypes = new ArrayList<>();
        videoTypes.add(VideoType.STANDARD);
        videoTypes.add(VideoType.STANDARD);

        player.setVideos(videos, videoTypes, false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        player.releasePlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initPlayer();
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case ExoPlayer.STATE_READY:
                break;
            case ExoPlayer.STATE_BUFFERING:
                break;
            case ExoPlayer.STATE_IDLE:
                break;
            case ExoPlayer.STATE_ENDED:
                break;
        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }
}
