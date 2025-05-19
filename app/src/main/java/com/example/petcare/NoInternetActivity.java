package com.example.petcare;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class NoInternetActivity extends AppCompatActivity {

    private Button bRetry;
    private TextView tvConnectionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_internet);

        initComponents();
        checkConnection();
    }

    private void checkConnection() {
        boolean connected = isNetworkAvailable();

        if (connected) {
            startActivity(new Intent(NoInternetActivity.this, MainActivity.class));
            finish();
        } else {
            Toast.makeText(NoInternetActivity.this, "Internet connection required", Toast.LENGTH_SHORT).show();
            bRetry.setVisibility(View.VISIBLE);
            tvConnectionStatus.setVisibility(View.VISIBLE);
            bRetry.setOnClickListener(v -> {
                boolean retryConnected = isNetworkAvailable();
                if (retryConnected) {
                    Toast.makeText(NoInternetActivity.this, "Internet connection restored!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(NoInternetActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(NoInternetActivity.this, "Still no internet connection", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void initComponents() {
        bRetry = findViewById(R.id.bRetry);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatusText);
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
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
