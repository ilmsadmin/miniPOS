# 9. Backup & Restore lên Google Drive

## 9.1 Tổng quan

Tính năng cho phép Owner **sao lưu (backup)** toàn bộ dữ liệu cửa hàng lên **Google Drive** và **khôi phục (restore)** khi cần. Đây là lớp bảo vệ dữ liệu quan trọng trong trường hợp mất thiết bị, hỏng máy, hoặc muốn chuyển dữ liệu sang thiết bị mới mà không cần kết nối P2P.

### Tại sao cần Cloud Backup?

| Vấn đề | P2P Sync | Google Drive Backup |
|--------|----------|-------------------|
| Mất tất cả thiết bị | ❌ Mất dữ liệu | ✅ Khôi phục được |
| Thiết bị hỏng, duy nhất | ❌ Mất dữ liệu | ✅ Khôi phục được |
| Chuyển thiết bị mới (không cùng WiFi) | ❌ Cần cùng mạng | ✅ Restore từ cloud |
| Backup định kỳ phòng sự cố | ❌ Không hỗ trợ | ✅ Tự động / Thủ công |
| Lưu trữ lịch sử dữ liệu | ❌ Chỉ có hiện tại | ✅ Nhiều phiên bản |

### Tại sao chọn Google Drive?

| Tiêu chí | Google Drive | iCloud | Dropbox |
|----------|-------------|--------|---------|
| Miễn phí | 15 GB | 5 GB | 2 GB |
| Android support | ✅ Native | ❌ | ✅ |
| iOS support | ✅ | ✅ Native | ✅ |
| API dễ tích hợp | ✅ | Trung bình | ✅ |
| Phổ biến ở VN | ✅ Rất phổ biến | Trung bình | Ít |
| **Kết luận** | **⭐ Ưu tiên** | Hỗ trợ sau (P3) | Không cần |

## 9.2 Kiến trúc tổng quan

```
┌─────────────────────────────────────────────────────────────────┐
│                        miniPOS App                               │
│                                                                  │
│  ┌──────────────┐    ┌──────────────────┐    ┌───────────────┐  │
│  │   SQLite DB   │───►│  BackupService   │───►│ Google Drive  │  │
│  │  (encrypted)  │    │                  │    │    API        │  │
│  └──────────────┘    │  1. Export DB     │    └───────┬───────┘  │
│                      │  2. Compress      │            │          │
│                      │  3. Encrypt       │            │          │
│                      │  4. Upload        │            ▼          │
│                      └──────────────────┘    ┌───────────────┐  │
│                                              │  Google Drive  │  │
│  ┌──────────────┐    ┌──────────────────┐    │   Cloud        │  │
│  │   SQLite DB   │◄───│  RestoreService  │◄───│               │  │
│  │  (restored)   │    │                  │    │  📁 miniPOS/  │  │
│  └──────────────┘    │  1. Download     │    │  ├─ backup_1   │  │
│                      │  2. Decrypt      │    │  ├─ backup_2   │  │
│                      │  3. Decompress   │    │  └─ backup_3   │  │
│                      │  4. Validate     │    └───────────────┘  │
│                      │  5. Import       │                        │
│                      └──────────────────┘                        │
└─────────────────────────────────────────────────────────────────┘
```

## 9.3 Yêu cầu chức năng

### FR-070: Kết nối Google Account

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | Owner kết nối tài khoản Google để sử dụng Google Drive |
| **Ưu tiên** | Trung bình (P1) |
| **Vai trò** | Owner |

**Luồng chính:**
1. Owner vào Settings → Backup & Restore
2. Nhấn "Kết nối Google Drive"
3. Hiển thị Google Sign-In (OAuth 2.0)
4. Chọn tài khoản Google
5. Cấp quyền truy cập Google Drive (chỉ scope `drive.appdata` hoặc `drive.file`)
6. Kết nối thành công → Hiển thị tên + email tài khoản
7. Lưu refresh token vào Keychain/Keystore (encrypted)

**Luồng thay thế:**
- 4a. Người dùng hủy → Quay lại Settings
- 5a. Cấp quyền thất bại → Hiển thị lỗi, cho phép thử lại
- Hỗ trợ ngắt kết nối (disconnect) Google Account

**Quy tắc nghiệp vụ:**
- Chỉ Owner mới được kết nối/ngắt kết nối Google Account
- Mỗi cửa hàng chỉ kết nối 1 Google Account
- Token được lưu encrypted trong Keychain/Keystore
- Khi token hết hạn → tự động refresh, nếu fail → yêu cầu đăng nhập lại

---

### FR-071: Backup thủ công

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | Owner tạo bản backup thủ công lên Google Drive |
| **Ưu tiên** | Trung bình (P1) |
| **Vai trò** | Owner |

**Luồng chính:**
1. Owner vào Settings → Backup & Restore
2. Nhấn "Tạo bản sao lưu ngay"
3. Nhập mật khẩu quản trị để xác nhận
4. Hiển thị progress: Đang chuẩn bị dữ liệu... → Đang mã hóa... → Đang tải lên...
5. Hoàn tất → Hiển thị thông tin backup:
   - Thời gian tạo
   - Kích thước file
   - Số lượng records (SP, đơn hàng, v.v.)
6. Ghi audit log

**Dữ liệu được backup:**

| Bảng | Backup | Ghi chú |
|------|--------|---------|
| stores | ✅ | Thông tin cửa hàng |
| users | ✅ | Bao gồm PIN hash & password hash |
| devices | ❌ | Không backup (thiết bị mới phải đăng ký lại) |
| categories | ✅ | |
| suppliers | ✅ | |
| products | ✅ | |
| product_variants | ✅ | |
| inventory | ✅ | |
| stock_movements | ✅ | |
| purchase_orders | ✅ | |
| purchase_order_items | ✅ | |
| orders | ✅ | |
| order_items | ✅ | |
| order_payments | ✅ | |
| audit_logs | ✅ | |
| sync_log | ❌ | Không cần backup |
| schema_migrations | ✅ | Để validate version khi restore |

**Dữ liệu KHÔNG backup:**
- Device records (mỗi thiết bị phải đăng ký lại sau restore)
- Sync logs (không cần thiết)
- Session data
- Google OAuth tokens

---

### FR-072: Backup tự động

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | Tự động backup theo lịch định kỳ |
| **Ưu tiên** | Trung bình (P1) |
| **Vai trò** | Owner (cấu hình) |

**Cấu hình auto backup:**

```
AutoBackupSettings {
    enabled: Boolean              // Bật/tắt auto backup
    frequency: Enum               // daily / weekly / monthly
    time: TimeOfDay               // Giờ backup (VD: 02:00 AM)
    dayOfWeek?: DayOfWeek         // Cho weekly (VD: Chủ nhật)
    dayOfMonth?: Integer          // Cho monthly (VD: ngày 1)
    wifiOnly: Boolean             // Chỉ backup khi có WiFi
    maxBackupCount: Integer       // Giữ tối đa bao nhiêu bản (default: 7)
    lastBackupAt: DateTime?       // Thời gian backup cuối
    lastBackupStatus: String?     // success / failed
}
```

**Logic auto backup:**
1. Background scheduler kiểm tra điều kiện:
   - Auto backup đã bật?
   - Đến thời gian backup?
   - Có WiFi (nếu `wifiOnly = true`)?
   - Google Account đã kết nối?
2. Nếu đủ điều kiện → Thực hiện backup (giống thủ công nhưng silent)
3. Sau khi backup xong → Kiểm tra số lượng backup trên Drive
4. Nếu vượt `maxBackupCount` → Xóa bản cũ nhất
5. Cập nhật `lastBackupAt` và `lastBackupStatus`
6. Nếu fail → Retry sau 1 giờ (tối đa 3 lần)
7. Nếu vẫn fail → Gửi notification cho Owner

**Điều kiện trigger backup tự động bổ sung:**
- Khi có > 50 đơn hàng mới kể từ lần backup cuối
- Khi Owner thay đổi cài đặt quan trọng (giá, sản phẩm hàng loạt)

---

### FR-073: Restore (Khôi phục dữ liệu)

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | Khôi phục dữ liệu từ bản backup trên Google Drive |
| **Ưu tiên** | Trung bình (P1) |
| **Vai trò** | Owner |

**Luồng chính:**
1. Owner vào Settings → Backup & Restore → "Khôi phục dữ liệu"
2. Đăng nhập Google (nếu chưa kết nối)
3. Hiển thị danh sách các bản backup:
   - Thời gian tạo
   - Kích thước
   - Phiên bản app
   - Tóm tắt dữ liệu (số SP, số đơn hàng...)
4. Chọn bản backup muốn khôi phục
5. **Cảnh báo quan trọng:** 
   > "⚠️ Khôi phục sẽ XÓA TOÀN BỘ dữ liệu hiện tại trên thiết bị này và thay thế bằng dữ liệu từ bản sao lưu. Hành động này không thể hoàn tác. Bạn có chắc chắn?"
6. Nhập mật khẩu quản trị xác nhận
7. Nhập mật khẩu backup (mật khẩu mã hóa backup)
8. Progress: Đang tải xuống... → Đang giải mã... → Đang kiểm tra... → Đang khôi phục...
9. Hoàn tất → Yêu cầu đăng nhập lại
10. Ghi audit log

**Luồng thay thế — Restore trên thiết bị mới (lần đầu mở app):**
1. Mở app lần đầu → Welcome Screen
2. Chọn "Khôi phục từ bản sao lưu" (nút thứ 3, bên dưới Tạo/Tham gia)
3. Đăng nhập Google
4. Chọn bản backup
5. Nhập mật khẩu backup
6. Restore dữ liệu
7. Tạo device record cho thiết bị mới
8. Đăng nhập bằng tài khoản Owner

**Validation khi restore:**
- Kiểm tra app version tương thích (backup version ≤ current version)
- Kiểm tra database schema version
- Kiểm tra checksum file backup (không bị corrupt)
- Kiểm tra mật khẩu giải mã đúng

---

### FR-074: Quản lý bản backup

| Thuộc tính | Chi tiết |
|-----------|---------|
| **Mô tả** | Xem, xóa các bản backup trên Google Drive |
| **Ưu tiên** | Thấp (P2) |
| **Vai trò** | Owner |

**Chức năng:**
- Xem danh sách tất cả bản backup
- Xem chi tiết từng bản backup (metadata)
- Xóa bản backup cũ
- Đổi tên bản backup (ghi chú)
- Tải backup về local (file .minipos.bak)

---

## 9.4 Thiết kế kỹ thuật chi tiết

### 9.4.1 Google Drive API Integration

**OAuth 2.0 Scopes:**
```
Scope tối thiểu (khuyến nghị):
- https://www.googleapis.com/auth/drive.appdata
  → Chỉ truy cập thư mục ẩn "appDataFolder" của app
  → Người dùng KHÔNG thấy file backup trong Google Drive UI
  → Bảo mật cao nhất

Scope thay thế (nếu muốn user thấy file):
- https://www.googleapis.com/auth/drive.file
  → Chỉ truy cập file do app tạo
  → Người dùng thấy file trong Google Drive
  → Có thể tự copy/di chuyển
```

**Cấu trúc thư mục trên Google Drive:**
```
Google Drive/
└── appDataFolder/ (ẩn) hoặc miniPOS/ (nếu dùng drive.file)
    └── {store_id}/
        ├── backup_20260325_143000.minipos.bak
        ├── backup_20260324_020000.minipos.bak
        ├── backup_20260323_020000.minipos.bak
        └── backup_metadata.json
```

**backup_metadata.json:**
```json
{
    "store_id": "uuid-of-store",
    "store_name": "Zin100",
    "store_code": "ZIN100",
    "backups": [
        {
            "id": "gdrive-file-id-1",
            "filename": "backup_20260325_143000.minipos.bak",
            "created_at": "2026-03-25T14:30:00+07:00",
            "app_version": "1.2.0",
            "db_version": 3,
            "file_size_bytes": 5242880,
            "checksum_sha256": "abc123...",
            "type": "manual",
            "summary": {
                "products_count": 150,
                "orders_count": 1250,
                "categories_count": 12,
                "suppliers_count": 8,
                "users_count": 4,
                "date_range": {
                    "first_order": "2026-01-15",
                    "last_order": "2026-03-25"
                }
            },
            "notes": ""
        }
    ]
}
```

### 9.4.2 Backup File Format

**Cấu trúc file `.minipos.bak`:**

```
┌─────────────────────────────────────┐
│           File Header (256 bytes)    │
│  ┌─────────────────────────────┐    │
│  │ Magic: "MINIPOS_BACKUP"     │    │
│  │ Version: 1                  │    │
│  │ App Version: "1.2.0"       │    │
│  │ DB Schema Version: 3       │    │
│  │ Created At: timestamp      │    │
│  │ Store ID: uuid             │    │
│  │ Encryption: AES-256-GCM    │    │
│  │ Compression: gzip          │    │
│  │ Checksum: SHA-256          │    │
│  │ Data Offset: 256           │    │
│  │ Data Length: xxxx bytes    │    │
│  └─────────────────────────────┘    │
├─────────────────────────────────────┤
│           Encrypted Data             │
│  ┌─────────────────────────────┐    │
│  │ IV (16 bytes)               │    │
│  │ Auth Tag (16 bytes)         │    │
│  │ Encrypted Payload:          │    │
│  │   gzip(json_data)          │    │
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
```

**JSON Data Structure (trước khi encrypt):**
```json
{
    "version": 1,
    "store": { "...store record..." },
    "users": [ { "...user records..." } ],
    "categories": [ { "..." } ],
    "suppliers": [ { "..." } ],
    "products": [ { "..." } ],
    "product_variants": [ { "..." } ],
    "inventory": [ { "..." } ],
    "stock_movements": [ { "..." } ],
    "purchase_orders": [ { "..." } ],
    "purchase_order_items": [ { "..." } ],
    "orders": [ { "..." } ],
    "order_items": [ { "..." } ],
    "order_payments": [ { "..." } ],
    "audit_logs": [ { "..." } ],
    "schema_version": 3,
    "record_counts": {
        "users": 4,
        "categories": 12,
        "products": 150,
        "orders": 1250
    }
}
```

### 9.4.3 Mã hóa Backup

```
Encryption Process:

1. Người dùng đặt "Mật khẩu backup" (≥ 8 ký tự)
   - Mật khẩu này KHÁC với mật khẩu đăng nhập
   - Lưu ý: NẾU QUÊN → KHÔNG THỂ RESTORE

2. Derive encryption key từ password:
   Key = PBKDF2(
       password: backup_password,
       salt: random_32_bytes,
       iterations: 100000,
       keyLength: 256 bits,
       hash: SHA-256
   )

3. Encrypt data:
   - Algorithm: AES-256-GCM
   - IV: random 16 bytes (mỗi backup khác nhau)
   - Input: gzip(json_data)
   - Output: IV + AuthTag + CipherText

4. File = Header + Salt + EncryptedData
```

```
Decryption Process (Restore):

1. Đọc Header → validate magic number, version
2. Đọc Salt
3. Nhập backup password
4. Derive key: PBKDF2(password, salt, ...)
5. Decrypt: AES-256-GCM(key, IV, ciphertext)
6. Verify AuthTag (tamper detection)
7. Decompress: gunzip(decrypted_data)
8. Parse JSON
9. Validate schema version
10. Import vào database
```

### 9.4.4 Chunked Upload/Download (Cho file lớn)

```
Khi backup file > 5MB:

Upload Strategy:
1. Sử dụng Google Drive Resumable Upload API
2. Chunk size: 256KB
3. Retry logic: 3 retries per chunk, exponential backoff
4. Progress tracking: (uploaded_bytes / total_bytes) * 100

Download Strategy:
1. Sử dụng Range header để download từng phần
2. Chunk size: 256KB
3. Verify checksum sau khi download xong
4. Resume download nếu bị gián đoạn

Estimated file sizes:
- 100 SP, 500 đơn: ~500KB
- 1,000 SP, 5,000 đơn: ~3MB
- 10,000 SP, 50,000 đơn: ~25MB
- Sau nén gzip: giảm ~60-70%
```

## 9.5 Service: BackupService

```
BackupService.connectGoogleAccount(): Result<GoogleAccount>
BackupService.disconnectGoogleAccount(): Result<void>
BackupService.getConnectedAccount(): Result<GoogleAccount?>

BackupService.createBackup(password: String, notes?: String): Stream<BackupProgress>
BackupService.getBackupList(): Result<List<BackupInfo>>
BackupService.getBackupDetail(backupId: String): Result<BackupDetail>
BackupService.deleteBackup(backupId: String): Result<void>
BackupService.renameBackup(backupId: String, notes: String): Result<void>

BackupService.restoreFromBackup(backupId: String, password: String): Stream<RestoreProgress>
BackupService.downloadBackupToLocal(backupId: String): Result<String>  // Returns local file path

BackupService.getAutoBackupSettings(): Result<AutoBackupSettings>
BackupService.updateAutoBackupSettings(settings: AutoBackupSettings): Result<void>
BackupService.getLastBackupInfo(): Result<BackupInfo?>
```

### 9.5.1 createBackup — Logic chi tiết

```
Stream<BackupProgress> createBackup(password, notes):

    yield BackupProgress(stage: 'preparing', progress: 0, message: 'Đang chuẩn bị dữ liệu...')

    // Step 1: Export data from SQLite
    data = {}
    tables = ['stores', 'users', 'categories', 'suppliers', 'products', 
              'product_variants', 'inventory', 'stock_movements', 
              'purchase_orders', 'purchase_order_items',
              'orders', 'order_items', 'order_payments', 'audit_logs']
    
    for (i, table) in tables.enumerated():
        data[table] = database.query("SELECT * FROM $table WHERE store_id = ?", [storeId])
        yield BackupProgress(stage: 'exporting', progress: (i / tables.length) * 30)
    
    yield BackupProgress(stage: 'exporting', progress: 30, message: 'Đã xuất dữ liệu')

    // Step 2: Build JSON
    json_string = JSON.encode({
        version: 1,
        ...data,
        schema_version: currentSchemaVersion,
        record_counts: { ... }
    })
    
    // Step 3: Compress
    compressed = gzip(json_string)
    yield BackupProgress(stage: 'compressing', progress: 50, message: 'Đang nén dữ liệu...')

    // Step 4: Encrypt
    salt = randomBytes(32)
    key = PBKDF2(password, salt, iterations: 100000)
    iv = randomBytes(16)
    encrypted = AES_GCM.encrypt(key, iv, compressed)
    yield BackupProgress(stage: 'encrypting', progress: 60, message: 'Đang mã hóa...')

    // Step 5: Build file
    file = buildBackupFile(header, salt, iv, encrypted.authTag, encrypted.cipherText)
    checksum = SHA256(file)
    
    // Step 6: Upload to Google Drive
    filename = "backup_${formatDate(now)}.minipos.bak"
    yield BackupProgress(stage: 'uploading', progress: 70, message: 'Đang tải lên Google Drive...')
    
    fileId = await googleDriveAPI.resumableUpload(
        file: file,
        filename: filename,
        folderId: getOrCreateStoreFolder(storeId),
        onProgress: (uploaded, total) => {
            yield BackupProgress(stage: 'uploading', progress: 70 + (uploaded/total) * 25)
        }
    )

    // Step 7: Update metadata
    await updateBackupMetadata(fileId, filename, checksum, notes)
    yield BackupProgress(stage: 'finalizing', progress: 98, message: 'Đang hoàn tất...')

    // Step 8: Cleanup old backups
    await cleanupOldBackups(maxCount: autoBackupSettings.maxBackupCount)

    // Step 9: Audit log
    auditLog.record('data.backup_created', { filename, size: file.length, checksum })

    yield BackupProgress(stage: 'completed', progress: 100, message: 'Sao lưu thành công!')
```

### 9.5.2 restoreFromBackup — Logic chi tiết

```
Stream<RestoreProgress> restoreFromBackup(backupId, password):

    yield RestoreProgress(stage: 'downloading', progress: 0, message: 'Đang tải xuống...')

    // Step 1: Download from Google Drive
    file = await googleDriveAPI.download(
        fileId: backupId,
        onProgress: (downloaded, total) => {
            yield RestoreProgress(stage: 'downloading', progress: (downloaded/total) * 30)
        }
    )

    // Step 2: Validate file header
    header = parseHeader(file)
    if (header.magic != "MINIPOS_BACKUP"):
        throw Error(INVALID_BACKUP_FILE, "File không phải backup miniPOS")
    if (header.dbVersion > currentSchemaVersion):
        throw Error(INCOMPATIBLE_VERSION, "Backup từ phiên bản app mới hơn. Vui lòng cập nhật app.")
    
    yield RestoreProgress(stage: 'validating', progress: 35, message: 'Đang kiểm tra...')

    // Step 3: Verify checksum
    if (SHA256(file.data) != header.checksum):
        throw Error(CORRUPTED_BACKUP, "File backup bị hỏng")

    // Step 4: Decrypt
    yield RestoreProgress(stage: 'decrypting', progress: 40, message: 'Đang giải mã...')
    salt = file.salt
    key = PBKDF2(password, salt, iterations: 100000)
    try:
        decrypted = AES_GCM.decrypt(key, file.iv, file.cipherText, file.authTag)
    catch AuthenticationError:
        throw Error(WRONG_BACKUP_PASSWORD, "Mật khẩu backup không đúng")

    // Step 5: Decompress
    json_string = gunzip(decrypted)
    data = JSON.decode(json_string)
    yield RestoreProgress(stage: 'decompressing', progress: 50, message: 'Đang xử lý dữ liệu...')

    // Step 6: Run migrations if needed
    if (data.schema_version < currentSchemaVersion):
        data = runDataMigrations(data, from: data.schema_version, to: currentSchemaVersion)

    // Step 7: Clear current database
    yield RestoreProgress(stage: 'clearing', progress: 55, message: 'Đang chuẩn bị database...')
    database.transaction(() => {
        // Xóa dữ liệu cũ theo thứ tự dependency ngược
        for table in reversed(tables):
            database.execute("DELETE FROM $table")
    })

    // Step 8: Import data
    yield RestoreProgress(stage: 'importing', progress: 60, message: 'Đang khôi phục dữ liệu...')
    importOrder = ['stores', 'users', 'categories', 'suppliers', 'products',
                   'product_variants', 'inventory', 'stock_movements',
                   'purchase_orders', 'purchase_order_items',
                   'orders', 'order_items', 'order_payments', 'audit_logs']
    
    database.transaction(() => {
        for (i, table) in importOrder.enumerated():
            records = data[table]
            for record in records:
                database.insert(table, record)
            yield RestoreProgress(
                stage: 'importing', 
                progress: 60 + (i / importOrder.length) * 30,
                message: "Đang khôi phục $table..."
            )
    })

    // Step 9: Register current device
    device = createDeviceRecord(currentDeviceInfo)
    database.insert('devices', device)
    
    // Step 10: Clear sessions, require re-login
    clearAllSessions()
    
    // Step 11: Audit log
    auditLog.record('data.restored', { 
        backup_id: backupId, 
        backup_date: header.createdAt,
        records_restored: data.record_counts 
    })
    
    yield RestoreProgress(stage: 'completed', progress: 100, message: 'Khôi phục thành công!')
    
    // Step 12: Navigate to login screen
    navigateToLogin()
```

## 9.6 Database — Bảng backup_history

```sql
CREATE TABLE backup_history (
    id              TEXT PRIMARY KEY,
    store_id        TEXT NOT NULL,
    gdrive_file_id  TEXT,                      -- Google Drive file ID
    filename        TEXT NOT NULL,
    file_size       INTEGER NOT NULL,          -- bytes
    checksum        TEXT NOT NULL,             -- SHA-256
    type            TEXT NOT NULL CHECK(type IN ('manual', 'auto')),
    status          TEXT NOT NULL CHECK(status IN ('in_progress', 'completed', 'failed')),
    error_message   TEXT,
    app_version     TEXT NOT NULL,
    db_version      INTEGER NOT NULL,
    notes           TEXT,
    
    -- Summary
    products_count  INTEGER DEFAULT 0,
    orders_count    INTEGER DEFAULT 0,
    users_count     INTEGER DEFAULT 0,
    
    created_by      TEXT NOT NULL,
    created_at      INTEGER NOT NULL,
    
    FOREIGN KEY (store_id) REFERENCES stores(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE backup_settings (
    id              TEXT PRIMARY KEY,
    store_id        TEXT NOT NULL UNIQUE,
    google_email    TEXT,                       -- Email tài khoản Google đã kết nối
    is_connected    INTEGER DEFAULT 0,
    auto_enabled    INTEGER DEFAULT 0,
    frequency       TEXT DEFAULT 'weekly' CHECK(frequency IN ('daily', 'weekly', 'monthly')),
    backup_time     TEXT DEFAULT '02:00',       -- HH:mm
    day_of_week     INTEGER DEFAULT 0,          -- 0=Sunday, 1=Monday...
    day_of_month    INTEGER DEFAULT 1,
    wifi_only       INTEGER DEFAULT 1,
    max_backup_count INTEGER DEFAULT 7,
    last_backup_at  INTEGER,
    last_backup_status TEXT,
    
    -- Metadata
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL,
    
    FOREIGN KEY (store_id) REFERENCES stores(id)
);

CREATE INDEX idx_backup_history_store ON backup_history(store_id);
CREATE INDEX idx_backup_history_date ON backup_history(created_at);
```

## 9.7 Thiết kế UI — Màn hình Backup & Restore

### 9.7.1 Màn hình chính

```
┌─────────────────────────────┐
│  ← Sao lưu & Khôi phục     │
├─────────────────────────────┤
│                             │
│  ☁️ Google Drive             │
│  ┌─────────────────────┐    │
│  │ 📧 minh@gmail.com   │    │
│  │ ✅ Đã kết nối        │    │
│  │         [Ngắt kết nối]│   │
│  └─────────────────────┘    │
│                             │
│  ─────────────────────      │
│                             │
│  📊 Bản sao lưu gần nhất    │
│  ┌─────────────────────┐    │
│  │ 📅 25/03/2026 14:30  │    │
│  │ 📦 2.3 MB            │    │
│  │ 📝 150 SP, 1250 đơn  │    │
│  │ ✅ Thành công         │    │
│  └─────────────────────┘    │
│                             │
│  ┌─────────────────────┐    │
│  │ ☁️ TẠO BẢN SAO LƯU   │    │
│  │      NGAY             │    │
│  └─────────────────────┘    │
│                             │
│  ─────────────────────      │
│                             │
│  ⏰ Sao lưu tự động         │
│  ┌─────────────────────┐    │
│  │ Bật/Tắt        [🔵] │    │
│  │ Tần suất:  Hàng tuần │    │
│  │ Thời gian: CN 02:00  │    │
│  │ Chỉ WiFi:      [🔵] │    │
│  │ Giữ tối đa: 7 bản   │    │
│  └─────────────────────┘    │
│                             │
│  ─────────────────────      │
│                             │
│  📋 Lịch sử sao lưu        │
│  ┌─────────────────────┐    │
│  │ 25/03 14:30  Thủ công│    │
│  │ 2.3MB  ✅    [⋮]     │    │
│  ├─────────────────────┤    │
│  │ 24/03 02:00  Tự động │    │
│  │ 2.1MB  ✅    [⋮]     │    │
│  ├─────────────────────┤    │
│  │ 17/03 02:00  Tự động │    │
│  │ 1.8MB  ✅    [⋮]     │    │
│  └─────────────────────┘    │
│                             │
│  ─────────────────────      │
│                             │
│  ┌─────────────────────┐    │
│  │ 🔄 KHÔI PHỤC DỮ LIỆU │   │
│  │  từ bản sao lưu      │    │
│  └─────────────────────┘    │
│                             │
└─────────────────────────────┘
```

### 9.7.2 Dialog — Tạo backup

```
┌─────────────────────────────┐
│        Tạo bản sao lưu      │
├─────────────────────────────┤
│                             │
│  Mật khẩu mã hóa backup *   │
│  ┌─────────────────────┐    │
│  │ ••••••••         👁  │    │
│  └─────────────────────┘    │
│  ⚠️ Hãy ghi nhớ mật khẩu   │
│  này! Không có cách khôi    │
│  phục nếu bạn quên.        │
│                             │
│  Xác nhận mật khẩu *        │
│  ┌─────────────────────┐    │
│  │ ••••••••         👁  │    │
│  └─────────────────────┘    │
│                             │
│  Ghi chú (tùy chọn)         │
│  ┌─────────────────────┐    │
│  │ Backup trước khi     │    │
│  │ cập nhật giá         │    │
│  └─────────────────────┘    │
│                             │
│  ┌──────────┐ ┌──────────┐ │
│  │   Hủy    │ │ Sao lưu  │ │
│  └──────────┘ └──────────┘ │
└─────────────────────────────┘
```

### 9.7.3 Progress — Đang backup/restore

```
┌─────────────────────────────┐
│                             │
│        ☁️↑                   │
│   Đang tải lên              │
│   Google Drive...            │
│                             │
│  ┌─────────────────────┐    │
│  │████████████░░░░░ 75%│    │
│  └─────────────────────┘    │
│                             │
│  Đã tải: 1.7 MB / 2.3 MB   │
│  Tốc độ: ~500 KB/s          │
│  Còn lại: ~2 giây           │
│                             │
│  ┌─────────────────────┐    │
│  │ ✅ Xuất dữ liệu      │    │
│  │ ✅ Nén dữ liệu       │    │
│  │ ✅ Mã hóa            │    │
│  │ ⏳ Tải lên Drive...   │    │
│  │ ○ Hoàn tất           │    │
│  └─────────────────────┘    │
│                             │
│      [Hủy sao lưu]         │
│                             │
└─────────────────────────────┘
```

### 9.7.4 Chọn bản backup để restore

```
┌─────────────────────────────┐
│  ← Chọn bản sao lưu        │
├─────────────────────────────┤
│                             │
│  📧 minh@gmail.com          │
│  🏪 Cửa hàng: Zin100        │
│                             │
│  ┌─────────────────────┐    │
│  │ 📅 25/03/2026 14:30  │    │
│  │ 📱 App v1.2.0        │    │
│  │ 📦 2.3 MB            │    │
│  │ ─────────────────── │    │
│  │ 📝 150 SP            │    │
│  │ 🛒 1,250 đơn hàng    │    │
│  │ 👥 4 tài khoản       │    │
│  │ 📂 12 danh mục       │    │
│  │ 📋 Backup trước khi  │    │
│  │    cập nhật giá       │    │
│  │                      │    │
│  │ [KHÔI PHỤC BẢN NÀY]  │    │
│  └─────────────────────┘    │
│                             │
│  ┌─────────────────────┐    │
│  │ 📅 24/03/2026 02:00  │    │
│  │ 📱 App v1.2.0        │    │
│  │ 📦 2.1 MB | Tự động  │    │
│  │ 🛒 1,230 đơn hàng    │    │
│  │                      │    │
│  │ [KHÔI PHỤC BẢN NÀY]  │    │
│  └─────────────────────┘    │
│                             │
│  ┌─────────────────────┐    │
│  │ 📅 17/03/2026 02:00  │    │
│  │ 📱 App v1.1.0        │    │
│  │ 📦 1.8 MB | Tự động  │    │
│  │ 🛒 1,100 đơn hàng    │    │
│  │                      │    │
│  │ [KHÔI PHỤC BẢN NÀY]  │    │
│  └─────────────────────┘    │
│                             │
└─────────────────────────────┘
```

### 9.7.5 Welcome Screen (cập nhật — thêm nút Restore)

```
┌─────────────────────────────┐
│         miniPOS             │
│                             │
│     ┌───────────────┐       │
│     │   📱 Logo     │       │
│     │   miniPOS     │       │
│     └───────────────┘       │
│                             │
│  Hệ thống bán hàng đơn giản│
│  cho cửa hàng của bạn       │
│                             │
│  ┌─────────────────────┐    │
│  │  🏪 Tạo cửa hàng   │    │
│  │       mới           │    │
│  └─────────────────────┘    │
│                             │
│  ┌─────────────────────┐    │
│  │  🔗 Tham gia        │    │
│  │     cửa hàng        │    │
│  └─────────────────────┘    │
│                             │
│  ┌─────────────────────┐    │
│  │  ☁️ Khôi phục từ     │    │
│  │     bản sao lưu     │    │
│  └─────────────────────┘    │
│                             │
└─────────────────────────────┘
```

## 9.8 Xử lý lỗi & Edge Cases

| Tình huống | Xử lý |
|-----------|-------|
| **Mất mạng khi đang upload** | Pause → auto resume khi có mạng (Resumable Upload) |
| **App bị kill khi đang backup** | File tạm bị xóa, backup chưa complete không hiển thị trong list |
| **Dung lượng Drive hết** | Thông báo lỗi, gợi ý xóa backup cũ hoặc dọn Drive |
| **Token Google hết hạn** | Tự refresh, nếu fail → yêu cầu đăng nhập lại |
| **Backup file bị corrupt** | Phát hiện qua checksum SHA-256, thông báo lỗi |
| **Sai mật khẩu backup** | AES-GCM auth tag fail, thông báo "Sai mật khẩu" |
| **Quên mật khẩu backup** | ❌ KHÔNG có cách khôi phục → hiển thị cảnh báo rõ ràng khi tạo |
| **Version app cũ restore backup mới** | Kiểm tra version, yêu cầu cập nhật app |
| **Restore khi có dữ liệu** | Cảnh báo sẽ xóa hết → yêu cầu confirm + nhập mật khẩu quản trị |
| **Nhiều cửa hàng trên 1 account** | Mỗi cửa hàng backup riêng theo store_id folder |
| **2 thiết bị backup cùng lúc** | Google Drive file locking → thiết bị thứ 2 đợi hoặc retry |

## 9.9 Giới hạn & Khuyến nghị

| Giới hạn | Chi tiết |
|---------|---------|
| **Dung lượng miễn phí** | 15 GB Google Drive (shared với dữ liệu khác) |
| **Kích thước backup tối đa** | ~100MB (đủ cho 50,000 đơn hàng) |
| **Số bản backup tối đa** | Khuyến nghị 7-10 bản (có thể tùy chỉnh) |
| **Tần suất tối thiểu** | 1 lần/ngày (auto), không giới hạn thủ công |
| **Thời gian backup** | ~10-30 giây cho 10,000 SP (tùy tốc độ mạng) |
| **Yêu cầu mạng** | Internet (WiFi khuyến nghị, 4G/5G được) |
| **Mật khẩu backup** | Tối thiểu 8 ký tự, có thể dùng chung hoặc khác cho mỗi bản |

## 9.10 Flutter Packages cần thiết

| Package | Mục đích |
|---------|---------|
| `google_sign_in` | Google OAuth 2.0 login |
| `googleapis` / `googleapis_auth` | Google Drive REST API |
| `http` | HTTP client cho API calls |
| `encrypt` / `pointycastle` | AES-256-GCM encryption |
| `crypto` | SHA-256, PBKDF2 |
| `archive` | gzip compression |
| `workmanager` | Background task scheduling (auto backup) |
| `connectivity_plus` | Kiểm tra trạng thái mạng |
| `flutter_local_notifications` | Thông báo backup thành công/thất bại |
| `flutter_secure_storage` | Lưu OAuth token encrypted |

## 9.11 Cấu trúc code bổ sung

```
lib/
├── data/
│   ├── database/
│   │   └── tables/
│   │       ├── backup_history_table.dart     ← NEW
│   │       └── backup_settings_table.dart    ← NEW
│   ├── datasources/
│   │   └── cloud/
│   │       ├── google_drive_datasource.dart  ← NEW
│   │       └── google_auth_datasource.dart   ← NEW
│   ├── models/
│   │   ├── backup_info_model.dart            ← NEW
│   │   └── backup_settings_model.dart        ← NEW
│   └── repositories/
│       └── backup_repository_impl.dart       ← NEW
│
├── domain/
│   ├── entities/
│   │   ├── backup_info.dart                  ← NEW
│   │   └── backup_settings.dart              ← NEW
│   ├── repositories/
│   │   └── backup_repository.dart            ← NEW
│   └── usecases/
│       └── backup/
│           ├── connect_google_account.dart    ← NEW
│           ├── create_backup.dart             ← NEW
│           ├── restore_backup.dart            ← NEW
│           ├── get_backup_list.dart           ← NEW
│           ├── delete_backup.dart             ← NEW
│           └── schedule_auto_backup.dart      ← NEW
│
└── presentation/
    └── screens/
        └── settings/
            ├── backup_restore_screen.dart     ← UPDATED
            ├── backup_progress_screen.dart    ← NEW
            ├── restore_select_screen.dart     ← NEW
            └── backup_restore_viewmodel.dart  ← NEW
```
