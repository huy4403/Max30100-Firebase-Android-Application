package com.iot.heartmonitor;

import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

public class NotifyManager extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notify_manager);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Thiết lập tên cho ActionBar
        getSupportActionBar().setTitle("Notification Manager");
        // Thiết lập Toolbar làm ActionBar
        setSupportActionBar(toolbar);

        // Nếu muốn có nút quay lại (back button)
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Đọc thông báo từ SQLite và hiển thị
        NotificationDatabaseHelper dbHelper = new NotificationDatabaseHelper(this);
        List<String> notifications = dbHelper.getAllNotifications();

        ListView listView = findViewById(R.id.notification_list);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, notifications);
        listView.setAdapter(adapter);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Quay lại MainActivity
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.clear) {
            NotificationDatabaseHelper dbHelper = new NotificationDatabaseHelper(this);
            dbHelper.deleteAllNotifications();
            List<String> notifications = dbHelper.getAllNotifications();

            ListView listView = findViewById(R.id.notification_list);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, notifications);
            listView.setAdapter(adapter);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu từ file XML
        getMenuInflater().inflate(R.menu.toolbar_notify, menu);
        return true;
    }
}