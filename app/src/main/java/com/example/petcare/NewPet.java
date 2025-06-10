package com.example.petcare;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

public class NewPet extends BaseActivity {

    ChoosePetFragment choosePetFragment;
    TextView tvHello;
    String stName;
    User user;
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
    }

    private void initComponents() {
        choosePetFragment = new ChoosePetFragment();
        tvHello = findViewById(R.id.tvHello);
        user = (User) getIntent().getSerializableExtra("user");
        context = NewPet.this;
    }
}