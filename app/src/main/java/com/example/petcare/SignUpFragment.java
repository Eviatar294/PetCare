package com.example.petcare;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class SignUpFragment extends Fragment {
    EditText etName, etPass, etConfirmPass, etEmail;
    String stName, stPass, stConfirmPass, stEmail;
    Button bSaveUser;
    View view;
    User newUser;
    Context context;
    String userId;

    public SignUpFragment() {
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
        view = inflater.inflate(R.layout.fragment_sign_up, container, false);

        initComponents();

        bSaveUser.setOnClickListener(v -> {
            if (etEmail.getText() == null || etName.getText() == null || etPass.getText() == null
                || etConfirmPass.getText() == null) {
                Toast.makeText(context, "please fill all the fields", Toast.LENGTH_SHORT).show();
                return;
            }
            stEmail = etEmail.getText().toString();
            stName = etName.getText().toString();
            stPass = etPass.getText().toString();
            stConfirmPass = etConfirmPass.getText().toString();

            if (!stConfirmPass.equals(stPass)) {
                Toast.makeText(context, "Passwords not matching", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseAuth auth = FirebaseAuth.getInstance();
            auth.createUserWithEmailAndPassword(stEmail, stPass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                userId = user.getUid();

                                newUser = new User(userId, stEmail, stName, stPass, "", "", 0);

                                DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users");
                                database.child(userId).setValue(newUser)
                                        .addOnCompleteListener(task2 -> {
                                            if (task2.isSuccessful()) {
                                                Toast.makeText(context, "Sign-up successful!", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(context, "Error saving1: " + task2.getException().getMessage(), Toast.LENGTH_LONG).show();
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(context, "Error saving2: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });

        });


        return view;
    }
    private void initComponents() {
        bSaveUser = view.findViewById(R.id.bSaveUser);
        etEmail = view.findViewById(R.id.etEmail);
        etName = view.findViewById(R.id.etName);
        etPass = view.findViewById(R.id.etPass);
        etConfirmPass = view.findViewById(R.id.etConfirmPass);
        context = getActivity();
    }
}

