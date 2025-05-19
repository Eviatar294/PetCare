package com.example.petcare;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;

public class MainHomeUser extends BaseActivity {

    BottomNavigationView bottomNavigationView;
    Intent inIntent;
    Bundle bundle;
    User user;
    Pet pet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_home_user);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize components and retrieve user, pet, and usersList data.
        initComponents();

        // Generate recurring task instances for the next week.
        if (pet != null && pet.getPetId() != null && !pet.getPetId().isEmpty()) {
            RecurringTaskGenerator.generateRecurringTaskInstances(pet.getPetId());
        }

        // Schedule daily task notification at 8 PM.
        TaskNotificationScheduler.scheduleTaskReminder(this);

        // Default fragment: Settings.
        bottomNavigationView.setSelectedItemId(R.id.navigation_setting);
        switchToFragment(new SettingFragment());

        // Handle bottom navigation changes.
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_setting) {
                switchToFragment(new SettingFragment());
                return true;
            } else if (item.getItemId() == R.id.navigation_task) {
                switchToFragment(new TasksFragment());
                return true;
            } else if (item.getItemId() == R.id.navigation_leaderboard) {
                switchToFragment(new LeaderboardFragment());
                return true;
            }
            return false;
        });
    }

    private void initComponents() {
        bottomNavigationView = findViewById(R.id.nav_view);
        inIntent = getIntent();
        user = (User) inIntent.getSerializableExtra("user");
        pet = (Pet) inIntent.getSerializableExtra("pet");

        bundle = new Bundle();
        bundle.putSerializable("user", user);
        bundle.putSerializable("pet", pet);
        ArrayList<User> usersList = (ArrayList<User>) getIntent().getSerializableExtra("usersList");
        bundle.putSerializable("usersList", usersList);
    }

    private void switchToFragment(Fragment fragment) {
        fragment.setArguments(bundle);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.flHome, fragment);
        ft.commit();
    }
}
