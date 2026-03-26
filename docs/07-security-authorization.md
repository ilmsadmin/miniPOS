# 7. Bảo mật & Phân quyền

## 7.1 Tổng quan bảo mật

Vì miniPOS lưu dữ liệu hoàn toàn trên thiết bị, bảo mật tập trung vào:
1. **Xác thực người dùng** — Ai được phép truy cập
2. **Phân quyền** — Được phép làm gì
3. **Bảo vệ dữ liệu local** — Mã hóa database
4. **Bảo mật truyền tải** — An toàn khi đồng bộ P2P
5. **Bảo vệ thiết bị** — Khi mất/đánh cắp

## 7.2 Xác thực (Authentication)

### 7.2.1 PIN Authentication

```
Cơ chế:
1. PIN 4-6 chữ số
2. Hash bằng bcrypt (cost factor = 10)
3. So sánh hash khi đăng nhập
4. Brute-force protection:
   - Tối đa 5 lần thử sai
   - Khóa 5 phút sau 5 lần sai
   - Khóa 30 phút sau 10 lần sai
   - Khóa 24 giờ sau 20 lần sai
   
Lưu ý: PIN hash được lưu local, KHÔNG lưu PIN plaintext
```

### 7.2.2 Password Authentication (Cho thao tác nhạy cảm)

```
Yêu cầu thao tác cần mật khẩu:
- Trả hàng / Hoàn tiền
- Xóa sản phẩm
- Xóa đơn hàng
- Quản lý nhân viên
- Xóa dữ liệu cửa hàng
- Reset PIN người khác
- Xóa thiết bị

Cơ chế:
1. Mật khẩu tối thiểu 6 ký tự
2. Hash bằng bcrypt (cost factor = 12)
3. Yêu cầu nhập lại mật khẩu cho mỗi thao tác nhạy cảm
4. Session timeout: 5 phút cho elevated permission
```

### 7.2.3 Biometric Authentication (Tùy chọn)

```
Hỗ trợ:
- Touch ID / Face ID (iOS)
- Fingerprint / Face Unlock (Android)

Cơ chế:
1. Biometric chỉ thay thế PIN cho đăng nhập nhanh
2. Không thay thế Password cho thao tác nhạy cảm
3. Người dùng tự bật/tắt trong Settings
4. Sử dụng platform Keychain/Keystore
```

### 7.2.4 Session Management

```
Session {
    userId: UUID
    storeId: UUID
    role: Role
    loginAt: DateTime
    lastActiveAt: DateTime
    deviceId: UUID
}

Auto-lock rules:
- Cashier: Lock sau 5 phút không hoạt động
- Manager: Lock sau 15 phút
- Owner: Lock sau 30 phút
- Có thể tùy chỉnh trong Settings

Lock behavior:
- Hiển thị PIN screen (không mất dữ liệu giỏ hàng)
- Giỏ hàng POS được giữ nguyên
```

## 7.3 Phân quyền (Authorization)

### 7.3.1 Ma trận phân quyền chi tiết

```
Permission Format: module.action
```

| Permission | Owner | Manager | Cashier |
|-----------|:-----:|:-------:|:-------:|
| **Store** |
| store.view | ✅ | ✅ | ✅ |
| store.edit | ✅ | ❌ | ❌ |
| store.delete | ✅ | ❌ | ❌ |
| store.settings | ✅ | ❌ | ❌ |
| **Users** |
| user.view | ✅ | ✅ | ❌ |
| user.create | ✅ | ✅* | ❌ |
| user.edit | ✅ | ✅* | ❌ |
| user.deactivate | ✅ | ✅* | ❌ |
| user.reset_pin | ✅ | ✅* | ❌ |
| **Categories** |
| category.view | ✅ | ✅ | ✅ |
| category.create | ✅ | ✅ | ❌ |
| category.edit | ✅ | ✅ | ❌ |
| category.delete | ✅ | ✅ | ❌ |
| **Products** |
| product.view | ✅ | ✅ | ✅ |
| product.create | ✅ | ✅ | ❌ |
| product.edit | ✅ | ✅ | ❌ |
| product.delete | ✅ | ✅ | ❌ |
| product.import | ✅ | ✅ | ❌ |
| **Suppliers** |
| supplier.view | ✅ | ✅ | ❌ |
| supplier.create | ✅ | ✅ | ❌ |
| supplier.edit | ✅ | ✅ | ❌ |
| supplier.delete | ✅ | ✅ | ❌ |
| **Customers** |
| customer.view | ✅ | ✅ | ✅ |
| customer.create | ✅ | ✅ | ✅ |
| customer.edit | ✅ | ✅ | ❌ |
| customer.delete | ✅ | ✅ | ❌ |
| **Inventory** |
| inventory.view | ✅ | ✅ | ❌ |
| inventory.purchase_in | ✅ | ✅ | ❌ |
| inventory.stock_check | ✅ | ✅ | ❌ |
| inventory.adjust | ✅ | ✅ | ❌ |
| **Orders / POS** |
| order.create (bán hàng) | ✅ | ✅ | ✅ |
| order.edit_price | ✅ | ✅ | ⚙️** |
| order.view_own | ✅ | ✅ | ✅ |
| order.view_all | ✅ | ✅ | ❌ |
| order.refund | ✅ | ✅ | ❌ |
| order.cancel | ✅ | ✅ | ❌ |
| order.apply_discount | ✅ | ✅ | ⚙️** |
| **Reports** |
| report.view | ✅ | ✅ | ❌ |
| report.export | ✅ | ✅ | ❌ |
| **Devices** |
| device.view | ✅ | ✅ | ❌ |
| device.approve | ✅ | ❌ | ❌ |
| device.remove | ✅ | ❌ | ❌ |
| **Data** |
| data.backup | ✅ | ❌ | ❌ |
| data.restore | ✅ | ❌ | ❌ |
| data.delete_all | ✅ | ❌ | ❌ |
| data.google_connect | ✅ | ❌ | ❌ |
| data.backup_settings | ✅ | ❌ | ❌ |

*\* Manager chỉ quản lý Cashier*  
*\*\* Cashier giảm giá tùy thuộc cài đặt: tắt / giới hạn % / không giới hạn*

### 7.3.2 Permission Check Implementation

```
// Pseudo code cho permission middleware

function checkPermission(user: User, permission: String): Boolean {
    // 1. Lấy role permissions
    rolePermissions = getRolePermissions(user.role)
    
    // 2. Kiểm tra custom permissions (nếu có override)
    customPermissions = getCustomPermissions(user.id)
    
    // 3. Merge: custom overrides role
    effectivePermissions = merge(rolePermissions, customPermissions)
    
    // 4. Check
    return effectivePermissions.contains(permission)
}

// Sử dụng trong Service
function createProduct(user: User, input: CreateProductInput): Result<Product> {
    if (!checkPermission(user, 'product.create')) {
        return Error(INSUFFICIENT_PERMISSION, "Bạn không có quyền thêm sản phẩm")
    }
    // ... logic tạo SP
}
```

### 7.3.3 Cài đặt phân quyền Cashier (tùy chỉnh bởi Owner)

```
CashierPermissionSettings {
    canApplyDiscount: Boolean           // Được phép giảm giá?
    maxDiscountPercent: Integer          // Giảm tối đa bao nhiêu %? (VD: 10%)
    canViewStock: Boolean               // Xem tồn kho?
    canViewPrice: Boolean               // Xem giá nhập?
    requirePasswordForRefund: Boolean   // Cần mật khẩu Manager để trả hàng?
    canOpenCashDrawer: Boolean          // Mở ngăn tiền?
    canCancelOrder: Boolean             // Hủy đơn hàng?
}
```

## 7.4 Bảo vệ dữ liệu local

### 7.4.1 Database Encryption

```
Sử dụng: SQLCipher (mã hóa toàn bộ SQLite database)

Encryption key:
- Sinh từ device unique key + store_id
- Lưu trong Keychain (iOS) / Android Keystore
- AES-256 encryption

Khi nào mã hóa:
- Database file luôn được mã hóa trên disk
- Giải mã in-memory khi app đang chạy
- Tự động khóa khi app vào background
```

### 7.4.2 Sensitive Data Handling

| Dữ liệu | Cách xử lý |
|---------|-----------|
| PIN | Hash bcrypt, không lưu plaintext |
| Password | Hash bcrypt, không lưu plaintext |
| Hình ảnh SP | Lưu trong app sandbox, không public |
| Logo cửa hàng | Lưu trong app sandbox |
| Thông tin NCC | Mã hóa trong database |
| Lịch sử đơn hàng | Mã hóa trong database |

### 7.4.3 Data Backup Security

```
Backup:
- Export database → Encrypt với password do Owner đặt
- File backup: .minipos.bak (encrypted)
- Không backup PIN/Password hash
- Backup lưu local hoặc share qua kênh an toàn

Restore:
- Yêu cầu nhập password backup
- Decrypt → Validate schema → Import
- Tạo lại accounts (Owner phải set PIN mới)
```

## 7.5 Bảo mật truyền tải (Sync Security)

### 7.5.1 Connection Security

```
1. Device Discovery:
   - Chỉ thiết bị biết store_code mới tìm được nhau
   - store_code KHÔNG được broadcast (thiết bị mới phải nhập)

2. Handshake:
   Device A                          Device B
      │                                  │
      │◄──── HELLO {store_code} ────────│
      │                                  │
      │ Verify store_code                │
      │                                  │
      │──── CHALLENGE {nonce} ─────────►│
      │                                  │
      │◄──── RESPONSE {signed_nonce} ───│
      │                                  │
      │ Verify signature                 │
      │                                  │
      │──── CONNECTED ─────────────────►│

3. Data Transfer:
   - TLS 1.3 cho WebSocket connection
   - Mỗi message có HMAC-SHA256 checksum
   - Replay attack protection (nonce + timestamp)
```

### 7.5.2 Data Sync Security Rules

```
1. PIN và Password hash ĐƯỢC đồng bộ (vì cần đăng nhập trên mọi thiết bị)
   → Nhưng chỉ sync HASH, không bao giờ sync plaintext

2. Chỉ sync dữ liệu trong phạm vi store_id hiện tại

3. Thiết bị bị xóa (removed):
   → Ngắt kết nối ngay lập tức
   → Xóa toàn bộ dữ liệu cửa hàng trên thiết bị đó
   → Revoke device certificate

4. Rate limiting:
   → Tối đa 100 messages/giây/thiết bị
   → Tối đa 10MB data/phút/thiết bị (cho full sync)
```

## 7.6 Bảo vệ khi mất thiết bị

### 7.6.1 Biện pháp phòng ngừa

```
1. Auto-lock: App tự khóa khi không hoạt động
2. PIN required: Luôn yêu cầu PIN khi mở app
3. No sensitive data in notifications
4. No sensitive data in app switcher (blur screenshot)
5. Database encrypted at rest (SQLCipher)
```

### 7.6.2 Khi thiết bị bị mất

```
Quy trình xử lý:

1. Owner sử dụng thiết bị khác đăng nhập vào cửa hàng
2. Vào Settings → Devices → Chọn thiết bị bị mất
3. Nhấn "Xóa thiết bị"
4. Thiết bị bị mất:
   - Nếu còn kết nối: nhận lệnh xóa → tự xóa dữ liệu
   - Nếu offline: dữ liệu vẫn được bảo vệ bởi encryption + PIN
   - Khi online lại: nhận lệnh xóa → tự xóa dữ liệu
5. Tất cả session của thiết bị bị mất bị invalidate
```

## 7.7 Audit Log

Ghi lại các thao tác quan trọng:

```sql
CREATE TABLE audit_logs (
    id          TEXT PRIMARY KEY,
    store_id    TEXT NOT NULL,
    user_id     TEXT NOT NULL,
    action      TEXT NOT NULL,      -- Thao tác
    entity_type TEXT,               -- Bảng/đối tượng
    entity_id   TEXT,               -- ID đối tượng
    old_value   TEXT,               -- JSON: giá trị cũ
    new_value   TEXT,               -- JSON: giá trị mới
    ip_address  TEXT,
    device_id   TEXT NOT NULL,
    created_at  INTEGER NOT NULL,
    
    -- Sync
    sync_version INTEGER DEFAULT 0
);
```

**Các thao tác cần audit:**

| Action | Mô tả |
|--------|-------|
| `auth.login` | Đăng nhập |
| `auth.login_failed` | Đăng nhập thất bại |
| `auth.logout` | Đăng xuất |
| `user.created` | Tạo người dùng |
| `user.deactivated` | Vô hiệu hóa tài khoản |
| `user.pin_reset` | Reset PIN |
| `product.deleted` | Xóa sản phẩm |
| `order.refunded` | Hoàn tiền |
| `order.cancelled` | Hủy đơn hàng |
| `inventory.adjusted` | Điều chỉnh tồn kho |
| `device.approved` | Chấp nhận thiết bị mới |
| `device.removed` | Xóa thiết bị |
| `store.settings_changed` | Thay đổi cài đặt |
| `data.backup_created` | Tạo backup |
| `data.backup_uploaded` | Upload backup lên Google Drive |
| `data.backup_deleted` | Xóa bản backup trên Google Drive |
| `data.restored` | Khôi phục dữ liệu |
| `data.google_connected` | Kết nối tài khoản Google |
| `data.google_disconnected` | Ngắt kết nối tài khoản Google |

## 7.8 Bảo mật Backup & Restore Google Drive

> Chi tiết xem tài liệu [09-backup-restore-gdrive.md](./09-backup-restore-gdrive.md)

| Biện pháp | Chi tiết |
|-----------|---------|
| **Mã hóa backup** | AES-256-GCM, key derived bằng PBKDF2 (100,000 iterations) |
| **Mật khẩu backup** | Tách biệt với mật khẩu đăng nhập, tối thiểu 8 ký tự |
| **OAuth token** | Lưu trong Keychain (iOS) / Keystore (Android), encrypted |
| **Scope tối thiểu** | `drive.appdata` — chỉ truy cập folder ẩn của app |
| **Checksum** | SHA-256 cho mỗi file backup, verify trước khi restore |
| **Tamper detection** | GCM auth tag phát hiện file bị chỉnh sửa |
| **Quên mật khẩu** | Không thể khôi phục — cảnh báo rõ ràng cho user |
| **Quyền backup** | Chỉ Owner mới được backup/restore |
| **Restore warning** | Xác nhận 2 bước (dialog + mật khẩu quản trị) trước khi restore |

## 7.9 Checklist bảo mật

- [ ] PIN hash với bcrypt (cost ≥ 10)
- [ ] Password hash với bcrypt (cost ≥ 12)
- [ ] Database encrypted với SQLCipher
- [ ] Sensitive data trong Keychain/Keystore
- [ ] Auto-lock khi inactive
- [ ] Blur app screenshot trong app switcher
- [ ] TLS cho P2P sync
- [ ] Message checksum (HMAC-SHA256)
- [ ] Device approval required
- [ ] Audit log cho thao tác nhạy cảm
- [ ] Brute-force protection (PIN/Password)
- [ ] No sensitive data in logs/console
- [ ] Data wipe khi device removed
- [ ] Secure backup/restore
- [ ] Backup encrypted với AES-256-GCM
- [ ] Backup password PBKDF2 (iterations ≥ 100,000)
- [ ] Google OAuth token trong Keychain/Keystore
- [ ] Google Drive scope tối thiểu (drive.appdata)
- [ ] Checksum SHA-256 cho backup file
- [ ] Restore requires admin password confirmation
