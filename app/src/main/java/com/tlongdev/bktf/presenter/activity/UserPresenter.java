    /**
 * Copyright 2016 Long Tran
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tlongdev.bktf.presenter.activity;

import android.os.AsyncTask;
import android.widget.Toast;

import com.tlongdev.bktf.BptfApplication;
import com.tlongdev.bktf.interactor.GetSearchedUserDataInteractor;
import com.tlongdev.bktf.model.User;
import com.tlongdev.bktf.presenter.Presenter;
import com.tlongdev.bktf.ui.view.activity.UserView;

/**
 * @author Long
 * @since 2016. 03. 19.
 */
public class UserPresenter implements Presenter<UserView>, GetSearchedUserDataInteractor.Callback {

    private UserView mView;
    private final BptfApplication mApplication;

    private boolean mLoaded = false;
    private String mSteamId = "";

    public UserPresenter(BptfApplication application) {
        application.getPresenterComponent().inject(this);
        mApplication = application;
    }

    @Override
    public void attachView(UserView view) {
        mView = view;
    }

    @Override
    public void detachView() {
        mView = null;
    }

    public void loadData(String steamId) {
        if (!steamId.equals(mSteamId)) {
            mLoaded = false;
            mSteamId = steamId;
        }

        if (!mLoaded) {
            GetSearchedUserDataInteractor interactor = new GetSearchedUserDataInteractor(
                    mApplication, steamId, this
            );
            interactor.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            mLoaded = true;
        } else {
            mView.hideLoadingAnimation();
        }
    }

    @Override
    public void onUserInfoFinished(User user) {
        if (mView != null) {
            mView.showData(user);
        }
    }

    @Override
    public void onUserInfoFailed(String errorMessage) {
        mLoaded = false;
        if (mView != null) {
            mView.showError();
            mView.showToast(errorMessage, Toast.LENGTH_SHORT);
        }
    }
}