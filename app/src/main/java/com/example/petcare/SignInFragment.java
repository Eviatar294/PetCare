package com.example.petcare;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;

public class SignInFragment extends Fragment {

    EditText etSignInEmail, etSignInPassword;
    String stEmail, stPassword;
    Button bLogUser;
    View view;
    User user = null;
    Pet myPet = null;
    ArrayList<User> myUserList = new ArrayList<>();
    Context context;
    CheckBox cbRememberMe;

    public SignInFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_sign_in, container, false);

        initComponents();

        bLogUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stEmail = etSignInEmail.getText().toString();
                stPassword = etSignInPassword.getText().toString();

                if (stEmail.isEmpty() || stPassword.isEmpty()) {
                    Toast.makeText(context, "Please fill all the fields.", Toast.LENGTH_SHORT).show();
                } else {

                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users");
                    databaseReference.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            boolean userFound = false;
                            // Reset user and userId for loop
                            user = null;
                            String userId = null;

                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                // Get the user id and object
                                userId = userSnapshot.getKey();
                                user = userSnapshot.getValue(User.class);

                                String email = userSnapshot.child("email").getValue(String.class);
                                String password = userSnapshot.child("password").getValue(String.class);

                                if (email.equals(stEmail) && password.equals(stPassword)) {
                                    userFound = true;
                                    break;
                                }
                            }

                            if (userFound) {
                                if (cbRememberMe.isChecked()) {
                                    saveUserIdToInternalStorage(userId);
                                }

                                Toast.makeText(context, "Log in successfully.", Toast.LENGTH_SHORT).show();
                                // Check if the user already has a pet
                                String petId = user.getPetId();
                                if (!petId.equals("")) {
                                    FirebaseFunctions.getPetClassFromFirebase(petId, new FirebaseFunctions.GetPetCallback() {
                                        @Override
                                        public void onSuccess(Pet pet) {
                                            myPet = pet;
                                            // Save the pet image to internal storage if available.
                                            savePetImageToInternalStorage(myPet);
                                            // Clear the heavy imageString from the pet before moving to the next page.
                                            myPet.setImageString("");

                                            FirebaseFunctions.fetchUsersWithSamePetId(petId, new FirebaseFunctions.FetchUsersCallback() {
                                                @Override
                                                public void onSuccess(ArrayList<User> userList) {
                                                    myUserList = userList;
                                                    navigateToNextActivity();
                                                }

                                                @Override
                                                public void onFailure(String errorMessage) {
                                                    // Handle error if needed
                                                }
                                            });
                                        }

                                        @Override
                                        public void onFailure(String errorMessage) {
                                            // Handle error if needed
                                        }
                                    });
                                } else {
                                    navigateToNextActivity();
                                }
                            } else {
                                user = null;
                                Toast.makeText(context, "The email or password is incorrect.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(context, "Error accessing the server.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

        return view;
    }

    /**
     * Saves the user's pet image from Firebase to internal storage.
     * The image is saved as "pet_image.png" in the app's files directory.
     */
    private void savePetImageToInternalStorage(Pet pet) {
        if (pet == null || pet.getImageString() == null || pet.getImageString().isEmpty()) {
            // No image available; nothing to save.
            return;
        }

        try {
            // Decode the Base64 string into a Bitmap
            byte[] decodedBytes = Base64.decode(pet.getImageString(), Base64.DEFAULT);
            Bitmap petBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            if (petBitmap == null) {
                return;
            }
            // Save the bitmap to internal storage as "pet_image.png"
            File file = new File(requireContext().getFilesDir(), "pet_image.png");
            FileOutputStream fos = new FileOutputStream(file);
            petBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Toast.makeText(requireContext(), "Pet image saved to internal storage.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveUserIdToInternalStorage(String userId) {
        File file = new File(requireContext().getFilesDir(), "user_id.txt");

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(userId.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initComponents() {
        etSignInEmail = view.findViewById(R.id.etSignInEmail);
        etSignInPassword = view.findViewById(R.id.etSignInPassword);
        bLogUser = view.findViewById(R.id.bLogUser);
        cbRememberMe = view.findViewById(R.id.cbRemeberMe);
        context = getActivity();
    }

    private void navigateToNextActivity() {
        if (myPet != null) {
            // Navigate to MainHomeUser if both user and pet are found
            Intent goToMainHome = new Intent(context, MainHomeUser.class);
            goToMainHome.putExtra("user", user);
            goToMainHome.putExtra("pet", myPet);
            goToMainHome.putExtra("usersList", myUserList);
            startActivity(goToMainHome);
        } else {
            // If no pet, navigate to NewPet
            Intent goToNewPet = new Intent(context, NewPet.class);
            goToNewPet.putExtra("user", user);
            startActivity(goToNewPet);
        }
    }
}
