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
            Log.d(TAG, "Starting to fetch user data for userId: " + userId);
            FirebaseFunctions.getUserClassFromFirebase(userId, new FirebaseFunctions.GetUserCallback() {
                @Override
                public void onSuccess(User user) {
                    myUser = user;
                    String petId = user.getPetId();
                    Log.d(TAG, "Got user data, fetching pet data for petId: " + petId);
                    
                    // Only fetch pet data if petId exists and is not empty
                    if (petId != null && !petId.isEmpty()) {
                        FirebaseFunctions.getPetClassFromFirebase(petId, new FirebaseFunctions.GetPetCallback() {
                            @Override
                            public void onSuccess(Pet pet) {
                                myPet = pet;
                                Log.d(TAG, "Got pet data, imageString exists: " + (pet.getImageString() != null && !pet.getImageString().isEmpty()));
                                
                                // Save the pet image to internal storage if available
                                if (pet.getImageString() != null && !pet.getImageString().isEmpty()) {
                                    Log.d(TAG, "Found pet image in Firebase, attempting to save to internal storage");
                                    boolean saved = savePetImageToInternalStorage(pet);
                                    Log.d(TAG, "Save to internal storage result: " + saved);
                                } else {
                                    Log.d(TAG, "No pet image found in Firebase");
                                }
                                
                                // Clear the image string after saving to avoid memory issues
                                myPet.setImageString("");
                                
                                FirebaseFunctions.fetchUsersWithSamePetId(petId, new FirebaseFunctions.FetchUsersCallback() {
                                    @Override
                                    public void onSuccess(ArrayList<User> userList) {
                                        myUserList = userList;
                                        Log.d(TAG, "Users found: " + userList.size());
                                        navigateToNextActivity();
                                    }

                                    @Override
                                    public void onFailure(String errorMessage) {
                                        Log.e(TAG, "Failed to fetch users: " + errorMessage);
                                        navigateToNextActivity();
                                    }
                                });
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Log.e(TAG, "Failed to fetch pet: " + errorMessage);
                                navigateToNextActivity();
                            }
                        });
                    } else {
                        Log.d(TAG, "User has no pet assigned yet");
                        navigateToNextActivity();
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Failed to fetch user: " + errorMessage);
                    myUser = null;
                    navigateToNextActivity();
                }
            });
        } else {
            Log.d(TAG, "No userId found in internal storage");
            navigateToNextActivity();
        }
    }

    /**
     * Saves the pet image from Firebase to internal storage.
     * The image is saved as "pet_image.png" in the app's files directory.
     * @return true if the image was saved successfully, false otherwise
     */
    private boolean savePetImageToInternalStorage(Pet pet) {
        if (pet == null || pet.getImageString() == null || pet.getImageString().isEmpty()) {
            Log.e(TAG, "Cannot save image: pet is null or has no image string");
            return false;
        }

        java.io.FileOutputStream fos = null;
        try {
            // Decode the Base64 string into a Bitmap
            byte[] decodedBytes = android.util.Base64.decode(pet.getImageString(), android.util.Base64.DEFAULT);
            Log.d(TAG, "Decoded Base64 string, bytes length: " + decodedBytes.length);
            
            android.graphics.Bitmap petBitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            if (petBitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from byte array");
                return false;
            }
            Log.d(TAG, "Successfully created bitmap, size: " + petBitmap.getWidth() + "x" + petBitmap.getHeight());
            
            // Save the bitmap to internal storage as "pet_image.png"
            java.io.File file = new java.io.File(getFilesDir(), "pet_image.png");
            fos = new java.io.FileOutputStream(file);
            boolean compressed = petBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos);
            if (!compressed) {
                Log.e(TAG, "Failed to compress bitmap to PNG");
                return false;
            }
            
            fos.flush();
            Log.d(TAG, "Successfully saved image to: " + file.getAbsolutePath());
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving pet image: " + e.getMessage(), e);
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (java.io.IOException e) {
                    Log.e(TAG, "Error closing output stream: " + e.getMessage(), e);
                }
            }
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
            String petId = myUser.getPetId();
            if (petId != null && !petId.isEmpty()) {
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
