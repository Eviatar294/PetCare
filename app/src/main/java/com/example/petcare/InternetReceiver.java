package com.example.petcare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

public class InternetReceiver extends BroadcastReceiver {
    private static final String TAG = "InternetReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!isNetworkAvailable(context)) {
            Log.d(TAG, "No internet connection detected. Returning to MainActivity.");
            Intent mainIntent = new Intent(context, NoInternetActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(mainIntent);
        }
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities networkCapabilities =
                connectivityManager.getNetworkCapabilities(network);
        return networkCapabilities != null && (
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        );
    }
}
