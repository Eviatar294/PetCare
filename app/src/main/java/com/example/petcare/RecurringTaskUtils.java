package com.example.petcare;

import android.util.Log;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Utility class for managing recurring task instance generation.
 * 
 * Uses a smart approach with lastGeneratedDate field on recurring templates:
 * - Tracks the last date up to which instances were generated
 * - Only creates new instances beyond this date
 * - Respects user deletions (won't recreate deleted instances)
 * - Prevents duplicate generation across multiple app sessions
 */
public class RecurringTaskUtils {
    private static final String TAG = "RecurringTaskUtils";

    /**
     * Generates recurring task instances for the next 7 days based on lastGeneratedDate.
     * Only creates instances beyond the lastGeneratedDate to respect user deletions.
     */
    public static void generateRecurringTaskInstancesForNext7Days(String petId) {
        Log.d(TAG, "Generating recurring task instances for the next 7 days for petId: " + petId);
        
        FirebaseDatabase.getInstance()
                .getReference("Tasks")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        Calendar today = Calendar.getInstance();
                        
                        // We want to generate exactly 7 days starting from the appropriate start date
                        
                        DatabaseReference tasksRef = FirebaseDatabase.getInstance().getReference("Tasks");
                        int instancesCreated = 0;
                        
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Task template = ds.getValue(Task.class);
                            if (template != null && petId.equals(template.getPetId()) && 
                                "daily".equalsIgnoreCase(template.getRecurrenceType())) {
                                
                                String lastGenerated = template.getLastGeneratedDate();
                                Calendar startDate = Calendar.getInstance();
                                boolean isNewTemplate = (lastGenerated == null || lastGenerated.isEmpty());
                                
                                if (!isNewTemplate) {
                                    // Start generating from the day after lastGeneratedDate
                                    try {
                                        startDate.setTime(sdf.parse(lastGenerated));
                                        startDate.add(Calendar.DAY_OF_MONTH, 1);
                                        Log.d(TAG, "Existing template '" + template.getTaskName() + "': starting from day after lastGenerated: " + sdf.format(startDate.getTime()));
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error parsing lastGeneratedDate: " + lastGenerated + ", starting from today", e);
                                        startDate = Calendar.getInstance(); // Fallback to today
                                    }
                                } else {
                                    // New template: start from today
                                    startDate = Calendar.getInstance();
                                    Log.d(TAG, "New template '" + template.getTaskName() + "': starting from today: " + sdf.format(startDate.getTime()));
                                }
                                
                                // Generate instances starting from startDate
                                Calendar currentDate = (Calendar) startDate.clone();
                                String latestGenerated = lastGenerated; // Track the latest date we generate
                                
                                // Always try to ensure we have tasks for the next 7 days
                                int targetInstanceCount = 7;
                                
                                Calendar maxEndDate = Calendar.getInstance();
                                maxEndDate.add(Calendar.DAY_OF_MONTH, 6); // Today + 6 more days = 7 days total
                                
                                int daysGenerated = 0;
                                while (!currentDate.after(maxEndDate) && daysGenerated < targetInstanceCount) {
                                    String dateStr = sdf.format(currentDate.getTime());
                                    
                                    // Create instance for this date
                                    Task instance = new Task(
                                        template.getTaskName(),
                                        template.getPetId(),
                                        template.getAssignedUserId(),
                                        dateStr,
                                        template.getDueTime(),
                                        "pending"
                                    );
                                    instance.setRecurrenceType("none"); // Instances are one-time tasks
                                    
                                    String instanceId = tasksRef.push().getKey();
                                    if (instanceId != null) {
                                        tasksRef.child(instanceId).setValue(instance);
                                        instancesCreated++;
                                        daysGenerated++;
                                        latestGenerated = dateStr;
                                        Log.d(TAG, "Created task instance " + daysGenerated + "/7: " + template.getTaskName() + " for " + dateStr);
                                    }
                                    
                                    currentDate.add(Calendar.DAY_OF_MONTH, 1);
                                }
                                
                                // Update the template's lastGeneratedDate to the latest date we generated
                                Log.d(TAG, "Template '" + template.getTaskName() + "': generated " + daysGenerated + " instances");
                                if (latestGenerated != null && !latestGenerated.equals(lastGenerated)) {
                                    template.setLastGeneratedDate(latestGenerated);
                                    tasksRef.child(ds.getKey()).setValue(template);
                                    Log.d(TAG, "Updated lastGeneratedDate for template '" + template.getTaskName() + "' to: " + latestGenerated);
                                }
                            }
                        }
                        
                        Log.d(TAG, "Recurring task instance generation completed. Created " + instancesCreated + " new instances.");
                    }
                    
                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "Error generating recurring task instances: " + error.getMessage());
                    }
                });
    }
} 