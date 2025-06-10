package com.example.petcare;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.bumptech.glide.Glide;
import com.google.firebase.database.FirebaseDatabase;
import java.io.File;

public class DialogImage extends DialogFragment {

    private User user;
    private Pet pet;
    private ImagePickerHelper imagePickerHelper;
    private SettingFragment settingFragment;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Get the user and pet objects from arguments
        Bundle args = getArguments();
        if (args != null) {
            user = (User) args.getSerializable("user");
            pet = (Pet) args.getSerializable("pet");
        }

        // Get reference to SettingFragment
        if (getActivity() instanceof MainHomeUser) {
            MainHomeUser activity = (MainHomeUser) getActivity();
            settingFragment = (SettingFragment) activity.getSupportFragmentManager()
                .findFragmentById(R.id.flHome);
        }

        // Initialize ImagePickerHelper
        imagePickerHelper = new ImagePickerHelper(this, uri -> {
            if (settingFragment != null) {
                settingFragment.uploadImageToFirebase(uri);
            }
            dismiss();
        });

        // Create dialog using the existing layout
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_image, null);

        // Initialize views
        ImageView dialogImageView = view.findViewById(R.id.dialogImageView);
        Button btnTakeNewImage = view.findViewById(R.id.btnTakeNewImage);
        Button btnDeleteImage = view.findViewById(R.id.btnDeleteImage);

        // Load the current image
        if (pet != null && getContext() != null) {
            File petImageFile = new File(getContext().getFilesDir(), "pet_image.png");
            if (petImageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(petImageFile.getAbsolutePath());
                if (bitmap != null) {
                    Glide.with(this)
                            .load(bitmap)
                            .centerCrop()
                            .into(dialogImageView);
                }
            }
        }

        // Set click listeners
        btnTakeNewImage.setOnClickListener(v -> {
            imagePickerHelper.pickImage();
        });

        btnDeleteImage.setOnClickListener(v -> {
            if (getContext() != null) {
                AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(getContext());
                confirmBuilder.setTitle("Delete Image")
                    .setMessage("Are you sure you want to delete this image?")
                    .setPositiveButton("Yes", (dialogInterface, i) -> {
                        if (settingFragment != null && settingFragment.isAdded()) {
                            // Store reference to avoid any potential issues
                            final SettingFragment fragment = settingFragment;
                            // Dismiss first
                            dismiss();
                            // Then delete
                            fragment.deleteImage();
                        } else {
                            dismiss();
                        }
                    })
                    .setNegativeButton("No", (dialogInterface, i) -> {
                        dismiss();
                    })
                    .show();
            }
        });

        builder.setView(view);
        return builder.create();
    }
} 