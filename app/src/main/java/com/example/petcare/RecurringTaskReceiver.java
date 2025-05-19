package com.example.petcare;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class RecurringTaskReceiver extends BroadcastReceiver {
    private static final String TAG = "RecurringTaskReceiver";
    private static final String CHANNEL_ID = "RecurringTaskChannel";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "RecurringTaskReceiver triggered");
        processRecurringTasks(context);
        scheduleNextAlarm(context);
        showNotification(context);
    }

    private void processRecurringTasks(Context context) {
        final DatabaseReference tasksRef = FirebaseDatabase.getInstance().getReference("Tasks");
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        tasksRef.addListenerForSingleValueEvent(new ValueEventListener(){
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Task task = ds.getValue(Task.class);
                    if (task == null) continue;
                    String recurrence = task.getRecurrenceType();

                    // Process only recurring tasks (daily or weekly)
                    if ("daily".equalsIgnoreCase(recurrence)) {
                        try {
                            Date currentDue = sdf.parse(task.getDueDate());
                            // Calculate the start (Sunday) of next week
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(currentDue);
                            cal.add(Calendar.WEEK_OF_YEAR, 1);
                            cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                            Date nextWeekSunday = cal.getTime();
                            String newTemplateDueDate = sdf.format(nextWeekSunday);

                            // Create clones for each day of next week (Sunday to Saturday)
                            Calendar cloneCal = Calendar.getInstance();
                            cloneCal.setTime(nextWeekSunday);
                            for (int i = 0; i < 7; i++) {
                                String cloneDueDate = sdf.format(cloneCal.getTime());
                                Task clone = new Task(
                                        task.getTaskName(),
                                        task.getPetId(),
                                        task.getAssignedUserId(), // or keep empty if desired
                                        cloneDueDate,
                                        task.getDueTime(),
                                        "pending"
                                );
                                // The clone is a one-time task, so recurrenceType becomes "none"
                                clone.setRecurrenceType("none");
                                String cloneId = tasksRef.push().getKey();
                                if (cloneId != null) {
                                    tasksRef.child(cloneId).setValue(clone);
                                }
                                cloneCal.add(Calendar.DAY_OF_MONTH, 1);
                            }
                            // Update the recurring template's dueDate to next week's Sunday
                            task.setDueDate(newTemplateDueDate);
                            tasksRef.child(ds.getKey()).setValue(task);
                        } catch (ParseException e) {
                            Log.e(TAG, "Date parse error: " + e.getMessage());
                        }
                    } else if ("weekly".equalsIgnoreCase(recurrence)) {
                        try {
                            Date currentDue = sdf.parse(task.getDueDate());
                            // Clone one instance with current dueDate
                            Task clone = new Task(
                                    task.getTaskName(),
                                    task.getPetId(),
                                    task.getAssignedUserId(),
                                    task.getDueDate(),
                                    task.getDueTime(),
                                    "pending"
                            );
                            clone.setRecurrenceType("none");
                            String cloneId = tasksRef.push().getKey();
                            if (cloneId != null) {
                                tasksRef.child(cloneId).setValue(clone);
                            }
                            // Update the recurring template's dueDate by adding 7 days
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(currentDue);
                            cal.add(Calendar.DAY_OF_MONTH, 7);
                            String newDueDate = sdf.format(cal.getTime());
                            task.setDueDate(newDueDate);
                            tasksRef.child(ds.getKey()).setValue(task);
                        } catch (ParseException e) {
                            Log.e(TAG, "Date parse error: " + e.getMessage());
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error processing recurring tasks: " + error.getMessage());
            }
        });
    }

    private void scheduleNextAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RecurringTaskReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar cal = Calendar.getInstance();
        // Set calendar to this Saturday 8pm
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
        cal.set(Calendar.HOUR_OF_DAY, 20);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // If this time has already passed today, schedule for next week
        if (Calendar.getInstance().after(cal)) {
            cal.add(Calendar.WEEK_OF_YEAR, 1);
        }

        long triggerAtMillis = cal.getTimeInMillis();
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            Log.d(TAG, "Next recurring task alarm scheduled for: " + cal.getTime());
        }
    }

    private void showNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // Create notification channel for Android O and above
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Recurring Task Channel";
            String description = "Notifications for recurring tasks";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Recurring Tasks Updated")
                .setContentText("New task instances have been added for the upcoming week.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }
}
