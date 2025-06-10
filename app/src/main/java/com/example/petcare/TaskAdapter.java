package com.example.petcare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_TASK = 1;

    private List<Object> items; // Can be either String (header) or Task
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
        this.currentUserId = currentUserId;
        this.isAdmin = isAdmin;
        this.usersList = usersList;
        this.completeListener = completeListener;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
        processTasksIntoSections(tasks);
    }

    private void processTasksIntoSections(List<Task> tasks) {
        items = new ArrayList<>();
        List<Task> todayTasks = new ArrayList<>();
        List<Task> futureTasks = new ArrayList<>();
        List<Task> recurringTasks = new ArrayList<>();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        // Sort tasks into today, future, and recurring
        for (Task task : tasks) {
            if (!"none".equalsIgnoreCase(task.getRecurrenceType())) {
                recurringTasks.add(task);
            } else if (task.getDueDate() != null) {
                if (task.getDueDate().equals(today)) {
                    todayTasks.add(task);
                } else if (task.getDueDate().compareTo(today) > 0) {
                    futureTasks.add(task);
                }
            }
        }

        // Add today's tasks section if there are any
        if (!todayTasks.isEmpty()) {
            items.add("Today's Tasks");
            items.addAll(todayTasks);
        }

        // Add future tasks section if there are any non-recurring tasks
        if (!futureTasks.isEmpty()) {
            items.add("Upcoming Tasks");
            items.addAll(futureTasks);
        }

        // Add recurring tasks without a header
        if (!recurringTasks.isEmpty()) {
            items.addAll(recurringTasks);
        }
    }

    public void updateData(List<Task> newTasks) {
        processTasksIntoSections(newTasks);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_TASK;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_task_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_task, parent, false);
            return new TaskViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).headerText.setText((String) items.get(position));
        } else if (holder instanceof TaskViewHolder) {
            TaskViewHolder taskHolder = (TaskViewHolder) holder;
            Task task = (Task) items.get(position);
            bindTaskViewHolder(taskHolder, task);
        }
    }

    private void bindTaskViewHolder(TaskViewHolder holder, Task task) {
        holder.tvTaskName.setText(task.getTaskName());

        // Set click listener for expanding/collapsing
        holder.layoutMain.setOnClickListener(v -> {
            boolean isExpanded = holder.layoutExpandable.getVisibility() == View.VISIBLE;
            holder.layoutExpandable.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
        });

        // For recurring tasks:
        if (!"none".equalsIgnoreCase(task.getRecurrenceType())) {
            if (task.getRecurrenceType().equalsIgnoreCase("daily")) {
                holder.tvDueDate.setText("Daily");
            } else {
                String formattedDay = task.getRecurrenceType().substring(0, 1).toUpperCase()
                        + task.getRecurrenceType().substring(1).toLowerCase();
                holder.tvDueDate.setText(formattedDay);
            }
            holder.tvStatus.setVisibility(View.GONE);
            holder.btnComplete.setVisibility(View.GONE);
            holder.btnEditTask.setVisibility(View.GONE);
            holder.btnDeleteTask.setVisibility(View.VISIBLE);
            holder.tvResponsibleName.setVisibility(View.GONE);
        } else {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Calendar.getInstance().getTime());
            holder.tvDueDate.setText("Due: " + task.getDueDate());
            holder.tvStatus.setVisibility(View.VISIBLE);
            
            boolean isOverdue = task.getDueDate() != null && task.getDueDate().compareTo(today) < 0;
            if (isOverdue) {
                holder.tvStatus.setText("Status: Overdue");
                holder.btnComplete.setVisibility(View.GONE);
                holder.btnEditTask.setVisibility(View.GONE);
            } else {
                holder.tvStatus.setText("Status: " + task.getStatus());
                if ("pending".equalsIgnoreCase(task.getStatus()) && 
                    task.getAssignedUserId() != null && 
                    task.getAssignedUserId().equals(currentUserId)) {
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
                
                holder.btnEditTask.setVisibility(View.VISIBLE);
                holder.btnEditTask.setOnClickListener(v -> {
                    if (editListener != null) {
                        editListener.onTaskEdit(task);
                    }
                });
            }

            holder.tvResponsibleName.setVisibility(View.VISIBLE);
            String responsibleName = "Unassigned";
            if (task.getAssignedUserId() != null && !task.getAssignedUserId().trim().isEmpty()) {
                for (User u : usersList) {
                    if (u.getUserId().equals(task.getAssignedUserId())) {
                        responsibleName = u.getName();
                        break;
                    }
                }
            }
            holder.tvResponsibleName.setText("Responsible: " + responsibleName);
        }

        holder.tvDueTime.setText(task.getDueTime());

        holder.btnDeleteTask.setVisibility(View.VISIBLE);
        holder.btnDeleteTask.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onTaskDelete(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerText;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerText = (TextView) itemView;
        }
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskName, tvResponsibleName, tvDueDate, tvDueTime, tvStatus;
        ImageButton btnComplete, btnEditTask, btnDeleteTask;
        View layoutMain;
        View layoutExpandable;

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
            layoutMain = itemView.findViewById(R.id.layoutMain);
            layoutExpandable = itemView.findViewById(R.id.layoutExpandable);
        }
    }
}
