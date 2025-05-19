package com.example.petcare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> tasks;
    private String currentUserId;
    private final boolean isAdmin;
    private List<User> usersList;
    private OnTaskCompleteListener completeListener;
    private OnTaskEditListener editListener;
    private OnTaskDeleteListener deleteListener;

    public interface OnTaskCompleteListener {
        void onTaskComplete(Task task);
    }

    public interface OnTaskEditListener {
        void onTaskEdit(Task task);
    }

    public interface OnTaskDeleteListener {
        void onTaskDelete(Task task);
    }

    public TaskAdapter(List<Task> tasks, String currentUserId, boolean isAdmin, List<User> usersList,
                       OnTaskCompleteListener completeListener,
                       OnTaskEditListener editListener,
                       OnTaskDeleteListener deleteListener) {
        this.tasks = tasks;
        this.currentUserId = currentUserId;
        this.isAdmin = isAdmin;
        this.usersList = usersList;
        this.completeListener = completeListener;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    public void updateData(List<Task> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.tvTaskName.setText(task.getTaskName());

        // For recurring tasks:
        if (!"none".equalsIgnoreCase(task.getRecurrenceType())) {
            // Display the recurring type.
            if (task.getRecurrenceType().equalsIgnoreCase("daily")) {
                holder.tvDueDate.setText("Daily");
            } else {
                String formattedDay = task.getRecurrenceType().substring(0, 1).toUpperCase()
                        + task.getRecurrenceType().substring(1).toLowerCase();
                holder.tvDueDate.setText(formattedDay);
            }
            // Hide status and responsible name.
            holder.tvStatus.setVisibility(View.GONE);
            holder.tvResponsibleName.setVisibility(View.GONE);
            // Recurring tasks do not have edit or complete buttons.
            holder.btnComplete.setVisibility(View.GONE);
            holder.btnEditTask.setVisibility(View.GONE);
        } else {
            // One-time task: show due date and status.
            // Compute today's date.
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Calendar.getInstance().getTime());
            holder.tvDueDate.setText("Due: " + task.getDueDate());
            holder.tvStatus.setVisibility(View.VISIBLE);
            // Check if the task is overdue.
            boolean isOverdue = task.getDueDate() != null && task.getDueDate().compareTo(today) < 0;
            if (isOverdue) {
                holder.tvStatus.setText("Status: Overdue");
                // For overdue tasks, hide both complete and edit buttons.
                holder.btnComplete.setVisibility(View.GONE);
                holder.btnEditTask.setVisibility(View.GONE);
            } else {
                holder.tvStatus.setText("Status: " + task.getStatus());
                // Show "Complete" button if task is pending and assigned to the current user.
                if ("pending".equalsIgnoreCase(task.getStatus()) && task.getAssignedUserId().equals(currentUserId)) {
                    holder.btnComplete.setVisibility(View.VISIBLE);
                    holder.btnComplete.setOnClickListener(v -> {
                        holder.btnComplete.setEnabled(false);
                        if (completeListener != null) {
                            completeListener.onTaskComplete(task);
                        }
                    });
                } else {
                    holder.btnComplete.setVisibility(View.GONE);
                }
                // Show "Edit" button only if a responsible owner is assigned.
                if (task.getAssignedUserId() != null && !task.getAssignedUserId().trim().isEmpty()) {
                    holder.btnEditTask.setVisibility(View.VISIBLE);
                    holder.btnEditTask.setOnClickListener(v -> {
                        if (editListener != null) {
                            editListener.onTaskEdit(task);
                        }
                    });
                } else {
                    holder.btnEditTask.setVisibility(View.GONE);
                }
            }
            // Lookup and display the responsible owner's name.
            String responsibleName = "Unknown";
            for (User u : usersList) {
                if (u.getUserId().equals(task.getAssignedUserId())) {
                    responsibleName = u.getName();
                    break;
                }
            }
            holder.tvResponsibleName.setVisibility(View.VISIBLE);
            holder.tvResponsibleName.setText("Responsible: " + responsibleName);
        }

        holder.tvDueTime.setText(task.getDueTime());

        // Always show the Delete button.
        holder.btnDeleteTask.setVisibility(View.VISIBLE);
        holder.btnDeleteTask.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onTaskDelete(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskName, tvResponsibleName, tvDueDate, tvDueTime, tvStatus;
        Button btnComplete, btnEditTask, btnDeleteTask;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskName = itemView.findViewById(R.id.tvTaskName);
            tvResponsibleName = itemView.findViewById(R.id.tvResponsibleName);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            tvDueTime = itemView.findViewById(R.id.tvDueTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnComplete = itemView.findViewById(R.id.btnComplete);
            btnEditTask = itemView.findViewById(R.id.btnEditTask);
            btnDeleteTask = itemView.findViewById(R.id.btnDeleteTask);
        }
    }
}
