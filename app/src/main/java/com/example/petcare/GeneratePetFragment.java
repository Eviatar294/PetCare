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
    }

    private void createNewPet(String name, String type, Uri imageUri, String petPassword) {
        petId = databaseReference.push().getKey();
        if (petId != null) {
            newPet = new Pet(petId, name, type);
            databaseReference.child(newPet.getPetId()).setValue(newPet)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getActivity(), "New pet saved successfully.", Toast.LENGTH_SHORT).show();
                            if (imageUri != null) {
                                uploadImageToFirebase(imageUri);
                            }
                            updateOwnerPet(user.getUserId(), newPet.getPetId(), petPassword);
                        } else {
                            Toast.makeText(getActivity(), "Error saving new pet.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(getActivity(), "Error: Missing petId", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImageToFirebase(Uri imageUri) {
        if (imageUri == null) return;
        try {
            // Call the local conversion method.
            imageString = convertUriToBase64(imageUri);
            newPet.setImageString(imageString);
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference petsRef = database.getReference("Pets");
            petsRef.child(petId).child("imageString").setValue(imageString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateOwnerPet(String userId, String petId, String petPassword) {
        DatabaseReference userReference = FirebaseDatabase.getInstance().getReference("Users");
        userReference.child(userId).child("petId").setValue(petId)
                .addOnSuccessListener(aVoid -> getUserList(petId))
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to save owner's pet", Toast.LENGTH_SHORT).show());
        userReference.child(userId).child("petPassword").setValue(petPassword)
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to save pet password", Toast.LENGTH_SHORT).show());
    }

    private void getUserList(String petId) {
        FirebaseFunctions.fetchUsersWithSamePetId(petId, new FirebaseFunctions.FetchUsersCallback() {
            @Override
            public void onSuccess(ArrayList<User> userList) {
                myUserList = userList;
                moveToNextPage();
            }
            @Override
            public void onFailure(String errorMessage) { }
        });
    }

    private void moveToNextPage() {
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
}
