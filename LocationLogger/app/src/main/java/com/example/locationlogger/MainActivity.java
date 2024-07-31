package com.example.locationlogger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private RadioGroup radioGroupPriority;
    private Switch switchAccuracy;
    private Button buttonStartLogging;
    private Button buttonStopLogging;
    private TextView statusTextView;
    private TextView textViewResults;
    private ScrollView scrollViewResults;

    private File logFile;
    private boolean isAccuracyOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        radioGroupPriority = findViewById(R.id.radioGroupPriority);
        switchAccuracy = findViewById(R.id.switchAccuracy);
        buttonStartLogging = findViewById(R.id.buttonStartLogging);
        buttonStopLogging = findViewById(R.id.buttonStopLogging);
        statusTextView = findViewById(R.id.textViewStatus);
        textViewResults = findViewById(R.id.textViewResults);
        scrollViewResults = findViewById(R.id.scrollViewResults);

        buttonStartLogging.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                statusTextView.setText("Logging...");
                statusTextView.setVisibility(View.VISIBLE);

                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            LOCATION_PERMISSION_REQUEST_CODE);
                } else {
                    startLocationLogging();
                }
            }
        });

        buttonStopLogging.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopLocationLogging();
            }
        });

        createLogFile();
    }

    private void createLogFile() {
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        logFile = new File(storageDir, "location_log.txt");
        try {
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            String currentDateAndTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.append("Log start date and time: ").append(currentDateAndTime).append("\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error creating log file", e);
        }
    }

    private void startLocationLogging() {
        int selectedPriorityId = radioGroupPriority.getCheckedRadioButtonId();
        int priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY; // Default
        String priorityStr = "Balanced Power Accuracy";

        if (selectedPriorityId == R.id.radioNoPower) {
            priority = LocationRequest.PRIORITY_NO_POWER;
            priorityStr = "No Power";
        } else if (selectedPriorityId == R.id.radioLowPower) {
            priority = LocationRequest.PRIORITY_LOW_POWER;
            priorityStr = "Low Power";
        } else if (selectedPriorityId == R.id.radioBalanced) {
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
            priorityStr = "Balanced Power Accuracy";
        } else if (selectedPriorityId == R.id.radioHighAccuracy) {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY;
            priorityStr = "High Accuracy";
        }

        locationRequest = LocationRequest.create()
                .setPriority(priority)
                .setInterval(10000)
                .setFastestInterval(5000);

        isAccuracyOn = switchAccuracy.isChecked();
        String finalPriorityStr = priorityStr;
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    logLocationToFile(location, finalPriorityStr, isAccuracyOn);
                }
            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            Toast.makeText(this, "Started logging location", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationLogging() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            statusTextView.setText("Logging stopped");
            Toast.makeText(this, "Stopped logging location", Toast.LENGTH_SHORT).show();
            displayLogFileContents(); // Display log file contents when stopped
        }
    }

    private void logLocationToFile(Location location, String priority, boolean accuracyOn) {
        String currentDateAndTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        String logEntry = "Timestamp: " + currentDateAndTime + "\n" +
                "Latitude: " + location.getLatitude() + "\n" +
                "Longitude: " + location.getLongitude() + "\n" +
                "Priority: " + priority + "\n" +
                "Accuracy On: " + accuracyOn + "\n" +
                "----------\n";

        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.append(logEntry);
        } catch (IOException e) {
            Log.e(TAG, "Error writing to log file", e);
        }
    }

    private void displayLogFileContents() {
        StringBuilder logContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logContent.append(line).append("\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading log file", e);
        }
        textViewResults.setText(logContent.toString());
        scrollViewResults.post(() -> scrollViewResults.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationLogging();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
