package com.example.petcare;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class LeaderboardFragment extends Fragment {

    private ArrayList<User> usersList = new ArrayList<>();
    private ListView listView;
    private UserAdapter adapter;
    private DatabaseReference usersRef;
    private User currentUser;
    private Pet pet;
    private View view;
    private static final String TAG = "LeaderboardFragment";

    public LeaderboardFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        view = inflater.inflate(R.layout.fragment_leaderboard, container, false);

        // Retrieve data from bundle
        Bundle bundle = getArguments();
        if(bundle != null){
            currentUser = (User) bundle.getSerializable("user");
            pet = (Pet) bundle.getSerializable("pet");
        } else {
            Log.e(TAG, "Bundle is null - no data received");
        }

        listView = view.findViewById(R.id.lvUsers);
        adapter = new UserAdapter(requireContext(), usersList);
        listView.setAdapter(adapter);

        // Initialize Firebase reference for Users and load data in real time
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        loadUsers();

        return view;
    }

    private void loadUsers(){
        if(pet == null){
            Toast.makeText(getContext(), "Pet data is missing", Toast.LENGTH_SHORT).show();
            return;
        }
        // Query users with petId equal to current pet's id.
        usersRef.orderByChild("petId").equalTo(pet.getPetId())
                .addValueEventListener(new ValueEventListener(){
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        usersList.clear();
                        for(DataSnapshot ds : snapshot.getChildren()){
                            User u = ds.getValue(User.class);
                            if(u != null){
                                usersList.add(u);
                            }
                        }
                        // Sort users descending by numOfTasks
                        Collections.sort(usersList, new Comparator<User>() {
                            @Override
                            public int compare(User u1, User u2) {
                                int t1 = u1.getNumOfTasks() != null ? u1.getNumOfTasks() : 0;
                                int t2 = u2.getNumOfTasks() != null ? u2.getNumOfTasks() : 0;
                                return t2 - t1;
                            }
                        });
                        adapter.notifyDataSetChanged();
                        Log.d(TAG, "Leaderboard updated: " + usersList.size() + " users");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Error loading leaderboard", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
