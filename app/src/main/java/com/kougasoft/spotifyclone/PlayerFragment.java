package com.kougasoft.spotifyclone;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class PlayerFragment extends Fragment {
    ImageButton ibPrev, ibPlay, ibNext;
    SeekBar sbSscrubBar;
    TextView tvCurrentTime, tvEndTime, tvArtistName, tvAlbumName,
            tvTrackName;
    ImageView ivAlbumArt;
    boolean playerIsPrepared = false;
    ArrayList<Track> mTrackList;
    int mPosition;
    MediaPlayer mMediaPlayer = null;
    Date mDuration;
    final DateFormat timeFormat = new SimpleDateFormat("mm:ss", Locale.US);
    Handler mHandler = new Handler();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_player, container, false);

        tvArtistName = (TextView) rootView.findViewById(R.id.artistName);
        tvAlbumName = (TextView) rootView.findViewById(R.id.albumName);
        tvTrackName = (TextView) rootView.findViewById(R.id.trackName);
        ivAlbumArt = (ImageView) rootView.findViewById(R.id.albumArt);
        tvCurrentTime = (TextView) rootView.findViewById(R.id.currentTime);
        tvEndTime = (TextView) rootView.findViewById(R.id.endTime);
        sbSscrubBar = (SeekBar) rootView.findViewById(R.id.seekBar);
        ibPrev = (ImageButton) rootView.findViewById(R.id.previousButton);
        ibPlay = (ImageButton) rootView.findViewById(R.id.playButton);
        ibNext = (ImageButton) rootView.findViewById(R.id.nextButton);

        sbSscrubBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser && playerIsPrepared) {
                    mMediaPlayer.seekTo(progress * 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        Bundle trackListBundle = getArguments();
        mTrackList = trackListBundle.getParcelableArrayList(Top10ListFragment.TRACK_LIST);
        mPosition = trackListBundle.getInt(Top10ListFragment.POSITION);

        setUpFragment();

        return rootView;
    }

    private void setUpFragment() {
        ibPrev.setClickable(false);
        ibPlay.setClickable(false);
        ibNext.setClickable(false);
        sbSscrubBar.setEnabled(false);
        sbSscrubBar.setProgress(0);
        removeMediaPlayer();
        tvArtistName.setText(mTrackList.get(mPosition).artistName);
        tvAlbumName.setText(mTrackList.get(mPosition).albumName);
        tvTrackName.setText(mTrackList.get(mPosition).trackName);
        String albumArtURL = mTrackList.get(mPosition).albumArtThumb;

        if(!albumArtURL.equals(""))
            Picasso.with(getActivity()).load(albumArtURL).into(ivAlbumArt);
        else
            Picasso.with(getActivity()).load(R.mipmap.ic_launcher).into(ivAlbumArt);

        mMediaPlayer = getMediaPlayer(mTrackList.get(mPosition).previewURL);
    }

    @Override
    public void onStop() {
        super.onStop();
        removeMediaPlayer();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mMediaPlayer == null)
            mMediaPlayer = getMediaPlayer(mTrackList.get(mPosition).previewURL);
    }

    private void removeMediaPlayer() {
        if(mMediaPlayer != null) {
            mMediaPlayer.stop();
            playerIsPrepared = false;
            ibPlay.setImageResource(android.R.drawable.ic_media_play);
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    public MediaPlayer getMediaPlayer(String previewUrl) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        playerIsPrepared = false;
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(previewUrl);
            mediaPlayer.setOnPreparedListener(new onPreparedListener());
            mediaPlayer.prepareAsync();
        }
        catch (IOException ex) {
            Toast.makeText(getActivity(), "Error retrieving track preview!", Toast.LENGTH_LONG).show();
            return null;
        }

        return mediaPlayer;
    }

    public class onPreparedListener implements MediaPlayer.OnPreparedListener {
        private void playMusic() {
            mMediaPlayer.start();
            ibPlay.setImageResource(android.R.drawable.ic_media_pause);
            getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                        int mCurrentPosition = mMediaPlayer.getCurrentPosition() / 1000;
                        sbSscrubBar.setProgress(mCurrentPosition);
                        tvCurrentTime.setText(timeFormat.format(new Date(mMediaPlayer.getCurrentPosition())));
                        mHandler.postDelayed(this, 1000);
                    }
                }
            });
        }

        private void pauseMusic() {
            mMediaPlayer.pause();
            ibPlay.setImageResource(android.R.drawable.ic_media_play);
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            playerIsPrepared = true;
            ibPrev.setClickable(true);
            ibPlay.setClickable(true);
            ibNext.setClickable(true);
            sbSscrubBar.setEnabled(true);

            sbSscrubBar.setMax(mp.getDuration() / 1000);
            mDuration = new Date(mp.getDuration());
            tvCurrentTime.setText("0:00");
            tvEndTime.setText(timeFormat.format(mDuration));
            playMusic();

            ibPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mMediaPlayer.isPlaying() && playerIsPrepared) {
                        playMusic();
                    } else {
                        pauseMusic();
                    }
                }
            });

            ibPrev.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPosition = ((mPosition - 1) >= 0 ? mPosition - 1 : mTrackList.size()-1);
                    setUpFragment();
                }
            });

            ibNext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPosition = ((mPosition + 1) < mTrackList.size() ? mPosition + 1 : 0);
                    setUpFragment();
                }
            });
        }
    }
}
