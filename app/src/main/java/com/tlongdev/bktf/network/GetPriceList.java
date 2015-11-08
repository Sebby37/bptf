package com.tlongdev.bktf.network;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.tlongdev.bktf.R;
import com.tlongdev.bktf.Utility;
import com.tlongdev.bktf.data.DatabaseContract.PriceEntry;
import com.tlongdev.bktf.model.Item;
import com.tlongdev.bktf.model.Price;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

/**
 * Task for fetching all data for prices database and updating it in the background.
 */
public class GetPriceList extends AsyncTask<Void, Integer, Void> {

    /**
     * Log tag for logging.
     */
    @SuppressWarnings("unused")
    private static final String LOG_TAG = GetPriceList.class.getSimpleName();

    //The context the task runs in
    private final Context mContext;

    //Whether it's an update or full database download
    private boolean updateDatabase;

    //Whether it was a user initiated update
    private boolean manualSync;

    //Dialog to indicate the download progress
    private ProgressDialog loadingDialog;

    //Error message to be displayed to the user
    private String errorMessage;

    //The listener that will be notified when the fetching finishes
    private OnPriceListFetchListener listener;

    //the variable that contains the birth time of the youngest price
    private int latestUpdate = 0;

    /**
     * Constructor
     *
     * @param context        the context the task was launched in
     * @param updateDatabase whether the database only needs an update
     * @param manualSync     whether this task was user initiated
     */
    public GetPriceList(Context context, boolean updateDatabase, boolean manualSync) {
        this.mContext = context;
        this.updateDatabase = updateDatabase;
        this.manualSync = manualSync;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPreExecute() {
        if (!updateDatabase)
            //Show the progress dialog
            loadingDialog = ProgressDialog.show(mContext, null, "Downloading data...", true);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Void doInBackground(Void... params) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        if (System.currentTimeMillis() - prefs.getLong(mContext
                .getString(R.string.pref_last_price_list_update), 0) < 3600000L
                && !manualSync) {
            //This task ran less than an hour ago and wasn't a manual sync, nothing to do.
            return null;
        }

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;

        try {
            //The prices api and input keys
            final String PRICES_BASE_URL = mContext.getString(R.string.tlongdev_prices);
            final String KEY_SINCE = "since";

            //Build the URI
            Uri.Builder builder = Uri.parse(PRICES_BASE_URL).buildUpon();

            //Get the youngest price from the database. If it's an update only prices newer than this
            //will be updated to speed up the update and reduce data usage.
            if (updateDatabase) {
                String[] columns = {PriceEntry.COLUMN_LAST_UPDATE};
                Cursor cursor = mContext.getContentResolver().query(
                        PriceEntry.CONTENT_URI,
                        columns,
                        null,
                        null,
                        PriceEntry.COLUMN_LAST_UPDATE + " DESC LIMIT 1"
                );
                if (cursor != null) {
                    if (cursor.moveToFirst())
                        latestUpdate = cursor.getInt(0);
                    cursor.close();
                }

                builder.appendQueryParameter(KEY_SINCE, String.valueOf(latestUpdate));
            }
            Uri uri = builder.build();

            //Initialize the URL
            URL url = new URL(uri.toString());

            /*if (Utility.isDebugging(mContext))
                Log.v(LOG_TAG, "Built uri: " + uri.toString());*/

            //Open connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            //Get the input stream
            InputStream inputStream = urlConnection.getInputStream();

            if (inputStream == null) {
                // Stream was empty. Nothing to do.
                return null;
            }

            //Get all the items from the JSON string
            if (getItemsFromJson(inputStream)) {
                //Get the shared preferences
                SharedPreferences.Editor editor = prefs.edit();

                //Save when the update finished
                editor.putLong(mContext.getString(R.string.pref_last_price_list_update),
                        System.currentTimeMillis());
                editor.putBoolean(mContext.getString(R.string.pref_initial_load), false);
                editor.apply();
            }

        } catch (IOException e) {
            //There was a network error
            errorMessage = mContext.getString(R.string.error_network);
            publishProgress(-1);
            if (Utility.isDebugging(mContext))
                e.printStackTrace();
            return null;
        } catch (JSONException e) {
            //There was an error parsing data
            errorMessage = "error while parsing data";
            publishProgress(-1);
            if (Utility.isDebugging(mContext))
                e.printStackTrace();
            return null;
        } finally {
            //Close the connection
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onProgressUpdate(Integer... values) {
        if (loadingDialog != null) {
            AlertDialog.Builder builder;
            AlertDialog alertDialog;
            switch (values[0]) {
                //Download finished. Replace dialog.
                case 0:
                    loadingDialog.dismiss();
                    loadingDialog = new ProgressDialog(mContext);
                    loadingDialog.setIndeterminate(false);
                    loadingDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    loadingDialog.setMessage(mContext.getString(R.string.message_database_create));
                    loadingDialog.setMax(values[1]);
                    loadingDialog.show();
                    break;
                //One item processed
                case 1:
                    loadingDialog.incrementProgressBy(1);
                    break;
                //There was an error (exception) while trying to create initial database.
                //Show a dialog that the download failed.
                case -1:
                    builder = new AlertDialog.Builder(mContext);
                    builder.setMessage(mContext.getString(R.string.message_database_fail_network))
                            .setCancelable(false)
                            .setPositiveButton(mContext.getString(R.string.action_close), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //Close app
                                    ((Activity) mContext).finish();
                                }
                            });
                    alertDialog = builder.create();
                    loadingDialog.dismiss();
                    alertDialog.show();
                    break;
                //Api returned 0, unsuccessful
                case -2:
                    builder = new AlertDialog.Builder(mContext);
                    builder.setMessage(mContext.getString(R.string.message_database_fail_network))
                            .setCancelable(false)
                            .setPositiveButton(mContext.getString(R.string.action_close), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //Close app
                                    ((Activity) mContext).finish();
                                }
                            });
                    alertDialog = builder.create();
                    loadingDialog.dismiss();
                    alertDialog.show();
                    break;
            }
        } else if (values[0] == -1) {
            //There was an error while trying to update database
            Toast.makeText(mContext, "bptf: " + errorMessage, Toast.LENGTH_SHORT).show();
        } else if (values[0] == -2) {
            Toast.makeText(mContext, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPostExecute(Void aVoid) {
        //Dismiss loading dialog
        if (loadingDialog != null && !updateDatabase)
            loadingDialog.dismiss();

        if (listener != null) {
            //Notify the listener that the update finished
            listener.onPriceListFetchFinished();
        }
    }

    /**
     * Register a listener which will be notified when the fetching finishes.
     *
     * @param listener the listener to be notified
     */
    public void setOnPriceListFetchListener(OnPriceListFetchListener listener) {
        this.listener = listener;
    }

    /**
     * Parse all the items from the JSON string.
     *
     * @param inputStream the input stream to parse from
     * @return whether the query was successful or not
     * @throws JSONException
     */
    @SuppressWarnings("ConstantConditions")
    private boolean getItemsFromJson(InputStream inputStream) throws JSONException, IOException {

        //All the JSON keys needed to parse
        final String KEY_SUCCESS = "success";
        final String KEY_MESSAGE = "message";
        final String KEY_COUNT = "count";
        final String KEY_DEFINDEX = "defindex";
        final String KEY_QUALITY = "quality";
        final String KEY_TRADABLE = "tradable";
        final String KEY_CRAFTABLE = "craftable";
        final String KEY_PRICE_INDEX = "price_index";
        final String KEY_AUSTRALIUM = "australium";
        final String KEY_CURRENCY = "currency";
        final String KEY_VALUE = "value";
        final String KEY_VALUE_HIGH = "value_high";
        final String KEY_VALUE_RAW = "value_raw";
        final String KEY_LAST_UPDATE = "last_update";
        final String KEY_DIFFERENCE = "difference";

        //Create a parser from the input stream for fast parsing and low impact on memory
        JsonFactory factory = new JsonFactory();
        JsonParser parser = factory.createParser(inputStream);

        //Not a JSON if it doesn't start with START OBJECT
        if (parser.nextToken() != JsonToken.START_OBJECT) {
            return false;
        }

        JsonToken token;
        while ((token = parser.nextToken()) != JsonToken.END_OBJECT) {

            //Start of the items that contains the items
            if (token == JsonToken.START_ARRAY) {

                //Iterator that will iterate through the items
                Vector<ContentValues> cVVector = new Vector<>();

                //Keep iterating whil the array hasn't ended
                while (parser.nextToken() != JsonToken.END_ARRAY) {

                    //Initial values
                    int defindex = 0;
                    int quality = 0;
                    int tradable = 0;
                    int craftable = 0;
                    int priceIndex = 0;
                    int australium = 0;
                    double value = 0;
                    String currency = null;
                    long lastUpdate = 0;
                    double difference = 0;
                    Double high = null;
                    double raw = 0;

                    //Parse an attribute and get the value of it
                    while (parser.nextToken() != JsonToken.END_OBJECT) {
                        switch (parser.getCurrentName()) {
                            case KEY_DEFINDEX:
                                parser.nextToken();
                                defindex = parser.getIntValue();
                                break;
                            case KEY_QUALITY:
                                parser.nextToken();
                                quality = parser.getIntValue();
                                break;
                            case KEY_TRADABLE:
                                parser.nextToken();
                                tradable = parser.getIntValue();
                                break;
                            case KEY_CRAFTABLE:
                                parser.nextToken();
                                craftable = parser.getIntValue();
                                break;
                            case KEY_PRICE_INDEX:
                                parser.nextToken();
                                priceIndex = parser.getIntValue();
                                break;
                            case KEY_AUSTRALIUM:
                                parser.nextToken();
                                australium = parser.getIntValue();
                                break;
                            case KEY_CURRENCY:
                                parser.nextToken();
                                currency = parser.getText();
                                break;
                            case KEY_VALUE:
                                parser.nextToken();
                                value = parser.getDoubleValue();
                                break;
                            case KEY_VALUE_HIGH:
                                parser.nextToken();
                                high = parser.getDoubleValue();
                                break;
                            case KEY_VALUE_RAW:
                                parser.nextToken();
                                raw = parser.getDoubleValue();
                                break;
                            case KEY_LAST_UPDATE:
                                parser.nextToken();
                                lastUpdate = parser.getLongValue();
                                break;
                            case KEY_DIFFERENCE:
                                parser.nextToken();
                                difference = parser.getDoubleValue();
                                break;
                        }
                    }

                    //Currency prices a processed slightly differently, some more info is
                    //saved to the default shared preferences
                    if (quality == 6 && tradable == 1 && craftable == 1) {
                        //Save extra info about the buds price
                        if (defindex == 143) {

                            //Get the sharedpreferences
                            SharedPreferences.Editor editor = PreferenceManager
                                    .getDefaultSharedPreferences(mContext).edit();

                            //Store the price in a string so it can be displayed in the
                            //header in the latest changes page
                            double highPrice = high == null ? 0 : high;

                            Price budPrice = new Price(
                                    value, highPrice, raw, lastUpdate, difference, currency
                            );

                            String priceString = budPrice.getFormattedPrice(mContext);
                            editor.putString(mContext.getString(R.string.pref_buds_price), priceString);

                            //Save the difference
                            Utility.putDouble(editor, mContext.getString(R.string.pref_buds_diff), difference);
                            //Save the raw price
                            Utility.putDouble(editor, mContext.getString(R.string.pref_buds_raw), raw);

                            editor.apply();

                        } else if (defindex == 5002) {//Save extra info about the refined price

                            //Get the sharedpreferences
                            SharedPreferences.Editor editor = PreferenceManager
                                    .getDefaultSharedPreferences(mContext).edit();

                            //Store the price in a string so it can be displayed in the
                            //header in the latest changes page
                            double highPrice = high == null ? 0 : high;

                            Price refPrice = new Price(
                                    value, highPrice, raw, lastUpdate, difference, currency
                            );

                            String priceString = refPrice.getFormattedPrice(mContext);
                            editor.putString(mContext.getString(R.string.pref_metal_price), priceString);

                            //Save the difference
                            Utility.putDouble(editor, mContext.getString(R.string.pref_metal_diff), difference);

                            if (highPrice > value) {
                                //If the metal has a high price, save the average as raw.
                                Utility.putDouble(editor, mContext.getString(R.string.pref_metal_raw_usd), ((value + highPrice) / 2));
                            } else {
                                //save as raw price
                                Utility.putDouble(editor, mContext.getString(R.string.pref_metal_raw_usd), value);
                            }

                            editor.apply();
                        } else if (defindex == 5021) {//Save extra info about the key price

                            //Get the sharedpreferences
                            SharedPreferences.Editor editor = PreferenceManager
                                    .getDefaultSharedPreferences(mContext).edit();

                            //Store the price in a string so it can be displayed in the
                            //header in the latest changes page
                            double highPrice = high == null ? 0 : high;

                            Price keyPrice = new Price(
                                    value, highPrice, raw, lastUpdate, difference, currency
                            );

                            String priceString = keyPrice.getFormattedPrice(mContext);
                            editor.putString(mContext.getString(R.string.pref_key_price), priceString);

                            //Save the difference
                            Utility.putDouble(editor, mContext.getString(R.string.pref_key_diff), difference);
                            //Save the raw price
                            Utility.putDouble(editor, mContext.getString(R.string.pref_key_raw), raw);

                            editor.apply();
                        }
                    }

                    //Add the price to the CV vector
                    cVVector.add(buildContentValues(defindex, quality, tradable, craftable,
                            priceIndex, australium, currency, value, high, lastUpdate, difference));

                    //Parsed one item
                    publishProgress(1);
                }

                if (cVVector.size() > 0) {
                    ContentValues[] cvArray = new ContentValues[cVVector.size()];
                    cVVector.toArray(cvArray);
                    //Insert all the data into the database
                    int rowsInserted = mContext.getContentResolver()
                            .bulkInsert(PriceEntry.CONTENT_URI, cvArray);
                    if (Utility.isDebugging(mContext))
                        Log.v(LOG_TAG, "inserted " + rowsInserted + " rows");
                }
                parser.close();
                return true;
            }

            //success object
            if (parser.getCurrentName().equals(KEY_SUCCESS)) {
                parser.nextToken();
                if (parser.getIntValue() == 0) {
                    publishProgress(-2);
                    //Unsuccessful query, nothing to do

                    while (parser.nextToken() != JsonToken.END_OBJECT) {
                        if (parser.getCurrentName().equals(KEY_MESSAGE)) {
                            errorMessage = parser.getText();
                            if (Utility.isDebugging(mContext))
                                Log.e(LOG_TAG, errorMessage);
                        }
                    }
                    parser.close();
                    return false;
                }
            }

            //count object since with this type of parser we don't actually know how long the array is
            if (parser.getCurrentName().equals(KEY_COUNT)) {
                parser.nextToken();
                //Notify the task that the download finished and the processing begins
                publishProgress(0, parser.getIntValue());
            }
        }

        parser.close();
        return false;
    }


    /**
     * Convenient method for building content values which are to be inserted into the database
     *
     * @param defindex   defindex of the item
     * @param quality    quality of the item
     * @param tradable   tradability if the item
     * @param craftable  craftability of the item
     * @param priceIndex price index of the item
     * @param currency   currency of the price
     * @param value      price of the item
     * @param high       higher price of the item
     * @param update     time of the update
     * @param difference difference of the the new and old price
     * @return the ContentValues object containing the data
     */
    private ContentValues buildContentValues(int defindex, int quality, int tradable, int craftable, int priceIndex, int australium,
                                             String currency, double value, Double high, long update, double difference) {
        //Fix the defindex for pricing
        Item item = new Item(defindex, null, 0, false, false, false, 0, null);
        defindex = item.getFixedDefindex();

        //The DV that will contain all the data
        ContentValues itemValues = new ContentValues();

        //Put all the data into the content values
        itemValues.put(PriceEntry.COLUMN_DEFINDEX, defindex);
        itemValues.put(PriceEntry.COLUMN_ITEM_QUALITY, quality);
        itemValues.put(PriceEntry.COLUMN_ITEM_TRADABLE, tradable);
        itemValues.put(PriceEntry.COLUMN_ITEM_CRAFTABLE, craftable);
        itemValues.put(PriceEntry.COLUMN_PRICE_INDEX, priceIndex);
        itemValues.put(PriceEntry.COLUMN_AUSTRALIUM, australium);
        itemValues.put(PriceEntry.COLUMN_CURRENCY, currency);
        itemValues.put(PriceEntry.COLUMN_PRICE, value);
        itemValues.put(PriceEntry.COLUMN_PRICE_HIGH, high);
        itemValues.put(PriceEntry.COLUMN_LAST_UPDATE, update);
        itemValues.put(PriceEntry.COLUMN_DIFFERENCE, difference);
        itemValues.put(PriceEntry.COLUMN_WEAPON_WEAR, 0);

        return itemValues;
    }

    /**
     * Listener interface for listening for the end of the fetch.
     */
    public interface OnPriceListFetchListener {

        /**
         * Notify the listener, that the fetching has stopped.
         */
        void onPriceListFetchFinished();
    }
}