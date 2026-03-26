# 3. Thiết kế Database

## 3.1 Tổng quan

- **Database Engine:** SQLite (embedded, local trên mỗi thiết bị)
- **Primary Key:** UUID v4 (tránh xung đột khi đồng bộ P2P)
- **Soft Delete:** Tất cả bảng sử dụng `is_deleted` + `deleted_at` thay vì xóa thật
- **Sync Tracking:** Mỗi bản ghi có `sync_id`, `updated_at`, `device_id` để hỗ trợ đồng bộ

## 3.2 Quy ước chung

| Quy ước | Mô tả |
|---------|-------|
| Tên bảng | snake_case, số nhiều (products, categories) |
| Tên cột | snake_case |
| Primary Key | `id` UUID v4 |
| Timestamp | `created_at`, `updated_at` (Unix timestamp milliseconds) |
| Soft delete | `is_deleted` (boolean), `deleted_at` (timestamp) |
| Sync | `sync_version` (auto increment), `device_id` (UUID thiết bị tạo/sửa) |

## 3.3 Entity Relationship Diagram (ERD)

```
┌──────────────┐       ┌──────────────────┐       ┌──────────────┐
│   stores     │       │     users        │       │   devices    │
├──────────────┤       ├──────────────────┤       ├──────────────┤
│ id (PK)      │◄──┐   │ id (PK)          │   ┌──►│ id (PK)      │
│ name         │   │   │ store_id (FK)    │───┘   │ store_id(FK) │
│ code         │   │   │ display_name     │       │ device_name  │
│ address      │   └───│ role             │       │ device_uuid  │
│ phone        │       │ pin_hash         │       │ platform     │
│ logo_path    │       │ password_hash    │       │ last_sync_at │
│ settings     │       │ is_active        │       │ is_approved  │
└──────────────┘       └──────────────────┘       └──────────────┘
                              │
                              │ created_by
                              ▼
┌──────────────┐       ┌──────────────────┐       ┌──────────────┐
│ categories   │       │    products      │       │  suppliers   │
├──────────────┤       ├──────────────────┤       ├──────────────┤
│ id (PK)      │◄──┐   │ id (PK)          │   ┌──►│ id (PK)      │
│ store_id(FK) │   │   │ store_id (FK)    │   │   │ store_id(FK) │
│ parent_id(FK)│   └───│ category_id (FK) │   │   │ name         │
│ name         │       │ supplier_id (FK) │───┘   │ phone        │
│ description  │       │ sku              │       │ email        │
│ icon         │       │ barcode          │       │ address      │
│ color        │       │ name             │       │ tax_code     │
│ sort_order   │       │ cost_price       │       │ notes        │
└──────────────┘       │ selling_price    │       └──────────────┘
                       │ unit             │
                       │ image_path       │
                       │ min_stock        │
                       │ track_inventory  │
                       │ tax_rate         │
                       └────────┬─────────┘
                                │
              ┌─────────────────┼─────────────────┐
              ▼                 ▼                  ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│product_variants  │ │   inventory      │ │  order_items     │
├──────────────────┤ ├──────────────────┤ ├──────────────────┤
│ id (PK)          │ │ id (PK)          │ │ id (PK)          │
│ product_id (FK)  │ │ product_id (FK)  │ │ order_id (FK)    │
│ variant_name     │ │ variant_id (FK)  │ │ product_id (FK)  │
│ sku              │ │ store_id (FK)    │ │ variant_id (FK)  │
│ barcode          │ │ quantity         │ │ product_name     │
│ cost_price       │ │ reserved_qty     │ │ quantity         │
│ selling_price    │ └──────────────────┘ │ unit_price       │
│ attributes       │                      │ discount         │
└──────────────────┘                      │ total_price      │
                                          └────────┬─────────┘
                                                   │
                                                   ▼
                    ┌──────────────────┐  ┌──────────────────┐
                    │order_payments    │  │    orders        │
                    ├──────────────────┤  ├──────────────────┤
                    │ id (PK)          │  │ id (PK)          │
                    │ order_id (FK)    │──│ store_id (FK)    │
                    │ method           │  │ order_code       │
                    │ amount           │  │ customer_id (FK) │──┐
                    │ reference_no     │  │ subtotal         │  │
                    └──────────────────┘  │ discount_amount  │  │
                                          │ tax_amount       │  │
┌──────────────────┐                      │ total_amount     │  │
│ stock_movements  │                      │ status           │  │
├──────────────────┤                      │ notes            │  │
│ id (PK)          │                      │ created_by (FK)  │  │
│ store_id (FK)    │                      └──────────────────┘  │
│ product_id (FK)  │                                            │
│ variant_id (FK)  │  ┌──────────────────┐                     │
│ type             │  │   customers      │◄────────────────────┘
│ quantity         │  ├──────────────────┤
│ reference_id     │  │ id (PK)          │
│ reference_type   │  │ store_id (FK)    │
│ notes            │  │ name             │
│ created_by (FK)  │  │ phone            │
└──────────────────┘  │ email            │
                      │ address          │
                      │ notes            │
                      │ total_spent      │
                      │ visit_count      │
                      └──────────────────┘
```

## 3.4 Chi tiết các bảng

### 3.4.1 Bảng `stores` — Cửa hàng

```sql
CREATE TABLE stores (
    id              TEXT PRIMARY KEY,           -- UUID v4
    name            TEXT NOT NULL,              -- Tên cửa hàng
    code            TEXT NOT NULL UNIQUE,       -- Mã cửa hàng (dùng để kết nối)
    address         TEXT,                       -- Địa chỉ
    phone           TEXT,                       -- SĐT
    logo_path       TEXT,                       -- Đường dẫn ảnh logo
    settings        TEXT,                       -- JSON: cài đặt cửa hàng
    currency        TEXT DEFAULT 'VND',         -- Đơn vị tiền tệ
    
    -- Metadata
    created_at      INTEGER NOT NULL,           -- Unix timestamp (ms)
    updated_at      INTEGER NOT NULL,
    is_deleted      INTEGER DEFAULT 0,
    deleted_at      INTEGER,
    
    -- Sync
    sync_version    INTEGER DEFAULT 0,
    device_id       TEXT NOT NULL               -- Thiết bị tạo
);
```

**`settings` JSON structure:**
```json
{
    "receipt_header": "Cảm ơn quý khách!",
    "receipt_footer": "Hẹn gặp lại!",
    "tax_enabled": false,
    "default_tax_rate": 0,
    "low_stock_alert": true,
    "auto_print_receipt": false,
    "printer_config": {
        "type": "bluetooth",
        "address": "XX:XX:XX:XX:XX",
        "paper_size": "58mm"
    }
}
```

### 3.4.2 Bảng `users` — Người dùng

```sql
CREATE TABLE users (
    id              TEXT PRIMARY KEY,
    store_id        TEXT NOT NULL,
    display_name    TEXT NOT NULL,
    avatar_path     TEXT,
    role            TEXT NOT NULL CHECK(role IN ('owner', 'manager', 'cashier')),
    pin_hash        TEXT NOT NULL,              -- Hashed PIN (bcrypt/argon2)
    password_hash   TEXT,                       -- Hashed password (cho owner/manager)
    is_active       INTEGER DEFAULT 1,
    last_login_at   INTEGER,
    
    -- Metadata
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL,
    is_deleted      INTEGER DEFAULT 0,
    deleted_at      INTEGER,
    
    -- Sync
    sync_version    INTEGER DEFAULT 0,
    device_id       TEXT NOT NULL,
    
    FOREIGN KEY (store_id) REFERENCES stores(id)
);
```

### 3.4.3 Bảng `devices` — Thiết bị

```sql
CREATE TABLE devices (
    id              TEXT PRIMARY KEY,
    store_id        TEXT NOT NULL,
    device_name     TEXT NOT NULL,              -- Tên thiết bị (VD: "iPhone của An")
    device_uuid     TEXT NOT NULL UNIQUE,       -- UUID phần cứng thiết bị
    platform        TEXT NOT NULL,              -- ios / android
    app_version     TEXT,
    last_sync_at    INTEGER,
    is_approved     INTEGER DEFAULT 0,          -- Owner phải approve
    approved_by     TEXT,                       -- User ID của người approve
    
    -- Metadata
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL,
    is_deleted      INTEGER DEFAULT 0,
    deleted_at      INTEGER,
    
    -- Sync
    sync_version    INTEGER DEFAULT 0,
    device_id       TEXT NOT NULL,
    
    FOREIGN KEY (store_id) REFERENCES stores(id),
    FOREIGN KEY (approved_by) REFERENCES users(id)
);
```

### 3.4.4 Bảng `categories` — Danh mục

```sql
CREATE TABLE categories (
    id              TEXT PRIMARY KEY,
    store_id        TEXT NOT NULL,
    parent_id       TEXT,                       -- Danh mục cha (NULL = root)
    name            TEXT NOT NULL,
    description     TEXT,
    icon            TEXT,                       -- Emoji hoặc icon name
    color           TEXT,                       -- HEX color code
    sort_order      INTEGER DEFAULT 0,
    is_active       INTEGER DEFAULT 1,
    
    -- Metadata
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL,
    is_deleted      INTEGER DEFAULT 0,
    deleted_at      INTEGER,
    
    -- Sync
    sync_version    INTEGER DEFAULT 0,
    device_id       TEXT NOT NULL,
    
    FOREIGN KEY (store_id) REFERENCES stores(id),
    FOREIGN KEY (parent_id) REFERENCES categories(id)
);

CREATE INDEX idx_categories_store ON categories(store_id);
CREATE INDEX idx_categories_parent ON categories(parent_id);
```

### 3.4.5 Bảng `suppliers` — Nhà cung cấp

```sql
CREATE TABLE suppliers (
    id              TEXT PRIMARY KEY,
    store_id        TEXT NOT NULL,
    name            TEXT NOT NULL,
    contact_person  TEXT,
    phone           TEXT,
    email           TEXT,
    address         TEXT,
    tax_code        TEXT,
    notes           TEXT,
    is_active       INTEGER DEFAULT 1,
    
    -- Metadata
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL,
    is_deleted      INTEGER DEFAULT 0,
    deleted_at      INTEGER,
    
    -- Sync
    sync_version    INTEGER DEFAULT 0,
    device_id       TEXT NOT NULL,
    
    FOREIGN KEY (store_id) REFERENCES stores(id)
);

CREATE INDEX idx_suppliers_store ON suppliers(store_id);
```

### 3.4.6 Bảng `products` — Sản phẩm

```sql
CREATE TABLE products (
    id              TEXT PRIMARY KEY,
    store_id        TEXT NOT NULL,
    category_id     TEXT,
    supplier_id     TEXT,
    sku             TEXT NOT NULL,              -- Mã SP: tự sinh hoặc nhập
    barcode         TEXT,
    name            TEXT NOT NULL,
    description     TEXT,
    cost_price      REAL NOT NULL DEFAULT 0,    -- Giá nhập
    selling_price   REAL NOT NULL DEFAULT 0,    -- Giá bán
    unit            TEXT NOT NULL DEFAULT 'cái', -- Đơn vị tính
    image_path      TEXT,
    min_stock       INTEGER DEFAULT 0,
    max_stock       INTEGER,
    is_active       INTEGER DEFAULT 1,
    track_inventory INTEGER DEFAULT 1,          -- 1: quản lý tồn kho
    tax_rate        REAL DEFAULT 0,
    has_variants    INTEGER DEFAULT 0,          -- 1: có biến thể
    
    -- Metadata
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL,
    is_deleted      INTEGER DEFAULT 0,
    deleted_at      INTEGER,
    
    -- Sync
    sync_version    INTEGER DEFAULT 0,
    device_id       TEXT NOT NULL,
    
    FOREIGN KEY (store_id) REFERENCES stores(id),
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

CREATE INDEX idx_products_store ON products(store_id);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_sku ON products(store_id, sku);
CREATE INDEX idx_products_barcode ON products(barcode);
CREATE UNIQUE INDEX idx_products_sku_unique ON products(store_id, sku) WHERE is_deleted = 0;
```

### 3.4.7 Bảng `product_variants` — Biến thể sản phẩm

```sql
CREATE TABLE product_variants (
    id              TEXT PRIMARY KEY,
    store_id        TEXT NOT NULL,
    product_id      TEXT NOT NULL,
    variant_name    TEXT NOT NULL,              -- VD: "Đen - Size L"
    sku             TEXT NOT NULL,
    barcode         TEXT,
    cost_price      REAL,                       -- NULL = dùng giá sản phẩm gốc
    selling_price   REAL,                       -- NULL = dùng giá sản phẩm gốc
    attributes      TEXT NOT NULL,              -- JSON: {"size":"L","color":"Đen"}
    is_active       INTEGER DEFAULT 1,
    
    -- Metadata
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL,
    is_deleted      INTEGER DEFAULT 0,
    deleted_at      INTEGER,
    
    -- Sync
    sync_version    INTEGER DEFAULT 0,
    device_id       TEXT NOT NULL,
    
    FOREIGN KEY (store_id) REFERENCES stores(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE INDEX idx_variants_product ON product_variants(product_id);
```

### 3.4.8 Bảng `inventory` — Tồn kho

```sql
CREATE TABLE inventory (
    id              TEXT PRIMARY KEY,
    store_id        TEXT NOT NULL,
    product_id      TEXT NOT NULL,
    variant_id      TEXT,                       -- NULL nếu sản phẩm không có biến thể
    quantity         REAL NOT NULL DEFAULT 0,   -- Số lượng hiện tại (REAL cho đơn vị kg, lít)
    reserved_qty    REAL DEFAULT 0,             -- Số lượng đã đặt (cho tương lai)
    
    -- Metadata
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL,
    
    -- Sync
    sync_version    INTEGER DEFAULT 0,
    device_id       TEXT NOT NULL,
    
    FOREIGN KEY (store_id) REFERENCES stores(id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (variant_id) REFERENCES product_variants(id),
    
    UNIQUE(store_id, product_id, variant_id)
);

CREATE INDEX idx_inventory_product ON inventory(product_id);
```

### 3.4.9 Bảng `stock_movements` — Lịch sử biến động kho

```sql
CREATE TABLE stock_movements (
    id              TEXT PRIMARY KEY,
    store_id        TEXT NOT NULL,
    product_id      TEXT NOT NULL,
    variant_id      TEXT,
    type            TEXT NOT NULL CHECK(type IN (
                        'purchase_in',      -- Nhập kho từ NCC
                        'sale_out',         -- Bán hàng
                        'return_in',        -- Khách trả hàng
                        'return_out',       -- Trả hàng NCC
                        'adjustment_in',    -- Điều chỉnh tăng (kiểm kho)
                        'adjustment_out',   -- Điều chỉnh giảm (kiểm kho)
                        'damage_out',       -- Hư hỏng
                        'transfer'          -- Chuyển kho (tương lai)
                    )),
    quantity        REAL NOT NULL,             -- Số lượng (+ nhập, - xuất)
    quantity_before REAL NOT NULL,             -- Tồn kho trước
    quantity_after  REAL NOT NULL,             -- Tồn kho sau
    unit_cost       REAL,                      -- Giá tại thời điểm
    reference_id    TEXT,                      -- ID phiếu nhập/đơn hàng/phiếu kiểm
    reference_type  TEXT,                      -- purchase_order / order / stock_check
    notes           TEXT,
    created_by      TEXT NOT NULL,
    
    -- Metadata
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL,
    
    -- Sync
    sync_version    INTEGER DEFAULT 0,
    device_id       TEXT NOT NULL,
    
    FOREIGN KEY (store_id) REFERENCES stores(id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX idx_movements_product ON stock_movements(product_id);
CREATE INDEX idx_movements_type ON stock_movements(type);
CREATE INDEX idx_movements_reference ON stock_movements(reference_id, reference_type);
CREATE INDEX idx_movements_date ON stock_movements(created_at);
```

### 3.4.10 Bảng `purchase_orders` — Phiếu nhập kho

```sql
CREATE TABLE purchase_orders (
    id              TEXT PRIMARY KEY,
    store_id        TEXT NOT NULL,
    code            TEXT NOT NULL,              -- NK-20260325-001
    supplier_id     TEXT,
    total_amount    REAL NOT NULL DEFAULT 0,
    total_items     INTEGER NOT NULL DEFAULT 0,
    notes           TEXT,
    status          TEXT NOT NULL DEFAULT 'draft' CHECK(status IN ('draft', 'confirmed', 'cancelled')),
    created_by      TEXT NOT NULL,
    confirmed_at    INTEGER,
    
    -- Metadata
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL,
    is_deleted      INTEGER DEFAULT 0,
    deleted_at      INTEGER,
    
    -- Sync
    sync_version    INTEGER DEFAULT 0,
    device_id       TEXT NOT NULL,
    
    FOREIGN KEY (store_id) REFERENCES stores(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE purchase_order_items (
    id              TEXT PRIMARY KEY,
    purchase_order_id TEXT NOT NULL,
    product_id      TEXT NOT NULL,
    variant_id      TEXT,
    quantity        REAL NOT NULL,
    unit_cost       REAL NOT NULL,
    total_cost      REAL NOT NULL,
    
    -- Metadata
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL,
    
    -- Sync
    sync_version    INTEGER DEFAULT 0,
    device_id       TEXT NOT NULL,
    
    FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);
```

### 3.4.11 Bảng `customers` — Khách hàng

```sql
CREATE TABLE customers (
    id              TEXT PRIMARY KEY,
    store_id        TEXT NOT NULL,
    name            TEXT NOT NULL,                  -- Tên khách hàng
    phone           TEXT,                           -- Số điện thoại (unique per store)
    email           TEXT,                           -- Email (tùy chọn)
    address         TEXT,                           -- Địa chỉ
    notes           TEXT,                           -- Ghi chú
    
    -- Thống kê (cached, cập nhật khi tạo đơn)
    total_spent     REAL DEFAULT 0,                 -- Tổng chi tiêu
    visit_count     INTEGER DEFAULT 0,              -- Số lần mua hàng
    last_visit_at   INTEGER,                        -- Lần mua gần nhất
    
    -- Metadata
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL,
    is_deleted      INTEGER DEFAULT 0,
    deleted_at      INTEGER,
    
    -- Sync
    sync_version    INTEGER DEFAULT 0,
    device_id       TEXT NOT NULL,
    
    FOREIGN KEY (store_id) REFERENCES stores(id)
);

CREATE INDEX idx_customers_store ON customers(store_id);
CREATE INDEX idx_customers_phone ON customers(store_id, phone);
CREATE INDEX idx_customers_name ON customers(store_id, name);
```

### 3.4.12 Bảng `orders` — Đơn hàng

```sql
CREATE TABLE orders (
    id              TEXT PRIMARY KEY,
    store_id        TEXT NOT NULL,
    order_code      TEXT NOT NULL,              -- HD-20260325-001
    customer_id     TEXT,                       -- FK → customers (nullable = Khách lẻ)
    customer_name   TEXT,                       -- Snapshot tên KH tại thời điểm mua
    customer_phone  TEXT,                       -- Snapshot SĐT KH tại thời điểm mua
    
    subtotal        REAL NOT NULL DEFAULT 0,    -- Tổng tiền hàng
    discount_type   TEXT CHECK(discount_type IN ('percent', 'fixed')),
    discount_value  REAL DEFAULT 0,             -- Giá trị giảm giá
    discount_amount REAL DEFAULT 0,             -- Số tiền giảm
    tax_amount      REAL DEFAULT 0,             -- Thuế
    total_amount    REAL NOT NULL DEFAULT 0,    -- Tổng thanh toán
    
    status          TEXT NOT NULL DEFAULT 'completed' 
                    CHECK(status IN ('completed', 'refunded', 'partially_refunded', 'cancelled')),
    notes           TEXT,
    created_by      TEXT NOT NULL,
    
    -- Metadata
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL,
    is_deleted      INTEGER DEFAULT 0,
    deleted_at      INTEGER,
    
    -- Sync
    sync_version    INTEGER DEFAULT 0,
    device_id       TEXT NOT NULL,
    
    FOREIGN KEY (store_id) REFERENCES stores(id),
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX idx_orders_store ON orders(store_id);
CREATE INDEX idx_orders_code ON orders(order_code);
CREATE INDEX idx_orders_date ON orders(created_at);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_customer ON orders(customer_id);
```

### 3.4.13 Bảng `order_items` — Chi tiết đơn hàng

```sql
CREATE TABLE order_items (
    id              TEXT PRIMARY KEY,
    order_id        TEXT NOT NULL,
    product_id      TEXT NOT NULL,
    variant_id      TEXT,
    product_name    TEXT NOT NULL,              -- Snapshot tên SP tại thời điểm bán
    variant_name    TEXT,                       -- Snapshot tên biến thể
    quantity        REAL NOT NULL,
    unit_price      REAL NOT NULL,              -- Giá bán tại thời điểm
    cost_price      REAL NOT NULL DEFAULT 0,    -- Giá nhập (để tính lợi nhuận)
    discount_type   TEXT CHECK(discount_type IN ('percent', 'fixed')),
    discount_value  REAL DEFAULT 0,
    discount_amount REAL DEFAULT 0,
    tax_amount      REAL DEFAULT 0,
    total_price     REAL NOT NULL,              -- Thành tiền sau giảm giá
    
    -- Metadata
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL,
    
    -- Sync
    sync_version    INTEGER DEFAULT 0,
    device_id       TEXT NOT NULL,
    
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);
```

### 3.4.14 Bảng `order_payments` — Thanh toán

```sql
CREATE TABLE order_payments (
    id              TEXT PRIMARY KEY,
    order_id        TEXT NOT NULL,
    method          TEXT NOT NULL CHECK(method IN ('cash', 'transfer', 'ewallet', 'other')),
    amount          REAL NOT NULL,
    received_amount REAL,                       -- Số tiền khách đưa (cho tiền mặt)
    change_amount   REAL DEFAULT 0,             -- Tiền thừa
    reference_no    TEXT,                       -- Mã giao dịch chuyển khoản
    notes           TEXT,
    
    -- Metadata
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL,
    
    -- Sync
    sync_version    INTEGER DEFAULT 0,
    device_id       TEXT NOT NULL,
    
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE INDEX idx_payments_order ON order_payments(order_id);
```

### 3.4.15 Bảng `sync_log` — Nhật ký đồng bộ

```sql
CREATE TABLE sync_log (
    id              TEXT PRIMARY KEY,
    store_id        TEXT NOT NULL,
    source_device   TEXT NOT NULL,              -- Thiết bị gửi
    target_device   TEXT NOT NULL,              -- Thiết bị nhận
    table_name      TEXT NOT NULL,              -- Bảng được đồng bộ
    records_synced  INTEGER NOT NULL,           -- Số bản ghi
    sync_type       TEXT NOT NULL CHECK(sync_type IN ('full', 'incremental')),
    status          TEXT NOT NULL CHECK(status IN ('success', 'failed', 'partial')),
    error_message   TEXT,
    started_at      INTEGER NOT NULL,
    completed_at    INTEGER,
    
    FOREIGN KEY (store_id) REFERENCES stores(id)
);

CREATE INDEX idx_sync_log_store ON sync_log(store_id);
CREATE INDEX idx_sync_log_date ON sync_log(started_at);
```

### 3.4.16 Bảng `backup_history` — Lịch sử backup

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

CREATE INDEX idx_backup_history_store ON backup_history(store_id);
CREATE INDEX idx_backup_history_date ON backup_history(created_at);
```

### 3.4.17 Bảng `backup_settings` — Cấu hình backup

```sql
CREATE TABLE backup_settings (
    id              TEXT PRIMARY KEY,
    store_id        TEXT NOT NULL UNIQUE,
    google_email    TEXT,                       -- Email tài khoản Google đã kết nối
    is_connected    INTEGER DEFAULT 0,
    auto_enabled    INTEGER DEFAULT 0,
    frequency       TEXT DEFAULT 'weekly' CHECK(frequency IN ('daily', 'weekly', 'monthly')),
    backup_time     TEXT DEFAULT '02:00',       -- HH:mm
    day_of_week     INTEGER DEFAULT 0,          -- 0=Sunday
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
```

> Chi tiết về backup/restore xem tài liệu [09-backup-restore-gdrive.md](./09-backup-restore-gdrive.md)

## 3.5 Seed Data (Dữ liệu khởi tạo)

Khi tạo cửa hàng mới, tự động tạo:

```sql
-- Danh mục mặc định
INSERT INTO categories (id, store_id, name, icon, sort_order, is_active)
VALUES 
    (uuid(), store_id, 'Chưa phân loại', '📦', 0, 1),
    (uuid(), store_id, 'Đồ uống', '🥤', 1, 1),
    (uuid(), store_id, 'Thực phẩm', '🍜', 2, 1),
    (uuid(), store_id, 'Gia dụng', '🏠', 3, 1);

-- Đơn vị tính mặc định (lưu trong store settings)
-- cái, chiếc, hộp, gói, kg, g, lít, ml, chai, lon, túi, bịch, thùng, bộ
```

## 3.6 Chiến lược Migration

```
migrations/
├── 001_initial_schema.sql          -- Tạo bảng ban đầu
├── 002_add_indexes.sql             -- Thêm index
├── 003_seed_data.sql               -- Dữ liệu mặc định
└── xxx_future_migration.sql        -- Các thay đổi sau
```

Mỗi migration có:
- `version`: Số phiên bản (tự tăng)
- `up()`: Áp dụng thay đổi
- `down()`: Rollback thay đổi

Bảng tracking migration:
```sql
CREATE TABLE schema_migrations (
    version     INTEGER PRIMARY KEY,
    applied_at  INTEGER NOT NULL
);
```
