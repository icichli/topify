package applicationname.companydomain.simpleapp;

/*
    Code References:


        Swipe to refresh:
                http://sapandiwakar.in/pull-to-refresh-for-android-recyclerview-or-any-other-vertically-scrolling-view/

        RecyclerView with multiple ViewHolders:
                http://www.digitstory.com/recyclerview-multiple-viewholders/

        Home icon:
                https://cdn4.iconfinder.com/data/icons/pictype-free-vector-icons/16/home-512.png

        Question mark image:
                http://www.cartoomics.it/wp-content/uploads/2018/02/bianco-punto-interrogativo-su-uno-sfondo-circolare-nero_318-35996.jpg

        Spotify Android SDK:
                https://developer.spotify.com/documentation/android-sdk/

        spotify-web-api-android:
                https://github.com/kaaes/spotify-web-api-android

        Spotify Web API:
                https://developer.spotify.com/documentation/web-api/
 */

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.view.MenuItem;
import android.view.View;
import android.util.Log;

import android.support.v4.widget.DrawerLayout;
import android.support.v4.view.GravityCompat;

import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;

import kaaes.spotify.webapi.android.models.AudioFeaturesTrack;
import kaaes.spotify.webapi.android.models.AudioFeaturesTracks;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Pager;

import kaaes.spotify.webapi.android.models.UserPrivate;
import retrofit.RetrofitError;
import retrofit.Callback;
import retrofit.client.Response;

import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.HashMap;


import android.support.v7.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

public class MainActivity extends SpotifyCodeActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout mDrawerLayout;

    static final SpotifyApi spotifyApi = new SpotifyApi();
    static final SpotifyService spotify = spotifyApi.getService();

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerViewAdapter mRecyclerViewAdapter;

    private ArrayList<Object> feed;
    private String time_range = "short_term";

    private boolean noArtists = false;
    private boolean noTracks = false;

    public static final Map<String, String> TIME_LABELS = createTimeLabels();
    private static Map<String, String> createTimeLabels() {
        Map<String, String> labels = new HashMap<String, String>();
        labels.put("short_term", "Based on the past 4 weeks.");
        labels.put("medium_term", "Based on the past 6 months.");
        labels.put("long_term", "Based on several years.");

        return labels;
    }

    private static final Map<String, String> TERM_LABELS = createTermLabels();
    private static Map<String, String> createTermLabels() {
        Map<String, String> labels = new HashMap<String, String>();
        labels.put("short_term", "Short-term");
        labels.put("medium_term", "Medium-term");
        labels.put("long_term", "Long-term");

        return labels;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        switch (item.getItemId()) {

            case R.id.log_out: {
                logOut(MainActivity.this);
                break;
            }
        }
        //close navigation drawer
        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setNavigationViewListener() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initially, hide the avatar.
        ImageView avatarView = findViewById(R.id.avatarView);
        avatarView.setVisibility(View.GONE);

        Bundle args = getIntent().getExtras();
        if (args != null) {
            String token = args.getString("ACCESS_TOKEN", "");
            Log.d("onCreate token", token);
            spotifyApi.setAccessToken(token);

            // Fetch our Spotify profile
            fetchMyAvatar();
        }

        // Recycler View
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        mRecyclerViewAdapter = new RecyclerViewAdapter(MainActivity.this, recyclerView);
        recyclerView.setAdapter(mRecyclerViewAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));


        // Get a reference to the navigation drawer.
        mDrawerLayout = findViewById(R.id.drawer_layout);

        // Get a reference to the refresh layout
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Refresh items
                refreshItems();
            }
        });

        // Set the distance in order to refresh.
        mSwipeRefreshLayout.setDistanceToTriggerSync(600);

        // Set the NavigationView listener.
        setNavigationViewListener();
    }

    private void setHeaderLabel() {
        TextView timeLabel = (TextView) findViewById(R.id.timeLabel);
        timeLabel.setText(MainActivity.TERM_LABELS.get(time_range));
    }

    public void onShortTermClicked(View v) {
        time_range = "short_term";
        setHeaderLabel();
        refreshItems();
    }

    public void onMediumTermClicked(View v) {
        time_range = "medium_term";
        setHeaderLabel();
        refreshItems();
    }

    public void onLongTermClicked(View v) {
        time_range = "long_term";
        setHeaderLabel();
        refreshItems();
    }

    private void fetchTopArtists() {
        Map<String, Object> options = new HashMap<>();
        options.put("time_range", time_range);

        feed = new ArrayList<>();
        feed.add(new CategoryItem("Top Artists", time_range));

        spotify.getTopArtists(options, new Callback<Pager<Artist>>() {
            @Override
            public void success(Pager<Artist> artists, Response response) {
                if (artists.items.size() > 0) {
                    noArtists = false;

                    for (int i = 0; i < artists.items.size(); i++) {

                        String sdURL = "";
                        String hdURL = "";

                        if (artists.items.get(i).images != null && artists.items.get(i).images.size() > 0) {
                            sdURL = artists.items.get(i).images.get(artists.items.get(i).images.size() - 1).url;
                            hdURL = artists.items.get(i).images.get(0).url;
                        }

                        feed.add(new ArtistItem(artists.items.get(i).name.toString(),
                                sdURL,
                                hdURL,
                                (i % 2) == 0, artists.items.get(i).id,
                                (i + 1),
                                artists.items.get(i).popularity));
                    }
                } else {
                    noArtists = true;
                    feed.add(new NoResultsItem());
                }

                fetchTopTracks();
            }

            @Override
            public void failure(RetrofitError error) {
                fetchNewCode(MainActivity.this);
            }
        });
    }

    private void fetchTopTracks() {
        Map<String, Object> options = new HashMap<>();
        options.put("time_range", time_range);

        feed.add(new CategoryItem("Top Tracks", time_range));

        spotify.getTopTracks(options, new Callback<Pager<Track>>() {
            @Override
            public void success(Pager<Track> tracks, Response response) {
                getTrackFeatures(tracks.items);
            }

            @Override
            public void failure(RetrofitError error) {
                fetchNewCode(MainActivity.this);
            }
        });
    }

    public void onPopUpMenuClicked(final View view) {
        PopupMenu popupMenu = new PopupMenu(MainActivity.this, view);
        popupMenu.getMenuInflater().inflate(R.menu.popup_menu_term, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.one) {
                    onShortTermClicked(view);
                } else if (item.getItemId() == R.id.two) {
                    onMediumTermClicked(view);
                } else {
                    onLongTermClicked(view);
                }
                return true;
            }
        });

        popupMenu.show();
    }

    private void fetchMyAvatar() {
        // Load my username or (display name)
        spotify.getMe(new Callback<UserPrivate>() {

            public void success(UserPrivate userPrivate, Response response) {

                String displayName = userPrivate.display_name;
                if (displayName == null || displayName.trim().equals("")) {
                    displayName = userPrivate.id;
                }

                Object url; // We don't know if it's going to be a string or integer.
                if (userPrivate.images.size() > 0) {
                    url = userPrivate.images.get(userPrivate.images.size() - 1).url;
                } else {
                    url = R.drawable.unknown;
                }

                ImageView avatar = (ImageView) findViewById(R.id.avatarView);
                ImageView avatar2 = (ImageView) findViewById(R.id.avatarView2);

                Glide.with(MainActivity.this)
                        .load(url)
                        .apply(RequestOptions.circleCropTransform())
                        .into(avatar);

                // Show the avatar.
                avatar.setVisibility(View.VISIBLE);

                avatar2.setOnClickListener(new View.OnClickListener(){
                    public void onClick(View v) {
                        mDrawerLayout.openDrawer(GravityCompat.START);
                    }
                });


                //set the avatar in the nav view
                NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
                View header = navigationView.getHeaderView(0);
                TextView username_ye = (TextView) header.findViewById(R.id.textView3);
                ImageView avatarView_nav = (ImageView) header.findViewById(R.id.avatarView3);
                username_ye.setText(displayName);
                Glide.with(MainActivity.this)
                        .load(url)
                        .apply(RequestOptions.circleCropTransform())
                        .into(avatarView_nav);
				
                // Fetch our top artists and tracks
                fetchTopArtists();
            }

            public void failure(RetrofitError error) {
                fetchNewCode(MainActivity.this);
            }
        });
    }

    // We want to set it to synchronized just in-case.
    private synchronized void refreshItems() {
        // Fetch our profile
        fetchMyAvatar();
    }

    private void getTrackFeatures(final List<Track> items) {
        if (items.size() > 0) {
            StringBuilder sb = new StringBuilder("");

            for (int i = 0; i < items.size(); i++) {
                if (i != (items.size() - 1)) {
                    sb.append(items.get(i).id + ",");
                } else {
                    sb.append(items.get(i).id);
                }
            }

            noTracks = false;

            spotify.getTracksAudioFeatures(sb.toString(), new Callback<AudioFeaturesTracks>() {
                @Override
                public void success(AudioFeaturesTracks features, Response response) {

                    List<AudioFeaturesTrack> audioFeaturesTracks = features.audio_features;

                    for (int i = 0; i < items.size(); i++) {

                        String url = "";
                        String artist = "";

                        if (items.get(i).album != null && items.get(i).album.images != null
                                && items.get(i).album.images.size() > 0) {
                            url = items.get(i).album.images.get(items.get(i).album.images.size() - 1).url;
                            artist = items.get(i).artists.get(0).name.toString();
                        }

                        if (audioFeaturesTracks.get(i) != null) {
                            feed.add(new TrackItem(items.get(i).name.toString(),
                                    artist,
                                    url,
                                    (i % 2) == 0, items.get(i).id,
                                    (i + 1),
                                    items.get(i).popularity,
                                    audioFeaturesTracks.get(i).danceability,
                                    audioFeaturesTracks.get(i).energy,
                                    audioFeaturesTracks.get(i).valence));
                        } else {
                            feed.add(new TrackItem(items.get(i).name.toString(),
                                    artist,
                                    url,
                                    (i % 2) == 0, items.get(i).id,
                                    (i + 1),
                                    items.get(i).popularity,
                                    -1f,
                                    -1f,
                                    -1f));
                        }
                    }
                    onItemsLoadComplete();
                }

                @Override
                public void failure(RetrofitError error) {
                    fetchNewCode(MainActivity.this);
                }
            });
        } else {
            noTracks = true;
            feed.add(new NoResultsItem());

            onItemsLoadComplete();
        }
    }

    void onItemsLoadComplete() {
        // We successfully loaded the whole page.
        // So, reset the login attempts.
        resetLoginAttempts();

        // Set the new top feed and update the RecyclerView
        if (mRecyclerViewAdapter != null && feed != null) {
            mRecyclerViewAdapter.setTopFeed(feed);
            mRecyclerViewAdapter.notifyDataSetChanged();

            // Stop the refresh animation
            mSwipeRefreshLayout.setRefreshing(false);

            // If there are no results, try going to the next duration term.
            if (noArtists && noTracks) {
                if (time_range.equals("short_term")) {
                    onMediumTermClicked(null);
                } else if (time_range.equals("medium_term")) {
                    onLongTermClicked(null);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        doActivityResult(requestCode, resultCode, intent, new myCallback() {
            @Override
            public void onSuccess() {
                refreshItems();
            }
        });
    }
}
