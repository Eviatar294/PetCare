package com.example.petcare;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FirebaseFunctions {


    public static void fetchUsersWithSamePetId(String petId, FetchUsersCallback callback) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<User> userList = new ArrayList<>();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    if (user != null && petId.equals(user.getPetId())) {
                        userList.add(user);
                    }
                }
                callback.onSuccess(userList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure(error.getMessage());
            }
        });
    }

    public interface FetchUsersCallback {
        void onSuccess(ArrayList<User> userList);
        void onFailure(String errorMessage);
    }


    public static void getUserClassFromFirebase(String userId, GetUserCallback callback) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    callback.onSuccess(user);
                } else {
                    callback.onFailure("User not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure(error.getMessage());
            }
        });
    }

    // Callback interface for async user retrieval
    public interface GetUserCallback {
        void onSuccess(User user);
        void onFailure(String errorMessage);
    }


    public static void getPetClassFromFirebase(String petId, GetPetCallback callback) {
        DatabaseReference petRef = FirebaseDatabase.getInstance().getReference("Pets").child(petId);

        petRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Pet pet = snapshot.getValue(Pet.class);
                    callback.onSuccess(pet);
                } else {
                    callback.onFailure("Pet not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure(error.getMessage());
            }
        });
    }

    // Callback interface for async pet retrieval
    public interface GetPetCallback {
        void onSuccess(Pet pet);
        void onFailure(String errorMessage);
    }
}
