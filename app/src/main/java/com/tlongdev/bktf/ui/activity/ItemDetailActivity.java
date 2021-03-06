package com.tlongdev.bktf.ui.activity;

import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.f2prateek.dart.Dart;
import com.f2prateek.dart.InjectExtra;
import com.tlongdev.bktf.R;
import com.tlongdev.bktf.model.BackpackItem;
import com.tlongdev.bktf.model.Price;
import com.tlongdev.bktf.model.Quality;
import com.tlongdev.bktf.presenter.activity.ItemDetailPresenter;
import com.tlongdev.bktf.ui.view.activity.ItemDetailView;
import com.tlongdev.bktf.util.Utility;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * The (dialog) activity for showing info about an item in a backpack.
 */
public class ItemDetailActivity extends BptfActivity implements ItemDetailView {

    //Keys for the extra data in the intent
    public static final String EXTRA_ITEM = "item";
    public static final String EXTRA_ITEM_NAME = "name";
    public static final String EXTRA_ITEM_TYPE = "type";
    public static final String EXTRA_PROPER_NAME = "proper";

    @Inject ItemDetailPresenter mPresenter;

    @InjectExtra(EXTRA_ITEM) BackpackItem mItem;
    @InjectExtra(EXTRA_PROPER_NAME) int mProperName;
    @InjectExtra(EXTRA_ITEM_NAME) String mItemName;
    @InjectExtra(EXTRA_ITEM_TYPE) String mItemType;

    //References to all the text views in the view
    @BindView(R.id.text_view_name) TextView name;
    @BindView(R.id.text_view_level) TextView level;
    @BindView(R.id.text_view_effect_name) TextView effect;
    @BindView(R.id.text_view_custom_name) TextView customName;
    @BindView(R.id.text_view_custom_desc) TextView customDesc;
    @BindView(R.id.text_view_crafted) TextView crafterName;
    @BindView(R.id.text_view_gifted) TextView gifterName;
    @BindView(R.id.text_view_origin) TextView origin;
    @BindView(R.id.text_view_paint) TextView paint;
    @BindView(R.id.text_view_price) TextView priceView;

    //References to the image view
    @BindView(R.id.icon) ImageView icon;
    @BindView(R.id.effect) ImageView effectView;
    @BindView(R.id.paint) ImageView paintView;
    @BindView(R.id.quality) ImageView quality;
    @BindView(R.id.card_view) CardView cardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);
        ButterKnife.bind(this);
        Dart.inject(this);

        mApplication.getActivityComponent().inject(this);

        mPresenter.attachView(this);

        //Return to the previous activity if the user taps outside te dialog.
        ((View) cardView.getParent()).setOnClickListener(v -> finishAfterTransition());

        //Do nothing if the user taps on the card view itself
        cardView.setOnClickListener(null);

        mPresenter.loadItemDetails(mItem);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter.detachView();
    }

    @Override
    public void showItemDetails(Price price) {
        mItem.setPrice(price);

        BackpackItem item = mItem;

        item.setName(mItemName);
        //Set the name of the item
        name.setText(item.getFormattedName(this, mProperName == 1));

        //Set the level of the item, get the type from the intent
        if (item.getDefindex() >= 15000 && item.getDefindex() <= 15059) {
            level.setText(item.getDecoratedWeaponDesc(this, mItemType));
        } else {
            level.setText(getString(R.string.item_detail_level, item.getLevel(), mItemType));
        }

        //Set the origin of the item. Get the origin from the string array resource
        origin.setText(String.format("%s: %s", getString(R.string.item_detail_origin), Utility.getOriginName(this, item.getOrigin())));

        //Set the effect of the item (if any)
        if (item.getPriceIndex() != 0 && (item.getQuality() == Quality.UNUSUAL
                || item.getQuality() == Quality.COMMUNITY
                || item.getQuality() == Quality.SELF_MADE)) {
            effect.setText(String.format("%s: %s", getString(R.string.item_detail_effect),
                    Utility.getUnusualEffectName(this, item.getPriceIndex())));
            effect.setVisibility(View.VISIBLE);
        }

        //set the custom name of the item (if any)
        if (item.getCustomName() != null) {
            customName.setText(Html.fromHtml(String.format("%s: <i>%s</i>",
                    getString(R.string.item_detail_custom_name), item.getCustomName())));
            customName.setVisibility(View.VISIBLE);
        }

        //Set the custom description of the item (if any)
        if (item.getCustomDescription() != null) {
            customDesc.setText(Html.fromHtml(String.format("%s: <i>%s</i>",
                    getString(R.string.item_detail_custom_description), item.getCustomDescription())));
            customDesc.setVisibility(View.VISIBLE);
        }

        //Set the crafter's name (if any)
        if (item.getCreatorName() != null) {
            crafterName.setText(Html.fromHtml(String.format("%s: <i>%s</i>",
                    getString(R.string.item_detail_craft), item.getCreatorName())));
            crafterName.setVisibility(View.VISIBLE);
        }

        //Set the gifter's name (if any)
        if (item.getGifterName() != null) {
            gifterName.setText(Html.fromHtml(String.format("%s: <i>%s</i>",
                    getString(R.string.item_detail_gift), item.getGifterName())));
            gifterName.setVisibility(View.VISIBLE);
        }

        //Set the paint text (if any)
        if (item.getPaint() != 0) {
            paint.setText(String.format("%s: %s", getString(R.string.item_detail_paint), item.getPaintName(this)));
            paint.setVisibility(View.VISIBLE);
        }

        //Set the icon and the background
        Glide.with(this)
                .load(item.getIconUrl(this))
                .into(icon);

        if (item.getPriceIndex() != 0 && item.canHaveEffects()) {
            Glide.with(this)
                    .load(item.getEffectUrl())
                    .into(effectView);
        }

        if (!item.isTradable()) {
            quality.setVisibility(View.VISIBLE);
            if (!item.isCraftable()) {
                quality.setImageResource(R.drawable.uncraft_untrad);
            } else {
                quality.setImageResource(R.drawable.untrad);
            }
        } else if (!item.isCraftable()) {
            quality.setVisibility(View.VISIBLE);
            quality.setImageResource(R.drawable.uncraft);
        }

        if (BackpackItem.isPaint(item.getPaint())) {
            Glide.with(this)
                    .load("file:///android_asset/paint/" + item.getPaint() + ".png")
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(paintView);
        }

        cardView.setCardBackgroundColor(item.getColor(this, true));

        if (price != null) {
            //Show the priceView
            priceView.setVisibility(View.VISIBLE);
            priceView.setText(String.format("%s: %s",
                    getString(R.string.item_detail_suggested_price),
                    price.getFormattedPrice(this)));
        }
    }
}
