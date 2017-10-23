package com.example.seg.mediaplayerfragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.seg.mediaplayerfragment.Listener.RecyclerSongClickListener;
import com.example.seg.mediaplayerfragment.Singletons.SongListSingleton;
import com.example.seg.mediaplayerfragment.adapter.SongListRecyclerViewAdapter;
import com.example.seg.mediaplayerfragment.objects.Song;

import java.util.List;

/**
 *
 */
public class ListOfSongFragment extends Fragment {

    public ListOfSongFragment() {
    }

    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private SongListRecyclerViewAdapter mSongListAdapter;
    private DividerItemDecoration mDividerItemDecoration;
    private List<Song> mSongList = SongListSingleton.getInstance();
    private Context mOwner;
    OnSongSelectedListener mCallback;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_list_of_song, container, false);

        // Avoid have an null pointer
        if (mOwner == null) {
            return view;
        }

        // get the view
        mRecyclerView = view.findViewById(R.id.my_recycler_view);
        if (mRecyclerView == null) {
            return view;
        }

        // Instantiate the adapter
        mSongListAdapter = new SongListRecyclerViewAdapter(mSongList);

        mLayoutManager = new LinearLayoutManager(getActivity());

        // Create a divider
        mDividerItemDecoration = new DividerItemDecoration(
                mOwner,
                LinearLayout.VERTICAL
        );
        mDividerItemDecoration.setDrawable(this.getResources().getDrawable(R.drawable.sk_line_divider));

        // build the recycler
        mRecyclerView.setAdapter(mSongListAdapter);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(mDividerItemDecoration);
        mRecyclerView.setLayoutManager(mLayoutManager);


        mRecyclerView.addOnItemTouchListener(
                new RecyclerSongClickListener(getContext(), new RecyclerSongClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        if (mCallback != null) {
                            mCallback.onSongSelected(position);
                        }
                    }
                })
        );


        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        try {
            mOwner = activity;
            mCallback = (OnSongSelectedListener) activity;
        } catch (ClassCastException e) {
            e.printStackTrace();
            mCallback = null;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mOwner = null;
        mCallback = null;
    }

    public void commitListHasBeenUpdated() {
        mSongListAdapter.notifyDataSetChanged();
    }

    // Container Activity must implement this interface
    public interface OnSongSelectedListener {
        public void onSongSelected(int position);
    }
}
