package com.example.face_tracker.util;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class PlayServicesUtil {

    private static final String TAG = PlayServicesUtil.class.getSimpleName();

    public static boolean isPlayServicesAvailable(@NonNull Activity activity, final int requestCode) {
        //noinspection ConstantConditions
        if (activity == null) {
            return false;
        }

        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(activity);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                Log.w(TAG, "GooglePlayServices resolvable error occurred: " + apiAvailability.getErrorString(resultCode));
                apiAvailability.getErrorDialog(activity, resultCode, requestCode).show();
            } else {
                Log.e(TAG, "GooglePlayServices not supported");

                // finish activity
                activity.finish();
            }

            return false;
        }

        // All good
        return true;
    }
}
