/**
 * Copyright (C) dbychkov.com.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbychkov.words.activity;

import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.widget.NestedScrollView;
import android.view.*;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import com.cmcm.adsdk.nativead.NativeAdManager;
import com.cmcm.baseapi.ads.INativeAd;
import com.cmcm.baseapi.ads.INativeAdLoaderListener;
import com.daimajia.numberprogressbar.NumberProgressBar;
import com.dbychkov.domain.repository.FlashcardRepository;
import com.dbychkov.words.R;
import com.dbychkov.words.ad.OrionNativeAdview;
import com.dbychkov.words.navigator.Navigator;
import com.dbychkov.words.view.FlashcardsView;
import com.dbychkov.words.widgets.RecyclerViewWithEmptyView;
import com.squareup.picasso.Picasso;

import javax.inject.Inject;

/**
 * Flashcards activity
 */
public abstract class FlashcardsActivity extends AbstractExpandingActivity
        implements AppBarLayout.OnOffsetChangedListener, FlashcardsView {

    public static final String EXTRA_LESSON_ID = "lessonId";
    public static final String EXTRA_LESSON_NAME = "lessonName";
    public static final String EXTRA_LESSON_IMAGE_PATH = "imagePath";
    public static final String EXTRA_EDITABLE_LESSON = "isLessonEditable";

    @Inject
    Navigator navigator;

    @Inject
    FlashcardRepository flashcardRepository;

    @Bind(R.id.main_content)
    CoordinatorLayout rootCoordinatorLayout;

    @Bind(R.id.test_cards_button)
    ImageView testCardsButton;

    @Bind(R.id.collapsing_toolbar)
    CollapsingToolbarLayout collapsingToolbar;

    @Bind(R.id.header_image)
    ImageView headerImage;

    @Bind(R.id.lesson_progress)
    NumberProgressBar numberProgressBar;

    @Bind(R.id.list)
    RecyclerViewWithEmptyView recyclerView;

    @Bind(R.id.list_empty)
    TextView textView;

    @Bind(R.id.scroll)
    NestedScrollView nestedScrollView;

    private int testCardsButtonHeight;

    private int testCardsButtonWidth;

    protected long lessonId;

    protected String lessonImagePath;

    protected String lessonName;
    private NativeAdManager nativeAdManager;
    private FrameLayout nativeAdContainer;
    /* 广告 Native大卡样式 */
    private Button loadAdButton;
    private String mAdPosid = "1377100";

    private OrionNativeAdview mAdView = null;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_view_cards, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.clear:
            clearProgressClicked();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    abstract void clearProgressClicked();

    private void initTitle() {
        collapsingToolbar.setTitle(lessonName);
    }

    private void initExtra() {
        lessonId = getIntent().getLongExtra(EXTRA_LESSON_ID, -1);
        lessonImagePath = getIntent().getStringExtra(EXTRA_LESSON_IMAGE_PATH);
        lessonName = getIntent().getStringExtra(EXTRA_LESSON_NAME);
    }

    @OnClick(R.id.test_cards_button)
    void proceedToExercises() {
        navigator.navigateToStudyFlashcardsActivity(this, lessonId);
        overridePendingTransition(R.anim.slide_in_right_100, R.anim.slide_out_left_100);
    }

    private void initHeader() {
        Picasso
                .with(this)
                .load("file:///android_asset/" + lessonImagePath)
                .into(headerImage);
        testCardsButton.setColorFilter(getResources().getColor(R.color.colorAccent));
        testCardsButtonWidth = testCardsButton.getLayoutParams().width;
        testCardsButtonHeight = testCardsButton.getLayoutParams().height;

        AppBarLayout mAppBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        mAppBarLayout.addOnOffsetChangedListener(this);
    }

    @Override
    public View getRootLayout() {
        return rootCoordinatorLayout;
    }

    @Override
    public void onCreateExpandingActivity(Bundle savedInstanceState) {
        setContentView(R.layout.activity_cards);
        ButterKnife.bind(this);
        initExtra();
        initTitle();
        initHeader();
        nativeAdContainer = (FrameLayout) findViewById(R.id.big_ad_container);
        initNativeAd();
        nativeAdManager.loadAd();
    }

    @Override
    public void renderProgress(Integer renderProgress) {
        numberProgressBar.setProgress(renderProgress);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
        int maxScroll = appBarLayout.getTotalScrollRange();
        float progressPercentage = (float) Math.abs(offset) / (float) maxScroll;
        testCardsButton.setAlpha(1 - progressPercentage);
        ViewGroup.LayoutParams layoutParams = testCardsButton.getLayoutParams();
        layoutParams.width = (int) (testCardsButtonWidth * (1 - progressPercentage));
        layoutParams.height = (int) (testCardsButtonHeight * (1 - progressPercentage));
        testCardsButton.setLayoutParams(layoutParams);
    }

    private void initNativeAd() {




        nativeAdManager = new NativeAdManager(this, mAdPosid);
        nativeAdManager.setNativeAdListener(new INativeAdLoaderListener() {
            @Override
            public void adLoaded() {
                INativeAd ad = nativeAdManager.getAd();
                Toast.makeText(FlashcardsActivity.this, "adLoaded", Toast.LENGTH_SHORT).show();
                if (mAdView != null) {
                    // 把旧的广告view从广告容器中移除
                    nativeAdContainer.removeView(mAdView);
                }
                if (ad == null) {
                    Toast.makeText(FlashcardsActivity.this,
                            "no native ad loaded!", Toast.LENGTH_SHORT).show();
                    return;
                }
                //通过广告数据渲染广告View
                mAdView = OrionNativeAdview.createAdView(getApplicationContext(), ad);
                nativeAdContainer.addView(mAdView);
            }

            @Override
            public void adFailedToLoad(int i) {
                Toast.makeText(FlashcardsActivity.this, "Ad failed to load errorCode:" + i,
                        Toast.LENGTH_SHORT).show();
            }


            @Override
            public void adClicked(INativeAd ad) {
                Toast.makeText(FlashcardsActivity.this, "adClicked",
                        Toast.LENGTH_SHORT).show();
            }

        });
    }

}

