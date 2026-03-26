# 1. Tổng quan hệ thống

## 1.1 Kiến trúc hệ thống

miniPOS sử dụng kiến trúc **Local-First** với khả năng đồng bộ P2P (Peer-to-Peer).

```
┌─────────────────────────────────────────────────────────┐
│                    miniPOS Architecture                  │
├─────────────────────────────────────────────────────────┤
│                                                         │
│   ┌──────────┐     P2P Sync      ┌──────────┐         │
│   │ Device A │ ◄──────────────► │ Device B │         │
│   │ (Owner)  │     LAN/WiFi      │(Cashier) │         │
│   └────┬─────┘                   └────┬─────┘         │
│        │                              │                 │
│   ┌────▼─────┐                   ┌────▼─────┐         │
│   │ SQLite   │                   │ SQLite   │         │
│   │ Local DB │                   │ Local DB │         │
│   └──────────┘                   └──────────┘         │
│                                                         │
│   ┌──────────┐     P2P Sync      ┌──────────┐         │
│   │ Device C │ ◄──────────────► │ Device D │         │
│   │(Manager) │     LAN/WiFi      │(Cashier) │         │
│   └────┬─────┘                   └────┬─────┘         │
│        │                              │                 │
│   ┌────▼─────┐                   ┌────▼─────┐         │
│   │ SQLite   │                   │ SQLite   │         │
│   │ Local DB │                   │ Local DB │         │
│   └──────────┘                   └──────────┘         │
│                                                         │
│        Tất cả thiết bị cùng cửa hàng "Zin100"         │
│        đồng bộ dữ liệu qua mạng LAN                   │
└─────────────────────────────────────────────────────────┘
```

## 1.2 Mô hình kiến trúc ứng dụng (App Architecture)

```
┌─────────────────────────────────────────────┐
│              Presentation Layer              │
│  ┌─────────┐ ┌──────────┐ ┌─────────────┐  │
│  │ Screens │ │ Widgets  │ │ View Models │  │
│  └─────────┘ └──────────┘ └─────────────┘  │
├─────────────────────────────────────────────┤
│              Business Logic Layer            │
│  ┌───────────────┐ ┌─────────────────────┐  │
│  │   Services    │ │   Use Cases         │  │
│  │ - AuthService │ │ - CreateOrder       │  │
│  │ - POSService  │ │ - ManageInventory   │  │
│  │ - SyncService │ │ - SyncData          │  │
│  │ - StoreService│ │ - ManageProducts    │  │
│  └───────────────┘ └─────────────────────┘  │
├─────────────────────────────────────────────┤
│              Data Layer                      │
│  ┌─────────────┐ ┌───────────────────────┐  │
│  │ Repositories│ │   Data Sources        │  │
│  │             │ │ - SQLite Database     │  │
│  │             │ │ - Shared Preferences  │  │
│  │             │ │ - P2P Connection      │  │
│  └─────────────┘ └───────────────────────┘  │
├─────────────────────────────────────────────┤
│              Infrastructure Layer            │
│  ┌──────────┐ ┌──────────┐ ┌────────────┐  │
│  │  SQLite  │ │ P2P Sync │ │  Security  │  │
│  │  Engine  │ │  Engine  │ │  Module    │  │
│  └──────────┘ └──────────┘ └────────────┘  │
└─────────────────────────────────────────────┘
```

## 1.3 Luồng hoạt động tổng quan

### Luồng 1: Lần đầu sử dụng (Tạo cửa hàng mới)

```
Mở app → Tạo cửa hàng (VD: Zin100)
       → Tạo tài khoản Owner (PIN/mật khẩu)
       → Thiết lập thông tin cửa hàng
       → Vào Home Screen (card-based launcher)
```

### Luồng 2: Thiết bị mới tham gia cửa hàng

```
Mở app → Nhập mã cửa hàng "Zin100"
       → Kết nối với thiết bị Owner (cùng mạng WiFi)
       → Owner phê duyệt
       → Đồng bộ dữ liệu ban đầu
       → Đăng nhập bằng tài khoản được cấp
       → Vào trang chủ (theo quyền)
```

### Luồng 3: Bán hàng hàng ngày (POS step-by-step)

```
Mở app → Đăng nhập (PIN 4-6 số)
       → Home Screen → Nhấn card "Bán hàng (POS)"
       → Step 1: Chọn sản phẩm / Quét barcode
       → Step 2: Chỉnh số lượng & giá bán
       → Step 3: Chọn khách hàng (hoặc Khách lẻ)
       → Step 4: Xem hoá đơn → Thanh toán
       → Step 5: In/Chia sẻ hóa đơn → Đơn mới hoặc Về Home
       → Đồng bộ đơn hàng đến các thiết bị khác
```

## 1.4 Nguyên tắc thiết kế

| Nguyên tắc | Mô tả |
|------------|-------|
| **Offline-First** | App hoạt động đầy đủ khi không có mạng. Đồng bộ khi có kết nối. |
| **Eventually Consistent** | Dữ liệu giữa các thiết bị sẽ nhất quán sau khi đồng bộ hoàn tất. |
| **Conflict Resolution** | Sử dụng timestamp-based "Last Write Wins" + merge strategy cho conflicts. |
| **Minimal Permission** | Mỗi vai trò chỉ có quyền truy cập tối thiểu cần thiết. |
| **Data Integrity** | Sử dụng UUID cho primary key để tránh xung đột giữa các thiết bị. |

## 1.5 Yêu cầu hệ thống

| Yêu cầu | Chi tiết |
|---------|---------|
| **iOS** | iOS 14.0 trở lên |
| **Android** | Android 8.0 (API 26) trở lên |
| **Dung lượng** | ~50MB (app) + dữ liệu phụ thuộc lượng sản phẩm |
| **RAM** | Tối thiểu 2GB |
| **Mạng** | WiFi (cho đồng bộ P2P), không bắt buộc |
| **Phần cứng thêm** | Máy in Bluetooth (tùy chọn), Máy quét barcode (tùy chọn) |
