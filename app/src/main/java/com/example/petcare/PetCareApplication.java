package com.example.petcare;

import android.app.Application;
import com.bumptech.glide.Glide;

public class PetCareApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Glide
        Glide.init(this, new com.bumptech.glide.GlideBuilder());
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        // Clear Glide memory cache when system is low on memory
        if (Glide.get(this) != null) {
            Glide.get(this).clearMemory();
        }
    }
} 