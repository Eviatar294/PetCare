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
                        
                        DatabaseReference tasksRef = FirebaseDatabase.getInstance().getReference("Tasks");
                        int instancesCreated = 0;
                        
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Task template = ds.getValue(Task.class);
                            if (template != null && petId.equals(template.getPetId()) && 
                                isRecurringTask(template.getRecurrenceType())) {
                                
                                String recurrenceType = template.getRecurrenceType();
                                String lastGenerated = template.getLastGeneratedDate();
                                Calendar startDate = Calendar.getInstance();
                                boolean isNewTemplate = (lastGenerated == null || lastGenerated.isEmpty());
                                
                                Log.d(TAG, "=== Processing template: " + template.getTaskName() + " ===");
                                Log.d(TAG, "RecurrenceType: " + recurrenceType);
                                Log.d(TAG, "LastGeneratedDate: " + lastGenerated);
                                Log.d(TAG, "IsNewTemplate: " + isNewTemplate);
                                Log.d(TAG, "Today: " + sdf.format(Calendar.getInstance().getTime()));
                                
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
                                
                                // Generate instances based on recurrence type
                                String latestGenerated = lastGenerated;
                                int instancesForThisTemplate = 0;
                                
                                // Calculate the end date: today + 6 days (7 days total including today)
                                Calendar endDate = Calendar.getInstance();
                                endDate.add(Calendar.DAY_OF_MONTH, 6);
                                String endDateStr = sdf.format(endDate.getTime());
                                
                                Log.d(TAG, "End date calculated: " + endDateStr);
                                
                                // Determine the start date for generation
                                Calendar generationStartDate;
                                if (!isNewTemplate) {
                                    // Start from the day after lastGeneratedDate
                                    try {
                                        generationStartDate = Calendar.getInstance();
                                        generationStartDate.setTime(sdf.parse(lastGenerated));
                                        generationStartDate.add(Calendar.DAY_OF_MONTH, 1);
                                        Log.d(TAG, "Existing template '" + template.getTaskName() + "': generating from " + sdf.format(generationStartDate.getTime()) + " to " + endDateStr);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error parsing lastGeneratedDate: " + lastGenerated + ", starting from today", e);
                                        generationStartDate = Calendar.getInstance();
                                    }
                                } else {
                                    // New template: start from today
                                    generationStartDate = Calendar.getInstance();
                                    Log.d(TAG, "New template '" + template.getTaskName() + "': generating from " + sdf.format(generationStartDate.getTime()) + " to " + endDateStr);
                                }
                                
                                // Skip generation if start date is already beyond end date
                                if (generationStartDate.after(endDate)) {
                                    Log.d(TAG, "Skipping template '" + template.getTaskName() + "': start date " + sdf.format(generationStartDate.getTime()) + " is beyond end date " + endDateStr);
                                    continue;
                                }
                                
                                Log.d(TAG, "Proceeding with generation for '" + template.getTaskName() + "'");
                                
                                if ("daily".equalsIgnoreCase(recurrenceType)) {
                                    // Daily tasks: create instance for each day from start to end
                                    Calendar currentDate = (Calendar) generationStartDate.clone();
                                    while (!currentDate.after(endDate)) {
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
                                            instancesForThisTemplate++;
                                            latestGenerated = dateStr;
                                            Log.d(TAG, "Created daily task instance: " + template.getTaskName() + " for " + dateStr);
                                        }
                                        
                                        currentDate.add(Calendar.DAY_OF_MONTH, 1);
                                    }
                                } else if (isWeeklyRecurrenceType(recurrenceType)) {
                                    // Weekly tasks: create instance for matching days from start to end
                                    int targetDayOfWeek = getDayOfWeekFromRecurrenceType(recurrenceType);
                                    if (targetDayOfWeek != -1) {
                                        Calendar currentDate = (Calendar) generationStartDate.clone();
                                        while (!currentDate.after(endDate)) {
                                            if (currentDate.get(Calendar.DAY_OF_WEEK) == targetDayOfWeek) {
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
                                                    instancesForThisTemplate++;
                                                    latestGenerated = dateStr;
                                                    Log.d(TAG, "Created weekly task instance: " + template.getTaskName() + " for " + dateStr + " (" + recurrenceType + ")");
                                                }
                                            }
                                            currentDate.add(Calendar.DAY_OF_MONTH, 1);
                                        }
                                    }
                                } else {
                                    Log.w(TAG, "Unknown recurrence type: " + recurrenceType + " for task: " + template.getTaskName());
                                }
                                
                                // Update lastGeneratedDate to the end date to mark this period as processed
                                if (instancesForThisTemplate > 0) {
                                    // If instances were created, set to end date
                                    latestGenerated = endDateStr;
                                    Log.d(TAG, "Template '" + template.getTaskName() + "': generated " + instancesForThisTemplate + " instances, updating lastGeneratedDate to " + endDateStr);
                                } else if (isNewTemplate) {
                                    // For new templates with no instances, still set lastGeneratedDate to prevent future duplicate checks
                                    latestGenerated = endDateStr;
                                    Log.d(TAG, "New template '" + template.getTaskName() + "': no instances generated, but setting lastGeneratedDate to " + endDateStr + " to mark period as processed");
                                }
                                
                                // Update the template's lastGeneratedDate
                                if (latestGenerated != null && !latestGenerated.equals(lastGenerated)) {
                                    template.setLastGeneratedDate(latestGenerated);
                                    
                                    // Create final variables for lambda
                                    final String finalLatestGenerated = latestGenerated;
                                    final String finalTaskName = template.getTaskName();
                                    
                                    tasksRef.child(ds.getKey()).setValue(template)
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d(TAG, "SUCCESS: Firebase updated for template '" + finalTaskName + "' lastGeneratedDate = '" + finalLatestGenerated + "'");
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "FAILED: Firebase update failed for template '" + finalTaskName + "': " + e.getMessage());
                                            });
                                    Log.d(TAG, "UPDATED Firebase: Template '" + template.getTaskName() + "' lastGeneratedDate changed from '" + lastGenerated + "' to '" + latestGenerated + "'");
                                } else {
                                    Log.d(TAG, "NO UPDATE: Template '" + template.getTaskName() + "' lastGeneratedDate remains '" + lastGenerated + "'");
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

    /**
     * Helper method to check if a task is a recurring task
     */
    private static boolean isRecurringTask(String recurrenceType) {
        if (recurrenceType == null) return false;
        return "daily".equalsIgnoreCase(recurrenceType) || 
               "MONDAY".equalsIgnoreCase(recurrenceType) ||
               "TUESDAY".equalsIgnoreCase(recurrenceType) ||
               "WEDNESDAY".equalsIgnoreCase(recurrenceType) ||
               "THURSDAY".equalsIgnoreCase(recurrenceType) ||
               "FRIDAY".equalsIgnoreCase(recurrenceType) ||
               "SATURDAY".equalsIgnoreCase(recurrenceType) ||
               "SUNDAY".equalsIgnoreCase(recurrenceType);
    }

    /**
     * Helper method to convert recurrence type to Calendar day of week constant
     */
    private static int getDayOfWeekFromRecurrenceType(String recurrenceType) {
        if (recurrenceType == null) return -1;
        switch (recurrenceType.toUpperCase()) {
            case "SUNDAY": return Calendar.SUNDAY;
            case "MONDAY": return Calendar.MONDAY;
            case "TUESDAY": return Calendar.TUESDAY;
            case "WEDNESDAY": return Calendar.WEDNESDAY;
            case "THURSDAY": return Calendar.THURSDAY;
            case "FRIDAY": return Calendar.FRIDAY;
            case "SATURDAY": return Calendar.SATURDAY;
            default: return -1;
        }
    }

    private static boolean isWeeklyRecurrenceType(String recurrenceType) {
        if (recurrenceType == null) return false;
        return "MONDAY".equalsIgnoreCase(recurrenceType) ||
               "TUESDAY".equalsIgnoreCase(recurrenceType) ||
               "WEDNESDAY".equalsIgnoreCase(recurrenceType) ||
               "THURSDAY".equalsIgnoreCase(recurrenceType) ||
               "FRIDAY".equalsIgnoreCase(recurrenceType) ||
               "SATURDAY".equalsIgnoreCase(recurrenceType) ||
               "SUNDAY".equalsIgnoreCase(recurrenceType);
    }
} 