#include <SoftwareSerial.h> //Khai báo thư viện giao tiếp UART mềm(ảo) thông qua các chân GPIO
#include <Wire.h> //Khai báo thư viện giao tiếp I2C
#include <MAX30100_PulseOximeter.h> //Khai báo thư viện cảm biến nhịp tim MAX30100
SoftwareSerial mySerial(10, 11); //Khởi tạo Giao tiếp UART thông qua chân 10 làm RX và chân 11 làm TX
//Chân RX của Uno kết nối với chân TX của ESP, và ngược lại
PulseOximeter pox; //Khai báo đối tượng(Object) pox từ class PulseOximeter. thực hiện nhiệm vụ đọc dữ liệu cảm biến
unsigned long timeS = 0; //Khai báo biến timeS
void setup() { //Hàm setup chỉ chạy duy nhất 1 lần(đầu tiên) khi khởi động thiết bị
  Serial.begin(9600); //Khởi tạo giao tiếp Serial với máy tính thông qua USB
  mySerial.begin(4800); //Khởi tạo giao tiếp UART ảo với esp thông qua các chân RX, TX đã khai báo ở trên
  // Khởi tạo cảm biến
  if (!pox.begin()) { //Khởi tạo thất bại thì in FAILED và tiếp tục cố gắng khởi tạo lại cảm biến
    Serial.println("FAILED");
    for (;;);
  } else { //Khởi tạo thành công
    Serial.println("SUCCESS");
  }
  pox.setIRLedCurrent(MAX30100_LED_CURR_27_1MA); //cấu hình dòng điện phát sáng của đèn LED hồng ngoại (IR LED) trong cảm biến MAX30100.
  timeS = millis(); //Đặt biến timeS làm thời gian hiện tại
}

void loop() { //Vòng lặp tương tự hàm main
    pox.update(); //Cập nhật giá trị cảm biến
    int heartRate = 0; //Khai báo biến heartRate
    int spo2 = 0; //Khai báo biến spo2
    if(millis() - timeS > 1000){ //Thời gian vòng lặp cách nhau 1 giây tương tự delay(1000)
      heartRate = pox.getHeartRate(); //Lấy dữ liệu nhịp tim đọc được từ cảm biến và lưu vào biến heartRate
      spo2 = pox.getSpO2(); //Tương tự nhịp tim
      // Tạo chuỗi dữ liệu với ký tự phân tách để gửi cho esp8266
      String dataString = String(heartRate) + ";" + String(spo2);

      mySerial.println(dataString);  // Gửi chuỗi dữ liệu qua esp8266 thông qua UART
      Serial.println(dataString); //In ra màn hình serial monitor thông qua usb
      timeS = millis(); //Đặt lại thời gian thực cho biến timeS
  }
}