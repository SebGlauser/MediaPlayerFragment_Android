package com.example.seg.mediaplayerfragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.seg.mediaplayerfragment.Singletons.SongListSingleton;
import com.example.seg.mediaplayerfragment.objects.Song;

import java.util.Date;
import java.util.List;


/**
 * A  {@link Fragment} that contain the information about the view.
 */
public class SongInformationFragment extends Fragment {
    private static String mPositionKey = "position";
    private List<Song> mSongList = SongListSingleton.getInstance();
    private Integer mCurrentSongIndex = 1;
    private TextView title, artist;
    private ImageView img;


    public SongInformationFragment() {
        // Required empty public constructor
    }

    /**
     * Create a new instance of DetailsFragment, initialized to
     * show the text at 'index'.
     */
    public static SongInformationFragment newInstance(int position) {
        SongInformationFragment fragment = new SongInformationFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt(mPositionKey, position);
        fragment.setArguments(args);

        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_song_information, container, false);

        // If the container is empty use the position 1
        if (container == null) {
            mCurrentSongIndex = 1;
        } else {
            mCurrentSongIndex = getArguments().getInt(mPositionKey, 1);
        }
        // Get back item from layout
        title = view.findViewById(R.id.song_title);
        artist = view.findViewById(R.id.song_artist);
        img = view.findViewById(R.id.cover);

        updateInformation();

        return view;
    }

    /**
     * Update the view in function of the
     */
    private void updateInformation() {
        // get back the current song
        Song song = getSongFromIndex();

        if (song == null)
            return;

        // Set information in the view
        title.setText(song.getName());
        artist.setText(song.getAuthor());
        if (song.getAlbumart() != null)
            img.setImageBitmap(song.getAlbumart());
        else
            img.setImageResource(R.drawable.ic_music_note_black_24dp);
    }

    /**
     * Update the view in function of the position
     */
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
     * Update the view in function of the position
     *
     * @param position the position of the song in the singleton list
     */
    public void updateSongView(int position) {
        mCurrentSongIndex = position;
        updateInformation();
    }
}
