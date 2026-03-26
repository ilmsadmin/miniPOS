# 2. Yêu cầu chức năng (Functional Requirements)

## 2.1 Module: Xác thực & Quản lý cửa hàng (Auth & Store Management)

### FR-001: Tạo cửa hàng mới

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | Người dùng lần đầu có thể tạo một cửa hàng mới |
| **Ưu tiên** | Cao (P0) |
| **Vai trò** | Bất kỳ (lần đầu sử dụng) |

**Luồng chính:**
1. Người dùng mở app lần đầu
2. Chọn "Tạo cửa hàng mới"
3. Nhập thông tin cửa hàng:
   - Tên cửa hàng (VD: `Zin100`) — bắt buộc
   - Mã cửa hàng (tự sinh hoặc tùy chỉnh, 4-8 ký tự) — bắt buộc
   - Địa chỉ — tùy chọn
   - Số điện thoại — tùy chọn
   - Logo cửa hàng — tùy chọn
4. Tạo tài khoản Owner:
   - Tên hiển thị — bắt buộc
   - PIN đăng nhập (4-6 số) — bắt buộc
   - Mật khẩu quản trị (cho các thao tác nhạy cảm) — bắt buộc
5. Hệ thống tạo cửa hàng và tài khoản Owner
6. Chuyển đến Home Screen

**Quy tắc nghiệp vụ:**
- Mã cửa hàng phải unique trên thiết bị
- Mỗi cửa hàng phải có ít nhất 1 Owner
- Store ID được sử dụng như định danh để các thiết bị khác kết nối

---

### FR-002: Tham gia cửa hàng đã có

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | Thiết bị mới tham gia vào cửa hàng đã tồn tại |
| **Ưu tiên** | Cao (P0) |
| **Vai trò** | Bất kỳ (thiết bị mới) |

**Luồng chính:**
1. Người dùng mở app lần đầu
2. Chọn "Tham gia cửa hàng"
3. Nhập mã cửa hàng (VD: `Zin100`)
4. App tìm kiếm thiết bị Owner/Manager trong cùng mạng WiFi
5. Hiển thị danh sách thiết bị tìm được
6. Chọn thiết bị để kết nối
7. Thiết bị Owner nhận thông báo yêu cầu kết nối → Chấp nhận/Từ chối
8. Nếu chấp nhận:
   - Owner chọn vai trò cho thiết bị mới (Manager/Cashier)
   - Đồng bộ dữ liệu ban đầu (full sync)
   - Thiết bị mới nhận tài khoản và đăng nhập

**Luồng thay thế:**
- 4a. Không tìm thấy thiết bị → Hiển thị hướng dẫn (cùng WiFi, bật Bluetooth)
- 7a. Owner từ chối → Thông báo cho thiết bị yêu cầu

---

### FR-003: Đăng nhập

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | Người dùng đăng nhập vào cửa hàng trên thiết bị |
| **Ưu tiên** | Cao (P0) |
| **Vai trò** | Tất cả |

**Luồng chính:**
1. Mở app (cửa hàng đã được thiết lập)
2. Hiển thị danh sách tài khoản trên thiết bị (avatar + tên)
3. Chọn tài khoản
4. Nhập PIN (4-6 số)
5. Xác thực thành công → Vào Home Screen theo vai trò

**Luồng thay thế:**
- 4a. PIN sai → Hiển thị lỗi, cho phép thử lại (tối đa 5 lần)
- 4b. Quá 5 lần sai → Khóa 5 phút
- Hỗ trợ đăng nhập bằng vân tay/Face ID (tùy chọn)

---

### FR-004: Quản lý nhân viên

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | Owner/Manager tạo, sửa, xóa tài khoản nhân viên |
| **Ưu tiên** | Cao (P0) |
| **Vai trò** | Owner, Manager |

**Chức năng:**
- Tạo tài khoản nhân viên mới (tên, PIN, vai trò)
- Sửa thông tin nhân viên
- Vô hiệu hóa / Kích hoạt tài khoản
- Reset PIN nhân viên
- Xem lịch sử hoạt động nhân viên

---

## 2.2 Module: Quản lý Danh mục (Category Management)

### FR-010: Quản lý danh mục sản phẩm

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | CRUD danh mục sản phẩm |
| **Ưu tiên** | Cao (P0) |
| **Vai trò** | Owner, Manager |

**Chức năng:**
- Tạo danh mục mới (tên, mô tả, icon/màu sắc, thứ tự hiển thị)
- Sửa thông tin danh mục
- Xóa danh mục (soft delete, kiểm tra sản phẩm liên quan)
- Sắp xếp danh mục (kéo thả)
- Hỗ trợ danh mục cha-con (tối đa 2 cấp)

**Quy tắc nghiệp vụ:**
- Không cho xóa danh mục đang có sản phẩm active
- Tên danh mục phải unique trong cùng cấp
- Danh mục mặc định "Chưa phân loại" không thể xóa

**Dữ liệu danh mục:**

| Trường | Kiểu | Bắt buộc | Mô tả |
|--------|------|----------|-------|
| id | UUID | Có | Khóa chính |
| name | String(100) | Có | Tên danh mục |
| description | String(500) | Không | Mô tả |
| parent_id | UUID | Không | Danh mục cha |
| icon | String | Không | Icon/emoji |
| color | String(7) | Không | Mã màu HEX |
| sort_order | Integer | Có | Thứ tự hiển thị |
| is_active | Boolean | Có | Trạng thái |

---

## 2.3 Module: Quản lý Sản phẩm (Product Management)

### FR-020: Quản lý sản phẩm

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | CRUD sản phẩm |
| **Ưu tiên** | Cao (P0) |
| **Vai trò** | Owner, Manager |

**Chức năng:**
- Thêm sản phẩm mới
- Sửa thông tin sản phẩm
- Xóa sản phẩm (soft delete)
- Tìm kiếm sản phẩm (theo tên, barcode, danh mục)
- Quét barcode để thêm nhanh
- Import sản phẩm từ file Excel/CSV
- Sao chép sản phẩm

**Dữ liệu sản phẩm:**

| Trường | Kiểu | Bắt buộc | Mô tả |
|--------|------|----------|-------|
| id | UUID | Có | Khóa chính |
| sku | String(50) | Có | Mã sản phẩm (tự sinh) |
| barcode | String(50) | Không | Mã vạch |
| name | String(200) | Có | Tên sản phẩm |
| description | String(1000) | Không | Mô tả |
| category_id | UUID | Không | Danh mục |
| supplier_id | UUID | Không | Nhà cung cấp |
| cost_price | Decimal(12,2) | Có | Giá nhập |
| selling_price | Decimal(12,2) | Có | Giá bán |
| unit | String(20) | Có | Đơn vị tính (cái, kg, hộp...) |
| image | String | Không | Đường dẫn ảnh local |
| min_stock | Integer | Không | Tồn kho tối thiểu (cảnh báo) |
| max_stock | Integer | Không | Tồn kho tối đa |
| is_active | Boolean | Có | Trạng thái kinh doanh |
| track_inventory | Boolean | Có | Có quản lý tồn kho không |
| tax_rate | Decimal(5,2) | Không | Thuế suất (%) |

### FR-021: Quản lý biến thể sản phẩm

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | Sản phẩm có nhiều biến thể (size, màu sắc...) |
| **Ưu tiên** | Trung bình (P1) |
| **Vai trò** | Owner, Manager |

**Ví dụ:** Áo thun có biến thể Size (S, M, L, XL) và Màu (Đen, Trắng)

**Dữ liệu biến thể:**

| Trường | Kiểu | Bắt buộc | Mô tả |
|--------|------|----------|-------|
| id | UUID | Có | Khóa chính |
| product_id | UUID | Có | Sản phẩm gốc |
| variant_name | String(100) | Có | Tên biến thể (VD: "Đen - Size L") |
| sku | String(50) | Có | Mã riêng cho biến thể |
| barcode | String(50) | Không | Mã vạch riêng |
| cost_price | Decimal(12,2) | Không | Giá nhập (nếu khác sản phẩm gốc) |
| selling_price | Decimal(12,2) | Không | Giá bán (nếu khác) |
| attributes | JSON | Có | {"size": "L", "color": "Đen"} |

---

## 2.4 Module: Quản lý Nhà cung cấp (Supplier Management)

### FR-030: Quản lý nhà cung cấp

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | CRUD nhà cung cấp |
| **Ưu tiên** | Trung bình (P1) |
| **Vai trò** | Owner, Manager |

**Chức năng:**
- Thêm nhà cung cấp mới
- Sửa thông tin nhà cung cấp
- Xóa nhà cung cấp (soft delete)
- Tìm kiếm nhà cung cấp
- Xem lịch sử nhập hàng từ nhà cung cấp
- Xem công nợ nhà cung cấp

**Dữ liệu nhà cung cấp:**

| Trường | Kiểu | Bắt buộc | Mô tả |
|--------|------|----------|-------|
| id | UUID | Có | Khóa chính |
| name | String(200) | Có | Tên nhà cung cấp |
| contact_person | String(100) | Không | Người liên hệ |
| phone | String(20) | Không | Số điện thoại |
| email | String(100) | Không | Email |
| address | String(500) | Không | Địa chỉ |
| tax_code | String(20) | Không | Mã số thuế |
| notes | String(1000) | Không | Ghi chú |
| is_active | Boolean | Có | Trạng thái |

---

## 2.5 Module: Quản lý Kho (Inventory Management)

### FR-040: Quản lý tồn kho

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | Theo dõi số lượng tồn kho |
| **Ưu tiên** | Cao (P0) |
| **Vai trò** | Owner, Manager |

**Chức năng:**
- Xem tổng quan tồn kho (dashboard)
- Xem chi tiết tồn kho từng sản phẩm
- Cảnh báo sản phẩm sắp hết hàng (dưới min_stock)
- Cảnh báo sản phẩm tồn kho cao (trên max_stock)
- Lọc theo danh mục, nhà cung cấp, trạng thái tồn kho

### FR-041: Nhập kho

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | Tạo phiếu nhập kho |
| **Ưu tiên** | Cao (P0) |
| **Vai trò** | Owner, Manager |

**Luồng chính:**
1. Chọn "Nhập kho" → Tạo phiếu nhập mới
2. Chọn nhà cung cấp (tùy chọn)
3. Thêm sản phẩm vào phiếu nhập:
   - Quét barcode hoặc tìm kiếm
   - Nhập số lượng
   - Nhập giá nhập (mặc định lấy giá nhập của sản phẩm)
4. Xem tổng tiền
5. Thêm ghi chú (tùy chọn)
6. Xác nhận nhập kho
7. Hệ thống cập nhật tồn kho, ghi log

**Dữ liệu phiếu nhập kho:**

| Trường | Kiểu | Bắt buộc | Mô tả |
|--------|------|----------|-------|
| id | UUID | Có | Khóa chính |
| code | String(20) | Có | Mã phiếu (tự sinh: NK-20260325-001) |
| supplier_id | UUID | Không | Nhà cung cấp |
| total_amount | Decimal(15,2) | Có | Tổng tiền |
| total_items | Integer | Có | Tổng số mặt hàng |
| notes | String(1000) | Không | Ghi chú |
| status | Enum | Có | draft / confirmed / cancelled |
| created_by | UUID | Có | Người tạo |
| created_at | DateTime | Có | Thời gian tạo |

### FR-042: Xuất kho / Kiểm kho

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | Xuất kho thủ công và kiểm kho |
| **Ưu tiên** | Trung bình (P1) |
| **Vai trò** | Owner, Manager |

**Chức năng xuất kho:**
- Tạo phiếu xuất kho (hư hỏng, hết hạn, trả hàng NCC, khác)
- Nhập lý do xuất
- Cập nhật tồn kho tự động

**Chức năng kiểm kho:**
- Tạo phiên kiểm kho
- Nhập số lượng thực tế từng sản phẩm
- So sánh chênh lệch (hệ thống vs thực tế)
- Điều chỉnh tồn kho theo kết quả kiểm

---

## 2.6 Module: Khách hàng (Customer Management)

### FR-045: Quản lý khách hàng

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | CRUD khách hàng của cửa hàng |
| **Ưu tiên** | Cao (P0) |
| **Vai trò** | Owner, Manager, Cashier |

**Chức năng:**
- Xem danh sách khách hàng (search theo tên, SĐT)
- Tạo khách hàng mới (Tên*, SĐT*, Email, Địa chỉ, Ghi chú)
- Sửa thông tin khách hàng
- Xóa khách hàng (soft delete)
- Xem chi tiết khách hàng:
  - Thông tin cá nhân
  - Lịch sử mua hàng (danh sách đơn)
  - Tổng chi tiêu / Số lần mua
  - Ghi chú

**Dữ liệu khách hàng:**

| Trường | Kiểu | Bắt buộc | Mô tả |
|--------|------|----------|-------|
| id | UUID | Có | Khóa chính |
| store_id | UUID | Có | FK → stores |
| name | String(100) | Có | Tên khách hàng |
| phone | String(20) | Có | Số điện thoại (unique/store) |
| email | String(100) | Không | Email |
| address | String(500) | Không | Địa chỉ |
| notes | String(500) | Không | Ghi chú |
| total_spent | Decimal(15,2) | Có | Tổng chi tiêu (cached) |
| visit_count | Integer | Có | Số lần mua hàng (cached) |
| last_visit_at | DateTime | Không | Lần mua gần nhất |

**Quy tắc nghiệp vụ:**
- SĐT unique trong cùng 1 store
- Khi tạo đơn hàng, `total_spent` và `visit_count` tự động cập nhật
- Tạo nhanh khách hàng ngay trong POS flow (chỉ cần Tên + SĐT)

---

## 2.7 Module: Bán hàng POS (Point of Sale) — Step-by-step

### FR-050: POS Flow tổng quan

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | Quy trình bán hàng theo 5 bước |
| **Ưu tiên** | Cao (P0) |
| **Vai trò** | Owner, Manager, Cashier |

**POS Flow gồm 5 bước:**

```
Step 1          Step 2           Step 3          Step 4           Step 5
Chọn SP    →   Số lượng &   →  Chọn khách  →  Tạo hoá đơn  →  In hoá đơn
               Giá bán          hàng            & Thanh toán
```

### FR-050a: Step 1 — Chọn sản phẩm

**Chức năng:**
- Hiển thị sản phẩm dạng lưới (grid) theo danh mục
- Tìm kiếm sản phẩm (tên, mã, barcode)
- Quét barcode bằng camera
- Chạm vào sản phẩm để thêm vào giỏ (badge hiển thị số lượng)
- Chạm lần nữa để tăng quantity
- Long press xem chi tiết SP
- Thanh summary bottom: số SP đã chọn + tổng tạm tính

### FR-050b: Step 2 — Số lượng & Giá bán

**Chức năng:**
- Hiển thị danh sách SP đã chọn
- Chỉnh số lượng bằng nút +/- hoặc nhập trực tiếp
- **Chỉnh giá bán** cho từng SP (nhấn ✏️ → numpad)
- Giá bán khác giá gốc → highlight vàng cảnh báo
- Áp dụng giảm giá từng SP (% hoặc fixed)
- Swipe left để xóa SP
- Hiển thị thành tiền = giá bán × số lượng (real-time)

### FR-050c: Step 3 — Chọn khách hàng

**Chức năng:**
- Mặc định: "Khách lẻ" (bỏ qua, không bắt buộc)
- Tìm kiếm khách hàng (tên, SĐT)
- Hiển thị danh sách khách hàng gần đây (theo last_visit_at)
- Chọn khách hàng (radio selection)
- **Tạo khách hàng mới** nhanh (inline form: Tên + SĐT)
- Sau khi tạo → tự động chọn khách hàng vừa tạo

### FR-051: Step 4 — Tạo hoá đơn & Thanh toán

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | Review đơn hàng + xử lý thanh toán |
| **Ưu tiên** | Cao (P0) |
| **Vai trò** | Owner, Manager, Cashier |

**Hiển thị:**
- Thông tin khách hàng (nếu có) + nút [Đổi]
- Danh sách SP + số lượng + giá bán + thành tiền
- Tạm tính / Giảm giá / Thuế / Tổng thanh toán
- Nút "Thêm giảm giá" (giảm giá toàn đơn)

**Phương thức thanh toán:**
- 💵 Tiền mặt (nhập số tiền khách đưa, tính tiền thừa)
- 🏦 Chuyển khoản ngân hàng
- 💳 Ví điện tử (Momo, ZaloPay...)
- 🔀 Kết hợp nhiều phương thức

**Luồng thanh toán tiền mặt:**
1. Chọn "Tiền mặt"
2. Gợi ý nhanh tiền khách đưa (làm tròn lên)
3. Nhập số tiền khách đưa
4. Hiển thị tiền thừa
5. Nhấn "Xác nhận thanh toán"
6. → Chuyển sang Step 5

**Dữ liệu đơn hàng:**

| Trường | Kiểu | Bắt buộc | Mô tả |
|--------|------|----------|-------|
| id | UUID | Có | Khóa chính |
| order_code | String(20) | Có | Mã đơn (HD-20260325-001) |
| customer_id | UUID | Không | Khách hàng |
| subtotal | Decimal(15,2) | Có | Tổng tiền hàng |
| discount_amount | Decimal(15,2) | Có | Giảm giá |
| tax_amount | Decimal(15,2) | Có | Thuế |
| total_amount | Decimal(15,2) | Có | Tổng thanh toán |
| payment_method | Enum | Có | cash / transfer / ewallet / mixed |
| payment_amount | Decimal(15,2) | Có | Số tiền khách đưa |
| change_amount | Decimal(15,2) | Có | Tiền thừa |
| status | Enum | Có | completed / refunded / cancelled |
| notes | String(500) | Không | Ghi chú |
| created_by | UUID | Có | Nhân viên bán |
| created_at | DateTime | Có | Thời gian |

### FR-052: Step 5 — Hoá đơn & In

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | Hiển thị kết quả + tạo và in hóa đơn |
| **Ưu tiên** | Trung bình (P1) |
| **Vai trò** | Owner, Manager, Cashier |

**Chức năng:**
- Hiển thị animation thành công (confetti)
- Preview hóa đơn (receipt) với đầy đủ thông tin:
  - Tên cửa hàng, địa chỉ, SĐT
  - Mã đơn hàng, ngày giờ, nhân viên bán
  - Tên khách hàng (nếu có)
  - Danh sách SP (tên, SL, đơn giá, thành tiền)
  - Tổng, giảm giá, thuế, thanh toán, tiền thừa
- In hóa đơn qua máy in Bluetooth (thermal printer 58mm/80mm)
- Chia sẻ hóa đơn qua Zalo, SMS, Email (dạng ảnh/PDF)
- Tùy chỉnh mẫu hóa đơn (logo, footer, thông tin liên hệ)
- Nút "Tạo đơn hàng mới" → reset về Step 1
- Nút "Về trang chủ" → quay về Home Screen

### FR-053: Trả hàng / Hoàn tiền

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | Xử lý trả hàng và hoàn tiền |
| **Ưu tiên** | Trung bình (P1) |
| **Vai trò** | Owner, Manager |

**Luồng chính:**
1. Tìm đơn hàng (mã đơn, ngày, khách hàng)
2. Chọn sản phẩm cần trả (toàn bộ hoặc một phần)
3. Nhập lý do trả hàng
4. Nhập mật khẩu quản trị để xác nhận (yêu cầu với Cashier)
5. Hoàn tiền
6. Cập nhật tồn kho (nhập lại kho)

---

## 2.7 Module: Báo cáo (Reports) — P1

### FR-060: Báo cáo doanh thu

- Doanh thu theo ngày / tuần / tháng / tùy chỉnh
- Doanh thu theo nhân viên
- Doanh thu theo phương thức thanh toán
- Biểu đồ doanh thu (line chart)

### FR-061: Báo cáo sản phẩm

- Sản phẩm bán chạy nhất
- Sản phẩm ít bán
- Lợi nhuận theo sản phẩm
- Tồn kho hiện tại

### FR-062: Báo cáo tổng quan Dashboard

```
┌─────────────────────────────────────────────┐
│              Dashboard hôm nay               │
├──────────────────────┬──────────────────────┤
│  💰 Doanh thu        │  📦 Đơn hàng        │
│  2,450,000đ          │  23 đơn              │
├──────────────────────┼──────────────────────┤
│  📈 Lợi nhuận       │  ⚠️ Cảnh báo tồn kho │
│  680,000đ            │  5 SP sắp hết        │
├──────────────────────┴──────────────────────┤
│  📊 Biểu đồ doanh thu 7 ngày gần nhất      │
│  ████                                        │
│  ██████                                      │
│  ████████                                    │
│  ███████                                     │
│  █████████                                   │
│  ████████████                                │
│  ██████████████  ← Hôm nay                  │
└─────────────────────────────────────────────┘
```

---

## 2.8 Module: Backup & Restore Google Drive

### FR-070: Kết nối Google Account

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | Owner kết nối tài khoản Google để backup/restore lên Google Drive |
| **Ưu tiên** | Trung bình (P1) |
| **Vai trò** | Owner |

> Chi tiết xem tài liệu [09-backup-restore-gdrive.md](./09-backup-restore-gdrive.md)

### FR-071: Backup thủ công lên Google Drive

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | Owner tạo bản backup thủ công, mã hóa AES-256 và upload lên Google Drive |
| **Ưu tiên** | Trung bình (P1) |
| **Vai trò** | Owner |

### FR-072: Backup tự động

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | Tự động backup theo lịch (hàng ngày/tuần/tháng) |
| **Ưu tiên** | Trung bình (P1) |
| **Vai trò** | Owner (cấu hình) |

### FR-073: Restore từ Google Drive

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | Khôi phục toàn bộ dữ liệu từ bản backup trên Google Drive |
| **Ưu tiên** | Trung bình (P1) |
| **Vai trò** | Owner |

### FR-074: Quản lý bản backup

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | Xem danh sách, xóa, tải về các bản backup trên Google Drive |
| **Ưu tiên** | Thấp (P2) |
| **Vai trò** | Owner |

---

## 2.9 Tổng hợp chức năng theo vai trò

| Chức năng | Owner | Manager | Cashier |
|-----------|:-----:|:-------:|:-------:|
| Tạo cửa hàng | ✅ | ❌ | ❌ |
| Quản lý nhân viên | ✅ | ✅* | ❌ |
| Quản lý danh mục | ✅ | ✅ | ❌ |
| Quản lý sản phẩm | ✅ | ✅ | 👁️ Xem |
| Quản lý nhà cung cấp | ✅ | ✅ | ❌ |
| Quản lý khách hàng | ✅ | ✅ | ✅ (tạo/xem) |
| Nhập kho | ✅ | ✅ | ❌ |
| Xuất kho / Kiểm kho | ✅ | ✅ | ❌ |
| Bán hàng POS (step-by-step) | ✅ | ✅ | ✅ |
| Chỉnh giá bán trong POS | ✅ | ✅ | ⚙️** |
| Trả hàng / Hoàn tiền | ✅ | ✅ | ❌ |
| Xem báo cáo | ✅ | ✅ | ❌ |
| Cài đặt cửa hàng | ✅ | ❌ | ❌ |
| Quản lý thiết bị kết nối | ✅ | ❌ | ❌ |
| Backup lên Google Drive | ✅ | ❌ | ❌ |
| Restore từ Google Drive | ✅ | ❌ | ❌ |
| Xóa dữ liệu | ✅ | ❌ | ❌ |

*\* Manager chỉ quản lý Cashier, không quản lý Owner/Manager khác*
*\*\* ⚙️ Cashier chỉnh giá bán: tùy cấu hình Owner cho phép hay không*
