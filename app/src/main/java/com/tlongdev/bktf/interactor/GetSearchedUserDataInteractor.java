package com.tlongdev.bktf.interactor;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import com.tlongdev.bktf.BptfApplication;
import com.tlongdev.bktf.R;
import com.tlongdev.bktf.model.User;
import com.tlongdev.bktf.network.BackpackTfInterface;
import com.tlongdev.bktf.network.SteamUserInterface;
import com.tlongdev.bktf.network.model.bptf.BackpackTfPayload;
import com.tlongdev.bktf.network.model.bptf.BackpackTfPlayer;
import com.tlongdev.bktf.network.model.steam.UserSummariesPayload;
import com.tlongdev.bktf.network.model.steam.UserSummariesPlayer;
import com.tlongdev.bktf.network.model.steam.UserSummariesResponse;
import com.tlongdev.bktf.network.model.steam.VanityUrl;
import com.tlongdev.bktf.util.HttpUtil;
import com.tlongdev.bktf.util.Utility;

import java.io.IOException;

import javax.inject.Inject;

import retrofit2.Response;

/**
 * @author Long
 * @since 2016. 03. 19.
 */
public class GetSearchedUserDataInteractor extends AsyncTask<Void, Void, Integer> {

    private static final String TAG = GetSearchedUserDataInteractor.class.getSimpleName();

    @Inject
    BackpackTfInterface mBackpackTfInterface;

    @Inject
    SteamUserInterface mSteamUserInterface;

    @Inject
    Application mContext;

    //The error message that will be presented to the user, when an error occurs
    private String errorMessage;

    //The mCallback that will be notified when the fetching finishes
    private final Callback mCallback;

    private String mSteamId;

    private User mUser;

    /**
     * Constructor.
     */
    public GetSearchedUserDataInteractor(BptfApplication application, String steamId,
                                 Callback callback) {
        application.getInteractorComponent().inject(this);
        mCallback = callback;
        mSteamId = steamId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Integer doInBackground(Void... params) {
        if (mSteamId == null) {
            errorMessage = "no steamID provided";
            return -1;
        }

        try {
            //Check if the given steamid is a resolved 64bit steamId
            if (!Utility.isSteamId(mSteamId)) {
                Log.i(TAG, "Resolving steam id of " + mSteamId);

                //First we try to resolve the steamId if the provided one isn't a 64bit steamId
                Response<VanityUrl> response = mSteamUserInterface.resolveVanityUrl(
                        mContext.getString(R.string.api_key_steam_web), mSteamId).execute();
                if (response.body() != null) {
                    VanityUrl vanityUrl = response.body();

                    if (vanityUrl.getResponse().getSteamid() == null) {
                        errorMessage = vanityUrl.getResponse().getMessage();
                        return -1;
                    }
                    mSteamId = vanityUrl.getResponse().getSteamid();
                } else if (response.raw().code() >= 400) {
                    errorMessage = HttpUtil.buildErrorMessage(response);
                    return 1;
                }
            }

            Log.i(TAG, "Resolved id " + mSteamId);

            mUser = new User();

            //Save the resolved steamId
            mUser.setResolvedSteamId(mSteamId);

            Log.i(TAG, "Fetching data from steam api for id " + mSteamId);

            Response<UserSummariesPayload> steamResponse =
                    mSteamUserInterface.getUserSummaries(
                            mContext.getString(R.string.api_key_steam_web), mSteamId).execute();

            if (steamResponse.body() != null) {
                saveSteamData(steamResponse.body());
            } else if (steamResponse.raw().code() >= 400) {
                errorMessage = HttpUtil.buildErrorMessage(steamResponse);
                return -1;
            }

            publishProgress();

            Log.i(TAG, "Fetching data from backpack.tf for id " + mSteamId);

            Response<BackpackTfPayload> bptfResponse = mBackpackTfInterface.getUserData(mSteamId).execute();

            if (bptfResponse.body() != null) {
                saveUserData(bptfResponse.body());
            } else if (bptfResponse.raw().code() >= 400) {
                errorMessage = HttpUtil.buildErrorMessage(bptfResponse);
                return -1;
            }

            return 0;
        } catch (IOException e) {
            //There was a network error
            //There was a network error
            errorMessage = e.getMessage();
            Log.e(TAG, "network error", e);
            return -1;
        }
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        if (mCallback != null) {
            mCallback.onSteamInfo(mUser);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPostExecute(Integer integer) {
        //Finished fetching the backpack
        Log.i(TAG, "Finished loading user data for " + mSteamId);
        if (mCallback != null) {
            if (integer >= 0) {
                //notify the mCallback, that the fetching has finished.
                mCallback.onUserInfoFinished(mUser);
            } else {
                mCallback.onUserInfoFailed(errorMessage);
            }
        }
    }

    private void saveSteamData(UserSummariesPayload payload) {
        if (payload.getResponse() != null) {
            UserSummariesResponse response = payload.getResponse();
            if (response.getPlayers() != null && response.getPlayers().size() > 0) {
                UserSummariesPlayer player = response.getPlayers().get(0);
                if (player != null) {
                    mUser.setName(player.getPersonaName());
                    mUser.setLastOnline(player.getLastLogoff());
                    mUser.setState(player.getPersonaState());
                    mUser.setProfileCreated(player.getTimeCreated());
                    mUser.setAvatarUrl(player.getAvatarFull());

                    if (player.getGameId() != 0) {
                        mUser.setState(7);
                    }
                }
            }
        }
    }

    private void saveUserData(BackpackTfPayload payload) {
        if (payload.getResponse() == null) {
            return;
        }

        if (payload.getResponse().getSuccess() == 0) {
            //The api query was unsuccessful, nothing to do.
            return;
        }

        if (payload.getResponse().getPlayers() == null) {
            return;
        }

        BackpackTfPlayer player = payload.getResponse().getPlayers().get(mSteamId);
        if (player == null) {
            return;
        }

        if (player.getSuccess() == 1) {
            if (player.getBackpackValue() != null) {
                mUser.setBackpackValue(player.getBackpackValue().get440());
            }

            mUser.setName(player.getName());
            mUser.setReputation(player.getBackpackTfReputation());
            mUser.setInGroup(player.getBackpackTfGroup());
            mUser.setBanned(player.getBackpackTfBanned() != null);
            mUser.setScammer(player.getSteamrepScammer());
            mUser.setEconomyBanned(player.getBanCommunity());
            mUser.setCommunityBanned(player.getBanCommunity());
            mUser.setVacBanned(player.getBanVac());

            if (player.getBackpackTfTrust() != null) {
                mUser.setTrustNegative(player.getBackpackTfTrust().getAgainst());
                mUser.setTrustPositive(player.getBackpackTfTrust().getFor());
            }
        }
    }

    /**
     * Listener interface.
     */
    public interface Callback {

        /**
         * Callback which will be called when both tasks finish.
         *
         * @param user the user
         */
        void onUserInfoFinished(User user);

        void onSteamInfo(User user);

        void onUserInfoFailed(String errorMessage);
    }
}
