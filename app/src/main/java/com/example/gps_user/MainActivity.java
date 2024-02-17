package com.example.gps_user;

import android.Manifest;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private LocationManager locationManager;
    private TextView showInfo;

    private int locationChangeCount = 0;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        showInfo = findViewById(R.id.show_info);
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0x123);
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

            // Hỏi người dùng có cho phép ghi vào bộ nhớ ngoại không
            askPermissionForExternalStorage();
        }
    }

    private void askPermissionForExternalStorage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required");
        builder.setMessage("Do you allow the app to save location data to external storage?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Người dùng đã cho phép, tiếp tục lấy vị trí và ghi vào bộ nhớ ngoại
                startLocationUpdates();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Người dùng không cho phép, có thể thực hiện xử lý khác nếu cần
                // Ví dụ: thông báo cho người dùng về việc không lưu trữ dữ liệu
            }
        });
        builder.show();
    }

    private void startLocationUpdates() {
        // Kiểm tra quyền truy cập vị trí
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Nếu có quyền, lấy GPS information every three seconds
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 8f, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    updateView(location);
                    // Push data
                    pushData(location);
                }
            });
        } else {
            requestLocationPermission();
        }
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
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

            // Lưu vị trí vào SD card
            locationChangeCount++;
            saveLocationToFile(location, locationChangeCount);
        } else {
            // Xử lý trường hợp location là null nếu cần
            Log.e(TAG, "pushData: Location is null");
        }
    }

//    private void saveLocationToSDCard(Location location, int count) {
//        try {
//            // Tạo một đối tượng File cho tệp tin lưu trữ vị trí
//            File file = new File(Environment.getExternalStorageDirectory(), "location.txt");
//
//            // Mở luồng để ghi vào tệp tin
//            FileWriter writer = new FileWriter(file, true);
//
//            // Ghi thông tin vị trí theo định dạng mong muốn
//            writer.append(count + "th:\n");
//            writer.append("Longitude = ").append(String.valueOf(location.getLongitude())).append("\n");
//            writer.append("Latitude = ").append(String.valueOf(location.getLatitude())).append("\n\n");
//
//            // Đóng luồng ghi
//            writer.close();
//
//            Log.d(TAG, "saveLocationToSDCard: Location saved to SD card");
//        } catch (IOException e) {
//            e.printStackTrace();
//            Log.e(TAG, "saveLocationToSDCard: Error saving location to SD card");
//        }
//    }

    private void saveLocationToFile(Location location, int count) {
        try {
            // Tạo một đối tượng File cho tệp tin lưu trữ vị trí trong thư mục ứng dụng
            File directory = getFilesDir();
            File file = new File(directory, "location.txt");

            // Mở luồng để ghi vào tệp tin
            FileWriter writer = new FileWriter(file, true);

            // Ghi thông tin vị trí theo định dạng mong muốn
            writer.append(count + "th:\n");
            writer.append("Longitude = ").append(String.valueOf(location.getLongitude())).append("\n");
            writer.append("Latitude = ").append(String.valueOf(location.getLatitude())).append("\n\n");

            // Đóng luồng ghi
            writer.close();

            Log.d(TAG, "saveLocationToFile: Location saved to file");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "saveLocationToFile: Error saving location to file");
        }
    }
}