package com.example.petcare;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingFragment extends Fragment {

    private View view;
    private Context context;
    private ImageButton ibPetImagePreview;
    private Button bChangeNotificationTime;
    private TextView tvBestOwner, tvOwnerEmail, tvIsLeader, tvPetName, tvPetType, tvNotificationTime;
    private TextView tvUserPassword, tvPetPassword;
    private ImageButton ibToggleUserPassword, ibTogglePetPassword;
    private LinearLayout layoutAdminSection;
    private Uri imageUri = null;
    private String imageString;
    private User user;
    private Pet pet;
    private boolean isUserPasswordVisible = false;
    private boolean isPetPasswordVisible = false;

    // Shared ImagePickerHelper instance
    private ImagePickerHelper imagePickerHelper;

    public interface ImageDeletionCallback {
        void onImageDeleted();
    }

    public ImageDeletionCallback getImageDeletionCallback() {
        return () -> {
            // Update UI
            updateImagePreview();
            // Show success message
            if (getContext() != null) {
                Toast.makeText(getContext(), "Image deleted successfully", Toast.LENGTH_SHORT).show();
            }
        };
    }

    public SettingFragment() {
        // Required empty constructor.
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_setting, container, false);
        initComponents();

        // Initialize ImagePickerHelper
        imagePickerHelper = new ImagePickerHelper(this, uri -> {
            imageUri = uri;
            ibPetImagePreview.setImageURI(uri);
            uploadImageToFirebase(uri);
        });

        ibPetImagePreview.setOnClickListener(v -> {
            File petImageFile = new File(getContext().getFilesDir(), "pet_image.png");
            if (!petImageFile.exists()) {
                imagePickerHelper.pickImage();
            } else {
                DialogImage dialogImage = new DialogImage();
                Bundle args = new Bundle();
                args.putSerializable("user", user);
                args.putSerializable("pet", pet);
                dialogImage.setArguments(args);
                dialogImage.show(getParentFragmentManager(), "dialog_image");
            }
        });

        bChangeNotificationTime.setOnClickListener(v -> showTimePickerDialog());

        // Set up password toggles
        if (ibToggleUserPassword != null) {
            ibToggleUserPassword.setOnClickListener(v -> toggleUserPasswordVisibility());
        }
        
        if (ibTogglePetPassword != null) {
            ibTogglePetPassword.setOnClickListener(v -> togglePetPasswordVisibility());
        }

        return view;
    }

    private void initComponents() {
        context = getActivity();
        bChangeNotificationTime = view.findViewById(R.id.bChangeNotificationTime);
        ibPetImagePreview = view.findViewById(R.id.ibPetImagePreview);
        tvBestOwner = view.findViewById(R.id.tvBestOwner);
        tvOwnerEmail = view.findViewById(R.id.tvOwnerEmail);
        tvIsLeader = view.findViewById(R.id.tvIsLeader);
        tvPetName = view.findViewById(R.id.tvPetName);
        tvPetType = view.findViewById(R.id.tvPetType);
        tvNotificationTime = view.findViewById(R.id.tvNotificationTime);
        
        // Initialize password section components
        tvUserPassword = view.findViewById(R.id.tvUserPassword);
        ibToggleUserPassword = view.findViewById(R.id.ibToggleUserPassword);
        
        // Initialize admin section components
        layoutAdminSection = view.findViewById(R.id.layoutAdminSection);
        tvPetPassword = view.findViewById(R.id.tvPetPassword);
        ibTogglePetPassword = view.findViewById(R.id.ibTogglePetPassword);

        Button btnDisconnectPet = view.findViewById(R.id.btnDisconnectPet);
        btnDisconnectPet.setOnClickListener(v -> showDisconnectConfirmation());

        Bundle bundle = getArguments();
        if (bundle != null) {
            user = (User) bundle.getSerializable("user");
            pet = (Pet) bundle.getSerializable("pet");
        }

        if (user != null) {
            tvBestOwner.setText(user.getName() + " is the best owner ever!");
            tvOwnerEmail.setText("Email: " + user.getEmail());
            tvUserPassword.setText("Password: ********");
            
            if (user.getPetPassword() != null && !user.getPetPassword().isEmpty()) {
                tvIsLeader.setText("Leader Email: " + user.getEmail());
                // Show admin section if user has pet password
                layoutAdminSection.setVisibility(View.VISIBLE);
                tvPetPassword.setText("Pet Password: ********");
            } else {
                fetchLeaderEmail();
                // Hide admin section for non-admin users
                layoutAdminSection.setVisibility(View.GONE);
            }
            tvNotificationTime.setText("Notification Time: " + user.getNotificationTime());
        }
        if (pet != null) {
            tvPetName.setText("Pet Name: " + pet.getName());
            tvPetType.setText("Pet Type: " + pet.getPetType());
        }

        updateImagePreview();
    }

    private void showTimePickerDialog() {
        // Parse current notification time:
        String[] parts = user.getNotificationTime().split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        new TimePickerDialog(getContext(), (tp, h, m) -> {
            String formatted = String.format("%02d:%02d", h, m);
            user.setNotificationTime(formatted);
            tvNotificationTime.setText("Notification Time: " + formatted);

            // Persist new time to Firebase and immediately reschedule the alarm for today:
            FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(user.getUserId())
                    .child("notificationTime")
                    .setValue(formatted)
                    .addOnSuccessListener(a -> {
                        TaskNotificationScheduler.scheduleTaskReminder(context, user);
                        Toast.makeText(context, "Notification time updated", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(context, "Failed to update time: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        }, hour, minute, true).show();
    }

    private void fetchLeaderEmail() {
        if (user != null && user.getPetId() != null) {
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
            usersRef.orderByChild("petId").equalTo(user.getPetId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            String leaderEmail = "Unknown";
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                User u = ds.getValue(User.class);
                                if (u != null && u.getPetPassword() != null && !u.getPetPassword().isEmpty()) {
                                    leaderEmail = u.getEmail();
                                    break;
                                }
                            }
                            if (isAdded() && getContext() != null) {
                                tvIsLeader.setText("Leader Email: " + leaderEmail);
                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError error) {
                            if (isAdded() && getContext() != null) {
                                tvIsLeader.setText("Leader Email: Error");
                            }
                        }
                    });
        }
    }

    /**
     * Uploads the image (converted to a Base64 string) to Firebase,
     * saves the image locally, and updates the preview.
     */
    public void uploadImageToFirebase(Uri imageUri) {
        if (imageUri == null || getContext() == null) return;
        try {
            imageString = convertUriToBase64(imageUri);
            if (imageString == null || imageString.isEmpty()) {
                return;
            }

            // Save to internal storage
            saveImageFromUriToInternalStorage(imageUri);

            // Update pet object
            pet.setImageString(imageString);
            
            // Update Firebase
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference petsRef = database.getReference("Pets");
            if (pet.getPetId() == null || pet.getPetId().isEmpty()) {
                return;
            }
            
            petsRef.child(pet.getPetId()).child("imageString").setValue(imageString)
                    .addOnSuccessListener(aVoid -> {
                        updateImagePreview();
                        Toast.makeText(context, "Image updated successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        // If Firebase upload fails, clean up local storage
                        File petImageFile = new File(getContext().getFilesDir(), "pet_image.png");
                        if (petImageFile.exists()) {
                            petImageFile.delete();
                        }
                        pet.setImageString(null);
                        Toast.makeText(context, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        updateImagePreview();
                    });
        } catch (IOException e) {
            Toast.makeText(context, "Error converting image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImageFromUriToInternalStorage(Uri imageUri) {
        if (getContext() == null) return;
        FileOutputStream fos = null;
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(getContext().getContentResolver().openInputStream(imageUri));
            if (bitmap == null) {
                Toast.makeText(getContext(), "Error: Could not process image.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            File file = new File(getContext().getFilesDir(), "pet_image.png");
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error saving image locally: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String convertUriToBase64(Uri imageUri) throws IOException {
        if (getContext() == null) throw new IOException("Context is null");
        ContentResolver contentResolver = getContext().getContentResolver();
        InputStream inputStream = contentResolver.openInputStream(imageUri);
        if (inputStream == null) {
            throw new IOException("Unable to open input stream from URI: " + imageUri);
        }
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        if (bitmap == null) {
            throw new IOException("Failed to decode bitmap from URI: " + imageUri);
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    public void updateImagePreview() {
        if (!isAdded() || getContext() == null) {
            Log.e("SettingFragment", "Fragment not attached, cannot update image preview.");
            return;
        }

        File petImageFile = new File(getContext().getFilesDir(), "pet_image.png");
        Log.d("SettingFragment", "Checking for pet image at: " + petImageFile.getAbsolutePath());
        Log.d("SettingFragment", "File exists: " + petImageFile.exists());

        if (petImageFile.exists()) {
            // Clear any existing cache first
            Glide.get(getContext()).clearMemory();
            
            // Load the image with no caching
            Glide.with(getContext())
                    .load(petImageFile)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .circleCrop()
                    .into(ibPetImagePreview);
        } else {
            Log.d("SettingFragment", "Loading default image because file doesn't exist");
            loadDefaultImage();
        }
    }

    public void loadDefaultImage() {
        if (!isAdded() || getContext() == null) {
            Log.e("SettingFragment", "Fragment not attached, cannot load default image.");
            return;
        }

        Log.d("SettingFragment", "Loading default image.");
        
        // Clear any current image first
        ibPetImagePreview.setImageDrawable(null);
        
        // Clear memory cache only (disk cache requires background thread)
        Glide.get(getContext()).clearMemory();
        
        // Load default image with cache disabled and force load
        Glide.with(getContext())
                .load(R.drawable.cartoon_black_cat_with_question_mark_above_head_vector)
                .circleCrop()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .dontAnimate()
                .override(Target.SIZE_ORIGINAL)  // Use original image size
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        ibPetImagePreview.setImageDrawable(resource);
                        ibPetImagePreview.invalidate();
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        ibPetImagePreview.setImageDrawable(placeholder);
                    }
                });
    }

    private void toggleUserPasswordVisibility() {
        if (user != null) {
            isUserPasswordVisible = !isUserPasswordVisible;
            if (isUserPasswordVisible) {
                tvUserPassword.setText("Password: " + user.getPassword());
                ibToggleUserPassword.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            } else {
                tvUserPassword.setText("Password: ********");
                ibToggleUserPassword.setImageResource(android.R.drawable.ic_menu_view);
            }
        }
    }

    private void togglePetPasswordVisibility() {
        if (user != null && user.getPetPassword() != null && !user.getPetPassword().isEmpty()) {
            isPetPasswordVisible = !isPetPasswordVisible;
            if (isPetPasswordVisible) {
                tvPetPassword.setText("Pet Password: " + user.getPetPassword());
                ibTogglePetPassword.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            } else {
                tvPetPassword.setText("Pet Password: ********");
                ibTogglePetPassword.setImageResource(android.R.drawable.ic_menu_view);
            }
        }
    }

    public void deleteImage() {
        if (!isAdded() || getContext() == null || pet == null) {
            return;
        }

        // Delete local file first
        File petImageFile = new File(getContext().getFilesDir(), "pet_image.png");
        final boolean localDeleted = petImageFile.exists() && petImageFile.delete();

        // Store current image string in case of failure
        final String currentImageString = pet.getImageString();

        // Update pet object
        pet.setImageString(null);

        // Update Firebase and UI
        if (pet.getPetId() != null) {
            FirebaseDatabase.getInstance()
                    .getReference("Pets")
                    .child(pet.getPetId())
                    .child("imageString")
                    .removeValue()
                    .addOnSuccessListener(aVoid -> {
                        if (isAdded() && getContext() != null) {
                            // Update UI
                            updateImagePreview();
                            // Show success message
                            Toast.makeText(getContext(), "Image deleted successfully", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded() && getContext() != null) {
                            // If Firebase delete fails but local delete succeeded, restore the local file
                            if (localDeleted) {
                                pet.setImageString(currentImageString);
                            }
                            Toast.makeText(getContext(), "Failed to delete image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            // Update UI to reflect the current state
                            updateImagePreview();
                        }
                    });
        } else {
            Log.e("SettingFragment", "Pet ID is null");
        }
    }

    private void showDisconnectConfirmation() {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Disconnect from Pet")
                .setMessage("Are you sure you want to disconnect from this pet? All your task history will be deleted.")
                .setPositiveButton("Yes", (dialog, which) -> disconnectFromPet())
                .setNegativeButton("No", null)
                .show();
    }

    private void disconnectFromPet() {
        if (!isAdded() || getContext() == null || user == null || pet == null) {
            return;
        }

        String userId = user.getUserId();
        String petId = pet.getPetId();

        if (userId == null || petId == null) {
            return;
        }

        // Check if user is admin
        if (user.getPetPassword() != null && !user.getPetPassword().isEmpty()) {
            Log.d("SettingFragment", "Current user is admin, fetching connected users");
            // Get all users connected to this pet
            FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .orderByChild("petId")
                    .equalTo(petId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Log.d("SettingFragment", "Found " + dataSnapshot.getChildrenCount() + " connected users");
                            List<User> connectedUsers = new ArrayList<>();
                            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                                try {
                                    String connectedUserId = userSnapshot.getKey();
                                    String connectedUserEmail = userSnapshot.child("email").getValue(String.class);
                                    String connectedUserName = userSnapshot.child("name").getValue(String.class);
                                    String connectedUserPetId = userSnapshot.child("petId").getValue(String.class);
                                    
                                    Log.d("SettingFragment", "Processing user: " + connectedUserEmail);
                                    
                                    if (connectedUserId != null && !connectedUserId.equals(userId)) {
                                        User connectedUser = new User();
                                        connectedUser.setUserId(connectedUserId);
                                        connectedUser.setEmail(connectedUserEmail);
                                        connectedUser.setName(connectedUserName);
                                        connectedUser.setPetId(connectedUserPetId);
                                        
                                        connectedUsers.add(connectedUser);
                                        Log.d("SettingFragment", "Added user to list: " + connectedUserEmail);
                                    }
                                } catch (Exception e) {
                                    Log.e("SettingFragment", "Error processing user: " + e.getMessage());
                                }
                            }

                            Log.d("SettingFragment", "Final connected users count: " + connectedUsers.size());
                            
                            if (connectedUsers.isEmpty()) {
                                Log.d("SettingFragment", "No other users connected, deleting pet");
                                deletePetAndDisconnect();
                            } else {
                                Log.d("SettingFragment", "Showing admin selection dialog");
                                showChooseNewAdminDialog(connectedUsers);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.e("SettingFragment", "Failed to get connected users: " + databaseError.getMessage());
                            Toast.makeText(getContext(), "Failed to get connected users: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Log.d("SettingFragment", "Current user is not admin, proceeding with normal disconnect");
            disconnectUserAndCleanup();
        }
    }

    private void showChooseNewAdminDialog(List<User> connectedUsers) {
        if (getContext() == null) return;

        Log.d("SettingFragment", "Preparing admin selection dialog with " + connectedUsers.size() + " users");
        
        // Create array of user information strings
        String[] userInfoArray = new String[connectedUsers.size()];
        for (int i = 0; i < connectedUsers.size(); i++) {
            User user = connectedUsers.get(i);
            userInfoArray[i] = user.getEmail();
            Log.d("SettingFragment", "Adding to dialog: " + userInfoArray[i]);
        }

        Log.d("SettingFragment", "User info array created with " + userInfoArray.length + " items");

        if (userInfoArray.length == 0) {
            Log.e("SettingFragment", "No users available for admin selection");
            Toast.makeText(getContext(), "Error: No users available for admin selection", Toast.LENGTH_LONG).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Choose New Admin")
               .setItems(userInfoArray, (dialog, which) -> {
                   User newAdmin = connectedUsers.get(which);
                   Log.d("SettingFragment", "Selected new admin: " + newAdmin.getEmail());
                   transferAdminRights(newAdmin);
                   dialog.dismiss();
               })
               .setNegativeButton("Cancel", (dialog, which) -> {
                   Log.d("SettingFragment", "Admin selection cancelled");
                   dialog.dismiss();
               });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void transferAdminRights(User newAdmin) {
        if (user == null || pet == null) return;

        // Update new admin's petPassword
        FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(newAdmin.getUserId())
                .child("petPassword")
                .setValue(user.getPetPassword())
                .addOnSuccessListener(aVoid -> {
                    // Update pet's admin information
                    FirebaseDatabase.getInstance()
                            .getReference("Pets")
                            .child(pet.getPetId())
                            .child("adminId")
                            .setValue(newAdmin.getUserId())
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(getContext(), 
                                    "Admin rights transferred to " + newAdmin.getEmail(), 
                                    Toast.LENGTH_SHORT).show();
                                // Now disconnect the current user
                                disconnectUserAndCleanup();
                            })
                            .addOnFailureListener(e ->
                                Toast.makeText(getContext(), 
                                    "Failed to update pet admin: " + e.getMessage(), 
                                    Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                    Toast.makeText(getContext(), 
                        "Failed to transfer admin rights: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show()
                );
    }

    private void deletePetAndDisconnect() {
        if (pet == null || user == null) return;

        // Delete the pet from Firebase
        FirebaseDatabase.getInstance()
                .getReference("Pets")
                .child(pet.getPetId())
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), 
                        "Pet deleted as no users remain", 
                        Toast.LENGTH_SHORT).show();
                    disconnectUserAndCleanup();
                })
                .addOnFailureListener(e ->
                    Toast.makeText(getContext(), 
                        "Failed to delete pet: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show()
                );
    }

    private void disconnectUserAndCleanup() {
        if (user == null) return;

        // Delete pet image from internal storage
        if (getContext() != null) {
            File petImageFile = new File(getContext().getFilesDir(), "pet_image.png");
            if (petImageFile.exists()) {
                boolean deleted = petImageFile.delete();
                Log.d("SettingFragment", "Pet image deleted from internal storage: " + deleted);
            }
        }

        // Delete user's tasks
        FirebaseDatabase.getInstance()
                .getReference("Tasks")
                .orderByChild("userId")
                .equalTo(user.getUserId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                            taskSnapshot.getRef().removeValue();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(getContext(), 
                            "Failed to delete tasks", 
                            Toast.LENGTH_SHORT).show();
                    }
                });

        // Clear user's petId and petPassword
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(user.getUserId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("petId", "");
        updates.put("petPassword", "");

        userRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    // Clear local user data
                    user.setPetId("");
                    user.setPetPassword("");
                    
                    Toast.makeText(getContext(), 
                        "Successfully disconnected from pet", 
                        Toast.LENGTH_SHORT).show();
                    
                    // Redirect to NewPet activity
                    if (getActivity() != null) {
                        Intent intent = new Intent(getActivity(), NewPet.class);
                        intent.putExtra("user", user);
                        startActivity(intent);
                        getActivity().finish();
                    }
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(getContext(), 
                        "Failed to disconnect: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show()
                );
    }
}

