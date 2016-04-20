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

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import butterknife.*;

import com.cmcm.adsdk.nativead.NativeAdManager;
import com.cmcm.baseapi.ads.INativeAd;
import com.cmcm.baseapi.ads.INativeAdLoaderListener;
import com.dbychkov.domain.Flashcard;
import com.dbychkov.words.R;
import com.dbychkov.words.ad.OrionNativeAdview;
import com.dbychkov.words.adapter.CardPagerAdapter;
import com.dbychkov.words.anim.ZoomOutPageTransformer;
import com.dbychkov.words.dagger.component.ActivityComponent;
import com.dbychkov.words.fragment.CardContainerFragment;
import com.dbychkov.words.presentation.StudyFlashcardsActivityPresenter;
import com.dbychkov.words.view.StudyFlashcardsView;
import com.dbychkov.words.widgets.ViewPagerCustomDuration;

import javax.inject.Inject;
import java.util.List;

/**
 * Study session activity
 */
public class StudyFlashcardsActivity extends BaseActivity implements StudyFlashcardsView {

    public static final String EXTRA_LESSON_ID = "lessonId";

    @Inject
    StudyFlashcardsActivityPresenter presenter;

    @Bind(R.id.knowButton)
    ImageView knowButton;

    @Bind(R.id.dontKnowButton)
    ImageView dontKnowButton;

    @Bind(R.id.view_pager)
    ViewPagerCustomDuration viewPager;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @BindString(R.string.flashcards_activity_title)
    String title;

    @BindString(R.string.flashcards_activity_lesson_ended_title)
    String lessonEndedTitle;

    @BindString(R.string.flashcards_activity_lesson_ended_text)
    String lessonEndedText;

    @BindString(R.string.flashcards_activity_words_learnt_title)
    String wordsLearntTitle;

    @BindString(R.string.flashcards_activity_words_learnt_text)
    String wordsLearntText;

    @BindColor(R.color.know_button_color)
    int knowButtonColor;

    @BindColor(R.color.dont_know_button_color)
    int dontKnowButtonColor;

    @Bind(R.id.main_container)
    RelativeLayout relativeLayout;

    private long lessonId;

    private CardPagerAdapter adapter;

    private NativeAdManager nativeAdManager;
    private FrameLayout nativeAdContainer;
    /* 广告 Native大卡样式 */
    private Button loadAdButton;
    private String mAdPosid = "1377100";

    private OrionNativeAdview mAdView = null;
    private static int count = 0;


    public static Intent createIntent(Context context, Long lessonId) {
        Intent intent = new Intent(context, StudyFlashcardsActivity.class);
        intent.putExtra(EXTRA_LESSON_ID, lessonId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_cards);
        ButterKnife.bind(this);
        initExtra();
        initToolbar();
        initButtons();
        initPresenter();
        count = 0;
        nativeAdContainer = (FrameLayout) findViewById(R.id.big_ad_container);
        initNativeAd();
        //nativeAdManager.loadAd();
    }

    private void initExtra() {
        lessonId = getIntent().getLongExtra(FlashcardsActivity.EXTRA_LESSON_ID, -1L);
    }

    private void initPresenter() {
        presenter.setView(this);
        presenter.initialize(lessonId);
    }

    @Override
    public void injectActivity(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left_100, R.anim.slide_out_right_100);
    }

    private void initToolbar() {
        setSupportActionBar(toolbar);
        try {
            final ActionBar ab = getSupportActionBar();
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            ab.setTitle(title);
        } catch (Exception ex) {
        }
    }

    private void initButtons() {
        knowButton.setColorFilter(knowButtonColor);
        dontKnowButton.setColorFilter(dontKnowButtonColor);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void showLessonEndedDialog() {
        buildDialog(lessonEndedText, lessonEndedTitle);
    }

    @Override
    public void showAllWordsLearntDialog() {
        buildDialog(wordsLearntText, wordsLearntTitle);
    }

    private void buildDialog(String message, String title) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        onBackPressed();

                    }
                })
                .setCancelable(false)
                .create()
                .show();
    }

    @Override
    public void renderFlashcards(List<Flashcard> wordsFromLesson) {
        adapter = new CardPagerAdapter(getFragmentManager(), wordsFromLesson);
        viewPager.setPageTransformer(true, new ZoomOutPageTransformer());
        viewPager.setAdapter(adapter);
        viewPager.setScrollDurationFactor(3);
    }

    public Fragment findFragmentByPosition(int position) {
        return getFragmentManager().findFragmentByTag("android:switcher:" + viewPager.getId() + ":"
                + adapter.getItemId(position));
    }

    @Override
    public boolean showCardBack(int position) {
        CardContainerFragment cardContainerFragment = ((CardContainerFragment) findFragmentByPosition(position));
        if (!cardContainerFragment.isFlipped()) {
            cardContainerFragment.flipCard();
            return true;
        }
        return false;
    }

    @OnClick(R.id.knowButton)
    public void onKnowButtonClicked() {
        presenter.knowWordButtonPressed();
        count ++;
        if(count % 10 == 0)
            nativeAdManager.loadAd();

    }

    @OnClick(R.id.dontKnowButton)
    public void onDontKnowButtonClicked() {
        presenter.dontKnowWordButtonPressed();
    }

    @Override
    public void showFlashcard(int flashCardNumber) {
        viewPager.setCurrentItem(flashCardNumber);
    }


    private void initNativeAd() {




        nativeAdManager = new NativeAdManager(this, mAdPosid);
        nativeAdManager.setNativeAdListener(new INativeAdLoaderListener() {
            @Override
            public void adLoaded() {
                INativeAd ad = nativeAdManager.getAd();
                Toast.makeText(StudyFlashcardsActivity.this, "adLoaded", Toast.LENGTH_SHORT).show();
                if (mAdView != null) {
                    // 把旧的广告view从广告容器中移除
                    nativeAdContainer.removeView(mAdView);
                }
                if (ad == null) {
                    Toast.makeText(StudyFlashcardsActivity.this,
                            "no native ad loaded!", Toast.LENGTH_SHORT).show();
                    return;
                }
                //通过广告数据渲染广告View
                mAdView = OrionNativeAdview.createAdView(getApplicationContext(), ad);
                nativeAdContainer.addView(mAdView);
            }

            @Override
            public void adFailedToLoad(int i) {
                Toast.makeText(StudyFlashcardsActivity.this, "Ad failed to load errorCode:" + i,
                        Toast.LENGTH_SHORT).show();
            }


            @Override
            public void adClicked(INativeAd ad) {
                Toast.makeText(StudyFlashcardsActivity.this, "adClicked",
                        Toast.LENGTH_SHORT).show();
            }

        });
    }
}

