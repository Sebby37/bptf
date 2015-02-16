package com.tlongdev.bktf.adapter;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tlongdev.bktf.R;
import com.tlongdev.bktf.Utility;
import com.tlongdev.bktf.fragment.HomeFragment;

import java.io.IOException;
import java.io.InputStream;

public class PriceListCursorAdapter extends CursorAdapter {


    public PriceListCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_changes, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        String itemTag = Utility.formatItemName(cursor.getString(HomeFragment.COL_PRICE_LIST_NAME),
                cursor.getInt(HomeFragment.COL_PRICE_LIST_TRAD),
                cursor.getInt(HomeFragment.COL_PRICE_LIST_CRAF),
                cursor.getInt(HomeFragment.COL_PRICE_LIST_QUAL),
                cursor.getInt(HomeFragment.COL_PRICE_LIST_INDE));
        viewHolder.nameView.setText(itemTag);

        viewHolder.background.setTag(itemTag);

        viewHolder.icon.setImageDrawable(null);
        viewHolder.background.setBackgroundDrawable(null);
        viewHolder.change.setImageDrawable(null);

        LoadImagesTask task = (LoadImagesTask)viewHolder.icon.getTag();
        if (task != null){
            task.cancel(true);
        }
        task = new LoadImagesTask(context, viewHolder.icon, viewHolder.background, viewHolder.change);
        viewHolder.icon.setTag(task);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                (double) cursor.getInt(HomeFragment.COL_PRICE_LIST_DEFI),
                (double) cursor.getInt(HomeFragment.COL_PRICE_LIST_INDE),
                cursor.getDouble(HomeFragment.COL_PRICE_LIST_DIFF),
                cursor.getDouble(HomeFragment.COL_PRICE_LIST_PRAW),
                (double) cursor.getInt(HomeFragment.COL_PRICE_LIST_QUAL),
                (double) cursor.getInt(HomeFragment.COL_PRICE_LIST_TRAD),
                (double) cursor.getInt(HomeFragment.COL_PRICE_LIST_CRAF)
        );

        try {
            viewHolder.priceView.setText(Utility.formatPrice(context,
                    cursor.getDouble(HomeFragment.COL_PRICE_LIST_PRIC),
                    cursor.getDouble(HomeFragment.COL_PRICE_LIST_PMAX),
                    cursor.getString(HomeFragment.COL_PRICE_LIST_CURR),
                    cursor.getString(HomeFragment.COL_PRICE_LIST_CURR), false));
        } catch (Throwable throwable) {
            Toast.makeText(context, "bptf: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
            if (Utility.isDebugging(context))
                throwable.printStackTrace();
        }
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    public static class ViewHolder {
        public final ImageView change;
        public final ImageView icon;
        public final ImageView background;

        public final TextView nameView;
        public final TextView priceView;

        public ViewHolder(View view) {
            change = (ImageView) view.findViewById(R.id.image_view_change);
            icon = (ImageView) view.findViewById(R.id.image_view_item_icon);
            background = (ImageView) view.findViewById(R.id.image_view_item_background);
            nameView = (TextView) view.findViewById(R.id.item_name);
            priceView = (TextView) view.findViewById(R.id.item_price);
        }
    }

    private class LoadImagesTask extends AsyncTask<Double, Void, Drawable[]>{
        private Context mContext;
        private ImageView icon;
        private ImageView background;
        private ImageView change;
        private String name;
        private String errorMessage;

        private LoadImagesTask(Context context, ImageView icon, ImageView background, ImageView change) {
            mContext = context;
            this.icon = icon;
            this.change = change;
            this.background = background;
            name = (String)background.getTag();
        }

        @Override
        protected Drawable[] doInBackground(Double... params) {
            try {
                Drawable[] returnVal = new Drawable[3];
                AssetManager assetManager = mContext.getAssets();

                InputStream ims;
                if (name.contains("Australium") && params[0] != 5037) {
                    ims = assetManager.open("items/" + params[0].intValue() + "aus.png");
                } else {
                    ims = assetManager.open("items/" + params[0].intValue() + ".png");
                }

                Drawable iconDrawable = Drawable.createFromStream(ims, null);
                if (params[1] != 0 && !name.contains("Crate")) {
                    ims = assetManager.open("effects/" + params[1].intValue() + "_188x188.png");
                    Drawable effectDrawable = Drawable.createFromStream(ims, null);
                    returnVal[0] = new LayerDrawable(new Drawable[]{effectDrawable, iconDrawable});
                } else {
                    returnVal[0] = iconDrawable;
                }

                if (params[2].equals(params[3])) {
                    ims = mContext.getAssets().open("changes/new.png");
                }
                else if (params[2] == 0.0) {
                    ims = mContext.getAssets().open("changes/refresh.png");
                }
                else if (params[2] > 0.0) {
                    ims = mContext.getAssets().open("changes/up.png");
                }
                else {
                    ims = mContext.getAssets().open("changes/down.png");
                }

                returnVal[2] = Drawable.createFromStream(ims, null);

                returnVal[1] = Utility.getItemBackground(mContext, params[4].intValue(), params[5].intValue(), params[6].intValue());

                return returnVal;
            } catch (IOException e) {
                errorMessage = e.getMessage();
                publishProgress();
                if (Utility.isDebugging(mContext))
                    e.printStackTrace();
                return null;
            }
        }


        @Override
        protected void onProgressUpdate(Void... values) {
            Toast.makeText(mContext, "bptf: " + errorMessage, Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onPostExecute(Drawable[] drawable) {
            if (drawable != null) {
                icon.setImageDrawable(drawable[0]);
                background.setBackgroundDrawable(drawable[1]);
                change.setImageDrawable(drawable[2]);
            }
        }
    }
}
