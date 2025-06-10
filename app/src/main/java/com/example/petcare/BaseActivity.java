package com.example.petcare;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class BaseActivity extends AppCompatActivity {

    // InternetReceiver handling remains as before
    private InternetReceiver internetReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        internetReceiver = new InternetReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(internetReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(internetReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_user_guide) {
            Intent intent = new Intent(this, UserGuideActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_credit) {
            Intent intent = new Intent(this, CreditActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_logout) {
            performLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void performLogout() {
        try {
            // Delete user ID file
            deleteUserIdFromInternalStorage();
            
            // Delete pet image file if it exists
            File petImageFile = new File(getFilesDir(), "pet_image.png");
            if (petImageFile.exists()) {
                petImageFile.delete();
            }

            // Cancel all notifications
            NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancelAll();
            }
            TaskNotificationScheduler.cancelAllAlarms(this);
            
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            
            // Navigate to sign in screen
            Intent intent = new Intent(this, MainSignIn.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error during logout", Toast.LENGTH_SHORT).show();
        }
    }

    protected void deleteUserIdFromInternalStorage() {
        try {
            File directory = getFilesDir();
            File textFile = new File(directory, "user_id.txt");
            if (textFile.exists()) {
                textFile.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
