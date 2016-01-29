package com.givekesh.baboon;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.animation.OvershootInterpolator;

import com.givekesh.baboon.CustomViews.recyclerView;
import com.givekesh.baboon.Utils.FeedProvider;
import com.givekesh.baboon.Utils.Feeds;
import com.givekesh.baboon.Utils.FeedsAdapter;
import com.givekesh.baboon.Utils.Interfaces;
import com.givekesh.baboon.Utils.MainMenu;
import com.givekesh.baboon.Utils.Utils;
import com.mxn.soul.flowingdrawer_core.FlowingView;
import com.mxn.soul.flowingdrawer_core.LeftDrawerLayout;

import net.steamcrafted.materialiconlib.MaterialDrawableBuilder;

import java.util.ArrayList;

import jp.co.recruit_lifestyle.android.widget.WaveSwipeRefreshLayout;
import jp.wasabeef.recyclerview.animators.FadeInAnimator;


public class MainActivity extends AppCompatActivity implements Interfaces.VolleyCallback, Interfaces.OnNavClickListener {

    private LeftDrawerLayout mLeftDrawerLayout;
    private Utils utils;
    private ArrayList<Feeds> mFeedsArrayList = new ArrayList<>();
    private WaveSwipeRefreshLayout mWaveSwipeRefreshLayout;
    private FeedsAdapter mAdapter;
    private FeedProvider mFeedProvider;
    private recyclerView recyclerView;

    private boolean isFirstLoad = true;
    private boolean isLoadingMore = false;
    private boolean isSwipeRefresh = false;

    private String category = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFeedProvider = new FeedProvider(this);

        init();

        if (savedInstanceState != null) {
            mFeedsArrayList = savedInstanceState.getParcelableArrayList("main_feed");
            refreshRecycler();
        } else
            mFeedProvider.getFeedsArrayList(1, category, this);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("main_feed", mFeedsArrayList);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mFeedsArrayList = savedInstanceState.getParcelableArrayList("main_feed");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mAdapter.clear();
        mFeedProvider.getFeedsArrayList(1, item.getItemId() == R.id.homePage ? null : category, MainActivity.this);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        menu.findItem(R.id.refresh).setIcon(utils.getMaterialIcon(MaterialDrawableBuilder.IconValue.REFRESH, Color.WHITE));
        menu.findItem(R.id.homePage).setIcon(utils.getMaterialIcon(MaterialDrawableBuilder.IconValue.HOME, Color.WHITE));

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if (mLeftDrawerLayout.isShownMenu())
            mLeftDrawerLayout.closeDrawer();
        else if (mWaveSwipeRefreshLayout.isRefreshing())
            mWaveSwipeRefreshLayout.setRefreshing(false);
        else
            super.onBackPressed();
    }

    @Override
    public void onPreRequest() {
        recyclerView.defaultEmptyView();
    }

    @Override
    public void onSuccess(ArrayList<Feeds> result) {
        if (result != null) {
            if (isSwipeRefresh) {
                mFeedsArrayList.clear();
                mWaveSwipeRefreshLayout.setRefreshing(false);
            }
            if (isFirstLoad) {
                mFeedsArrayList = result;
                isFirstLoad = false;
            } else
                mFeedsArrayList.addAll(result);

            if (isLoadingMore)
                mAdapter.setLoading(false);

            //getIntent().putParcelableArrayListExtra("main_feed", mFeedsArrayList);
            refreshRecycler();
        }
    }

    @Override
    public void onFailure(String error) {
        if (isLoadingMore && error.equalsIgnoreCase("lastPage")) {
            recyclerView.disableLoadMore();
            Snackbar.make(recyclerView, R.string.no_more_post, Snackbar.LENGTH_LONG).show();
            isLoadingMore = false;
        } else {
            recyclerView.setError(error);
        }
    }

    @Override
    public void onSelect(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.telegram:
                openTelegram();
                return;

            case R.id.html:
                category = "html-css";
                break;

            case R.id.js:
                category = "javascript";
                break;

            case R.id.node:
                category = "node-js";
                break;

            case R.id.angular:
                category = "angularjs";
                break;

            case R.id.vagrant:
                category = "vagrant-tutorials";
                break;

            case R.id.laravel:
                category = "laravel-tutorials";
                break;

            case R.id.jq:
                category = "jquery";
                break;

            case R.id.php:
                category = "php";
                break;

            case R.id.bootstrap:
                category = "bootstrap";
                break;

            case R.id.ruby_on_rails:
                category = "ruby-on-rails";
                break;
        }
        loadBasedOnCategory();
    }

    private void init() {
        utils = new Utils(this);
        setupToolbar();
        setupMenu();
        setupContent();
    }


    private void setupContent() {
        mAdapter = new FeedsAdapter(mFeedsArrayList, this);
        mWaveSwipeRefreshLayout = (WaveSwipeRefreshLayout) findViewById(R.id.main_swipe);

        setupRecyclerView();

        mWaveSwipeRefreshLayout.setOnRefreshListener(new WaveSwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                isSwipeRefresh = true;
                mFeedProvider.getFeedsArrayList(1, category, MainActivity.this);
            }
        });
        mWaveSwipeRefreshLayout.setWaveColor(ContextCompat.getColor(this, R.color.colorPrimary));

    }

    private void setupRecyclerView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);

        recyclerView = (recyclerView) findViewById(R.id.RecyclerView);
        recyclerView.setEmptyView(findViewById(R.id.emptyView));
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                outRect.bottom = getResources().getDimensionPixelOffset(R.dimen.activity_vertical_margin);
            }
        });

        recyclerView.setAdapter(mAdapter);

        recyclerView.setOnLoadMoreListener(new Interfaces.OnLoadMoreListener() {
            @Override
            public void loadMore(int itemsCount) {
                if (itemsCount >= 10) {
                    isLoadingMore = true;
                    mFeedProvider.getFeedsArrayList(getPage(itemsCount), category, MainActivity.this);
                } else
                    recyclerView.disableLoadMore();
            }
        });

        FadeInAnimator animator = new FadeInAnimator();
        animator.setRemoveDuration(1000);
        animator.setAddDuration(1000);
        animator.setInterpolator(new OvershootInterpolator(1f));
        recyclerView.setItemAnimator(animator);
    }

    private void setupMenu() {
        mLeftDrawerLayout = (LeftDrawerLayout) findViewById(R.id.drawer_layout);
        FragmentManager fm = getSupportFragmentManager();
        MainMenu mainMenu = (MainMenu) fm.findFragmentById(R.id.id_container_menu);
        FlowingView flowingView = (FlowingView) findViewById(R.id.sv);
        if (mainMenu == null)
            fm.beginTransaction().add(R.id.id_container_menu, mainMenu = new MainMenu()).commit();

        mLeftDrawerLayout.setFluidView(flowingView);
        mLeftDrawerLayout.setMenuFragment(mainMenu);
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(utils.getMaterialIcon(MaterialDrawableBuilder.IconValue.MENU, Color.WHITE));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLeftDrawerLayout.toggle();
            }
        });
    }

    private int getPage(int itemCount) {
        return (itemCount / 10) + 1;
    }

    private void refreshRecycler() {
        mAdapter.refresh(mFeedsArrayList);
    }


    private void loadBasedOnCategory() {
        mLeftDrawerLayout.closeDrawer();
        isFirstLoad = true;
        mAdapter.clear();
        mFeedProvider.getFeedsArrayList(1, category, this);
    }


    private void openTelegram() {
        mLeftDrawerLayout.closeDrawer();

        startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://telegram.me/baboon_ir")));
    }

}