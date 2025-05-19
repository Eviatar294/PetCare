package com.example.petcare;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends BaseActivity {

    Context context;
    String userId = null;
    User myUser = null;
    Pet myPet = null;
    ArrayList<User> myUserList = new ArrayList<>();

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initComponents();

        // Check for notification permission.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission not granted. Requesting permission...");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            } else {
                Log.d(TAG, "Notification permission already granted.");
                requestExactAlarmPermission();
                getAllDataFromUserId(userId);
            }
        } else {
            // For devices older than Android 13, proceed immediately.
            requestExactAlarmPermission();
            getAllDataFromUserId(userId);
        }
    }

    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "Exact Alarm permission is NOT granted! Asking user...");
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            } else {
                Log.d(TAG, "Exact Alarm permission is already granted.");
            }
        }
    }

    // Notification permission callback. After the user responds, proceed with data fetching.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: Notification permission response received.");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted.");
            } else {
                Log.e(TAG, "Notification permission denied.");
            }
            requestExactAlarmPermission();
            getAllDataFromUserId(userId);
        }
    }

    private void initComponents() {
        context = MainActivity.this;
        userId = getUserIdFromInternalStorage();
    }

    // Data fetching is performed after the permission response.
    private void getAllDataFromUserId(String userId) {
        if (userId != null) {
            FirebaseFunctions.getUserClassFromFirebase(userId, new FirebaseFunctions.GetUserCallback() {
                @Override
                public void onSuccess(User user) {
                    myUser = user;
                    String petId = user.getPetId();
                    FirebaseFunctions.getPetClassFromFirebase(petId, new FirebaseFunctions.GetPetCallback() {
                        @Override
                        public void onSuccess(Pet pet) {
                            myPet = pet;
                            myPet.setImageString("");
                            FirebaseFunctions.fetchUsersWithSamePetId(petId, new FirebaseFunctions.FetchUsersCallback() {
                                @Override
                                public void onSuccess(ArrayList<User> userList) {
                                    myUserList = userList;
                                    Log.d(TAG, "Users found: " + userList.size());
                                    navigateToNextActivity();
                                }

                                @Override
                                public void onFailure(String errorMessage) { }
                            });
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            navigateToNextActivity();
                        }
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    myUser = null;
                    navigateToNextActivity();
                }
            });
        } else {
            navigateToNextActivity();
        }
    }

    private void navigateToSignIn() {
        Intent goToRegister = new Intent(context, MainSignIn.class);
        startActivity(goToRegister);
        finish();
    }

    private String getUserIdFromInternalStorage() {
        File file = new File(getFilesDir(), "user_id.txt");
        if (!file.exists()) return null;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            return br.readLine(); // Read only the first line (user ID)
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void navigateToNextActivity() {
        if (myUser != null) {
            if (!myUser.getPetId().isEmpty()) {
                Intent goToMainHome = new Intent(context, MainHomeUser.class);
                goToMainHome.putExtra("user", myUser);
                goToMainHome.putExtra("pet", myPet);
                goToMainHome.putExtra("usersList", myUserList);
                startActivity(goToMainHome);
            } else {
                Intent goToNewPet = new Intent(context, NewPet.class);
                goToNewPet.putExtra("user", myUser);
                startActivity(goToNewPet);
            }
        } else {
            navigateToSignIn();
        }
        finish();
    }
}
