package com.example.petcare;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import java.io.File;

public class NewPet extends BaseActivity {

    ChoosePetFragment choosePetFragment;
    TextView tvHello;
    String stName;
    User user;
    Button bLogOutNoPet;
    Context context;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_pet);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initComponents();
        stName = user.getName();
        tvHello.setText("Welcome " + stName);

        FragmentTransaction ftFirst = getSupportFragmentManager().beginTransaction();
        ftFirst.replace(R.id.flNewPet, choosePetFragment);
        ftFirst.commit();

        bLogOutNoPet.setOnClickListener(v -> {
            deleteUserIdFromInternalStorage();
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(context, MainSignIn.class);
            startActivity(intent);
        });
    }

    private void initComponents() {
        choosePetFragment = new ChoosePetFragment();
        tvHello = findViewById(R.id.tvHello);
        user = (User) getIntent().getSerializableExtra("user");
        bLogOutNoPet = findViewById(R.id.bLogOutNoPet);
        context = NewPet.this;
    }

    private void deleteUserIdFromInternalStorage() {
        File directory = getFilesDir();
        File textFile = new File(directory, "user_id.txt");

        if (textFile.exists()) {
            textFile.delete();
        }
    }


}