// Copyright 2016 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.unity;

import android.app.Activity;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.unity3d.ads.BuildConfig;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
import com.unity3d.ads.metadata.MediationMetaData;

import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * The {@link UnitySingleton} class is used to load {@link UnityAds}, handle multiple
 * {@link UnityAdapter} instances and mediate their callbacks.
 */
public final class UnitySingleton {

    /**
     * A list of adapter listeners with their respective placement IDs to prevent duplicate
     * requests.
     */
    private HashMap<String, WeakReference<UnityAdapterDelegate>> mPlacementsInUse;

    /**
     * A weak reference to the {@link UnityAdapterDelegate} of the {@link UnityAdapter} that is
     * currently displaying an ad.
     */
    private WeakReference<UnityAdapterDelegate> mAdShowingAdapterDelegate;
    private WeakReference<UnityAdapterDelegate> mAdLoadingAdapterDelegate;
    /**
     * The only instance of
     * {@link com.google.ads.mediation.unity.UnitySingleton.UnitySingletonListener}.
     */
    private UnitySingletonListener unitySingletonListenerInstance;

    /**
     * The only instance of
     * {@link com.google.ads.mediation.unity.UnitySingleton}.
     *
    private static UnitySingleton unitySingletonInstance;
    */
    /**
     * Used by Unity Ads to track failures in the mediation lifecycle
     */
    private int impressionOrdinal;
    private int missedImpressionOrdinal;

    /**
     * This method will return a
     * {@link com.google.ads.mediation.unity.UnitySingleton} instance.
     *
     * @return the {@link #unitySingletonInstance}.
     *
    public static UnitySingleton getInstance() {
        if (unitySingletonInstance == null) {
            unitySingletonInstance = new UnitySingleton();
        }
        return unitySingletonInstance;
    }
    */

    public UnitySingleton() {
        mPlacementsInUse = new HashMap<>();
    }

    /**
     * This method will return the
     * {@link com.google.ads.mediation.unity.UnitySingleton.UnitySingletonListener} instance.
     *
     * @return the {@link #unitySingletonListenerInstance}.
     *
    private UnitySingletonListener getUnitySingletonListenerInstance() {
        if (unitySingletonListenerInstance == null) {
            unitySingletonListenerInstance = new UnitySingletonListener();
        }
        return unitySingletonListenerInstance;
    }
    */

    /**
     * This method will initialize {@link UnityAds}.
     *
     * @param activity    The Activity context.
     * @param gameId      Unity Ads Game ID.
     * @return {@code true} if the {@link UnityAds} has initialized successfully, {@code false}
     * otherwise.
     */
    public boolean initializeUnityAds(Activity activity, String gameId) {
        // Check if the current device is supported by Unity Ads before initializing.
        if (!UnityAds.isSupported()) {
            Log.w(UnityAdapter.TAG, "The current device is not supported by Unity Ads.");
            return false;
        }

        if (UnityAds.isInitialized()) {
            // Unity Ads is already initialized.
            return true;
        }

        // Set mediation meta data before initializing.
        MediationMetaData mediationMetaData = new MediationMetaData(activity);
        mediationMetaData.setName("AdMob");
        mediationMetaData.setVersion(BuildConfig.VERSION_NAME);
        mediationMetaData.set("adapter_version", "3.3.0");
        mediationMetaData.commit();

        // old method
        // UnitySingletonListener listener = unitySingletonInstance.getUnitySingletonListenerInstance();
        //UnityAds.initialize(activity, gameId, listener, false, true);

        // new method
        UnityAds.initialize(activity, gameId, false, true, new IUnityAdsInitializationListener() {

                    @Override
                    public void onInitializationComplete() {
                        Log.d(UnityAdapter.TAG, "Unity Ads successfully initialized");
                    }

                    @Override
                    public void onInitializationFailed(UnityAds.UnityAdsInitializationError unityAdsInitializationError, String s) {
                        Log.e(UnityAdapter.TAG, "Unity Ads initialization failed: [" + unityAdsInitializationError + "] " + s);
                    }
                });


        return true;
    }

    /**
     * This method will load Unity ads for a given Placement ID and send the ad loaded event if the
     * ads have already loaded.
     *
     * @param delegate Used to forward Unity Ads events to the adapter.
     */
    protected void loadAd(UnityAdapterDelegate delegate) {

        // Calling load before UnityAds.initialize() will cause the placement to load on init

        // old method
        // UnityAds.load(delegate.getPlacementId());

        mAdLoadingAdapterDelegate = new WeakReference<>(delegate);
        Log.d(UnityAdapter.TAG, "Trying to load ad");

        // new method
        UnityAds.load(delegate.getPlacementId(), new IUnityAdsLoadListener() {
            @Override
            public void onUnityAdsAdLoaded(String s) {

                Log.d(UnityAdapter.TAG, "Ad successfully loaded " + s);
            }

            @Override
            public void onUnityAdsFailedToLoad(String s) {
                Log.e(UnityAdapter.TAG, "Ad load failure " +s);
            }
        });

        // TODO
        if (UnityAds.isInitialized()) {
            //If ads are currently being loaded, wait for the callbacks from
            // unitySingletonListenerInstance.
            // Check if an AdMob Ad request has already loaded or is in progress of requesting
            // an Ad from Unity Ads for a single placement, and fail if there's any.
            if (mPlacementsInUse.containsKey(delegate.getPlacementId()) &&
                    mPlacementsInUse.get(delegate.getPlacementId()).get() != null) {
                Log.e(UnityMediationAdapter.TAG,
                        "An ad is already loading for placement ID: " + delegate.getPlacementId());
                delegate.onUnityAdsError(UnityAds.UnityAdsError.INTERNAL_ERROR,
                        delegate.getPlacementId());
                return;
            }

            mPlacementsInUse.put(delegate.getPlacementId(), new WeakReference<>(delegate));
            if (UnityAds.isReady(delegate.getPlacementId())) {
                delegate.onUnityAdsReady(delegate.getPlacementId());
            }
        }
    }

    /**
     * This method will show an Unity Ad.
     *
     * @param delegate Used to forward Unity Ads events to the adapter.
     * @param activity An Android {@link Activity} required to show an ad.
     */
    protected void showAd(UnityAdapterDelegate delegate, Activity activity) {
        mAdShowingAdapterDelegate = new WeakReference<>(delegate);

        Log.d(UnityMediationAdapter.TAG, "trying to show ad");
        // Every call to UnityAds#show will result in an onUnityAdsFinish callback (even when
        // Unity Ads fails to shown an ad).

        if(UnityAds.isReady(delegate.getPlacementId())) {
            // Notify UnityAds that the adapter made a successful show request
            MediationMetaData metadata = new MediationMetaData(activity);
            metadata.setOrdinal(++impressionOrdinal);
            metadata.commit();

            Log.d(UnityMediationAdapter.TAG, "calling show ad");
            UnityAds.show(activity, delegate.getPlacementId());
        } else {
            // Notify UnityAds that the adapter failed to show
            MediationMetaData metadata = new MediationMetaData(activity);
            metadata.setMissedImpressionOrdinal(++missedImpressionOrdinal);
            metadata.commit();
        }
    }

    /**
     * The {@link com.google.ads.mediation.unity.UnitySingleton.UnitySingletonListener} is used
     * to forward events from Unity Ads SDK to {@link UnityAdapter} based on the delegates added
     * to {@link #mPlacementsInUse} and which adapter is currently showing an ad.
     */
    private final class UnitySingletonListener
            implements IUnityAdsExtendedListener {

        /**
         * {@link IUnityAdsExtendedListener} implementation
         */
        @Override
        public void onUnityAdsReady(String placementId) {
            // Unity Ads is ready to show ads for the given placementId. Send ready callback to the
            // appropriate delegate.
            if (mPlacementsInUse.containsKey(placementId) &&
                    mPlacementsInUse.get(placementId).get() != null) {
                mPlacementsInUse.get(placementId).get().onUnityAdsReady(placementId);
            }

//            if (mAdLoadingAdapterDelegate != null)
//            {
//                UnityAdapterDelegate delegate = mAdLoadingAdapterDelegate.get();
//                if (delegate != null) {
//                    delegate.onUnityAdsStart(placementId);
//                }
//            }
        }

        @Override
        public void onUnityAdsStart(String placementId) {
            // Unity Ads video ad started. Send video started event to currently showing
            // adapter's delegate.
            if (mAdShowingAdapterDelegate != null) {
                UnityAdapterDelegate delegate = mAdShowingAdapterDelegate.get();
                if (delegate != null) {
                    delegate.onUnityAdsStart(placementId);
                }
            }
        }

        @Override
        public void onUnityAdsClick(String placementId) {
            // An Unity Ads ad has been clicked. Send ad clicked event to currently showing
            // adapter's delegate.
            if (mAdShowingAdapterDelegate != null) {
                UnityAdapterDelegate delegate = mAdShowingAdapterDelegate.get();
                if (delegate != null) {
                    delegate.onUnityAdsClick(placementId);
                }
            }
        }

        @Override
        public void onUnityAdsPlacementStateChanged(String placementId,
                                                    UnityAds.PlacementState oldState,
                                                    UnityAds.PlacementState newState) {
            // The onUnityAdsReady and onUnityAdsError callback methods are used to forward Unity
            // Ads SDK states to the adapters. No need to forward this callback to the adapters.
        }

        @Override
        public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
            // An Unity Ads ad has been closed. Forward the finish event to the currently showing
            // adapter's delegate.
            if (mAdShowingAdapterDelegate != null) {
                UnityAdapterDelegate delegate = mAdShowingAdapterDelegate.get();
                if (delegate != null) {
                    delegate.onUnityAdsFinish(placementId, finishState);
                    mPlacementsInUse.remove(placementId);
                }
            }
        }

        @Override
        public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String placementId) {
            // An error occurred with Unity Ads. Send error event to the appropriate delegate.
            if (mPlacementsInUse.containsKey(placementId) &&
                    mPlacementsInUse.get(placementId).get() != null) {
                mPlacementsInUse.get(placementId).get()
                        .onUnityAdsError(unityAdsError, placementId);
                mPlacementsInUse.remove(placementId);
            }

            // TODO
//            if (mAdLoadingAdapterDelegate.get().getPlacementId() == placementId)
//            {
//                UnityAdapterDelegate delegate = mAdLoadingAdapterDelegate.get();
//                if (delegate != null) {
//                    delegate.onUnityAdsError(unityAdsError, placementId);
//                }
//            }
//            if (mAdShowingAdapterDelegate.get().getPlacementId() == placementId)
//            {
//                UnityAdapterDelegate delegate = mAdShowingAdapterDelegate.get();
//                if (delegate != null) {
//                    delegate.onUnityAdsError(unityAdsError, placementId);
//                }
//            }
        }
    }
}
