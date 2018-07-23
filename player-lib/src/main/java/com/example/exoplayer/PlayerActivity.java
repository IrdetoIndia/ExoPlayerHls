/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
* limitations under the License.
 */
package com.example.exoplayer;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.util.UUID;

/**
 * A fullscreen activity to play audio or video streams.
 */
public class PlayerActivity extends FragmentActivity {
  private static final String TAG = "PlayerActivity";
  private SimpleExoPlayer player;
  private SimpleExoPlayerView playerView;
  String manifest = null;
  private String hlsVideoUri = "http://playertest.longtailvideo.com/adaptive/bbbfull/bbbfull.m3u8";
  //private String hlsVideoUri = "https://s3-ap-southeast-1.amazonaws.com/clearkey/TestVideo.m3u8";
  private ComponentListener componentListener;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_player);

    Handler mainHandler = new Handler();
    TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(new DefaultBandwidthMeter());
    TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

    DrmSessionManager<FrameworkMediaCrypto> drmSessionManager = getDrmSessionManager();

    //player = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(this, drmSessionManager), trackSelector, new DefaultLoadControl());
    player = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(this, null), trackSelector, new DefaultLoadControl());
    playerView = (SimpleExoPlayerView) findViewById(R.id.video_view);
    playerView.setPlayer(player);

    // Measures bandwidth during playback. Can be null if not required.
    DefaultBandwidthMeter defaultBandwidthMeter = new DefaultBandwidthMeter();
    // Produces DataSource instances through which media data is loaded.
    DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
            Util.getUserAgent(this, "Exo2"), defaultBandwidthMeter);


    HlsMediaSource hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(hlsVideoUri), mainHandler, null);
    player.prepare(hlsMediaSource);
  }

  @Override
  public void onStart() {
    super.onStart();
    System.out.println("Hello from onStart\n");
    if (Util.SDK_INT > 23) {
      initializePlayer();
    }
  }

  private void initializePlayer() {
    if(player == null) {
      player.addListener(componentListener);
      player.addVideoDebugListener(componentListener);
      player.addAudioDebugListener(componentListener);
      playerView.requestFocus();
      player.setPlayWhenReady(true);
    }
  }

  class MyDRMCallBack implements MediaDrmCallback {

    String keyString;

    @Override
    public byte[] executeProvisionRequest(UUID uuid, ExoMediaDrm.ProvisionRequest request) throws Exception {
      return new byte[0];
    }
    String keyValue = "66b52defba4cc35c75316847f5020da1";
    //String keyId = "e8ad5972-8819-4326-ad6a-f9a15168bca7";

    public MyDRMCallBack() {
      //this.keyString = "{\"keys\":[{\"kty\":\"oct\",\"k\":\""+keyValue+"\",\"kid\":\""+keyId+"\"}],\"type\":\"temporary\"}";
      this.keyString = "{\"keys\":[{\"kty\":\"oct\",\"k\":\""+keyValue+"\"}],\"type\":\"temporary\"}";
    }

    @Override
    public byte[] executeKeyRequest(UUID uuid, ExoMediaDrm.KeyRequest request) throws Exception {
      return keyString.getBytes();
    }
  }

  private DrmSessionManager getDrmSessionManager() {
    DrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;
    try {
      FrameworkMediaDrm fmDrm = FrameworkMediaDrm.newInstance(C.CLEARKEY_UUID);
      MyDRMCallBack myDRMCallBack = new MyDRMCallBack();
      drmSessionManager = new DefaultDrmSessionManager<FrameworkMediaCrypto>(C.CLEARKEY_UUID, fmDrm,
              myDRMCallBack, null, null, null);
    } catch (UnsupportedDrmException e) {
    }
    return drmSessionManager;
  }

  private class ComponentListener extends Player.DefaultEventListener implements
          VideoRendererEventListener, AudioRendererEventListener {

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
      String stateString;
      switch (playbackState) {
        case Player.STATE_IDLE:
          stateString = "ExoPlayer.STATE_IDLE      -";
          break;
        case Player.STATE_BUFFERING:
          stateString = "ExoPlayer.STATE_BUFFERING -";
          break;
        case Player.STATE_READY:
          stateString = "ExoPlayer.STATE_READY     -";
          break;
        case Player.STATE_ENDED:
          stateString = "ExoPlayer.STATE_ENDED     -";
          break;
        default:
          stateString = "UNKNOWN_STATE             -";
          break;
      }
      Log.d(TAG, "changed state to " + stateString + " playWhenReady: " + playWhenReady);
    }

    // Implementing VideoRendererEventListener.

    @Override
    public void onVideoEnabled(DecoderCounters counters) {
      // Do nothing.
    }

    @Override
    public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
      // Do nothing.
    }

    @Override
    public void onVideoInputFormatChanged(Format format) {
      // Do nothing.
    }

    @Override
    public void onDroppedFrames(int count, long elapsedMs) {
      // Do nothing.
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
      // Do nothing.
    }

    @Override
    public void onRenderedFirstFrame(Surface surface) {
      // Do nothing.
    }

    @Override
    public void onVideoDisabled(DecoderCounters counters) {
      // Do nothing.
    }

    // Implementing AudioRendererEventListener.

    @Override
    public void onAudioEnabled(DecoderCounters counters) {
      // Do nothing.
    }

    @Override
    public void onAudioSessionId(int audioSessionId) {
      // Do nothing.
    }

    @Override
    public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
      // Do nothing.
    }

    @Override
    public void onAudioInputFormatChanged(Format format) {
      // Do nothing.
    }

    @Override
    public void onAudioSinkUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
      // Do nothing.
    }

    @Override
    public void onAudioDisabled(DecoderCounters counters) {
      // Do nothing.
    }

  }
}