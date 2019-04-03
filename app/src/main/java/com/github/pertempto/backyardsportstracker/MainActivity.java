package com.github.pertempto.backyardsportstracker;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import com.github.pertempto.backyardsportstracker.data.DataViewModel;
import com.github.pertempto.backyardsportstracker.data.Game;
import com.github.pertempto.backyardsportstracker.data.Grouping;
import com.github.pertempto.backyardsportstracker.data.Player;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        GamesListFragment.OnGameClickListener, GroupingsListFragment.OnGroupingClickListener,
        PlayersListFragment.OnPlayerClickListener {
    private static final String LOG_TAG = "MainActivity";
    private static final String STATE_PAGE_ID = "pageId";

    private DataViewModel dataViewModel;
    private SharedPreferences sharedPref;
    private int pageId = R.id.nav_groupings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        dataViewModel = ViewModelProviders.of(this).get(DataViewModel.class);
    }


    @Override
    protected void onStart() {
        super.onStart();
        // load ui state
        sharedPref = getPreferences(Context.MODE_PRIVATE);
        pageId = sharedPref.getInt(STATE_PAGE_ID, R.id.nav_groupings);
        setPage(pageId);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // save ui state
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(STATE_PAGE_ID, pageId);
        editor.apply();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        setPage(id);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    void setPage(int pageId) {
        Log.d(LOG_TAG, String.format("setting page id: %s", pageId));
        this.pageId = pageId;

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setCheckedItem(pageId);

        int title = R.string.groupings;
        Fragment fragment = null;
        Class fragmentClass = GroupingsListFragment.class;
        if (pageId == R.id.nav_groupings) {
            title = R.string.groupings;
            fragmentClass = GroupingsListFragment.class;
        } else if (pageId == R.id.nav_games) {
            title = R.string.games;
            fragmentClass = GamesListFragment.class;
        } else if (pageId == R.id.nav_players) {
            title = R.string.players;
            fragmentClass = PlayersListFragment.class;
        } else if (pageId == R.id.nav_settings) {
            title = R.string.settings;
            fragmentClass = SettingsFragment.class;
        } else if (pageId == R.id.nav_about) {
            title = R.string.about;
            fragmentClass = AboutFragment.class;
        }
        getSupportActionBar().setTitle(title);
        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content, fragment).commit();
    }

    @Override
    public void onGameClick(Game game) {
        Log.d(LOG_TAG, String.format("game #%d clicked", game.id));
        Intent intent = new Intent(this, GameDetailActivity.class);
        intent.putExtra(GameDetailActivity.ARG_GAME_ID, game.id);
        startActivity(intent);
    }

    @Override
    public void onGroupingClick(Grouping grouping) {
        Log.d(LOG_TAG, String.format("grouping #%d clicked", grouping.id));
        Intent intent = new Intent(this, GroupingDetailActivity.class);
        intent.putExtra(GroupingDetailActivity.ARG_GROUPING_ID, grouping.id);
        startActivity(intent);
    }

    @Override
    public void onPlayerClick(Player player) {
        Log.d(LOG_TAG, String.format("player #%d clicked", player.id));
        Intent intent = new Intent(this, PlayerDetailActivity.class);
        intent.putExtra(PlayerDetailActivity.ARG_PLAYER_ID, player.id);
        startActivity(intent);
    }
}
