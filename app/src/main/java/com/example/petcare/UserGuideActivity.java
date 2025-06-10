package com.example.petcare;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class UserGuideActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_guide);

        Button btnReturn = findViewById(R.id.btnReturn);
        btnReturn.setOnClickListener(v -> finish());
    }
}
