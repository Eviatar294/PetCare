package com.example.petcare;

import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
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

public class SettingFragment extends Fragment {

    private View view;
    private Context context;
    private ImageButton ibPetImagePreview;
    private Button bLogOut, bChangeNotificationTime;
    private TextView tvBestOwner, tvOwnerEmail, tvIsLeader, tvPetName, tvPetType, tvNotificationTime;
    private Uri imageUri = null;
    private String imageString;
    private User user;
    private Pet pet;

    // Shared ImagePickerHelper instance – it now handles permission checks and the image source dialog.
    private ImagePickerHelper imagePickerHelper;

    public SettingFragment() {
        // Required empty constructor.
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_setting, container, false);
        initComponents();

        bLogOut.setOnClickListener(v -> {
            deleteUserIdFromInternalStorage();
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(context, MainSignIn.class);
            startActivity(intent);
        });

        // Initialize ImagePickerHelper (it internally handles permission requests and the camera/gallery dialog).
        imagePickerHelper = new ImagePickerHelper(this, uri -> {
            imageUri = uri;
            ibPetImagePreview.setImageURI(uri);
            // Immediately upload the new image to Firebase.
            uploadImageToFirebase(uri);
        });

        // When the pet image is clicked, simply invoke pickImage() from the helper.
        ibPetImagePreview.setOnClickListener(v -> imagePickerHelper.pickImage());

        bChangeNotificationTime.setOnClickListener(v -> showTimePickerDialog());

        return view;
    }

    private void initComponents() {
        context = getActivity();
        bLogOut = view.findViewById(R.id.bLogOut);
        bChangeNotificationTime = view.findViewById(R.id.bChangeNotificationTime);
        ibPetImagePreview = view.findViewById(R.id.ibPetImagePreview);
        tvBestOwner = view.findViewById(R.id.tvBestOwner);
        tvOwnerEmail = view.findViewById(R.id.tvOwnerEmail);
        tvIsLeader = view.findViewById(R.id.tvIsLeader);
        tvPetName = view.findViewById(R.id.tvPetName);
        tvPetType = view.findViewById(R.id.tvPetType);
        tvNotificationTime = view.findViewById(R.id.tvNotificationTime);

        // Retrieve the User and Pet objects from arguments.
        Bundle bundle = getArguments();
        if (bundle != null) {
            user = (User) bundle.getSerializable("user");
            pet = (Pet) bundle.getSerializable("pet");
        }

        if (user != null) {
            tvBestOwner.setText(user.getName() + " is the best owner ever!");
            tvOwnerEmail.setText("Email: " + user.getEmail());
            if (user.getPetPassword() != null && !user.getPetPassword().isEmpty()) {
                tvIsLeader.setText("Leader Email: " + user.getEmail());
            } else {
                fetchLeaderEmail();
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
                        TaskNotificationScheduler.scheduleTaskReminder(context);
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
    private void uploadImageToFirebase(Uri imageUri) {
        if (imageUri == null) return;
        try {
            imageString = convertUriToBase64(imageUri);
            if (imageString == null || imageString.isEmpty()) {
                Toast.makeText(context, "Error: Converted image string is empty.", Toast.LENGTH_SHORT).show();
                return;
            }
            pet.setImageString(imageString);
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference petsRef = database.getReference("Pets");
            if (pet.getPetId() == null || pet.getPetId().isEmpty()) {
                Toast.makeText(context, "Error: Invalid pet id.", Toast.LENGTH_SHORT).show();
                return;
            }
            petsRef.child(pet.getPetId()).child("imageString").setValue(imageString)
                    .addOnSuccessListener(aVoid -> {
                        saveImageFromUriToInternalStorage(imageUri);
                        updateImagePreview();
                        Toast.makeText(context, "Image uploaded successfully.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(context, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } catch (IOException e) {
            Toast.makeText(context, "Error converting image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImageFromUriToInternalStorage(Uri imageUri) {
        if (getContext() == null) return;
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(getContext().getContentResolver().openInputStream(imageUri));
            if (bitmap == null) {
                Toast.makeText(getContext(), "Error: Bitmap is null.", Toast.LENGTH_SHORT).show();
                return;
            }
            File file = new File(getContext().getFilesDir(), "pet_image.png");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Toast.makeText(getContext(), "Pet image saved locally.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error saving image locally: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
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

    private void updateImagePreview() {
        if (!isAdded() || getContext() == null) {
            Log.e("SettingFragment", "Fragment not attached, cannot update image preview.");
            return;
        }
        File petImageFile = new File(getContext().getFilesDir(), "pet_image.png");
        if (petImageFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(petImageFile.getAbsolutePath());
            if (bitmap != null) {
                Glide.with(getContext())
                        .load(bitmap)
                        .circleCrop()
                        .skipMemoryCache(true)
                        .into(ibPetImagePreview);
                Log.d("SettingFragment", "Loaded pet image from internal storage.");
            } else {
                Log.e("SettingFragment", "Failed to decode pet image from internal storage.");
                Glide.with(getContext())
                        .load(R.drawable.cartoon_black_cat_with_question_mark_above_head_vector)
                        .circleCrop()
                        .skipMemoryCache(true)
                        .into(ibPetImagePreview);
            }
        } else {
            Log.d("SettingFragment", "Pet image not found in internal storage, loading default image.");
            Glide.with(getContext())
                    .load(R.drawable.cartoon_black_cat_with_question_mark_above_head_vector)
                    .circleCrop()
                    .skipMemoryCache(true)
                    .into(ibPetImagePreview);
        }
    }

    private void deleteUserIdFromInternalStorage() {
        if (!isAdded() || getContext() == null) return;
        File directory = getContext().getFilesDir();
        File textFile = new File(directory, "user_id.txt");
        if (textFile.exists()) {
            textFile.delete();
        }
    }
}
