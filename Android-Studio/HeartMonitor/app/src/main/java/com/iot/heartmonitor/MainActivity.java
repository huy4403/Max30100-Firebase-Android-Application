package com.iot.heartmonitor;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import me.tankery.lib.circularseekbar.CircularSeekBar;

public class MainActivity extends AppCompatActivity {

    private long backPressedTime;
    private TextView tvHR;
    private TextView tvSpo2;
    private CircularSeekBar circularSeekBarHR;
    private CircularSeekBar circularSeekBarSpo2;
    private LineChart chart;
    public static NotificationManager notificationManager;

    private List<Entry> heartRateEntries = new ArrayList<>();
    private static final int MAX_SIZE = 60;
    public static Ringtone currentRingtone;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Intent serviceIntent = new Intent(this, MyForegroundService.class);
        startForegroundService(serviceIntent);
        ForegroundServiceRunning();

        Toolbar toolbar = findViewById(R.id.toolbar);
        // Thiết lập Toolbar làm ActionBar
        setSupportActionBar(toolbar);

        circularSeekBarHR = findViewById(R.id.circularSeekBarHR);
        tvHR = findViewById(R.id.tvHR);

        circularSeekBarSpo2 = findViewById(R.id.circularSeekBarSpo2);
        tvSpo2 = findViewById(R.id.tvSpo2);

        chart = findViewById(R.id.chart);

        configureChart();


        // Đọc dữ liệu từ Firebase ngay khi Activity được tạo
        readDatabase();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Cài đặt chế độ NightMode tự động theo hệ thống
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Kiểm tra chế độ sáng/tối và thay đổi logo
        updateToolbarLogo(toolbar);
    }
    public boolean ForegroundServiceRunning(){
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)){
            if(MyForegroundService.class.getName().equals(service.service.getClassName())){
                return true;
            }
        }
        return false;
    }

    private void updateChart(List<Entry> heartRateEntries) {
        // Tạo LineDataSet với các điểm dữ liệu
        LineDataSet lineDataSet = new LineDataSet(heartRateEntries, "Nhịp tim");

        // Cấu hình LineDataSet
        lineDataSet.setColor(Color.RED);  // Màu đường kẻ
        lineDataSet.setValueTextColor(Color.BLACK);  // Màu văn bản giá trị
        lineDataSet.setDrawCircles(true);  // Vẽ các điểm trên đường kẻ
        lineDataSet.setDrawValues(false);  // Tắt vẽ giá trị số bên cạnh các điểm
        lineDataSet.setLineWidth(2f);  // Độ dày của đường kẻ

        // Tạo LineData từ LineDataSet
        LineData lineData = new LineData(lineDataSet);

        // Cập nhật dữ liệu vào LineChart
        chart.setData(lineData);
        chart.invalidate();  // Làm mới biểu đồ
    }

    private void configureChart() {
        // Cấu hình trục Y (nhịp tim)
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);  // Đặt giá trị tối thiểu của trục Y là 0
        leftAxis.setAxisMaximum(220f);  // Đặt giá trị tối đa của trục Y là 250
        leftAxis.setGranularity(10f);  // Đặt độ phân giải của trục Y (mỗi 50 bpm)
        leftAxis.setTextColor(Color.RED);

        // Tắt trục Y bên phải
        chart.getAxisRight().setEnabled(false);

        // Cấu hình trục X (thời gian từ 1 đến 60 giây)
        XAxis xAxis = chart.getXAxis();
        xAxis.setAxisMinimum(0f);  // Đặt giá trị tối thiểu của trục X là 1 giây
        xAxis.setAxisMaximum(60f);  // Đặt giá trị tối đa của trục X là 60 giây
        xAxis.setGranularity(1f);  // Đặt độ phân giải của trục X (mỗi giây)
        xAxis.setLabelCount(5, true);  // Hiển thị 6 nhãn trên trục X (từ 1 đến 60 giây)
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);  // Đặt trục X ở dưới cùng
        xAxis.setDrawGridLines(false);  // Tắt các đường lưới của trục X
        xAxis.setTextColor(Color.RED);
    }

    private void updateToolbarLogo(Toolbar toolbar) {
        // Kiểm tra chế độ sáng/tối và thay đổi logo
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            toolbar.setLogo(R.drawable.baseline_home_light);  // Logo cho chế độ tối
        } else {
            toolbar.setLogo(R.drawable.baseline_home_light);  // Logo cho chế độ sáng
        }
    }


    private void readDatabase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("HeartRate");
        DatabaseReference myRef1 = database.getReference("SpO2");

        // Lắng nghe sự thay đổi dữ liệu nhịp tim (HeartRate)
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Integer heartRate = dataSnapshot.getValue(Integer.class);
                if (heartRate != null) {
                    int progress = Math.min(heartRate, 200);
                    circularSeekBarHR.setProgress(progress);
                    tvHR.setText(progress + " bpm");

                    long currentTime = System.currentTimeMillis();
                    heartRateEntries.add(new Entry(heartRateEntries.size() + 1, progress));

                    // Kiểm tra nếu nhịp tim > 120 và gửi thông báo
                    if (heartRate > 180) {
                        sendNotification("Cảnh báo nguy hiểm!", "Nhịp tim đã vượt quá 130\nNhịp tim: " +heartRate);
                    } else if (heartRate > 0 && heartRate < 50) {
                        sendNotification("Cảnh báo nguy hiểm!", "Nhịp tim đã giảm quá 50\nNhịp tim: " +heartRate);
                    }

                    updateChart(heartRateEntries);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle possible errors
            }
        });

        // Lắng nghe sự thay đổi dữ liệu SpO2
        myRef1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Integer spo2 = dataSnapshot.getValue(Integer.class);
                if (spo2 != null) {
                    int progress = Math.min(spo2, 100);
                    circularSeekBarSpo2.setProgress(progress);
                    tvSpo2.setText(progress + "%");

                    // Kiểm tra nếu nhịp tim > 120 và gửi thông báo
                    if (progress > 0 && progress < 98) {
                        sendNotification("Cảnh báo!", "Có dấu hiệu bị suy hô hấp");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle possible errors
            }
        });
    }

    @SuppressLint("NewApi")
    private void sendNotification(String title, String message) {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Kiểm tra phiên bản Android để tạo kênh thông báo nếu cần
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("HeartRateAlert", "Heart Rate Alerts", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // Tạo một Intent để mở ứng dụng khi nhấn vào thông báo
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent actionIntent = new Intent(this, ActionNoti.class);
        actionIntent.putExtra("notificationId", getNotificationId());
        PendingIntent actionPendingIntent = PendingIntent.getBroadcast(this, 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "HeartRateAlert")
                .setSmallIcon(R.mipmap.heartmonitorlogo) // Icon cho thông báo
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(R.mipmap.heartmonitorlogo,"Đánh dấu là đã đọc", actionPendingIntent);

        notificationManager.notify(getNotificationId(), builder.build());

        // Dừng ringtone hiện tại nếu đang phát
        if (currentRingtone != null && currentRingtone.isPlaying()) {
            currentRingtone.stop();
        }
        // Phát ringtone mới
        currentRingtone = RingtoneManager.getRingtone(MainActivity.this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        currentRingtone.setLooping(true);  // Để phát liên tục
        currentRingtone.play();

        // Lưu thông báo vào SQLite
        NotificationDatabaseHelper dbHelper = new NotificationDatabaseHelper(this);
        dbHelper.insertNotification(title, message);
    }
    public int getNotificationId(){
        return (int) new Date().getTime();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu từ file XML
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }
    @Override
    public void onBackPressed() {
        // Kiểm tra xem người dùng có nhấn nút back lần thứ hai trong vòng 2 giây không
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
            // Đóng hoàn toàn ứng dụng khi nhấn lần thứ hai
            finishAffinity();  // Đóng tất cả các Activity trong stack và thoát ứng dụng
            System.exit(0);  // Thoát hẳn ứng dụng
        } else {
            // Nếu là lần nhấn đầu tiên, hiển thị thông báo yêu cầu nhấn lại để thoát
            Toast.makeText(this, "Ấn lần nữa để thoát", Toast.LENGTH_SHORT).show();
        }

        // Cập nhật thời gian nhấn back
        backPressedTime = System.currentTimeMillis();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.offnotify){
            // Dừng ringtone hiện tại nếu đang phát
            if (currentRingtone != null && currentRingtone.isPlaying()) {
                currentRingtone.stop();
            }
            return true;
        }else if (item.getItemId() == R.id.notifications) {
            Intent intent = new Intent(MainActivity.this, NotifyManager.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.setting) {
            showSettingsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Dialog setting status
    private void showSettingsDialog() {
        // Tạo một builder cho AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");

        // Tạo một LayoutInflater để nạp layout tùy chỉnh
        LayoutInflater inflater = getLayoutInflater();

        // Tạo một LinearLayout để chứa tất cả các mục trong Dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16); // Thêm padding nếu cần

        // Mục 1: Option 1 với Switch
        View option1View = inflater.inflate(R.layout.custom_layout, layout, false);
        TextView option1Text = option1View.findViewById(R.id.option_text);
        Switch option1Switch = option1View.findViewById(R.id.option_switch);
        option1Text.setText("Cảnh báo thiết bị");

        // Lấy giá trị từ Firebase và cập nhật trạng thái của Switch
        readWarningStatus(new WarningStatusCallback() {
            @Override
            public void onWarningStatusRead(int status) {
                // Cập nhật trạng thái của Switch dựa trên giá trị WarningStatus từ Firebase
                option1Switch.setChecked(status == 1);
            }

            @Override
            public void onError(String errorMessage) {
                // Xử lý lỗi nếu có
                Log.e("Firebase", "Lỗi khi đọc WarningStatus: " + errorMessage);
            }
        });

        // Xử lý khi Switch thay đổi
        option1Switch.setOnCheckedChangeListener((buttonView, isChecked) -> {

            // Thông báo khi thay đổi
            if (isChecked) {
                // Khi bật, thay đổi màu thumb và track
                option1Switch.getThumbDrawable().setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.green), PorterDuff.Mode.SRC_IN);
                option1Switch.getTrackDrawable().setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.red), PorterDuff.Mode.SRC_IN);
            } else {
                // Khi tắt, thay đổi màu
                option1Switch.getThumbDrawable().setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.red), PorterDuff.Mode.SRC_IN);
                option1Switch.getTrackDrawable().setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.green), PorterDuff.Mode.SRC_IN);
            }

            // Cập nhật trạng thái WarningStatus lên Firebase khi Switch thay đổi
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("WarningStatus");

            // Gửi giá trị WarningStatus lên Firebase
            myRef.setValue(isChecked ? 1 : 0);  // Cập nhật giá trị WarningStatus trong Firebase

        });

        // Thêm View vào LinearLayout
        layout.addView(option1View);

        // Thiết lập layout vào trong AlertDialog
        builder.setView(layout);

        // Hiển thị dialog
        builder.create().show();
    }

    public void readWarningStatus(final WarningStatusCallback callback) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("WarningStatus");

        // Lắng nghe sự thay đổi của WarningStatus
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Lấy giá trị boolean từ Firebase
                Integer warningStatus = dataSnapshot.getValue(Integer.class);
                if (warningStatus != null) {
                    // Gọi callback và trả về true hoặc false tùy theo giá trị warningStatus
                    callback.onWarningStatusRead(warningStatus); // Trả về warningStatus trực tiếp
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Xử lý lỗi nếu có
                Log.e("Firebase", "Lỗi khi đọc dữ liệu: " + error.getMessage());
                callback.onError(error.getMessage()); // Trả về lỗi nếu có
            }
        });
    }

    public interface WarningStatusCallback {
        void onWarningStatusRead(int status); // Trả về true hoặc false
        void onError(String errorMessage); // Xử lý lỗi
    }

}
