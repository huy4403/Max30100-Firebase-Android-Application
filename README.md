# HỆ THỐNG GIÁM SÁT NHỊP TIM VÀ NỒNG ĐỘ OXY TRONG MÁU

## Các chức năng:

### Thiết bị giám sát nhịp tim

- Đọc giá trị nhịp tim và nồng độ oxy trong máu
- Hiển thị giá trị đọc được lên màn hình oled
- Nút bấm bật/tắt cảnh báo trên thiết bị đồng bộ với ứng dụng thông qua firebase bằng biến warningStatus 
- Còi và đèn led cảnh báo khi nhịp tim hoặc nồng độ oxy trong máu quá thấp hoặc quá cao khi thông báo được bật
- Gửi giá trị nhịp tim và nồng độ oxy trong máu lên firebase

### Ứng dụng giám sát nhịp tim

- Nhận giá trị nhịp tim và nồng độ oxy trong máu từ firebase
- Hiển thị giá trị đọc được lên các Circularseekbar được custom
- Hiển thị biểu đồ nhịp tim
- Thông báo kèm chuông cảnh báo khi nhịp tim hoặc nồng dộ oxy trong máu quá thấp hoặc quá cao
- Bật tắt cảnh báo trên thiết bị IoT từ xa
- Quản lý, lưu trữ thông báo
- Ứng dụng vẫn hoạt động khi bị tắt

## Project có 2 phần chính:

### Lưu ý

- Sử dụng cảm biến Max30100 để đọc giá trị nhịp tim và nồng độ oxy trong máu, Arduino Uno R3 để điều khiển cảm biến và gửi giá trị đến Esp8266 thông qua giao thức UART.

- Lý do phải sử dụng Cả Uno R3 và Esp8266 là vì việc cảm biến Max30100 đọc giá trị và đồng gửi giá trị lên firebase sẽ gây ra vấn đề bị đơ thiết bị, vì vậy phải sử dụng riêng Uno R3 để kết nối với cảm biến Max30100 và gửi dữ liệu đến Esp8266 thông qua giao thức UART.


#### Thông tin liên hệ
- Facebook: fb.com/Huy4403
- Zalo: 
<img src="image.png" alt="Zalo logo" width="200">