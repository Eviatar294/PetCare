package com.example.petcare;

import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.ArrayList;

public class NewRecurringTaskFragment extends Fragment {

    private View view;
    private EditText etTaskBody, etTime;
    private Spinner spinnerRecurringType, spinnerDayOfWeek;
    private Button bCancel, bCreate;
    private Context context;
    private User user;
    private Pet pet;
    private ArrayList<User> usersList;
    private FirebaseDatabase database;
    private DatabaseReference tasksRef;

    public NewRecurringTaskFragment() {
        // Required empty public constructor
    }

    public static NewRecurringTaskFragment newInstance() {
        return new NewRecurringTaskFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout
        view = inflater.inflate(R.layout.fragment_new_recurring_task, container, false);

        // Retrieve bundle data: user, pet, and usersList
        Bundle bundle = getArguments();
        if (bundle != null) {
            user = (User) bundle.getSerializable("user");
            pet = (Pet) bundle.getSerializable("pet");
            usersList = (ArrayList<User>) bundle.getSerializable("usersList");
        }

        initComponents();
        setupTimePicker();
        setupRecurringTypeSpinner();
        setupDayOfWeekSpinner();

        // Initialize Firebase
        database = FirebaseDatabase.getInstance();
        tasksRef = database.getReference("Tasks");

        // Button listeners
        bCreate.setOnClickListener(v -> createRecurringTask());
        bCancel.setOnClickListener(v -> goBackToTasksFragment());

        return view;
    }

    private void initComponents() {
        etTaskBody = view.findViewById(R.id.etTaskBody);
        etTime = view.findViewById(R.id.etTime);
        spinnerRecurringType = view.findViewById(R.id.spinnerRecurringType);
        spinnerDayOfWeek = view.findViewById(R.id.spinnerDayOfWeek);
        bCreate = view.findViewById(R.id.bCreateNewRecurringTask);
        bCancel = view.findViewById(R.id.bCancelNewRecurringTask);
        context = getContext();
    }

    private void setupTimePicker() {
        etTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            TimePickerDialog tpd = new TimePickerDialog(context,
                    (view, hourOfDay, minute) -> {
                        String selectedTime = String.format("%02d:%02d", hourOfDay, minute);
                        etTime.setText(selectedTime);
                    },
                    calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
            tpd.show();
        });
    }

    private void setupRecurringTypeSpinner() {
        // Options: Daily and Weekly
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Daily", "Weekly"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRecurringType.setAdapter(adapter);

        // Show or hide the day-of-week spinner based on the selection
        spinnerRecurringType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = spinnerRecurringType.getSelectedItem().toString();
                if (selected.equalsIgnoreCase("Weekly")) {
                    spinnerDayOfWeek.setVisibility(View.VISIBLE);
                } else {
                    spinnerDayOfWeek.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                spinnerDayOfWeek.setVisibility(View.GONE);
            }
        });
    }

    private void setupDayOfWeekSpinner() {
        // Set up spinner for days of the week
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDayOfWeek.setAdapter(adapter);
        // Initially hide the day-of-week spinner
        spinnerDayOfWeek.setVisibility(View.GONE);
    }

    private void createRecurringTask() {
        String taskName = etTaskBody.getText().toString();
        String dueTime = etTime.getText().toString();
        String recurrenceSelection = spinnerRecurringType.getSelectedItem().toString();
        String recurrenceType;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dueDate;
        Calendar calendar = Calendar.getInstance();

        if (recurrenceSelection.equalsIgnoreCase("Weekly")) {
            // Get the chosen day from spinnerDayOfWeek and compute next occurrence
            String chosenDay = spinnerDayOfWeek.getSelectedItem().toString();
            int chosenDayOfWeek = getDayOfWeekConstant(chosenDay);
            dueDate = getNextWeekdayDate(chosenDayOfWeek, sdf);
            recurrenceType = chosenDay.toUpperCase();  // e.g., MONDAY
        } else {
            // Daily: due date is tomorrow
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            dueDate = sdf.format(calendar.getTime());
            recurrenceType = "daily";
        }

        // Validate required fields
        if (taskName.isEmpty() || dueTime.isEmpty()) {
            Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Task newTask = new Task(taskName, pet.getPetId(), "", dueDate, dueTime, "pending", recurrenceType);

        String taskId = tasksRef.push().getKey();
        tasksRef.child(taskId).setValue(newTask)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Recurring task created successfully", Toast.LENGTH_SHORT).show();
                    // Generate recurring task instances for the next week.
                    if (pet != null && pet.getPetId() != null && !pet.getPetId().isEmpty()) {
                        RecurringTaskGenerator.generateRecurringTaskInstances(pet.getPetId());
                    }
                    clearFields();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error creating task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Helper method to map day name to Calendar constant
    private int getDayOfWeekConstant(String day) {
        switch (day.toLowerCase()) {
            case "sunday": return Calendar.SUNDAY;
            case "monday": return Calendar.MONDAY;
            case "tuesday": return Calendar.TUESDAY;
            case "wednesday": return Calendar.WEDNESDAY;
            case "thursday": return Calendar.THURSDAY;
            case "friday": return Calendar.FRIDAY;
            case "saturday": return Calendar.SATURDAY;
            default: return Calendar.SUNDAY;
        }
    }

    // Helper method to calculate the next occurrence of the given day of week
    private String getNextWeekdayDate(int chosenDay, SimpleDateFormat sdf) {
        Calendar cal = Calendar.getInstance();
        int today = cal.get(Calendar.DAY_OF_WEEK);
        int daysToAdd = (chosenDay - today + 7) % 7;
        if (daysToAdd == 0) {
            daysToAdd = 7;  // if today is the chosen day, schedule for next week
        }
        cal.add(Calendar.DAY_OF_YEAR, daysToAdd);
        return sdf.format(cal.getTime());
    }

    private void goBackToTasksFragment() {
        TasksFragment tasksFragment = new TasksFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("user", user);
        bundle.putSerializable("pet", pet);
        bundle.putSerializable("usersList", usersList);
        tasksFragment.setArguments(bundle);

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.flHome, tasksFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void clearFields() {
        etTaskBody.setText("");
        etTime.setText("");
        spinnerRecurringType.setSelection(0);
        spinnerDayOfWeek.setSelection(0);
        spinnerDayOfWeek.setVisibility(View.GONE);
    }
}
