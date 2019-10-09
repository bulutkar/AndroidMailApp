package com.example.mailapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;

public class PermissionRequestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_request);

        if (ContextCompat.checkSelfPermission(PermissionRequestActivity.this, RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            int permissionRequestId = 5;
            ActivityCompat.requestPermissions(PermissionRequestActivity.this, new String[]{RECORD_AUDIO, INTERNET}, permissionRequestId);

        } else {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        if (requestCode == 5) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
            } else {
                finishAndRemoveTask();
            }
        }
    }

    @Override
    public void onBackPressed() {
    }
}

