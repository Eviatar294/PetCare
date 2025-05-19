package com.example.petcare;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class ImagePickerHelper {

    private final Fragment fragment;
    private final ImagePickerCallback callback;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private Uri cameraImageUri;

    public interface ImagePickerCallback {
        void onImagePicked(Uri imageUri);
    }

    public ImagePickerHelper(Fragment fragment, ImagePickerCallback callback) {
        this.fragment = fragment;
        this.callback = callback;
        registerLaunchers();
        registerPermissionLauncher();
    }

    private void registerLaunchers() {
        galleryLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (callback != null && imageUri != null) {
                            callback.onImagePicked(imageUri);
                        }
                    }
                }
        );

        cameraLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (callback != null && cameraImageUri != null) {
                            callback.onImagePicked(cameraImageUri);
                        }
                    }
                }
        );
    }

    private void registerPermissionLauncher() {
        permissionLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean allGranted = true;
                    for (Boolean granted : result.values()) {
                        if (!granted) {
                            allGranted = false;
                            break;
                        }
                    }
                    if (allGranted) {
                        showImageSourceDialog();
                    } else {
                        Toast.makeText(fragment.getContext(), "Permissions not granted.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * Initiates the image picking process by checking for required permissions.
     * If the permissions are granted, the image source dialog is shown.
     * Otherwise, permissions are requested.
     */
    public void pickImage() {
        Context context = fragment.getContext();
        if (context == null) return;

        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES};
        } else {
            permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};
        }

        boolean allGranted = true;
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(context, perm) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (allGranted) {
            showImageSourceDialog();
        } else {
            permissionLauncher.launch(permissions);
        }
    }

    /**
     * Displays a dialog for choosing between camera and gallery.
     */
    public void showImageSourceDialog() {
        if (fragment.getContext() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getContext());
        CharSequence[] options = {"Camera", "Gallery", "Cancel"};
        builder.setTitle("Choose an option");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Camera")) {
                openCamera();
            } else if (options[item].equals("Gallery")) {
                openGallery();
            } else {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    public void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setData(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    public void openCamera() {
        Context context = fragment.requireContext();
        ContentValues values = new ContentValues();
        values.put(android.provider.MediaStore.Images.Media.TITLE, "New Picture");
        values.put(android.provider.MediaStore.Images.Media.DESCRIPTION, "From Camera");
        cameraImageUri = context.getContentResolver().insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, cameraImageUri);
        cameraLauncher.launch(intent);
    }
}
