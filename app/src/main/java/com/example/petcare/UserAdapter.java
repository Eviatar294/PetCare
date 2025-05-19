package com.example.petcare;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ArrayAdapter;

import java.util.List;

public class UserAdapter extends ArrayAdapter<User> {

    public UserAdapter(Context context, List<User> users) {
        super(context, 0, users);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_user, parent, false);
        }

        User user = getItem(position);

        // Get references to the views
        TextView userName = convertView.findViewById(R.id.userName);
        TextView numTasks = convertView.findViewById(R.id.numTasks);
        ImageView rankImage = convertView.findViewById(R.id.rankImage);

        if (user != null) {
            userName.setText(user.getName());
            numTasks.setText("Tasks: " + user.getNumOfTasks());

            // Set rank image for top 3 users
            if (position == 0) {
                rankImage.setImageResource(R.drawable.ic_gold_trophey);
                userName.setTextSize(20);
            } else if (position == 1) {
                rankImage.setImageResource(R.drawable.ic_silver_trophey);
            } else if (position == 2) {
                rankImage.setImageResource(R.drawable.ic_bronze_trophey);
            } else {
                rankImage.setVisibility(View.INVISIBLE);
            }
        }

        return convertView;
    }
}
