# Luồng Giao Hàng - Giải Thích Đơn Giản

## Tổng Quan

Đây là cách hệ thống quản lý việc giao hàng từ khi khách hàng đặt hàng đến khi hàng được giao tận tay.

Hãy tưởng tượng bạn đang quản lý một cửa hàng nội thất:
- Khách hàng đặt mua một bộ bàn ghế
- Bạn cần chuẩn bị hàng và giao cho khách
- Nhân viên giao hàng sẽ đi giao

Hệ thống này giúp bạn theo dõi từng bước một cách có tổ chức.

---

## Các Nhân Vật Trong Câu Chuyện

### 1. **Khách Hàng (Customer)**
- Người mua hàng
- Đặt hàng qua website/app

### 2. **Nhân Viên Cửa Hàng (STAFF)**
- Người làm việc tại cửa hàng
- Chuẩn bị hàng, in hóa đơn
- Phân công ai sẽ đi giao hàng

### 3. **Quản Lý Cửa Hàng (BRANCH_MANAGER)**
- Người quản lý cửa hàng
- Có thể phân công giao hàng
- Theo dõi tiến độ giao hàng

### 4. **Nhân Viên Giao Hàng (DELIVERY)**
- Người đi giao hàng
- Nhận nhiệm vụ giao hàng
- Cập nhật trạng thái khi đang giao và khi giao xong

---

## Câu Chuyện Giao Hàng - Từng Bước

### 🛒 BƯỚC 1: Khách Hàng Đặt Hàng

**Chuyện gì xảy ra?**
- Khách hàng vào website/app, chọn sản phẩm và đặt hàng
- Hệ thống tạo một "đơn hàng" (order) với thông tin: ai mua, mua gì, địa chỉ giao hàng

**Ví dụ:**
```
Khách hàng: Anh Nam
Sản phẩm: Bộ bàn ghế gỗ
Địa chỉ: 123 Đường ABC, Quận XYZ
Tổng tiền: 5,000,000 VNĐ
```

**Kết quả:** Đơn hàng được tạo, chờ xử lý

---

### 📋 BƯỚC 2: Nhân Viên Cửa Hàng Phân Công Giao Hàng

**Ai làm?** Nhân viên cửa hàng (STAFF) hoặc Quản lý (BRANCH_MANAGER)

**Chuyện gì xảy ra?**
- Nhân viên cửa hàng nhìn vào đơn hàng mới
- Quyết định: "Đơn này sẽ giao cho ai? Giao khi nào?"
- Ghi lại thông tin vào hệ thống

**Ví dụ:**
```
Nhân viên cửa hàng: "Đơn hàng #123 sẽ giao cho anh Tuấn (nhân viên giao hàng)"
Thời gian dự kiến: "Ngày mai, 10 giờ sáng"
Ghi chú: "Khách hàng ở nhà vào buổi sáng"
```

**Kết quả:** Hệ thống ghi nhận: "Đơn hàng #123 đã được phân công cho anh Tuấn"

**Trạng thái:** 🟡 **ASSIGNED** (Đã phân công)

---

### 🧾 BƯỚC 3: In Hóa Đơn (Có thể làm song song với Bước 4)

**Ai làm?** Nhân viên cửa hàng (STAFF)

**Chuyện gì xảy ra?**
- Nhân viên in hóa đơn cho đơn hàng
- Hóa đơn sẽ được gửi kèm khi giao hàng

**Ví dụ:**
```
Nhân viên: "Tôi sẽ in hóa đơn cho đơn hàng #123"
Hệ thống: "Đã ghi nhận hóa đơn đã được in"
```

**Kết quả:** Hệ thống đánh dấu: "Hóa đơn đã được in"

**Lưu ý:** Bước này và bước 4 có thể làm cùng lúc, không nhất thiết phải theo thứ tự

---

### 📦 BƯỚC 4: Chuẩn Bị Hàng (Có thể làm song song với Bước 3)

**Ai làm?** Nhân viên cửa hàng (STAFF)

**Chuyện gì xảy ra?**
- Nhân viên kiểm tra kho: "Có đủ hàng không?"
- Nếu đủ hàng: Lấy hàng ra, đóng gói, chuẩn bị sẵn sàng
- Nếu không đủ hàng: Báo lại, không thể chuẩn bị

**Ví dụ:**
```
Nhân viên: "Kiểm tra kho... Có đủ bộ bàn ghế gỗ không?"
Hệ thống: "Có, còn 10 bộ trong kho"
Nhân viên: "OK, tôi sẽ lấy 1 bộ ra và đóng gói"
Hệ thống: "Đã ghi nhận hàng đã được chuẩn bị"
```

**Kết quả:** Hệ thống đánh dấu: "Hàng đã được chuẩn bị, sẵn sàng giao"

**Trạng thái:** 🟢 **READY** (Sẵn sàng giao hàng)

**Lưu ý:** 
- Nếu không đủ hàng, hệ thống sẽ báo lỗi
- Sau khi chuẩn bị xong, trạng thái tự động chuyển sang "READY"

---

### 👀 BƯỚC 5: Nhân Viên Giao Hàng Xem Nhiệm Vụ

**Ai làm?** Nhân viên giao hàng (DELIVERY)

**Chuyện gì xảy ra?**
- Nhân viên giao hàng đăng nhập vào hệ thống
- Xem danh sách các đơn hàng được giao cho mình
- Xem thông tin: địa chỉ, thời gian, ghi chú

**Ví dụ:**
```
Anh Tuấn (nhân viên giao hàng): "Tôi sẽ xem các đơn hàng của tôi hôm nay"
Hệ thống: "Bạn có 3 đơn hàng:
  - Đơn #123: 123 Đường ABC, giao lúc 10h sáng
  - Đơn #124: 456 Đường XYZ, giao lúc 2h chiều
  - Đơn #125: 789 Đường DEF, giao lúc 4h chiều"
```

**Kết quả:** Nhân viên giao hàng biết mình cần giao những đơn nào

---

### 🚚 BƯỚC 6: Nhân Viên Giao Hàng Bắt Đầu Đi Giao

**Ai làm?** Nhân viên giao hàng (DELIVERY) hoặc Quản lý (BRANCH_MANAGER)

**Chuyện gì xảy ra?**
- Nhân viên giao hàng lấy hàng, lên xe, bắt đầu đi
- Cập nhật hệ thống: "Tôi đang đi giao hàng rồi"

**Ví dụ:**
```
Anh Tuấn: "Tôi đã lấy hàng, đang đi giao đơn #123"
Anh Tuấn cập nhật hệ thống: "Đang giao hàng"
Hệ thống: "Đã cập nhật: Đơn #123 đang được giao"
```

**Kết quả:** Hệ thống đánh dấu: "Đang giao hàng"

**Trạng thái:** 🔵 **IN_TRANSIT** (Đang giao hàng)

---

### ✅ BƯỚC 7: Nhân Viên Giao Hàng Xác Nhận Đã Giao Xong

**Ai làm?** Nhân viên giao hàng (DELIVERY)

**Chuyện gì xảy ra?**
- Nhân viên giao hàng đến địa chỉ, giao hàng cho khách
- Khách nhận hàng, ký xác nhận (hoặc scan QR code)
- Nhân viên cập nhật hệ thống: "Đã giao xong"

**Ví dụ:**
```
Anh Tuấn: "Đã đến nhà khách hàng, giao hàng thành công"
Khách hàng: "Cảm ơn, tôi đã nhận hàng"
Anh Tuấn cập nhật hệ thống: "Đã giao hàng xong"
Hệ thống: "Đã cập nhật: Đơn #123 đã được giao thành công"
```

**Kết quả:** Hệ thống đánh dấu: "Đã giao hàng thành công"

**Trạng thái:** ✅ **DELIVERED** (Đã giao hàng)

---

## Tóm Tắt Các Trạng Thái

Hãy tưởng tượng đơn hàng như một bưu kiện đang được chuyển:

| Trạng thái | Ý nghĩa | Ai làm | Ví dụ |
|------------|---------|--------|-------|
| 🟡 **ASSIGNED** | Đã phân công | Nhân viên cửa hàng | "Đơn này sẽ giao cho anh Tuấn" |
| 🟠 **PREPARING** | Đang chuẩn bị | Nhân viên cửa hàng | "Đang lấy hàng từ kho" |
| 🟢 **READY** | Sẵn sàng giao | Hệ thống tự động | "Hàng đã đóng gói xong, sẵn sàng" |
| 🔵 **IN_TRANSIT** | Đang giao hàng | Nhân viên giao hàng | "Đang đi trên đường" |
| ✅ **DELIVERED** | Đã giao xong | Nhân viên giao hàng | "Đã giao cho khách hàng" |
| ❌ **CANCELLED** | Đã hủy | Ai cũng có thể | "Hủy đơn hàng này" |

---

## Ai Làm Gì?

### 👔 Nhân Viên Cửa Hàng (STAFF)
- ✅ Phân công đơn hàng cho ai giao
- ✅ In hóa đơn
- ✅ Chuẩn bị hàng (lấy từ kho, đóng gói)
- ✅ Xem danh sách đơn hàng trong cửa hàng
- ❌ Không thể cập nhật trạng thái giao hàng (chỉ nhân viên giao hàng mới làm được)

### 👨‍💼 Quản Lý Cửa Hàng (BRANCH_MANAGER)
- ✅ Phân công đơn hàng cho ai giao
- ✅ Xem tất cả đơn hàng trong cửa hàng
- ✅ Theo dõi tiến độ giao hàng (có bao nhiêu đơn đang giao, đã giao xong)
- ✅ Cập nhật trạng thái giao hàng (nếu cần)
- ❌ Không thể in hóa đơn (chỉ nhân viên cửa hàng mới làm được)
- ❌ Không thể chuẩn bị hàng (chỉ nhân viên cửa hàng mới làm được)

### 🚚 Nhân Viên Giao Hàng (DELIVERY)
- ✅ Xem các đơn hàng được giao cho mình
- ✅ Cập nhật trạng thái: "Đang giao hàng", "Đã giao xong"
- ✅ Xác nhận giao hàng thành công
- ❌ Không thể phân công đơn hàng (chỉ nhân viên cửa hàng/quản lý mới làm được)
- ❌ Không thể in hóa đơn
- ❌ Không thể chuẩn bị hàng

---

## Ví Dụ Thực Tế - Một Ngày Làm Việc

### Sáng 8:30 - Nhân Viên Cửa Hàng Phân Công

```
Nhân viên cửa hàng: "Hôm nay có 5 đơn hàng mới"
Nhân viên: "Đơn #101 → giao cho anh Tuấn, giao lúc 10h"
Nhân viên: "Đơn #102 → giao cho chị Lan, giao lúc 11h"
...
Hệ thống: "Đã phân công xong"
```

### Sáng 9:00 - Nhân Viên Cửa Hàng Chuẩn Bị

```
Nhân viên: "Tôi sẽ chuẩn bị hàng cho đơn #101"
Nhân viên: "Kiểm tra kho... Có đủ hàng"
Nhân viên: "Lấy hàng, đóng gói xong"
Hệ thống: "Đơn #101 sẵn sàng giao"
```

### Sáng 9:30 - Nhân Viên Giao Hàng Xem Nhiệm Vụ

```
Anh Tuấn: "Tôi sẽ xem các đơn hàng của tôi hôm nay"
Hệ thống: "Bạn có 3 đơn hàng cần giao:
  - Đơn #101: 10h sáng
  - Đơn #103: 2h chiều
  - Đơn #105: 4h chiều"
```

### Sáng 10:00 - Nhân Viên Giao Hàng Bắt Đầu Giao

```
Anh Tuấn: "Tôi lấy hàng đơn #101, bắt đầu đi giao"
Anh Tuấn cập nhật: "Đang giao hàng"
Hệ thống: "Đơn #101 đang được giao"
```

### Sáng 10:30 - Nhân Viên Giao Hàng Giao Xong

```
Anh Tuấn: "Đã đến nhà khách, giao hàng thành công"
Anh Tuấn cập nhật: "Đã giao xong"
Hệ thống: "Đơn #101 đã được giao thành công"
```

### Chiều 2:00 - Quản Lý Theo Dõi

```
Quản lý: "Hôm nay có bao nhiêu đơn đã giao xong?"
Hệ thống: "Tổng cộng: 10 đơn
  - Đã giao xong: 7 đơn
  - Đang giao: 2 đơn
  - Chưa giao: 1 đơn"
```

---

## Câu Hỏi Thường Gặp

### ❓ Tại sao nhân viên giao hàng không thể tự phân công đơn hàng cho mình?

**Trả lời:** 
- Để đảm bảo công bằng, quản lý sẽ phân công dựa trên:
  - Khoảng cách (ai gần địa chỉ đó nhất)
  - Khối lượng công việc (ai đang ít việc)
  - Kinh nghiệm (ai quen đường đó)
- Nếu nhân viên giao hàng tự chọn, có thể dẫn đến phân công không công bằng

### ❓ Có thể bỏ qua bước chuẩn bị hàng không?

**Trả lời:**
- Không nên, vì:
  - Cần kiểm tra có đủ hàng trong kho không
  - Cần đóng gói cẩn thận để hàng không bị hỏng
  - Cần chuẩn bị sẵn để nhân viên giao hàng lấy đi ngay

### ❓ Nếu không đủ hàng thì sao?

**Trả lời:**
- Hệ thống sẽ báo lỗi: "Không đủ hàng trong kho"
- Nhân viên cửa hàng cần:
  - Liên hệ với khách hàng để thông báo
  - Hoặc nhập thêm hàng vào kho
  - Sau đó mới có thể chuẩn bị hàng

### ❓ Có thể hủy đơn hàng không?

**Trả lời:**
- Có, ở bất kỳ thời điểm nào
- Có thể hủy vì:
  - Khách hàng yêu cầu hủy
  - Không thể giao hàng (địa chỉ sai, khách không nhận)
  - Hàng bị hỏng trong quá trình vận chuyển

### ❓ Ai có thể xem tất cả đơn hàng?

**Trả lời:**
- **Nhân viên cửa hàng (STAFF):** Xem đơn hàng trong cửa hàng của mình
- **Quản lý (BRANCH_MANAGER):** Xem tất cả đơn hàng trong cửa hàng, theo dõi tiến độ
- **Nhân viên giao hàng (DELIVERY):** Chỉ xem đơn hàng được giao cho mình

---

## Lợi Ích Của Hệ Thống Này

### ✅ Cho Nhân Viên Cửa Hàng
- Biết rõ đơn hàng nào đã được phân công, đơn nào chưa
- Dễ dàng chuẩn bị hàng và in hóa đơn
- Theo dõi được tiến độ giao hàng

### ✅ Cho Quản Lý
- Theo dõi được tất cả đơn hàng trong cửa hàng
- Biết được nhân viên nào đang giao hàng, đã giao xong bao nhiêu đơn
- Phân công công việc một cách công bằng

### ✅ Cho Nhân Viên Giao Hàng
- Biết rõ mình cần giao những đơn nào
- Dễ dàng cập nhật trạng thái khi đang giao và khi giao xong
- Không cần phải nhớ quá nhiều thông tin

### ✅ Cho Khách Hàng
- Biết được đơn hàng đang ở đâu (đã được phân công, đang giao, đã giao)
- Nhận hàng đúng thời gian dự kiến
- Yên tâm về quy trình giao hàng chuyên nghiệp

---

## Kết Luận

Hệ thống này giúp quản lý việc giao hàng một cách có tổ chức:
- Mỗi người biết mình cần làm gì
- Mỗi bước được ghi nhận rõ ràng
- Dễ dàng theo dõi và kiểm soát

Giống như một cuốn sổ ghi chép thông minh, tự động cập nhật và thông báo cho mọi người!

