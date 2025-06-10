package com.example.petcare;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class GeneratePetFragment extends Fragment {

    View view;
    FirebaseStorage storage;
    DatabaseReference databaseReference;
    String petId, imageString;
    EditText etPetName, etPetType, etPetPassword;
    Button bCreatePet, bUploadImage;
    ImageView ivPetImage;  // To display the selected image
    Uri imageUri;
    Pet newPet;
    User user;
    private DatabaseReference petsRef;

    ArrayList<User> myUserList = new ArrayList<>();

    // Use the shared ImagePickerHelper.
    ImagePickerHelper imagePickerHelper;

    public GeneratePetFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        view = inflater.inflate(R.layout.fragment_generate_pet, container, false);
        initComponents();

        // Initialize the ImagePickerHelper with a callback.
        imagePickerHelper = new ImagePickerHelper(this, uri -> {
            imageUri = uri;
            ivPetImage.setImageURI(uri);
        });

        // Use the helper's pickImage() method.
        bUploadImage.setOnClickListener(v -> imagePickerHelper.pickImage());

        storage = FirebaseStorage.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Pets");

        bCreatePet.setOnClickListener(v -> {
            if (etPetName.getText() != null && etPetType.getText() != null && etPetPassword.getText() != null) {
                String stPetName = etPetName.getText().toString();
                String stPetType = etPetType.getText().toString();
                String stPetPassword = etPetPassword.getText().toString();
                createNewPet(stPetName, stPetType, imageUri, stPetPassword);
            } else {
                Toast.makeText(getContext(), "Please fill all the fields", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void initComponents() {
        etPetType = view.findViewById(R.id.etPetType);
        etPetName = view.findViewById(R.id.etPetName);
        etPetPassword = view.findViewById(R.id.etPetPassword);
        bCreatePet = view.findViewById(R.id.bCreatePet);
        bUploadImage = view.findViewById(R.id.bUploadImage);
        ivPetImage = view.findViewById(R.id.ivPetImage);
        user = (User) getActivity().getIntent().getSerializableExtra("user");

        Button bBackToChoose = view.findViewById(R.id.bBackToChoose);
        bBackToChoose.setOnClickListener(v -> {
            // Navigate back to ChoosePetFragment
            ChoosePetFragment choosePetFragment = new ChoosePetFragment();
            getParentFragmentManager().beginTransaction()
                .replace(R.id.flNewPet, choosePetFragment)
                .commit();
        });
    }

    private void createNewPet(String name, String type, Uri imageUri, String petPassword) {
        petId = FirebaseDatabase.getInstance()
                .getReference("Pets")
                .push()
                .getKey();
        if (petId == null) {
            Toast.makeText(getContext(),
                            "Fatal: could not generate pet ID.",
                            Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        // 1️⃣ Write the Pet object
        newPet = new Pet(petId, name, type);
        petsRef = FirebaseDatabase.getInstance()
                .getReference("Pets")
                .child(petId);

        petsRef.setValue(newPet, (dbError, ref) -> {
            if (dbError != null) {
                // ❌ Pet write failed
                Toast.makeText(getActivity(),
                                "Error saving pet: " + dbError.getMessage(),
                                Toast.LENGTH_LONG)
                        .show();
                return;
            }
            // ✅ Pet written → now handle image
            handleImageUploadThenOwnerLink(imageUri, petPassword);
        });
    }

    private void handleImageUploadThenOwnerLink(Uri imageUri, String petPassword) {
        if (imageUri == null) {
            // No image path → go straight to owner link
            linkOwnerAndNavigate(petPassword);
            return;
        }

        String base64;
        try {
            base64 = convertUriToBase64(imageUri);
            newPet.setImageString(base64);
            
            // Also save the image to local storage for immediate display
            savePetImageToInternalStorage(imageUri);
        } catch (IOException e) {
            Toast.makeText(getContext(),
                            "Image encoding failed: " + e.getMessage(),
                            Toast.LENGTH_LONG)
                    .show();
            // Decide: abort or continue without image?
            linkOwnerAndNavigate(petPassword);
            return;
        }

        // 2️⃣ Upload the image string
        petsRef.child("imageString")
                .setValue(base64, (dbError, ref) -> {
                    if (dbError != null) {
                        Toast.makeText(getContext(),
                                        "Failed to upload image: " + dbError.getMessage(),
                                        Toast.LENGTH_LONG)
                                .show();
                        // Optionally: continue or abort
                    }
                    // ✅ Image uploaded (or we choose to ignore failure) → link owner
                    linkOwnerAndNavigate(petPassword);
                });
    }

    private void linkOwnerAndNavigate(String petPassword) {
        updateOwnerPet(user.getUserId(), petId, petPassword, (dbError, ref) -> {
            if (dbError != null) {
                // Owner-link failed
                Toast.makeText(getContext(),
                                "Failed to link pet to user: " + dbError.getMessage(),
                                Toast.LENGTH_LONG)
                        .show();
                return;
            }
            // ✅ Owner-link successful → update local user object and get users list
            user.setPetId(petId);
            user.setPetPassword(petPassword);
            
            // Add a small delay to ensure Firebase propagation before fetching users list
            new android.os.Handler().postDelayed(() -> {
                getUserList(petId);
            }, 500); // 500ms delay
        });
    }

    private void updateOwnerPet(String userId, String petId, String petPassword,
                                DatabaseReference.CompletionListener completion) {
        DatabaseReference userRef = FirebaseDatabase
                .getInstance()
                .getReference("Users")
                .child(userId);

        // 1️⃣ Add the petId under the user
        userRef.child("petId")
                .setValue(petId, (err1, ref1) -> {
                    if (err1 != null) {
                        completion.onComplete(err1, ref1);
                        return;
                    }
                    // 2️⃣ Only after petId is saved, write the password
                    userRef.child("petPassword")
                            .setValue(petPassword, completion);
                });
    }

    private void getUserList(String petId) {
        android.util.Log.d("GeneratePetFragment", "Fetching users for petId: " + petId);
        FirebaseFunctions.fetchUsersWithSamePetId(petId, new FirebaseFunctions.FetchUsersCallback() {
            @Override
            public void onSuccess(ArrayList<User> userList) {
                android.util.Log.d("GeneratePetFragment", "Successfully fetched " + userList.size() + " users");
                myUserList = userList;
                // Ensure current user is in the list
                boolean userExists = false;
                for (User u : myUserList) {
                    android.util.Log.d("GeneratePetFragment", "Found user: " + u.getName() + " with ID: " + u.getUserId());
                    if (u.getUserId().equals(user.getUserId())) {
                        userExists = true;
                        break;
                    }
                }
                if (!userExists) {
                    android.util.Log.d("GeneratePetFragment", "Adding current user to list: " + user.getName());
                    myUserList.add(user);
                }
                android.util.Log.d("GeneratePetFragment", "Final user list size: " + myUserList.size());
                moveToNextPage();
            }
            @Override
            public void onFailure(String errorMessage) {
                android.util.Log.e("GeneratePetFragment", "Failed to fetch users: " + errorMessage);
                // If fetching users fails, at least add the current user to the list
                myUserList.clear();
                myUserList.add(user);
                android.util.Log.d("GeneratePetFragment", "Added current user to empty list, size: " + myUserList.size());
                moveToNextPage();
            }
        });
    }

    private void moveToNextPage() {
        // Clear the heavy imageString from the pet after saving to avoid memory issues
        if (newPet != null && newPet.getImageString() != null) {
            newPet.setImageString("");
        }
        
        // Show success toast
        Toast.makeText(getContext(), "Pet created successfully! Welcome to PetCare!", Toast.LENGTH_LONG).show();
        
        android.util.Log.d("GeneratePetFragment", "Moving to next page with:");
        android.util.Log.d("GeneratePetFragment", "User: " + (user != null ? user.getName() : "null"));
        android.util.Log.d("GeneratePetFragment", "User has petPassword: " + (user != null && user.getPetPassword() != null && !user.getPetPassword().isEmpty()));
        android.util.Log.d("GeneratePetFragment", "Pet: " + (newPet != null ? newPet.getName() : "null"));
        android.util.Log.d("GeneratePetFragment", "Users list size: " + (myUserList != null ? myUserList.size() : 0));
        
        Intent intent = new Intent(getActivity(), MainHomeUser.class);
        intent.putExtra("user", user);
        intent.putExtra("pet", newPet);
        intent.putExtra("usersList", myUserList);
        startActivity(intent);
    }

    public String convertUriToBase64(Uri imageUri) throws IOException {
        if (getContext() == null) throw new IOException("Context is null");
        // Conversion code using local context.
        android.content.ContentResolver contentResolver = getContext().getContentResolver();
        InputStream inputStream = contentResolver.openInputStream(imageUri);
        if (inputStream == null) {
            throw new IOException("Unable to open input stream from URI: " + imageUri);
        }
        Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
        if (bitmap == null) {
            throw new IOException("Failed to decode bitmap from URI: " + imageUri);
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT);
    }

    private void savePetImageToInternalStorage(Uri imageUri) {
        if (getContext() == null || imageUri == null) return;
        
        try {
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(
                getContext().getContentResolver().openInputStream(imageUri));
            if (bitmap == null) {
                return;
            }
            
            java.io.File file = new java.io.File(getContext().getFilesDir(), "pet_image.png");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (java.io.IOException e) {
            android.util.Log.e("GeneratePetFragment", "Error saving image locally: " + e.getMessage());
        }
    }
}
