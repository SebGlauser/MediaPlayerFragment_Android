package com.example.seg.mediaplayerfragment;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.seg.mediaplayerfragment.Singletons.MediaPlayerSingleton;
import com.example.seg.mediaplayerfragment.Singletons.SongListSingleton;
import com.example.seg.mediaplayerfragment.objects.Song;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ListOfSongFragment.OnSongSelectedListener{
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 100;

    private List<Song> mSongList = SongListSingleton.getInstance();
    private MediaPlayer mMediaPlayer = MediaPlayerSingleton.getInstance();
    private int mCurrentSongIndex = 0;
    private boolean isPaused;
    private boolean isTracking = false;

    private TextView duration, timeProgression;
    private ImageButton previous_btn, play_pause_btn, next_btn;
    private SeekBar seekProgress;
    private LinearLayout rootView;

    private SeekBarAsyncTaskRunner mSeekBarAsyncUpdater;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get back view items
        previous_btn = (ImageButton) findViewById(R.id.previous_btn);
        play_pause_btn = (ImageButton) findViewById(R.id.play_pause_btn);
        next_btn = (ImageButton) findViewById(R.id.next_btn);
        duration = (TextView) findViewById(R.id.duration);
        timeProgression = (TextView) findViewById(R.id.progression);
        seekProgress = (SeekBar) findViewById(R.id.bar_progression);
        rootView = (LinearLayout) findViewById(R.id.root_layout);

        // Change the background for @api > 25
        setStatusBarGradient(this);

        setListeners();

        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.container_fragment) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            // Create a new Fragment to be placed in the activity layout
            ListOfSongFragment firstFragment = new ListOfSongFragment();

            // In case this activity was started with special instructions from an
            // Intent, pass the Intent's extras to the fragment as arguments
            firstFragment.setArguments(getIntent().getExtras());

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container_fragment, firstFragment).commit();
        }

        requestInternalPermission();
    }

    /**
     * This function will set the listeners
     */
    private void setListeners() {
        // When a song is finished start the next one
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (mCurrentSongIndex < mSongList.size() - 1) {
                    mCurrentSongIndex++;
                    startSong();
                }
            }
        });

        // On click on the listener
        seekProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (isTracking)
                    mMediaPlayer.seekTo(progress);
            }

            // Without this lock the player bug every time we set the position
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isTracking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isTracking = false;
            }
        });

        // On next btn pressed, start the next song
        next_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentSongIndex < mSongList.size() - 1) {
                    mCurrentSongIndex++;
                    startSong();
                }
            }
        });

        // On previous btn pressed, start the previous song
        previous_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentSongIndex > 1) {
                    mCurrentSongIndex--;
                    startSong();
                }

            }
        });

        // On pause play btn pressed, play or paused the current song
        play_pause_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPaused) {
                    continueSong();
                } else {
                    pauseSong();
                }
            }
        });
    }

    /**
     * This function start the song pointed by the mCurrentSongIndex and update information
     */
    public void startSong() {
        // get back the current song
        Song song = mSongList.get(mCurrentSongIndex);

        // Create a format to print the time
        //@// TODO Update fragment
        SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");

        duration.setText(sdf.format(new Date(song.getDuration())));
        timeProgression.setText(sdf.format(new Date(0)));


        // Start the song
        try {
            Uri uri = Uri.parse("file:///" + song.getPath());
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            }
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(getApplicationContext(), uri);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Change the state and the button
        isPaused = false;
        play_pause_btn.setImageResource(R.drawable.ic_pause_black_24dp);
    }

    /**
     * This function pause the song and adapt the view
     */
    public void pauseSong() {
        // Pause the button and start the player
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            isPaused = true;
            play_pause_btn.setImageResource(R.drawable.ic_play_arrow_black_24dp);
        }
    }

    /**
     * Restart the song
     */
    public void continueSong() {
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
            isPaused = false;
            play_pause_btn.setImageResource(R.drawable.ic_pause_black_24dp);
        }
    }

    /**
     * Change the background by an animated gradient
     *
     * @param activity the activity to get the window
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setStatusBarGradient(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(this.getResources().getColor(R.color.white30)));
            Window window = activity.getWindow();
            rootView.setBackgroundColor(0x00000000);
            AnimationDrawable background = (AnimationDrawable) activity.getResources().getDrawable(R.drawable.annimation_list);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(activity.getResources().getColor(R.color.white30));
            window.setNavigationBarColor(activity.getResources().getColor(R.color.white30));
            window.setBackgroundDrawable(background);

            background.setEnterFadeDuration(2000);
            background.setExitFadeDuration(4000);
            background.start();
        }
    }

    /**
     * This function will be we call when the user granted access to the internal storage
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestInternalPermission() {
        // If the application has not access to the internal storage send a request
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

        } else {
            // The application seems to have internal access

            // Populate the song
            SongListSingleton.populate(this);

            // Start the async task
            mSeekBarAsyncUpdater = new SeekBarAsyncTaskRunner();
            mSeekBarAsyncUpdater.execute();
        }
    }

    /**
     * This function will be we call when the user granted access to the internal storage
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SongListSingleton.populate(this);
                    mSeekBarAsyncUpdater = new SeekBarAsyncTaskRunner();
                    mSeekBarAsyncUpdater.execute();
                } else {

                }
                return;
            }
        }
    }

    @Override
    public void onSongSelected(int position) {
        mCurrentSongIndex = position;
        startSong();
    }

    /**
     * Inner class to update the seek bar while the music is playing
     */
    private class SeekBarAsyncTaskRunner extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            // The isCancelled is needed if you want to kill the thread
            while (mMediaPlayer != null && !isCancelled()) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    publishProgress(mMediaPlayer.getCurrentPosition());
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                // Update the view
                SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
                int mediaPos_new = mMediaPlayer.getCurrentPosition();
                int mediaMax_new = mMediaPlayer.getDuration();
                seekProgress.setMax(mediaMax_new);
                seekProgress.setProgress(mediaPos_new);
                timeProgression.setText(sdf.format(new Date(mediaPos_new)));
            }
        }

    }
}
