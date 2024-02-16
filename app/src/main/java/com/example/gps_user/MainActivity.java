package com.example.gps_user;

import android.Manifest;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    public static final String NETWORK_PROVIDER = "network";
    public static final String GPS_PROVIDER = "gps";
    public static final String PASSIVE_PROVIDER = "passive";
    public static final String FUSED_PROVIDER = "fused";

    private LocationManager locationManager;
    private TextView showInfo;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        showInfo = findViewById(R.id.show_info);
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},0x123);
    }

    private void updateView(Location location) {
        if (location != null) {
            Log.d(TAG, "updateView: 222");
            String res = "Real-time location:\n" +
                    "Longitude:" + location.getLongitude() +
                    "\nLatitude:" + location.getLatitude() +
                    "\nHeight:" + location.getAltitude() +
                    "\nSpeed:" + location.getSpeed()
                    + "\nDirection:" + location.getBearing();
            showInfo.setText(res);
        } else {
            Log.d(TAG, "updateView: 111");
            showInfo.setText("");
        }
    }
    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0x123 && grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //Create locationManger object
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            //Get the latest positioning information
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            updateView(lastKnownLocation);

//            pushData(lastKnownLocation);
            //Get GPS information every three seconds
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 8f, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    updateView(location);
                    // Push data
                    pushData(location);
                }
            });
        }
    }

    private void pushData(Location location) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("location");

        if (location != null) {
            // Tạo một đối tượng Map để chứa dữ liệu cần đẩy lên
            Map<String, Double> locationData = new HashMap<>();
            locationData.put("longitude", location.getLongitude());
            locationData.put("latitude", location.getLatitude());

            // Đẩy dữ liệu lên Firebase Realtime Database
            myRef.setValue(locationData);
        } else {
            // Xử lý trường hợp location là null nếu cần
            Log.e(TAG, "pushData: Location is null");
        }
    }
}