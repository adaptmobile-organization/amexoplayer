package dk.adaptmobile.amexoplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;

import com.google.android.exoplayer2.BuildConfig;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;

/**
 * Created by Thomas on 10/12/2016.
 */

public class AMExoVideoPlayer implements ExoPlayer.EventListener {

    private Context context;
    private SimpleExoPlayer player;
    private Handler mainHandler;
    private EventLogger eventLogger;
    private DataSource.Factory mediaDataSourceFactory;
    private String userAgent;
    private final Timeline.Window currentWindow;

    private ArrayList<Uri> uris;
    private ArrayList<VideoType> videoTypes;

    private boolean shouldAutoPlay = true;
    private long playerPosition;
    private boolean looping = false;

    private PlayerStateListener stateListener;
    private PlayerErrorListener errorListener;

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private static final long MAX_POSITION_FOR_SEEK_TO_PREVIOUS = 3000;

    public AMExoVideoPlayer(Context context) {
        this.context = context;

        uris = new ArrayList<>();
        videoTypes = new ArrayList<>();

        userAgent = Util.getUserAgent(context, BuildConfig.APPLICATION_ID);

        currentWindow = new Timeline.Window();

        mainHandler = new Handler();
        mediaDataSourceFactory = buildDataSourceFactory(true);
    }

    public void initializePlayer() {
        // Create a default TrackSelector
        Handler mainHandler = new Handler();
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);

        DefaultTrackSelector trackSelector =
                new DefaultTrackSelector();

        eventLogger = new EventLogger(trackSelector);
        // Get instance of SimpleExoPLayer
        player = ExoPlayerFactory.newSimpleInstance(context, trackSelector, new DefaultLoadControl());

        // Add listeners to player
        player.addListener(this);
        player.addListener(eventLogger);
        player.setAudioDebugListener(eventLogger);
        player.setVideoDebugListener(eventLogger);

        player.setPlayWhenReady(shouldAutoPlay);

        preparePlayer();
    }

    private void preparePlayer() {
        MediaSource[] mediaSources = new MediaSource[uris.size()];
        for (int i = 0; i < uris.size(); i++) {
            mediaSources[i] = buildMediaSource(uris.get(i), videoTypes.get(i));
        }

        MediaSource mediaSource = mediaSources.length == 1 ? mediaSources[0] : new ConcatenatingMediaSource(mediaSources);

        if (looping) {
            // Loops the video indefinitely.
            mediaSource = new LoopingMediaSource(mediaSource);
        }

        player.prepare(mediaSource);
    }

    public SimpleExoPlayer getPlayer() {
        return player;
    }

    public void setVideo(Uri uri, VideoType type, boolean looping) {
        this.looping = looping;
        uris.add(uri);
        videoTypes.add(type);
    }

    public void setVideos(ArrayList<Uri> uris, ArrayList<VideoType> types, boolean looping) {
        this.looping = looping;
        this.uris = uris;
        this.videoTypes = types;
    }

    public void next() {
        Timeline currentTimeline = player.getCurrentTimeline();
        if (currentTimeline == null) {
            return;
        }
        int currentWindowIndex = player.getCurrentWindowIndex();
        if (currentWindowIndex < currentTimeline.getWindowCount() - 1) {
            player.seekToDefaultPosition(currentWindowIndex + 1);
        } else if (currentTimeline.getWindow(currentWindowIndex, currentWindow, false).isDynamic) {
            player.seekToDefaultPosition();
        }

    }

    public void next(Uri uri, VideoType type, boolean looping) {
        this.looping = looping;
        uris.add(uri);
        videoTypes.add(type);
        preparePlayer();

        int currentWindowIndex = player.getCurrentWindowIndex();
        player.seekToDefaultPosition(currentWindowIndex + 1);
    }

    public void previous() {
        Timeline currentTimeline = player.getCurrentTimeline();
        if (currentTimeline == null) {
            return;
        }
        int currentWindowIndex = player.getCurrentWindowIndex();
        currentTimeline.getWindow(currentWindowIndex, currentWindow);
        if (currentWindowIndex > 0 && (player.getCurrentPosition() <= MAX_POSITION_FOR_SEEK_TO_PREVIOUS
                || (currentWindow.isDynamic && !currentWindow.isSeekable))) {
            player.seekToDefaultPosition(currentWindowIndex - 1);
        } else {
            player.seekTo(0);
        }
    }

    public long getCurrentPosition() {
        return player.getCurrentPosition();
    }

    public long getDuration() {
        return player.getDuration();
    }

    public void setVolume(float volume) {
        player.setVolume(volume);
    }

    public float getVolume() {
        return player.getVolume();
    }

    public void releasePlayer() {
        if (player != null) {
            shouldAutoPlay = player.getPlayWhenReady();
            playerPosition = C.TIME_UNSET;
            Timeline timeline = player.getCurrentTimeline();
            if (timeline != null) {
                playerPosition = player.getCurrentPosition();
            }
            player.release();
            player = null;
            eventLogger = null;
        }
    }

    public void replay() {
        player.seekTo(0);
        player.setPlayWhenReady(true);
    }

    public void pause() {
        player.setPlayWhenReady(false);
    }

    public void play() {
        player.setPlayWhenReady(true);
    }

    private MediaSource buildMediaSource(Uri uri, VideoType videoType) {
        switch (videoType) {
            case SMOOTH_STREAMING:
                return new SsMediaSource(uri, buildDataSourceFactory(false),
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
            case DASH_STREAMING:
                return new DashMediaSource(uri, buildDataSourceFactory(false),
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
            case HLS_STREAMING:
                return new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, eventLogger);
            case STANDARD:
                return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
                        mainHandler, eventLogger);
            default:
                return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
                    mainHandler, eventLogger);
        }
    }

    /**
     * Returns a new DataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *     DataSource factory.
     * @return A new DataSource factory.
     */
    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return buildDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
    }

    /**
     * Returns a new HttpDataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *     DataSource factory.
     * @return A new HttpDataSource factory.
     */
    private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
        return buildHttpDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
    }

    DataSource.Factory buildDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultDataSourceFactory(context, bandwidthMeter, buildHttpDataSourceFactory(bandwidthMeter));
    }

    HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultHttpDataSourceFactory(userAgent, bandwidthMeter);
    }

    /*
    * INTERFACE LISTENERS
     */

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (stateListener != null) {
            stateListener.onPlayerStateChanged(playWhenReady, playbackState);
        }
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        if (errorListener != null) {
            errorListener.onPlayerError(error);
        }
    }

    @Override
    public void onPositionDiscontinuity() {

    }

    public void setStateListener(PlayerStateListener listener) {
        stateListener = listener;
    }

    public void setErrorListener(PlayerErrorListener listener) {
        errorListener = listener;
    }

    public interface PlayerStateListener {
        void onPlayerStateChanged(boolean playWhenReady, int playbackState);
    }


    public interface PlayerErrorListener {
        void onPlayerError(ExoPlaybackException error);
    }

}
