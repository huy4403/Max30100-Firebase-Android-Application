#include <ESP8266WiFi.h>
#include <FirebaseESP8266.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include "MAX30100_PulseOximeter.h"
#include <SoftwareSerial.h>
char ssid[] = "*******"; //Wifi username
char pass[] = "*******"; //Wifi password

unsigned long timeS = 0;  // Đặt lại timeS ban đầu thành 0

FirebaseData firebaseData;
FirebaseAuth auth;
FirebaseConfig config;

Adafruit_SSD1306 display(128, 64, &Wire, -1);

SoftwareSerial mySerial(D8, D7);  // Đặt chân D8 (RX) và D7 (TX)

#define loa D6
#define led D5
#define button D4
int isAlertEnabled = 1;
int warningStatus;

//Thêm
#define MAX_DATA_POINTS 60 // Số điểm dữ liệu nhịp tim trong 60 giây
int heartRateData[MAX_DATA_POINTS]; // Mảng lưu dữ liệu nhịp tim
int dataIndex = 0; // Chỉ số hiện tại trong mảng

const unsigned char bitmap [] PROGMEM = {
	0x00, 0x00, 0x00, 0x00, 0x0f, 0xc1, 0xf8, 0x00, 0x1f, 0xf3, 0xfe, 0x00, 0x3f, 0xff, 0xff, 0x00, 
	0x7f, 0xff, 0xff, 0x00, 0x7f, 0xff, 0xff, 0x80, 0x7f, 0xff, 0xff, 0x80, 0xff, 0xfe, 0xff, 0x80, 
	0xff, 0xfc, 0xff, 0x80, 0x7f, 0xdc, 0xff, 0x80, 0x7f, 0xcc, 0x7f, 0x80, 0x7f, 0x4d, 0x7f, 0x80, 
	0x20, 0x2d, 0x61, 0x00, 0x00, 0xa1, 0x40, 0x00, 0x1f, 0xf3, 0x1e, 0x00, 0x0f, 0xf3, 0xbc, 0x00, 
	0x07, 0xf3, 0xf0, 0x00, 0x03, 0xfb, 0xe0, 0x00, 0x01, 0xff, 0xc0, 0x00, 0x00, 0x7f, 0x80, 0x00, 
	0x00, 0x3f, 0x00, 0x00, 0x00, 0x1e, 0x00, 0x00, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
};


void setup() {
  Serial.begin(9600);
  mySerial.begin(4800);
  if (!display.begin(SSD1306_SWITCHCAPVCC, 0x3C)) {
    Serial.println(F("SSD1306 allocation failed"));
    for (;;);  // Loop forever if initialization fails
  }
  display.display();
  delay(2000);  // Pause for 2 seconds
  display.setTextSize(1);
  pinMode(loa, OUTPUT);
  pinMode(button, INPUT_PULLUP);
  digitalWrite(loa, LOW);
  pinMode(led, OUTPUT);
  digitalWrite(led, LOW);
  display.setTextColor(SSD1306_WHITE);
  
  WiFi.begin(ssid, pass);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println();
  Serial.println("Connected to WiFi");

  //put your firebase account host address in here
  config.host = "*******************************************************";
  
  // put your firebase account authentication secret here available in project-settings/service accounts/database-secrets
  config.signer.tokens.legacy_token = "****************************************"; 
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  Serial.println("Connected to Firebase");
  timeS = millis();
}

void loop() {
  int heartRate = 0;
  int spo2 = 0;

  int buttonState = digitalRead(button); // Đọc trạng thái button

  if (buttonState == LOW) { // Button được nhấn (khi pull-up, trạng thái LOW là nhấn)
      isAlertEnabled = !isAlertEnabled;
      if (Firebase.setInt(firebaseData, "/WarningStatus", isAlertEnabled)) {
        Serial.println("WarningStatus sent successfully");
      } else {
        Serial.println("Failed to send WarningStatus");
        Serial.println(firebaseData.errorReason());
      }
      display.fillRect(50, 0, 100, 8, BLACK);
      delay(50);
      display.setCursor(50, 0);
      display.print("Warning: ");
      display.print(isAlertEnabled ? "On" : "Off");
      display.display();
  }

  if (millis() - timeS > 1000) {
    if (mySerial.available()) {
      String dataString = "";
      while (mySerial.available()) {
        dataString += (char)mySerial.read();  // Đọc dữ liệu từng byte một
      }
      // Split the data string by the separator ';'
      int separatorIndex = dataString.indexOf(';');
      if (separatorIndex != -1) {
        heartRate = dataString.substring(0, separatorIndex).toInt();
        spo2 = dataString.substring(separatorIndex + 1).toInt();
      }
    }
    
    if (Firebase.getInt(firebaseData, "/WarningStatus")) {
      if (firebaseData.dataType() == "int") {
        warningStatus = firebaseData.intData();
        isAlertEnabled = (warningStatus == 1);
      }
    }

    if (Firebase.setInt(firebaseData, "/HeartRate", heartRate)) {
      Serial.println("HeartRate sent successfully");
    } else {
      Serial.println("Failed to send HeartRate");
      Serial.println(firebaseData.errorReason());
    }

    if (Firebase.setInt(firebaseData, "/SpO2", spo2)) {
      Serial.println("SpO2 sent successfully");
    } else {
      Serial.println("Failed to send SpO2");
      Serial.println(firebaseData.errorReason());
    }
    // Cập nhật mảng dữ liệu nhịp tim
    heartRateData[dataIndex] = heartRate; // Lưu giá trị nhịp tim vào mảng
    dataIndex = (dataIndex + 1) % MAX_DATA_POINTS; // Chuyển sang chỉ số tiếp theo, vòng lặp lại khi đủ 60 điểm
    
    display.clearDisplay();

    display.setCursor(0, 0);
    
    // Hiển thị mức độ sóng WiFi
    int wifiSignalStrength = WiFi.RSSI();
    int wifiBars = 0;
    if (wifiSignalStrength > - 50) wifiBars = 3;     // Tín hiệu mạnh
    else if (wifiSignalStrength > -70) wifiBars = 2; // Tín hiệu trung bình
    else if (wifiSignalStrength > -85) wifiBars = 1; // Tín hiệu yếu
    else wifiBars = 0;                               // Không có tín hiệu
    
    for (int i = 0; i < wifiBars; i++) {
      display.write(219);  // Ký tự biểu tượng cột sóng WiFi
    }

    display.setCursor(50, 0);
    display.print("Warning: ");
    display.print((isAlertEnabled == 1) ? "On" : "Off");
    // Hiển thị vạch WiFi và trạng thái cảnh báo trên cùng dòng
    display.setCursor(3, 16);
    display.print("Tinh trang benh nhan");
    int heartRateWidth = 6 * String(heartRate).length();
    display.setCursor(128 - 18 - heartRateWidth, 30);
    display.print(heartRate, 0);
    display.setCursor(128 - 18, 30);
    display.print("bpm");
    int spo2Width = 6 * String(spo2).length();
    display.setCursor(128 - 6 - spo2Width, 45);
    display.print(spo2);
    display.setCursor(128 - 6, 45);
    display.print("%");

    int bitmapX = 63;
    int bitmapY = 30; // Vị trí Y tương tự vị trí nhịp tim

    // Hiển thị bitmap ngay bên phải heartRate
    display.drawBitmap(bitmapX, bitmapY, bitmap, 26, 24, SSD1306_WHITE);

    // Vẽ đồ thị nhịp tim
    int graphHeight = 20; // Chiều cao tối đa của đồ thị
    int graphWidth = MAX_DATA_POINTS; // Chiều rộng đồ thị
    int graphX = 0; // Góc dưới bên trái
    int graphY = 60 - graphHeight; // Tọa độ Y của đồ thị

    int startIndex = (dataIndex == 0) ? 0 : dataIndex;
    for (int i = 0; i < MAX_DATA_POINTS - 1; i++) {
    int currentIndex = (startIndex + i) % MAX_DATA_POINTS;
    int nextIndex = (startIndex + i + 1) % MAX_DATA_POINTS;

    int x1 = graphX + i;
    int y1 = graphY + map(heartRateData[currentIndex], 0, 200, graphHeight, 0);
    int x2 = graphX + i + 1;
    int y2 = graphY + map(heartRateData[nextIndex], 0, 200, graphHeight, 0);

    display.drawLine(x1, y1, x2, y2, SSD1306_WHITE);
}

    display.display();

    if ((isAlertEnabled == 1) && ((heartRate > 150) || (heartRate < 50 && heartRate > 0)) || (spo2 > 0 && spo2 < 90)) {
      digitalWrite(loa, HIGH);
      digitalWrite(led, HIGH);
    } else {
      digitalWrite(loa, LOW);
      digitalWrite(led, LOW);
    }
    timeS = millis();
  }
}