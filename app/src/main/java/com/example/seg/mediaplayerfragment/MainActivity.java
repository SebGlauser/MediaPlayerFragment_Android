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
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
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
import android.widget.Toast;

import com.example.seg.mediaplayerfragment.Singletons.MediaPlayerSingleton;
import com.example.seg.mediaplayerfragment.Singletons.SongListSingleton;
import com.example.seg.mediaplayerfragment.objects.Song;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ListOfSongFragment.OnSongSelectedListener {
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

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

        requestInternalPermission();


        if (findViewById(R.id.container_fragment) != null) {
            if (savedInstanceState != null) {
                return;
            }
            ListOfSongFragment firstFragment = new ListOfSongFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container_fragment, firstFragment).commit();
        }

        // Start the async task
        mSeekBarAsyncUpdater = new SeekBarAsyncTaskRunner();
        mSeekBarAsyncUpdater.execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSeekBarAsyncUpdater.cancel(true);
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
                    commitSongChanged(mCurrentSongIndex);
                    startSong();
                }
            }
        });

        // On click on the listener
        seekProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (isTracking && mMediaPlayer != null) {

                    if (mMediaPlayer.getDuration() > 0) {
                        seekProgress.setMax(mMediaPlayer.getDuration());
                        if (mMediaPlayer.isPlaying()) {
                            mMediaPlayer.seekTo(progress);
                        }
                    }
                }
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
                    commitSongChanged(mCurrentSongIndex);
                    if (!isPaused)
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
                    commitSongChanged(mCurrentSongIndex);
                    if (!isPaused)
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
        Song song = getSongFromIndex();

        if (song == null)
            return;
        SimpleDateFormat sdf;
        if (song.getDuration() < 3600000) {
            sdf = new SimpleDateFormat("mm:ss");
        } else {
            sdf = new SimpleDateFormat("hh:mm:ss");
        }

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

    private Song getSongFromIndex() {
        Song song = null;
        try {
            song = mSongList.get(mCurrentSongIndex);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        return song;
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

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        } else {
            onStoragePermissionGranted();
        }
    }

    /**
     * This function should be called only when the storage permission is granted
     */
    private void onStoragePermissionGranted() {
        SongListSingleton.populate(this);

        // Note that it's possible the fragment has been loaded before the list has been populated
        // If that's the case we need to commit that the list has been updated
        ListOfSongFragment songFragment = (ListOfSongFragment)
                getSupportFragmentManager().findFragmentById(R.id.container_fragment);

        // if fragment is already loaded.
        if (songFragment != null) {
            songFragment.commitListHasBeenUpdated();
        }

    }

    /**
     * This function will be we call when the user granted access to the internal storage
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        if (requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {

            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onStoragePermissionGranted();
            } else {

            }
            return;
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        }
    }

    /**
     * Callback from the list fragment
     */
    @Override
    public void onSongSelected(int position) {
        mCurrentSongIndex = position;
        startSong();
        StartSongInformationFragment(position);
    }

    /**
     * This function start the fragment Song information and add it to the stack
     */
    private void StartSongInformationFragment(int position) {
        SongInformationFragment fragment = SongInformationFragment.newInstance(position);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.container_fragment, fragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }

    /**
     * This function update the fragment Song information
     */
    public void commitSongChanged(int position) {
        // Try to cast if this is not possible to cast that's mean the fragment has not be created
        try {
            SongInformationFragment articleFrag = (SongInformationFragment)
                    getSupportFragmentManager().findFragmentById(R.id.container_fragment);
            // if Fragment found
            if (articleFrag != null) {
                articleFrag.updateSongView(position);
            } else {
                StartSongInformationFragment(position);
            }
        } catch (Exception e) {
            StartSongInformationFragment(position);
        }
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
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");

                if (mMediaPlayer.getDuration() < 3600000) {
                    sdf = new SimpleDateFormat("mm:ss");
                }

                int mediaPos_new = mMediaPlayer.getCurrentPosition();
                int mediaMax_new = mMediaPlayer.getDuration();
                seekProgress.setMax(mediaMax_new);
                seekProgress.setProgress(mediaPos_new);
                timeProgression.setText(sdf.format(new Date(mediaPos_new)));
            }
        }

    }
}
