# 4. Thiết kế API / Services

## 4.1 Tổng quan kiến trúc Service Layer

Vì miniPOS là ứng dụng local-first, không có REST API truyền thống. Thay vào đó, sử dụng **Service Layer Pattern** để tổ chức business logic.

```
┌─────────────────────────────────────────────────────────┐
│                    UI / Screens                          │
├─────────────────────────────────────────────────────────┤
│                  ViewModels / Controllers                │
├─────────────────────────────────────────────────────────┤
│                    Service Layer                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │  Auth     │ │ Product  │ │  Order   │ │  Sync    │  │
│  │ Service   │ │ Service  │ │ Service  │ │ Service  │  │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘  │
├───────┼─────────────┼───────────┼─────────────┼────────┤
│       │    Repository Layer     │             │         │
│  ┌────▼─────┐ ┌────▼─────┐ ┌───▼──────┐ ┌───▼──────┐ │
│  │  User    │ │ Product  │ │  Order   │ │  Sync    │ │
│  │  Repo    │ │  Repo    │ │  Repo    │ │  Repo    │ │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ │
├───────┼─────────────┼───────────┼─────────────┼────────┤
│       └─────────────┴───────────┴─────────────┘         │
│                    SQLite Database                        │
└─────────────────────────────────────────────────────────┘
```

## 4.2 Service: AuthService — Xác thực

### 4.2.1 Tạo cửa hàng

```
AuthService.createStore(input: CreateStoreInput): Result<Store>
```

**Input:**
```
CreateStoreInput {
    storeName: String          // "Zin100"
    storeCode: String          // "ZIN100" (uppercase, 4-8 chars)
    address?: String
    phone?: String
    logoPath?: String
    ownerName: String          // "Nguyễn Văn A"
    ownerPin: String           // "123456"
    ownerPassword: String      // "admin@123"
}
```

**Logic:**
1. Validate input (tên không rỗng, PIN 4-6 số, mã cửa hàng 4-8 ký tự)
2. Kiểm tra mã cửa hàng chưa tồn tại trên thiết bị
3. Tạo Store record
4. Hash PIN và password (bcrypt)
5. Tạo User record với role = 'owner'
6. Tạo Device record cho thiết bị hiện tại
7. Tạo danh mục mặc định
8. Tạo dữ liệu seed
9. Lưu session (current_store_id, current_user_id)

**Output:** `Result<Store>` — Store vừa tạo hoặc Error

---

### 4.2.2 Đăng nhập

```
AuthService.login(userId: UUID, pin: String): Result<AuthSession>
```

**Logic:**
1. Tìm user theo ID
2. Kiểm tra user is_active
3. Kiểm tra số lần thử (max 5, lock 5 phút)
4. Verify PIN hash
5. Cập nhật last_login_at
6. Tạo session local (lưu SharedPreferences)
7. Return AuthSession

**Output:**
```
AuthSession {
    userId: UUID
    storeId: UUID
    role: Role
    displayName: String
    loginAt: DateTime
}
```

---

### 4.2.3 Tham gia cửa hàng

```
AuthService.joinStore(storeCode: String): Result<JoinRequest>
AuthService.approveJoinRequest(requestId: UUID, role: Role): Result<Device>
AuthService.rejectJoinRequest(requestId: UUID): Result<void>
```

---

## 4.3 Service: StoreService — Quản lý cửa hàng

```
StoreService.getStore(storeId: UUID): Result<Store>
StoreService.updateStore(storeId: UUID, input: UpdateStoreInput): Result<Store>
StoreService.getStoreSettings(storeId: UUID): Result<StoreSettings>
StoreService.updateStoreSettings(storeId: UUID, settings: StoreSettings): Result<void>
```

---

## 4.4 Service: UserService — Quản lý người dùng

```
UserService.createUser(input: CreateUserInput): Result<User>
UserService.updateUser(userId: UUID, input: UpdateUserInput): Result<User>
UserService.deactivateUser(userId: UUID): Result<void>
UserService.activateUser(userId: UUID): Result<void>
UserService.resetPin(userId: UUID, newPin: String): Result<void>
UserService.changePassword(userId: UUID, oldPassword: String, newPassword: String): Result<void>
UserService.listUsers(storeId: UUID, filters?: UserFilters): Result<List<User>>
```

**CreateUserInput:**
```
CreateUserInput {
    storeId: UUID
    displayName: String
    pin: String
    role: 'manager' | 'cashier'
    avatarPath?: String
}
```

**Phân quyền:**
- Owner: tạo/sửa/xóa Manager và Cashier
- Manager: tạo/sửa/xóa Cashier
- Cashier: không có quyền quản lý user

---

## 4.5 Service: CategoryService — Quản lý danh mục

```
CategoryService.create(input: CreateCategoryInput): Result<Category>
CategoryService.update(id: UUID, input: UpdateCategoryInput): Result<Category>
CategoryService.delete(id: UUID): Result<void>
CategoryService.getAll(storeId: UUID): Result<List<Category>>
CategoryService.getTree(storeId: UUID): Result<List<CategoryNode>>
CategoryService.reorder(storeId: UUID, orderedIds: List<UUID>): Result<void>
```

**CreateCategoryInput:**
```
CreateCategoryInput {
    storeId: UUID
    name: String
    description?: String
    parentId?: UUID
    icon?: String
    color?: String
    sortOrder?: Integer
}
```

**Business Rules:**
```
delete(id):
    1. Kiểm tra có sản phẩm active thuộc danh mục này?
       → Có: Return Error("Không thể xóa danh mục đang có sản phẩm")
    2. Kiểm tra có danh mục con?
       → Có: Return Error("Vui lòng xóa danh mục con trước")
    3. Soft delete (is_deleted = 1, deleted_at = now)
    4. Trigger sync
```

---

## 4.6 Service: SupplierService — Quản lý nhà cung cấp

```
SupplierService.create(input: CreateSupplierInput): Result<Supplier>
SupplierService.update(id: UUID, input: UpdateSupplierInput): Result<Supplier>
SupplierService.delete(id: UUID): Result<void>
SupplierService.getAll(storeId: UUID, filters?: SupplierFilters): Result<List<Supplier>>
SupplierService.getById(id: UUID): Result<Supplier>
SupplierService.getSupplierProducts(id: UUID): Result<List<Product>>
SupplierService.getSupplierPurchaseHistory(id: UUID): Result<List<PurchaseOrder>>
```

---

## 4.7 Service: ProductService — Quản lý sản phẩm

```
ProductService.create(input: CreateProductInput): Result<Product>
ProductService.update(id: UUID, input: UpdateProductInput): Result<Product>
ProductService.delete(id: UUID): Result<void>
ProductService.getAll(storeId: UUID, filters?: ProductFilters): Result<PaginatedList<Product>>
ProductService.getById(id: UUID): Result<ProductDetail>
ProductService.searchByBarcode(storeId: UUID, barcode: String): Result<Product?>
ProductService.searchByName(storeId: UUID, query: String): Result<List<Product>>
ProductService.generateSku(storeId: UUID): Result<String>
ProductService.duplicateProduct(id: UUID): Result<Product>
ProductService.importFromCsv(storeId: UUID, filePath: String): Result<ImportResult>
```

**ProductFilters:**
```
ProductFilters {
    categoryId?: UUID
    supplierId?: UUID
    isActive?: Boolean
    lowStock?: Boolean         // quantity < min_stock
    searchQuery?: String
    sortBy?: 'name' | 'price' | 'created_at' | 'stock'
    sortOrder?: 'asc' | 'desc'
    page?: Integer
    limit?: Integer            // default 50
}
```

### 4.7.1 Variant Management

```
ProductService.addVariant(productId: UUID, input: CreateVariantInput): Result<ProductVariant>
ProductService.updateVariant(variantId: UUID, input: UpdateVariantInput): Result<ProductVariant>
ProductService.deleteVariant(variantId: UUID): Result<void>
ProductService.getVariants(productId: UUID): Result<List<ProductVariant>>
```

---

## 4.8 Service: InventoryService — Quản lý kho

```
InventoryService.getStock(storeId: UUID, productId: UUID, variantId?: UUID): Result<InventoryItem>
InventoryService.getStockOverview(storeId: UUID): Result<StockOverview>
InventoryService.getLowStockProducts(storeId: UUID): Result<List<LowStockItem>>
InventoryService.getStockMovements(productId: UUID, filters?: MovementFilters): Result<List<StockMovement>>
```

### 4.8.1 Nhập kho

```
InventoryService.createPurchaseOrder(input: CreatePurchaseOrderInput): Result<PurchaseOrder>
InventoryService.confirmPurchaseOrder(id: UUID): Result<PurchaseOrder>
InventoryService.cancelPurchaseOrder(id: UUID): Result<void>
```

**confirmPurchaseOrder Logic:**
```
1. Validate trạng thái = 'draft'
2. Transaction BEGIN
3. For each item in purchase_order_items:
   a. Cập nhật inventory.quantity += item.quantity
   b. Tạo stock_movement (type: 'purchase_in')
   c. Cập nhật product.cost_price nếu giá nhập thay đổi
4. Cập nhật purchase_order.status = 'confirmed'
5. Transaction COMMIT
6. Trigger sync event
```

### 4.8.2 Kiểm kho

```
InventoryService.createStockCheck(storeId: UUID): Result<StockCheck>
InventoryService.addStockCheckItem(checkId: UUID, productId: UUID, actualQty: Real): Result<void>
InventoryService.confirmStockCheck(checkId: UUID): Result<StockCheckResult>
```

**confirmStockCheck Logic:**
```
1. For each item in stock_check_items:
   a. difference = actual_qty - system_qty
   b. If difference > 0: tạo stock_movement (type: 'adjustment_in')
   c. If difference < 0: tạo stock_movement (type: 'adjustment_out')
   d. Cập nhật inventory.quantity = actual_qty
2. Lưu kết quả kiểm kho
```

---

## 4.9 Service: CustomerService — Quản lý khách hàng

### 4.9.1 CRUD Khách hàng

```
CustomerService.createCustomer(storeId, data: {name, phone, email?, address?, notes?}): Result<Customer>
CustomerService.updateCustomer(customerId, data): Result<Customer>
CustomerService.deleteCustomer(customerId): Result<void>
CustomerService.getCustomer(customerId): Result<Customer>
CustomerService.getCustomers(storeId, {search?, page?, limit?}): Result<PaginatedList<Customer>>
CustomerService.getRecentCustomers(storeId, limit: Int): Result<List<Customer>>
CustomerService.searchByPhone(storeId, phone: String): Result<Customer?>
```

### 4.9.2 Cập nhật thống kê (auto-trigger khi tạo đơn)

```
CustomerService.incrementVisit(customerId, orderTotal: Real): Result<void>
  → Cập nhật: visit_count += 1, total_spent += orderTotal, last_visit_at = now()

CustomerService.decrementVisit(customerId, refundAmount: Real): Result<void>
  → Cập nhật khi trả hàng: total_spent -= refundAmount
```

**Validation:**
- Tên không rỗng (1-100 ký tự)
- SĐT format VN (10 số, bắt đầu 0)
- SĐT unique trong cùng store
- Email format (nếu có)

---

## 4.10 Service: OrderService — Bán hàng POS

### 4.10.1 Quản lý giỏ hàng (Cart — in-memory, không lưu DB)

```
CartService.addItem(product: Product, quantity: Real, variant?: ProductVariant): Cart
CartService.updateItemQuantity(itemIndex: Int, quantity: Real): Cart
CartService.updateItemPrice(itemIndex: Int, newPrice: Real): Cart
CartService.removeItem(itemIndex: Int): Cart
CartService.clearCart(): Cart
CartService.applyItemDiscount(itemIndex: Int, type: 'percent'|'fixed', value: Real): Cart
CartService.applyOrderDiscount(type: 'percent'|'fixed', value: Real): Cart
CartService.setCustomer(customer?: Customer): Cart
CartService.getCartTotal(): CartTotal
```

**Cart Model (in-memory):**
```
Cart {
    items: List<CartItem>
    orderDiscount?: Discount
    customer?: Customer            // null = Khách lẻ
    notes?: String
}

CartItem {
    product: Product
    variant?: ProductVariant
    quantity: Real
    unitPrice: Real                // Có thể edit khác giá gốc
    originalPrice: Real            // Giá gốc (để so sánh)
    discount?: Discount
    lineTotal: Real                // (unitPrice * quantity) - discount
}

CartTotal {
    subtotal: Real             // Tổng tiền hàng
    itemDiscountTotal: Real    // Tổng giảm giá theo SP
    orderDiscountAmount: Real  // Giảm giá đơn hàng
    taxAmount: Real            // Thuế
    grandTotal: Real           // Tổng thanh toán
    itemCount: Integer         // Số mặt hàng
    totalQuantity: Real        // Tổng số lượng
}
```

### 4.10.2 Tạo đơn hàng

```
OrderService.createOrder(cart: Cart, payments: List<PaymentInput>): Result<Order>
```

**createOrder Logic:**
```
1. Validate cart không rỗng
2. Validate tổng payments >= grandTotal
3. Transaction BEGIN
4. Tạo order record (với customer_id nếu có, snapshot customer_name/phone)
5. For each cart item:
   a. Tạo order_item record (snapshot giá bán thực tế, tên SP)
   b. If unitPrice != originalPrice → ghi log giá bán thay đổi
   c. If product.track_inventory:
      - Giảm inventory.quantity
      - Tạo stock_movement (type: 'sale_out')
      - Kiểm tra tồn kho < 0 → cảnh báo (không block)
6. For each payment:
   a. Tạo order_payment record
   b. Tính tiền thừa (cho tiền mặt)
7. Sinh order_code: HD-{YYYYMMDD}-{sequence}
8. If customer_id != null:
   a. CustomerService.incrementVisit(customer_id, total_amount)
9. Transaction COMMIT
10. Trigger sync event
11. Return Order
```

### 4.10.3 Trả hàng

```
OrderService.createRefund(orderId: UUID, items: List<RefundItem>, reason: String): Result<Refund>
```

**createRefund Logic:**
```
1. Validate đơn hàng tồn tại và status = 'completed'
2. Validate mật khẩu quản trị (nếu cashier thực hiện → cần manager/owner approve)
3. Transaction BEGIN
4. For each refund item:
   a. Cập nhật inventory.quantity += refund_qty
   b. Tạo stock_movement (type: 'return_in')
5. Tạo refund record
6. Cập nhật order.status = 'refunded' hoặc 'partially_refunded'
7. If order.customer_id != null:
   a. CustomerService.decrementVisit(customer_id, refund_amount)
8. Transaction COMMIT
9. Trigger sync event
```

### 4.10.4 Lịch sử đơn hàng

```
OrderService.getOrders(storeId: UUID, filters?: OrderFilters): Result<PaginatedList<Order>>
OrderService.getOrderById(id: UUID): Result<OrderDetail>
OrderService.getOrdersByDate(storeId: UUID, date: Date): Result<List<Order>>
OrderService.getOrdersByCustomer(customerId: UUID): Result<List<Order>>
```

---

## 4.11 Service: ReportService — Báo cáo

```
ReportService.getDashboard(storeId: UUID, date: Date): Result<DashboardData>
ReportService.getSalesReport(storeId: UUID, from: Date, to: Date): Result<SalesReport>
ReportService.getProductReport(storeId: UUID, from: Date, to: Date): Result<ProductReport>
ReportService.getStaffReport(storeId: UUID, from: Date, to: Date): Result<StaffReport>
```

**DashboardData:**
```
DashboardData {
    todayRevenue: Real
    todayOrders: Integer
    todayProfit: Real
    lowStockCount: Integer
    recentOrders: List<Order>       // 5 đơn gần nhất
    topProducts: List<TopProduct>   // 5 SP bán chạy nhất hôm nay
    revenueChart: List<ChartPoint>  // 7 ngày gần nhất
}
```

---

## 4.12 Service: SyncService — Đồng bộ dữ liệu

> Chi tiết xem tài liệu [05-p2p-sync-design.md](./05-p2p-sync-design.md)

```
SyncService.startDiscovery(storeCode: String): Stream<DiscoveredDevice>
SyncService.stopDiscovery(): void
SyncService.connectToDevice(deviceId: String): Result<Connection>
SyncService.disconnect(deviceId: String): void
SyncService.requestFullSync(connection: Connection): Result<SyncResult>
SyncService.startIncrementalSync(connection: Connection): Stream<SyncProgress>
SyncService.getLastSyncTime(deviceId: String): DateTime?
SyncService.getSyncLog(storeId: UUID): List<SyncLog>
```

---

## 4.13 Service: BackupService — Sao lưu & Khôi phục Google Drive

> Chi tiết xem tài liệu [09-backup-restore-gdrive.md](./09-backup-restore-gdrive.md)

### 4.12.1 Google Account

```
BackupService.connectGoogleAccount(): Result<GoogleAccount>
BackupService.disconnectGoogleAccount(): Result<void>
BackupService.getConnectedAccount(): Result<GoogleAccount?>
```

### 4.12.2 Backup

```
BackupService.createBackup(password: String, notes?: String): Stream<BackupProgress>
BackupService.getBackupList(): Result<List<BackupInfo>>
BackupService.getBackupDetail(backupId: String): Result<BackupDetail>
BackupService.deleteBackup(backupId: String): Result<void>
```

**BackupProgress:**
```
BackupProgress {
    stage: 'preparing' | 'exporting' | 'compressing' | 'encrypting' | 'uploading' | 'finalizing' | 'completed' | 'failed'
    progress: Real         // 0.0 - 100.0
    message: String
    bytesUploaded?: Integer
    totalBytes?: Integer
}
```

### 4.12.3 Restore

```
BackupService.restoreFromBackup(backupId: String, password: String): Stream<RestoreProgress>
BackupService.downloadBackupToLocal(backupId: String): Result<String>
```

**RestoreProgress:**
```
RestoreProgress {
    stage: 'downloading' | 'validating' | 'decrypting' | 'decompressing' | 'clearing' | 'importing' | 'completed' | 'failed'
    progress: Real
    message: String
    bytesDownloaded?: Integer
    totalBytes?: Integer
}
```

### 4.12.4 Auto Backup Settings

```
BackupService.getAutoBackupSettings(): Result<AutoBackupSettings>
BackupService.updateAutoBackupSettings(settings: AutoBackupSettings): Result<void>
BackupService.getLastBackupInfo(): Result<BackupInfo?>
```

---

## 4.14 Error Handling

Tất cả services trả về `Result<T>` pattern:

```
sealed class Result<T> {
    class Success(data: T)
    class Error(
        code: ErrorCode,
        message: String,
        details?: Map
    )
}

enum ErrorCode {
    // Validation
    INVALID_INPUT,
    DUPLICATE_ENTRY,
    
    // Auth
    INVALID_PIN,
    ACCOUNT_LOCKED,
    ACCOUNT_DISABLED,
    INSUFFICIENT_PERMISSION,
    
    // Business
    CATEGORY_HAS_PRODUCTS,
    INSUFFICIENT_STOCK,
    ORDER_NOT_FOUND,
    ORDER_ALREADY_REFUNDED,
    
    // Sync
    DEVICE_NOT_FOUND,
    SYNC_CONFLICT,
    CONNECTION_FAILED,
    
    // Backup & Restore
    GOOGLE_AUTH_FAILED,
    GOOGLE_DRIVE_FULL,
    BACKUP_IN_PROGRESS,
    INVALID_BACKUP_FILE,
    CORRUPTED_BACKUP,
    WRONG_BACKUP_PASSWORD,
    INCOMPATIBLE_VERSION,
    UPLOAD_FAILED,
    DOWNLOAD_FAILED,
    
    // System
    DATABASE_ERROR,
    UNKNOWN_ERROR
}
```
