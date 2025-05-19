package com.example.petcare;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;

public class NewTaskFragment extends Fragment {

    View view;
    EditText etDate, etTime, etTaskBody;
    Spinner spinnerResponsible;
    Button bCancelNewTask, bCreateNewTask, bAddRecurringTask;
    Context context;
    ArrayList<User> usersList;
    User user;
    Pet pet;
    FirebaseDatabase database;
    DatabaseReference tasksRef;

    public NewTaskFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_new_task, container, false);

        Bundle bundle = getArguments();
        if (bundle != null) {
            user = (User) bundle.getSerializable("user");
            pet = (Pet) bundle.getSerializable("pet");
            usersList = (ArrayList<User>) bundle.getSerializable("usersList");
        }

        initComponents();
        setupDateAndTimePickers();
        setupResponsibleSpinner();

        // Initialize Firebase
        database = FirebaseDatabase.getInstance();
        tasksRef = database.getReference("Tasks");

        // Set up button listeners
        bCreateNewTask.setOnClickListener(view -> createNewTask());
        bCancelNewTask.setOnClickListener(view -> goBackToTasksFragment());

        bAddRecurringTask.setOnClickListener(v -> {
            NewRecurringTaskFragment recurringFragment = NewRecurringTaskFragment.newInstance();

            // Pass user, pet, and usersList to the recurring fragment
            Bundle bundleRecurring = new Bundle();
            bundleRecurring.putSerializable("user", user);
            bundleRecurring.putSerializable("pet", pet);
            bundleRecurring.putSerializable("usersList", usersList);
            recurringFragment.setArguments(bundleRecurring);

            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.flHome, recurringFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        return view;
    }

    private void initComponents() {
        etDate = view.findViewById(R.id.etDate);
        etTime = view.findViewById(R.id.etTime);
        etTaskBody = view.findViewById(R.id.etTaskBody);
        spinnerResponsible = view.findViewById(R.id.spinnerResponsible);
        bCreateNewTask = view.findViewById(R.id.bCreateNewTask);
        bCancelNewTask = view.findViewById(R.id.bCancelNewTask);
        bAddRecurringTask = view.findViewById(R.id.bAddRecurringTask);
        context = getContext();
    }

    private void setupDateAndTimePickers() {
        etDate.setOnClickListener(dateTask -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(context, (view, yearSelected, monthSelected, daySelected) -> {
                String selectedDate = String.format("%04d-%02d-%02d", yearSelected, monthSelected + 1, daySelected);
                etDate.setText(selectedDate);
            }, year, month, day);

            datePickerDialog.show();
        });

        etTime.setOnClickListener(timeTask -> {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(context, (view, hourOfDay, minuteOfHour) -> {
                String selectedTime = String.format("%02d:%02d", hourOfDay, minuteOfHour);
                etTime.setText(selectedTime);
            }, hour, minute, true);

            timePickerDialog.show();
        });
    }

    private void setupResponsibleSpinner() {
        if (usersList != null) {
            ArrayList<String> userNames = new ArrayList<>();
            userNames.add("No One"); // Option to leave the task unassigned

            for (User user : usersList) {
                userNames.add(user.getName());
            }

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, userNames);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerResponsible.setAdapter(spinnerAdapter);
        } else {
            Toast.makeText(getContext(), "No users available", Toast.LENGTH_SHORT).show();
        }
    }

    private void createNewTask() {
        String taskName = etTaskBody.getText().toString();
        String dueDate = etDate.getText().toString();
        String dueTime = etTime.getText().toString();
        String responsibleUserName = spinnerResponsible.getSelectedItem().toString();
        String assignedUserId = "";  // Default: No one assigned

        if (taskName.isEmpty() || dueDate.isEmpty() || dueTime.isEmpty()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!responsibleUserName.equals("No One")) {
            User responsibleUser = getResponsibleUser(responsibleUserName);
            if (responsibleUser != null) {
                assignedUserId = responsibleUser.getUserId();
            }
        }

        Task newTask = new Task(taskName, pet.getPetId(), assignedUserId, dueDate, dueTime, "pending");

        String taskId = tasksRef.push().getKey();
        tasksRef.child(taskId).setValue(newTask)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Task created successfully", Toast.LENGTH_SHORT).show();
                    clearFields();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error creating task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private User getResponsibleUser(String responsibleUserName) {
        for (User u : usersList) {
            if (u.getName().equals(responsibleUserName)) {
                return u;
            }
        }
        return null;
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
        etDate.setText("");
        etTime.setText("");
        etTaskBody.setText("");
        spinnerResponsible.setSelection(0);
    }
}
