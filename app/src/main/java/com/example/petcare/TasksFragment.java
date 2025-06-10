package com.example.petcare;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class TasksFragment extends Fragment {

    private Spinner filterSpinner;
    private Spinner recurringSpinner; // New spinner for recurring tasks filter
    private EditText etFilterDate, etSearchTask;
    private RecyclerView recyclerViewTasks;
    private ImageButton ibNewTask;
    private Button btnDeleteAllOverdue;
    private TaskAdapter taskAdapter;
    private ArrayList<Task> allTasksList = new ArrayList<>();

    private DatabaseReference tasksRef;
    private DatabaseReference usersRef; // For updating user info

    // Data passed from previous fragments
    private User user;
    private Pet pet;
    private ArrayList<User> usersList;

    public TasksFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        // Retrieve data from arguments
        Bundle bundle = getArguments();
        if (bundle != null) {
            user = (User) bundle.getSerializable("user");
            pet = (Pet) bundle.getSerializable("pet");
            usersList = (ArrayList<User>) bundle.getSerializable("usersList");
        }

        // Get UI components from layout
        filterSpinner = view.findViewById(R.id.filterSpinner);
        recurringSpinner = view.findViewById(R.id.recurringSpinner); // New spinner
        etFilterDate = view.findViewById(R.id.etFilterDate);
        etSearchTask = view.findViewById(R.id.etSearchTask);
        recyclerViewTasks = view.findViewById(R.id.recyclerViewTasks);
        ibNewTask = view.findViewById(R.id.ibNewTask);
        btnDeleteAllOverdue = view.findViewById(R.id.btnDeleteAllOverdue);

        // Make the date filter EditText non-editable and show DatePicker on click
        etFilterDate.setFocusable(false);
        etFilterDate.setOnClickListener(v -> showDatePickerDialog());

        // Setup filter spinner with five options:
        // 0 - My Tasks, 1 - Recurring Tasks, 2 - All Tasks, 3 - Unassigned Tasks, 4 - Overdue Tasks.
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"My Tasks", "Recurring Tasks", "All Tasks", "Unassigned Tasks", "Overdue Tasks"});
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(spinnerAdapter);

        // Setup recurring spinner with options for recurring type filtering.
        ArrayAdapter<String> recurringAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"All", "Daily", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"});
        recurringAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        recurringSpinner.setAdapter(recurringAdapter);

        // Initially hide recurring spinner.
        recurringSpinner.setVisibility(View.GONE);

        // When the main filter changes, adjust visibility.
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 1) { // Recurring Tasks
                    etFilterDate.setVisibility(View.GONE);
                    recurringSpinner.setVisibility(View.VISIBLE);
                    btnDeleteAllOverdue.setVisibility(View.GONE);
                } else if (position == 4) { // Overdue Tasks
                    etFilterDate.setVisibility(View.VISIBLE);
                    recurringSpinner.setVisibility(View.GONE);
                    btnDeleteAllOverdue.setVisibility(View.VISIBLE);
                } else {
                    etFilterDate.setVisibility(View.VISIBLE);
                    recurringSpinner.setVisibility(View.GONE);
                    btnDeleteAllOverdue.setVisibility(View.GONE);
                }
                applyFilter(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // When user types in search field.
        etFilterDate.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(filterSpinner.getSelectedItemPosition());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        etSearchTask.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(filterSpinner.getSelectedItemPosition());
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        // Listen for changes on the recurring spinner to update the recurring tasks filter.
        recurringSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (filterSpinner.getSelectedItemPosition() == 1) {
                    applyFilter(1);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Setup RecyclerView.
        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        taskAdapter = new TaskAdapter(new ArrayList<>(), user.getUserId(), true, usersList,
                task -> markTaskCompleted(task),
                task -> editTask(task),
                task -> deleteTask(task));
        recyclerViewTasks.setAdapter(taskAdapter);

        // Initialize Firebase references and load tasks.
        tasksRef = FirebaseDatabase.getInstance().getReference("Tasks");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        loadTasks();

        // New Task Button - navigate to NewTaskFragment.
        ibNewTask.setOnClickListener(v -> {
            NewTaskFragment newTaskFragment = new NewTaskFragment();
            Bundle b = new Bundle();
            b.putSerializable("user", user);
            b.putSerializable("pet", pet);
            b.putSerializable("usersList", usersList);
            newTaskFragment.setArguments(b);
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.flHome, newTaskFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        // Setup delete all overdue tasks button
        btnDeleteAllOverdue.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                .setTitle("Delete All Overdue Tasks")
                .setMessage("Are you sure you want to delete all overdue tasks? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAllOverdueTasks())
                .setNegativeButton("Cancel", null)
                .show();
        });

        return view;
    }

    // Load tasks from Firebase for the current pet.
    private void loadTasks() {
        tasksRef.addValueEventListener(new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTasksList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Task task = ds.getValue(Task.class);
                    if (task != null && task.getPetId().equals(pet.getPetId())) {
                        task.setTaskId(ds.getKey());
                        allTasksList.add(task);
                    }
                }
                Collections.sort(allTasksList, new Comparator<Task>() {
                    @Override
                    public int compare(Task t1, Task t2) {
                        return t1.getDueDate().compareTo(t2.getDueDate());
                    }
                });
                applyFilter(filterSpinner.getSelectedItemPosition());
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error loading tasks", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Apply filtering based on the selected main filter option.
    // Spinner options:
    // 0 - My Tasks: non-recurring tasks assigned to the logged user that are upcoming.
    // 1 - Recurring Tasks: now filtered by recurring type (using recurringSpinner) and search text.
    // 2 - All Tasks: non-recurring upcoming tasks.
    // 3 - Unassigned Tasks: non-recurring unassigned upcoming tasks.
    // 4 - Overdue Tasks: non-recurring tasks overdue.
    private void applyFilter(int filterOption) {
        List<Task> filteredList = new ArrayList<>();
        String today = getTodayString();
        String searchQuery = etSearchTask.getText().toString().trim().toLowerCase();
        String filterDate = etFilterDate.getText().toString().trim();

        for (Task t : allTasksList) {
            // Skip completed tasks.
            if ("completed".equalsIgnoreCase(t.getStatus())) continue;

            boolean matchesSearch = true;
            if (!TextUtils.isEmpty(searchQuery)) {
                if (t.getTaskName() == null || !t.getTaskName().toLowerCase().contains(searchQuery)) {
                    matchesSearch = false;
                }
            }

            // For non-recurring tasks, determine if the due date is upcoming or overdue.
            boolean isUpcoming = false;
            boolean isOverdue = false;
            if ("none".equalsIgnoreCase(t.getRecurrenceType()) && t.getDueDate() != null) {
                int cmp = t.getDueDate().compareTo(today);
                isUpcoming = cmp >= 0;  // Changed from cmp > 0 to include today's tasks
                isOverdue = cmp < 0;
            }

            switch (filterOption) {
                case 0: // My Tasks: non-recurring tasks with assignedUserId equal to logged user; upcoming only.
                    if ("none".equalsIgnoreCase(t.getRecurrenceType()) &&
                            t.getAssignedUserId() != null &&
                            t.getAssignedUserId().equals(user.getUserId()) &&
                            isUpcoming) {
                        boolean matchesDate = true;
                        if (!TextUtils.isEmpty(filterDate)) {
                            if (t.getDueDate() == null || !t.getDueDate().equals(filterDate)) {
                                matchesDate = false;
                            }
                        }
                        if (matchesSearch && matchesDate) {
                            filteredList.add(t);
                        }
                    }
                    break;
                case 1: // Recurring Tasks: filter using recurringSpinner.
                    if (!"none".equalsIgnoreCase(t.getRecurrenceType()) && matchesSearch) {
                        String recurringType = recurringSpinner.getSelectedItem().toString();
                        if ("All".equalsIgnoreCase(recurringType)) {
                            filteredList.add(t);
                        } else if ("Daily".equalsIgnoreCase(recurringType)) {
                            if ("daily".equalsIgnoreCase(t.getRecurrenceType())) {
                                filteredList.add(t);
                            }
                        } else {
                            // For weekly recurring tasks, assume the dueDate field holds the day of the week.
                            if (t.getDueDate() != null && t.getDueDate().equalsIgnoreCase(recurringType)) {
                                filteredList.add(t);
                            }
                        }
                    }
                    break;
                case 2: // All Tasks: non-recurring tasks; upcoming only.
                    if ("none".equalsIgnoreCase(t.getRecurrenceType()) && isUpcoming) {
                        boolean matchesDate = true;
                        if (!TextUtils.isEmpty(filterDate)) {
                            if (t.getDueDate() == null || !t.getDueDate().equals(filterDate)) {
                                matchesDate = false;
                            }
                        }
                        if (matchesSearch && matchesDate) {
                            filteredList.add(t);
                        }
                    }
                    break;
                case 3: // Unassigned Tasks: non-recurring tasks with no assigned user; upcoming only.
                    if ("none".equalsIgnoreCase(t.getRecurrenceType()) &&
                            (t.getAssignedUserId() == null || t.getAssignedUserId().trim().isEmpty()) &&
                            isUpcoming) {
                        boolean matchesDate = true;
                        if (!TextUtils.isEmpty(filterDate)) {
                            if (t.getDueDate() == null || !t.getDueDate().equals(filterDate)) {
                                matchesDate = false;
                            }
                        }
                        if (matchesSearch && matchesDate) {
                            filteredList.add(t);
                        }
                    }
                    break;
                case 4: // Overdue Tasks: non-recurring tasks that are overdue.
                    if ("none".equalsIgnoreCase(t.getRecurrenceType()) && isOverdue) {
                        boolean matchesDate = true;
                        if (!TextUtils.isEmpty(filterDate)) {
                            if (t.getDueDate() == null || !t.getDueDate().equals(filterDate)) {
                                matchesDate = false;
                            }
                        }
                        if (matchesSearch && matchesDate) {
                            filteredList.add(t);
                        }
                    }
                    break;
            }
        }
        taskAdapter.updateData(filteredList);
    }

    // Helper method to get today's date in "YYYY-MM-DD" format.
    private String getTodayString() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1; // Calendar.MONTH is zero-based.
        int day = cal.get(Calendar.DAY_OF_MONTH);
        return String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, day);
    }

    // Mark a task as completed.
    private void markTaskCompleted(Task task) {
        String today = getTodayString();
        if (!today.equals(task.getDueDate())) {
            Toast.makeText(getContext(), "Task is not due today", Toast.LENGTH_SHORT).show();
            return;
        }
        task.setStatus("completed");
        tasksRef.child(task.getTaskId()).setValue(task)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Task completed", Toast.LENGTH_SHORT).show();
                    if (task.getAssignedUserId().equals(user.getUserId())) {
                        int currentCount = (user.getNumOfTasks() != null) ? user.getNumOfTasks() : 0;
                        user.setNumOfTasks(currentCount + 1);
                        usersRef.child(user.getUserId()).child("numOfTasks").setValue(user.getNumOfTasks());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Updated editTask method remains unchanged.
    private void editTask(Task task) {
        String[] options = new String[usersList.size() + 1];
        options[0] = "Unassigned";
        for (int i = 0; i < usersList.size(); i++) {
            options[i + 1] = usersList.get(i).getName();
        }
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Select Responsible Owner")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        task.setAssignedUserId("");
                    } else {
                        String newUserId = usersList.get(which - 1).getUserId();
                        task.setAssignedUserId(newUserId);
                    }
                    tasksRef.child(task.getTaskId()).setValue(task)
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Task updated", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .create()
                .show();
    }

    // Allow deleting a task.
    private void deleteTask(Task task) {
        tasksRef.child(task.getTaskId()).removeValue()
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Task deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Add new method to delete all overdue tasks
    private void deleteAllOverdueTasks() {
        String today = getTodayString();
        List<Task> overdueTasks = new ArrayList<>();
        
        // Collect all overdue tasks
        for (Task task : allTasksList) {
            if ("none".equalsIgnoreCase(task.getRecurrenceType()) && 
                task.getDueDate() != null && 
                task.getDueDate().compareTo(today) < 0 &&
                !"completed".equalsIgnoreCase(task.getStatus())) {
                overdueTasks.add(task);
            }
        }

        // If no overdue tasks, show message and return
        if (overdueTasks.isEmpty()) {
            Toast.makeText(getContext(), "No overdue tasks to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        // Delete all overdue tasks
        int totalTasks = overdueTasks.size();
        final int[] deletedCount = {0};
        
        for (Task task : overdueTasks) {
            tasksRef.child(task.getTaskId()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    deletedCount[0]++;
                    if (deletedCount[0] == totalTasks) {
                        Toast.makeText(getContext(), 
                            "Successfully deleted " + totalTasks + " overdue tasks", 
                            Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), 
                    "Error deleting task: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show());
        }
    }

    private void showDatePickerDialog() {
        // Get Current Date
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                (view, year1, monthOfYear, dayOfMonth) -> {
                    // Format the date as YYYY-MM-DD
                    String selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                            year1, monthOfYear + 1, dayOfMonth);
                    etFilterDate.setText(selectedDate);
                    // Apply the filter with the new date
                    applyFilter(filterSpinner.getSelectedItemPosition());
                }, year, month, day);

        // Add a clear button to remove the date filter
        datePickerDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Clear", (dialog, which) -> {
            etFilterDate.setText("");
            applyFilter(filterSpinner.getSelectedItemPosition());
        });

        datePickerDialog.show();
    }
}
