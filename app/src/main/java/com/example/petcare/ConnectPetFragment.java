package com.example.petcare;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ConnectPetFragment extends Fragment {

    View view;
    Context context;
    Button bJoinPet;
    EditText etEmailLeader, etPassJoin;
    String stEmailLeader, stPassJoin;
    String petId = null;
    User myUser;
    Pet myPet;
    ArrayList<User> myUserList = new ArrayList<>();

    public ConnectPetFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_connect_pet, container, false);

        initComponents();

        bJoinPet.setOnClickListener(v -> {
            if (etEmailLeader.getText() != null && etPassJoin.getText() != null) {
                stEmailLeader = etEmailLeader.getText().toString();
                stPassJoin = etPassJoin.getText().toString();

                findPetOfLeader(stEmailLeader, stPassJoin);
            } else {
                Toast.makeText(context, "Please fill all the fields", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void initComponents() {
        context = getActivity();
        bJoinPet = view.findViewById(R.id.bJoinPet);
        etEmailLeader = view.findViewById(R.id.etEmailLeader);
        etPassJoin = view.findViewById(R.id.etPassJoin);
        myUser = (User) getActivity().getIntent().getSerializableExtra("user");
    }

    private void findPetOfLeader(String emailLeader, String passJoin) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() { // Use SingleValueEvent for one-time retrieval
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean userFound = false;

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    User tempUser = userSnapshot.getValue(User.class);

                    String email = userSnapshot.child("email").getValue(String.class);
                    String petPassword = userSnapshot.child("petPassword").getValue(String.class);

                    if (email != null && petPassword != null && email.equals(emailLeader) && petPassword.equals(passJoin)) {
                        userFound = true;
                        petId = tempUser.getPetId();
                        break;
                    }
                }

                if (userFound) {
                    updatePetOfUser(petId);
                } else {
                    Toast.makeText(context, "The email or password is incorrect.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Error accessing the server.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void updatePetOfUser(String petId) {
        if (myUser == null) {
            return;
        }
        myUser.setPetId(petId);

        DatabaseReference userReference = FirebaseDatabase.getInstance().getReference("Users");
        userReference.child(myUser.getUserId()).child("petId").setValue(petId)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Successfully connected to the pet", Toast.LENGTH_SHORT).show();
                    FirebaseFunctions.getPetClassFromFirebase(petId, new FirebaseFunctions.GetPetCallback() {
                        @Override
                        public void onSuccess(Pet pet) {
                            myPet = pet;
                            FirebaseFunctions.fetchUsersWithSamePetId(petId, new FirebaseFunctions.FetchUsersCallback() {
                                @Override
                                public void onSuccess(ArrayList<User> userList) {
                                    myUserList = userList;
                                    moveToNextPage();
                                }

                                @Override
                                public void onFailure(String errorMessage) {

                                }
                            });
                        }

                        @Override
                        public void onFailure(String errorMessage) {

                        }
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Failed to save owner's pet", Toast.LENGTH_SHORT).show());
    }

    private void moveToNextPage() {
        Intent intent = new Intent(getActivity(), MainHomeUser.class);
        intent.putExtra("user", myUser);
        intent.putExtra("pet", myPet);
        intent.putExtra("usersList", myUserList);
        startActivity(intent);
    }
}
