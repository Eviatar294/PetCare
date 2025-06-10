package com.example.petcare;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class RecurringTaskGenerator {

    // Define your date format (assuming "yyyy-MM-dd" is used)
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    public static void generateRecurringTaskInstances(String petId) {
        final DatabaseReference tasksRef = FirebaseDatabase.getInstance().getReference("Tasks");

        // Query for tasks with this petId
        Query query = tasksRef.orderByChild("petId").equalTo(petId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get today's date and calculate one week ahead
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, 7);
                Date oneWeekLater = calendar.getTime();

                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());

                for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                    Task templateTask = taskSnapshot.getValue(Task.class);
                    if (templateTask == null) continue;

                    // Skip non-recurring tasks (regular one-time tasks)
                    if ("none".equalsIgnoreCase(templateTask.getRecurrenceType())) {
                        continue;
                    }

                    // Parse the template's dueDate (which stores the next scheduled occurrence)
                    Date nextDueDate;
                    try {
                        nextDueDate = sdf.parse(templateTask.getDueDate());
                    } catch (ParseException e) {
                        e.printStackTrace();
                        continue;
                    }

                    // Generate instance tasks as long as the template's dueDate is within the next 1 week
                    while (!nextDueDate.after(oneWeekLater)) {
                        // Create a new instance task based on the template
                        Task instanceTask = new Task(
                                templateTask.getTaskName(),
                                templateTask.getPetId(),
                                "",  // assignedUserId will be set later for the instance
                                sdf.format(nextDueDate), // use the current dueDate from the template
                                templateTask.getDueTime(),
                                "pending"
                        );
                        // Optionally, set recurrenceType to "none" for instance tasks so they aren't processed as templates
                        instanceTask.setRecurrenceType("none");

                        // Push the instance task to Firebase
                        String newTaskId = tasksRef.push().getKey();
                        tasksRef.child(newTaskId).setValue(instanceTask);

                        // Calculate the next occurrence date
                        Calendar nextCalendar = Calendar.getInstance();
                        nextCalendar.setTime(nextDueDate);
                        if ("daily".equalsIgnoreCase(templateTask.getRecurrenceType())) {
                            nextCalendar.add(Calendar.DAY_OF_YEAR, 1);
                        } else {
                            // For weekly recurring tasks, we assume the recurrenceType is a day of the week.
                            // So simply add 7 days.
                            nextCalendar.add(Calendar.DAY_OF_YEAR, 7);
                        }
                        nextDueDate = nextCalendar.getTime();

                        // Update the template's dueDate to the newly calculated next occurrence
                        String newDueDateStr = sdf.format(nextDueDate);
                        templateTask.setDueDate(newDueDateStr);
                        tasksRef.child(taskSnapshot.getKey()).setValue(templateTask);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Log the error if needed
            }
        });
    }
}
