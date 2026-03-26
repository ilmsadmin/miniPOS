# 6. Thiết kế UI/UX

## 6.1 Design Principles

| Nguyên tắc | Mô tả |
|------------|-------|
| **Simplicity** | Home screen card-based, mỗi card = 1 chức năng, không quá 5 bước cho POS |
| **Step-by-step** | POS flow theo wizard 5 bước: Chọn SP → Số lượng → Khách hàng → Hoá đơn → In |
| **Speed** | Tap sản phẩm = thêm vào giỏ ngay, numpad nhanh cho chỉnh số lượng/giá |
| **Consistency** | Material Design 3, pattern nhất quán |
| **Accessibility** | Font size tối thiểu 14sp, contrast ratio ≥ 4.5:1 |
| **Responsive** | Hỗ trợ cả phone (portrait) và tablet (landscape) |

## 6.2 Color Palette

```
Primary:        #2563EB (Blue 600)      — Thương hiệu, CTA chính
Primary Dark:   #1D4ED8 (Blue 700)      — Status bar, app bar
Secondary:      #059669 (Emerald 600)   — Thành công, tiền, doanh thu
Accent:         #F59E0B (Amber 500)     — Cảnh báo, highlight
Error:          #DC2626 (Red 600)       — Lỗi, xóa, hết hàng
Background:     #F8FAFC (Slate 50)      — Nền chính
Surface:        #FFFFFF (White)         — Card, dialog
Text Primary:   #1E293B (Slate 800)     — Text chính
Text Secondary: #64748B (Slate 500)     — Text phụ
Border:         #E2E8F0 (Slate 200)     — Viền, divider
```

## 6.3 Typography

```
Heading 1:    24sp / Bold    — Tiêu đề trang
Heading 2:    20sp / SemiBold — Tiêu đề section
Heading 3:    18sp / SemiBold — Tiêu đề card
Body:         16sp / Regular  — Nội dung chính
Body Small:   14sp / Regular  — Nội dung phụ
Caption:      12sp / Regular  — Label, hint
Price:        20sp / Bold     — Giá tiền (font monospace)
Price Large:  28sp / Bold     — Tổng tiền thanh toán
```

## 6.4 Sitemap (Cấu trúc màn hình)

```
📱 miniPOS App
│
├── 🔐 Onboarding
│   ├── Welcome Screen
│   ├── Create Store
│   │   ├── Store Info Form
│   │   └── Owner Account Setup
│   └── Join Store
│       ├── Enter Store Code
│       ├── Waiting for Approval
│       └── Initial Sync Progress
│
├── 🔑 Login Screen
│   ├── User Selection (avatars grid)
│   ├── PIN Input
│   └── Biometric Auth
│
├── 🏠 Home (Trang chủ) — Card-based Launcher
│   ├── Header (Store info, User avatar, Notifications)
│   ├── Today's Summary Strip (Revenue, Orders count)
│   ├── Function Cards Grid (2 columns)
│   │   ├── 🛒 Bán hàng (POS)
│   │   ├── 📦 Quản lý kho & Kiểm kho
│   │   ├── 📥 Nhập hàng
│   │   ├── 🏷️ Sản phẩm & Danh mục
│   │   ├── 🏭 Nhà cung cấp
│   │   ├── 👥 Khách hàng
│   │   ├── 📊 Báo cáo
│   │   ├── 📜 Lịch sử đơn hàng
│   │   └── ⚙️ Cài đặt
│   └── Low Stock Alert Banner (nếu có)
│
├── 🛒 POS (Bán hàng) ⭐ Step-by-step Flow
│   ├── Step 1: Chọn sản phẩm
│   │   ├── Category Tabs
│   │   ├── Search Bar + Barcode Scanner
│   │   ├── Product Grid / List
│   │   └── Selected Products Summary Bar
│   ├── Step 2: Số lượng & Giá bán
│   │   ├── Selected Product List
│   │   ├── Quantity Adjuster (+/-)
│   │   ├── Inline Price Edit (chỉnh giá bán)
│   │   ├── Discount per Item (optional)
│   │   └── Running Total
│   ├── Step 3: Chọn khách hàng
│   │   ├── Customer Search
│   │   ├── Recent Customers
│   │   ├── Select Customer Card
│   │   ├── Quick Create Customer (inline form)
│   │   └── Skip (Khách lẻ)
│   ├── Step 4: Tạo hoá đơn & Thanh toán
│   │   ├── Invoice Summary
│   │   │   ├── Customer Info
│   │   │   ├── Product List with Prices
│   │   │   ├── Subtotal / Discount / Tax
│   │   │   └── Grand Total
│   │   ├── Payment Method Selection
│   │   ├── Cash Calculator (nếu tiền mặt)
│   │   └── Confirm Payment Button
│   └── Step 5: Hoá đơn & In
│       ├── Success Animation
│       ├── Receipt Preview
│       ├── Print / Share / Download
│       └── New Order Button
│
├── 📦 Sản phẩm
│   ├── Product List (search, filter, sort)
│   ├── Product Detail
│   │   ├── Basic Info
│   │   ├── Pricing
│   │   ├── Inventory Info
│   │   ├── Variants (if any)
│   │   └── Stock Movements History
│   ├── Add/Edit Product Form
│   │   ├── Photo Capture
│   │   ├── Barcode Scan
│   │   └── Category/Supplier Picker
│   └── Import Products (CSV)
│
├── 📂 Danh mục
│   ├── Category List (drag to reorder)
│   └── Add/Edit Category
│
├── 🏭 Nhà cung cấp
│   ├── Supplier List
│   ├── Supplier Detail
│   │   ├── Info
│   │   ├── Products Supplied
│   │   └── Purchase History
│   └── Add/Edit Supplier
│
├── � Khách hàng
│   ├── Customer List (search, filter)
│   ├── Customer Detail
│   │   ├── Info (name, phone, email, address)
│   │   ├── Purchase History
│   │   ├── Total Spent / Visit Count
│   │   └── Notes
│   └── Add/Edit Customer Form
│
├── �📋 Kho hàng
│   ├── Stock Overview
│   │   ├── Summary Cards
│   │   ├── Low Stock List
│   │   └── Stock Value
│   ├── Purchase Orders (Nhập kho)
│   │   ├── PO List
│   │   ├── Create PO
│   │   └── PO Detail
│   ├── Stock Check (Kiểm kho)
│   │   ├── Create Stock Check
│   │   └── Stock Check Result
│   └── Stock Movements Log
│
├── 📊 Báo cáo
│   ├── Sales Report
│   ├── Product Report
│   └── Staff Report
│
├── 📜 Lịch sử đơn hàng
│   ├── Order List (filter by date, status)
│   ├── Order Detail
│   └── Refund Process
│
├── ⚙️ Cài đặt
│   ├── Store Settings
│   │   ├── Store Info Edit
│   │   ├── Receipt Template
│   │   └── Tax Settings
│   ├── Device Management
│   │   ├── Connected Devices
│   │   ├── Sync Status
│   │   └── Remove Device
│   ├── User Management
│   │   ├── User List
│   │   └── Add/Edit User
│   ├── Printer Setup
│   ├── Backup & Restore (Google Drive)
│   │   ├── Backup Overview
│   │   │   ├── Google Account Status
│   │   │   ├── Last Backup Info
│   │   │   └── Auto Backup Toggle
│   │   ├── Create Backup
│   │   │   ├── Backup Options (data selection)
│   │   │   ├── Set/Confirm Password
│   │   │   └── Backup Progress
│   │   ├── Backup History
│   │   │   ├── Backup List (date, size, status)
│   │   │   └── Delete Backup
│   │   ├── Restore from Backup
│   │   │   ├── Select Backup
│   │   │   ├── Enter Password
│   │   │   ├── Restore Preview (data summary)
│   │   │   └── Restore Progress
│   │   └── Auto Backup Settings
│   │       ├── Schedule (daily/weekly)
│   │       ├── Time Picker
│   │       └── Wi-Fi Only Toggle
│   └── About
│
└── 🔔 Notifications (Overlay)
    ├── Sync Status
    ├── Low Stock Alerts
    └── New Device Request
```

## 6.5 Wireframes chi tiết

### 6.5.1 Onboarding — Tạo cửa hàng

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
│  ──────── hoặc ──────────   │
│                             │
│  ┌─────────────────────┐    │
│  │  📥 Khôi phục từ     │    │
│  │     Google Drive     │    │
│  └─────────────────────┘    │
│                             │
└─────────────────────────────┘

         ↓ Nhấn "Tạo cửa hàng"

┌─────────────────────────────┐
│  ← Tạo cửa hàng    (1/2)   │
├─────────────────────────────┤
│                             │
│  Tên cửa hàng *             │
│  ┌─────────────────────┐    │
│  │ Zin100              │    │
│  └─────────────────────┘    │
│                             │
│  Mã cửa hàng *              │
│  ┌─────────────────────┐    │
│  │ ZIN100          🔄  │    │
│  └─────────────────────┘    │
│  ℹ️ Dùng để kết nối thiết bị│
│                             │
│  Địa chỉ                    │
│  ┌─────────────────────┐    │
│  │ 123 Nguyễn Huệ, Q1  │    │
│  └─────────────────────┘    │
│                             │
│  Số điện thoại               │
│  ┌─────────────────────┐    │
│  │ 0901234567           │    │
│  └─────────────────────┘    │
│                             │
│  ┌─────────────────────┐    │
│  │     Tiếp theo →     │    │
│  └─────────────────────┘    │
└─────────────────────────────┘

         ↓ Nhấn "Tiếp theo"

┌─────────────────────────────┐
│  ← Tạo tài khoản   (2/2)   │
├─────────────────────────────┤
│                             │
│        ┌──────┐             │
│        │  📷  │             │
│        │Avatar│             │
│        └──────┘             │
│                             │
│  Tên hiển thị *              │
│  ┌─────────────────────┐    │
│  │ Nguyễn Văn Minh     │    │
│  └─────────────────────┘    │
│                             │
│  PIN đăng nhập * (4-6 số)   │
│  ┌──┐ ┌──┐ ┌──┐ ┌──┐       │
│  │1 │ │2 │ │3 │ │4 │       │
│  └──┘ └──┘ └──┘ └──┘       │
│                             │
│  Mật khẩu quản trị *        │
│  ┌─────────────────────┐    │
│  │ ••••••••         👁 │    │
│  └─────────────────────┘    │
│                             │
│  ┌─────────────────────┐    │
│  │  ✅ Hoàn tất        │    │
│  └─────────────────────┘    │
└─────────────────────────────┘
```

### 6.5.2 Login Screen

```
┌─────────────────────────────┐
│                             │
│     🏪 Zin100               │
│     123 Nguyễn Huệ, Q1      │
│                             │
│  Chọn tài khoản:            │
│                             │
│  ┌────────┐ ┌────────┐     │
│  │  👤    │ │  👤    │     │
│  │ Minh   │ │  Lan   │     │
│  │ Owner  │ │Cashier │     │
│  └────────┘ └────────┘     │
│  ┌────────┐ ┌────────┐     │
│  │  👤    │ │   +    │     │
│  │ Tùng   │ │  Thêm  │     │
│  │Manager │ │        │     │
│  └────────┘ └────────┘     │
│                             │
│  ─────────────────────      │
│                             │
│  Nhập PIN:                   │
│  ┌──┐ ┌──┐ ┌──┐ ┌──┐       │
│  │● │ │● │ │  │ │  │       │
│  └──┘ └──┘ └──┘ └──┘       │
│                             │
│  [1] [2] [3]               │
│  [4] [5] [6]               │
│  [7] [8] [9]               │
│  [👆] [0] [⌫]              │
│                             │
└─────────────────────────────┘
```

### 6.5.3 Home Screen — Card-based Launcher

```
┌─────────────────────────────┐
│  🏪 Zin100        👤 Minh  │
│  Thứ 4, 25/03/2026  🔔     │
├─────────────────────────────┤
│                             │
│  ┌─────────────────────┐    │
│  │ 💰 Hôm nay: 2,450k  │    │
│  │ 📦 23 đơn │ ↑12%    │    │
│  └─────────────────────┘    │
│                             │
│  ── Chức năng chính ──────  │
│                             │
│  ┌───────────┐ ┌──────────┐│
│  │           │ │          ││
│  │   �      │ │   📦     ││
│  │           │ │          ││
│  │ Bán hàng  │ │ Quản lý  ││
│  │   (POS)   │ │   kho    ││
│  │           │ │& Kiểm kho││
│  └───────────┘ └──────────┘│
│  ┌───────────┐ ┌──────────┐│
│  │           │ │          ││
│  │   �      │ │   🏷️     ││
│  │           │ │          ││
│  │ Nhập hàng │ │ Sản phẩm ││
│  │           │ │& Danh mục││
│  │           │ │          ││
│  └───────────┘ └──────────┘│
│  ┌───────────┐ ┌──────────┐│
│  │           │ │          ││
│  │   🏭      │ │   👥     ││
│  │           │ │          ││
│  │Nhà cung   │ │ Khách    ││
│  │  cấp      │ │  hàng    ││
│  │           │ │          ││
│  └───────────┘ └──────────┘│
│  ┌───────────┐ ┌──────────┐│
│  │           │ │          ││
│  │   📊      │ │   📜     ││
│  │           │ │          ││
│  │ Báo cáo   │ │ Lịch sử  ││
│  │           │ │ đơn hàng ││
│  │           │ │          ││
│  └───────────┘ └──────────┘│
│  ┌───────────┐ ┌──────────┐│
│  │           │ │          ││
│  │   ⚙️      │ │   📡     ││
│  │           │ │          ││
│  │ Cài đặt   │ │ Thiết bị ││
│  │           │ │& Đồng bộ ││
│  │           │ │          ││
│  └───────────┘ └──────────┘│
│                             │
│  ┌─────────────────────┐    │
│  │ ⚠️ 3 sản phẩm sắp hết │  │
│  │ hàng. Xem chi tiết →  │  │
│  └─────────────────────┘    │
│                             │
└─────────────────────────────┘

Card Design:
┌──────────────────┐
│  Border radius: 16px
│  Shadow: elevation 2
│  Padding: 20px
│  Icon: 32sp, Primary color
│  Label: 14sp, SemiBold
│  Background: White
│  Tap: Ripple + scale 0.97
└──────────────────┘

Home Screen (Tablet — Landscape):
┌──────────────────────────────────────────────────────────────────┐
│  🏪 Zin100                            👤 Minh (Owner)     🔔   │
├──────────────────────────────────────────────────────────────────┤
│  💰 Hôm nay: 2,450,000đ  │  📦 23 đơn  │  📈 Lợi nhuận 680k  │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │   🛒     │ │   📦     │ │   📥     │ │   🏷️     │           │
│  │ Bán hàng │ │ Quản lý  │ │ Nhập hàng│ │ Sản phẩm │           │
│  │  (POS)   │ │   kho    │ │          │ │& Danh mục│           │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │   🏭     │ │   👥     │ │   📊     │ │   📜     │           │
│  │Nhà cung  │ │ Khách    │ │ Báo cáo  │ │ Lịch sử  │           │
│  │  cấp     │ │  hàng    │ │          │ │ đơn hàng │           │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘           │
│  ┌──────────┐ ┌──────────┐                                      │
│  │   ⚙️     │ │   📡     │   ┌──────────────────────────────┐  │
│  │ Cài đặt  │ │ Thiết bị │   │ ⚠️ 3 SP sắp hết hàng        │  │
│  │          │ │& Đồng bộ │   │ Coca Cola, Mì Hảo Hảo, ...  │  │
│  └──────────┘ └──────────┘   └──────────────────────────────┘  │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

Ghi chú thiết kế Home Screen:
- **Card grid 2 cột** trên phone, **4 cột** trên tablet
- Mỗi card có icon lớn + label ngắn, dễ nhận biết
- Card "Bán hàng (POS)" nổi bật nhất (viền primary, icon lớn hơn)
- Summary strip ở trên cùng hiển thị tóm tắt ngày (ẩn được)
- Low stock alert banner ở cuối (chỉ hiện khi có SP sắp hết)
- Nhấn vào card → chuyển đến màn hình quản lý tương ứng
- Có thể **long-press** card để xem quick info (VD: số SP, số đơn hôm nay)

### 6.5.4 POS — Step Indicator & Layout chung

```
Stepper bar (hiển thị ở top mọi step):

┌─────────────────────────────────────────────────┐
│  ①───────②───────③───────④───────⑤              │
│  Chọn SP  Số lượng  Khách   Hoá đơn   In       │
│  ● active ○ pending ○       ○         ○         │
└─────────────────────────────────────────────────┘

Step indicator design:
- Circle: 28dp, Active = Primary filled, Done = Green ✓, Pending = Gray outline
- Connecting line: 2dp, Done = Green, Pending = Gray
- Label: 12sp, Active = Bold Primary, Done = Green, Pending = Gray
- Swipe left/right hoặc nhấn Back/Next để chuyển step
```

### 6.5.5 POS Step 1 — Chọn sản phẩm

```
┌─────────────────────────────┐
│  ← Bán hàng                 │
├─────────────────────────────┤
│  ①━━━━━②─────③─────④─────⑤│
│  Chọn SP Số lượng Khách HĐ  In│
├─────────────────────────────┤
│ 🔍 Tìm kiếm sản phẩm  📷  │
├─────────────────────────────┤
│ [Tất cả][Đồ uống][Thực phẩm]│
│ [Gia dụng][Khác]     ► more│
├─────────────────────────────┤
│                             │
│ ┌──────┐ ┌──────┐ ┌──────┐ │
│ │ 🖼️   │ │ 🖼️   │ │ 🖼️   │ │
│ │Coca  │ │Pepsi │ │Sting │ │
│ │15,000│ │15,000│ │12,000│ │
│ │ ✅ 2 │ │      │ │      │ │
│ └──────┘ └──────┘ └──────┘ │
│ ┌──────┐ ┌──────┐ ┌──────┐ │
│ │ 🖼️   │ │ 🖼️   │ │ 🖼️   │ │
│ │Mì HH │ │Bánh  │ │Nước  │ │
│ │ 5,000│ │10,000│ │ 8,000│ │
│ │ ✅ 3 │ │      │ │      │ │
│ └──────┘ └──────┘ └──────┘ │
│ ┌──────┐ ┌──────┐ ┌──────┐ │
│ │ 🖼️   │ │ 🖼️   │ │ 🖼️   │ │
│ │Snack │ │Kẹo   │ │Sữa  │ │
│ │ 8,000│ │ 3,000│ │25,000│ │
│ │      │ │      │ │ ✅ 2 │ │
│ └──────┘ └──────┘ └──────┘ │
│                             │
├─────────────────────────────┤
│ 🛒 3 sản phẩm │ 95,000đ   │
│ ┌─────────────────────────┐ │
│ │      Tiếp theo →        │ │
│ └─────────────────────────┘ │
└─────────────────────────────┘

Tap sản phẩm:
- Lần 1: thêm 1 vào giỏ, hiện badge ✅ 1
- Lần 2+: tăng quantity, badge cập nhật
- Long press: xem chi tiết SP (tồn kho, mô tả)
- Badge xanh lá ở góc card = đã chọn + số lượng
```

### 6.5.6 POS Step 2 — Số lượng & Giá bán

```
┌─────────────────────────────┐
│  ← Bán hàng                 │
├─────────────────────────────┤
│  ✓━━━━━②━━━━━③─────④─────⑤│
│  Chọn SP Số lượng Khách HĐ  In│
├─────────────────────────────┤
│                             │
│  ┌─────────────────────┐    │
│  │ 🖼️ Coca Cola 330ml  │    │
│  │                      │    │
│  │  Giá gốc: 15,000đ   │    │
│  │  Giá bán:            │    │
│  │  ┌────────────┐      │    │
│  │  │  15,000  ✏️│      │    │
│  │  └────────────┘      │    │
│  │                      │    │
│  │  Số lượng:           │    │
│  │  ┌─┐ ┌─────┐ ┌─┐    │    │
│  │  │-│ │  2  │ │+│    │    │
│  │  └─┘ └─────┘ └─┘    │    │
│  │                      │    │
│  │  Thành tiền:  30,000đ│    │
│  └─────────────────────┘    │
│  ────────────────────────   │
│  ┌─────────────────────┐    │
│  │ 🖼️ Mì Hảo Hảo      │    │
│  │                      │    │
│  │  Giá gốc:  5,000đ   │    │
│  │  Giá bán:            │    │
│  │  ┌────────────┐      │    │
│  │  │   5,000  ✏️│      │    │
│  │  └────────────┘      │    │
│  │                      │    │
│  │  Số lượng:           │    │
│  │  ┌─┐ ┌─────┐ ┌─┐    │    │
│  │  │-│ │  3  │ │+│    │    │
│  │  └─┘ └─────┘ └─┘    │    │
│  │                      │    │
│  │  Thành tiền:  15,000đ│    │
│  └─────────────────────┘    │
│  ────────────────────────   │
│  ┌─────────────────────┐    │
│  │ �️ Sữa tươi TH     │    │
│  │  Giá bán: 25,000  ✏️ │    │
│  │  SL: [-] 2 [+]       │    │
│  │  Thành tiền:  50,000đ│    │
│  └─────────────────────┘    │
│                             │
├─────────────────────────────┤
│  Tổng: 3 SP │ 95,000đ      │
│ ┌──────────┐ ┌────────────┐│
│ │ ← Quay lại│ │ Tiếp theo →││
│ └──────────┘ └────────────┘│
└─────────────────────────────┘

Chức năng:
- Nhấn ✏️ để edit giá bán (mở numpad)
- Giá bán thay đổi = highlight vàng (khác giá gốc)
- Nhấn [-] khi qty = 1 → xóa SP khỏi giỏ (confirm dialog)
- Swipe left trên SP → xóa nhanh
- Running total cập nhật real-time
```

### 6.5.7 POS Step 3 — Chọn khách hàng

```
┌─────────────────────────────┐
│  ← Bán hàng                 │
├─────────────────────────────┤
│  ✓━━━━━✓━━━━━③━━━━━④─────⑤│
│  Chọn SP Số lượng Khách HĐ  In│
├─────────────────────────────┤
│                             │
│ 🔍 Tìm khách hàng...       │
│                             │
│  ┌─────────────────────┐    │
│  │ 👤 Khách lẻ          │    │
│  │ (Bỏ qua, không cần   │    │
│  │  chọn khách hàng)    │    │
│  │              ● Selected│   │
│  └─────────────────────┘    │
│                             │
│  ── Khách hàng gần đây ──  │
│                             │
│  ┌─────────────────────┐    │
│  │ 👩 Nguyễn Thị Lan    │    │
│  │ 📱 0901234567        │    │
│  │ Mua 12 lần │ 2.5M đ  │    │
│  │                    ○  │    │
│  └─────────────────────┘    │
│  ┌─────────────────────┐    │
│  │ 👨 Trần Văn Hùng    │    │
│  │ � 0912345678        │    │
│  │ Mua 8 lần │ 1.8M đ   │    │
│  │                    ○  │    │
│  └─────────────────────┘    │
│  ┌─────────────────────┐    │
│  │ 👩 Lê Thị Mai       │    │
│  │ 📱 0987654321        │    │
│  │ Mua 5 lần │ 950K đ   │    │
│  │                    ○  │    │
│  └─────────────────────┘    │
│                             │
│  ┌─────────────────────┐    │
│  │ ＋ Tạo khách hàng mới │   │
│  └─────────────────────┘    │
│                             │
├─────────────────────────────┤
│ ┌──────────┐ ┌────────────┐│
│ │ ← Quay lại│ │ Tiếp theo →││
│ └──────────┘ └────────────┘│
└─────────────────────────────┘

         ↓ Nhấn "Tạo khách hàng mới"

┌─────────────────────────────┐
│  ← Tạo khách hàng mới       │
├─────────────────────────────┤
│                             │
│  Tên khách hàng *            │
│  ┌─────────────────────┐    │
│  │ Phạm Văn Toàn       │    │
│  └─────────────────────┘    │
│                             │
│  Số điện thoại *             │
│  ┌─────────────────────┐    │
│  │ 0976543210           │    │
│  └─────────────────────┘    │
│                             │
│  Email (tùy chọn)           │
│  ┌─────────────────────┐    │
│  │                      │    │
│  └─────────────────────┘    │
│                             │
│  Địa chỉ (tùy chọn)        │
│  ┌─────────────────────┐    │
│  │                      │    │
│  └─────────────────────┘    │
│                             │
│  Ghi chú                    │
│  ┌─────────────────────┐    │
│  │ Mua sỉ thường xuyên │    │
│  └─────────────────────┘    │
│                             │
│  ┌─────────────────────┐    │
│  │  ✅ Lưu & chọn       │    │
│  └─────────────────────┘    │
└─────────────────────────────┘

Ghi chú:
- Mặc định chọn "Khách lẻ" → có thể bỏ qua bước này
- Tìm kiếm theo tên hoặc SĐT
- Hiển thị lịch sử mua hàng (số lần, tổng tiền)
- Form tạo mới inline nhanh, chỉ cần Tên + SĐT
```

### 6.5.8 POS Step 4 — Tạo hoá đơn & Thanh toán

```
┌─────────────────────────────┐
│  ← Bán hàng                 │
├─────────────────────────────┤
│  ✓━━━━━✓━━━━━✓━━━━━④━━━━━⑤│
│  Chọn SP Số lượng Khách HĐ  In│
├─────────────────────────────┤
│                             │
│  ── Khách hàng ────────     │
│  👩 Nguyễn Thị Lan          │
│  📱 0901234567     [Đổi]    │
│                             │
│  ── Chi tiết đơn hàng ───  │
│                             │
│  ┌─────────────────────┐    │
│  │ Coca Cola       x2  │    │
│  │ 15,000 × 2 = 30,000 │    │
│  ├─────────────────────┤    │
│  │ Mì Hảo Hảo     x3  │    │
│  │  5,000 × 3 = 15,000 │    │
│  ├─────────────────────┤    │
│  │ Sữa tươi TH    x2  │    │
│  │ 25,000 × 2 = 50,000 │    │
│  └─────────────────────┘    │
│                             │
│  Tạm tính:         95,000đ  │
│  Giảm giá:              0đ  │
│  ┌──────────────────┐       │
│  │ + Thêm giảm giá  │       │
│  └──────────────────┘       │
│  Thuế (VAT):            0đ  │
│  ━━━━━━━━━━━━━━━━━━━━━━━    │
│  TỔNG THANH TOÁN:   95,000đ │
│                             │
│  ── Phương thức TT ───────  │
│  ┌─────────────────────┐    │
│  │ 💵 Tiền mặt     ✅  │    │
│  ├─────────────────────┤    │
│  │ 🏦 Chuyển khoản     │    │
│  ├─────────────────────┤    │
│  │ 📱 Ví điện tử       │    │
│  └─────────────────────┘    │
│                             │
│  Tiền khách đưa:            │
│  ┌─────────────────────┐    │
│  │  100,000            │    │
│  └─────────────────────┘    │
│  [95k] [100k] [200k] [500k]│
│                             │
│  💵 Tiền thừa: 5,000đ       │
│                             │
│  Ghi chú đơn hàng:          │
│  ┌─────────────────────┐    │
│  │                     │    │
│  └─────────────────────┘    │
│                             │
├─────────────────────────────┤
│ ┌──────────┐ ┌────────────┐│
│ │ ← Quay lại│ │ ✅ XÁC NHẬN││
│ └──────────┘ │ THANH TOÁN ││
│              └────────────┘│
└─────────────────────────────┘

Chức năng:
- Nhấn [Đổi] khách hàng → quay lại Step 3
- Nhấn vào dòng SP → quay lại Step 2 edit
- "Thêm giảm giá" → bottom sheet (% hoặc fixed)
- Gợi ý nhanh tiền khách đưa (làm tròn lên)
- Nút "Xác nhận" disabled cho đến khi chọn PTTT
```

### 6.5.9 POS Step 5 — Hoá đơn & In

```
┌─────────────────────────────┐
│                             │
│         ✅                   │
│     Thanh toán               │
│     thành công!              │
│                             │
│  ✓━━━━━✓━━━━━✓━━━━━✓━━━━━⑤│
│  Chọn SP Số lượng Khách HĐ  In│
├─────────────────────────────┤
│                             │
│  ┌───────────────────────┐  │
│  │    🏪 CỬA HÀNG ZIN100  │  │
│  │  123 Nguyễn Huệ, Q1   │  │
│  │    SĐT: 0901234567    │  │
│  │  ─────────────────────  │  │
│  │  HĐ: HD-20260325-023  │  │
│  │  25/03/2026  14:30:00  │  │
│  │  NV: Minh              │  │
│  │  KH: Nguyễn Thị Lan   │  │
│  │  ─────────────────────  │  │
│  │  Coca Cola    x2 30,000│  │
│  │  Mì Hảo Hảo  x3 15,000│  │
│  │  Sữa tươi    x2 50,000│  │
│  │  ─────────────────────  │  │
│  │  Tổng:          95,000 │  │
│  │  Tiền mặt:    100,000  │  │
│  │  Thối:           5,000 │  │
│  │  ─────────────────────  │  │
│  │  Cảm ơn quý khách!     │  │
│  │  Hẹn gặp lại! 😊       │  │
│  └───────────────────────┘  │
│                             │
│  ┌──────────┐ ┌──────────┐ │
│  │ 🖨️ In    │ │ 📤 Chia sẻ│ │
│  └──────────┘ └──────────┘ │
│                             │
│  ┌─────────────────────┐    │
│  │  🛒 Tạo đơn hàng mới │    │
│  └─────────────────────┘    │
│  ┌─────────────────────┐    │
│  │  🏠 Về trang chủ     │    │
│  └─────────────────────┘    │
└─────────────────────────────┘

Chức năng:
- Animation confetti khi thành công
- Receipt preview scroll được
- In Bluetooth thermal printer
- Chia sẻ qua Zalo, Messenger, SMS...
- "Tạo đơn hàng mới" → reset về Step 1
- "Về trang chủ" → quay về Home Screen
```

### 6.5.10 POS — Tablet Landscape (Step 1 + Cart preview)

```
┌──────────────────────────────────────────────────────────────────┐
│  ← Bán hàng              ①━━━━②─────③─────④─────⑤    🏪 Zin100│
├──────────────────────────────────────┬───────────────────────────┤
│ 🔍 Tìm kiếm sản phẩm...       📷   │  📋 Đã chọn (3 SP)       │
├──────────────────────────────────────┤                           │
│ [Tất cả][Đồ uống][Thực phẩm][Gia dụng]│  Coca Cola    x2  30,000│
├──────────────────────────────────────┤  Mì Hảo Hảo  x3  15,000 │
│                                      │  Sữa tươi    x2  50,000 │
│ ┌────────┐ ┌────────┐ ┌────────┐   │                           │
│ │  🖼️    │ │  🖼️    │ │  🖼️    │   │                           │
│ │ Coca   │ │ Pepsi  │ │ Sting  │   │                           │
│ │ 15,000 │ │ 15,000 │ │ 12,000 │   │                           │
│ │  ✅ 2  │ │        │ │        │   │                           │
│ └────────┘ └────────┘ └────────┘   │                           │
│ ┌────────┐ ┌────────┐ ┌────────┐   │                           │
│ │  🖼️    │ │  🖼️    │ │  🖼️    │   │                           │
│ │ Mì HH  │ │ Bánh   │ │ Nước   │   │                           │
│ │  5,000  │ │ 10,000 │ │  8,000 │   │                           │
│ │  ✅ 3  │ │        │ │        │   │                           │
│ └────────┘ └────────┘ └────────┘   │                           │
│ ┌────────┐ ┌────────┐ ┌────────┐   │                           │
│ │  🖼️    │ │  🖼️    │ │  🖼️    │   ├───────────────────────────┤
│ │ Snack  │ │  Kẹo   │ │  Sữa   │   │  Tổng:          95,000đ  │
│ │  8,000  │ │  3,000 │ │ 25,000 │   │                           │
│ │         │ │        │ │  ✅ 2  │   │  ┌─────────────────────┐ │
│ └────────┘ └────────┘ └────────┘   │  │    Tiếp theo →       │ │
│                                      │  └─────────────────────┘ │
└──────────────────────────────────────┴───────────────────────────┘

Tablet layout:
- Left 60%: Product grid (giống phone)
- Right 40%: Cart preview panel (luôn hiển thị)
- Stepper bar nằm trên cùng, dài hơn
- Ở Step 4, right panel = invoice summary
```

### 6.5.11 Product Management

```
┌─────────────────────────────┐
│  ← Sản phẩm          ＋ 📥 │
├─────────────────────────────┤
│  🔍 Tìm kiếm sản phẩm...   │
├─────────────────────────────┤
│  [Tất cả] [Đồ uống] [T.Phẩm]│
│  Sắp xếp: [Tên ▼]          │
├─────────────────────────────┤
│                             │
│  ┌─────────────────────┐    │
│  │ 🖼️ Coca Cola 330ml  │    │
│  │ SKU: SP001           │    │
│  │ Giá: 15,000đ         │    │
│  │ Tồn: 50 lon  🟢      │    │
│  └─────────────────────┘    │
│  ┌─────────────────────┐    │
│  │ 🖼️ Mì Hảo Hảo      │    │
│  │ SKU: SP002           │    │
│  │ Giá: 5,000đ          │    │
│  │ Tồn: 5 gói  🟡       │    │
│  └─────────────────────┘    │
│  ┌─────────────────────┐    │
│  │ 🖼️ Khăn giấy Pulppy │    │
│  │ SKU: SP003           │    │
│  │ Giá: 25,000đ         │    │
│  │ Tồn: 2 bịch  🔴      │    │
│  └─────────────────────┘    │
│                             │
│  Hiển thị 3/150 sản phẩm    │
│                             │
└─────────────────────────────┘

Tồn kho indicators:
🟢 Đủ hàng (qty > min_stock * 2)
🟡 Sắp hết (min_stock < qty ≤ min_stock * 2)
🔴 Hết/Rất ít (qty ≤ min_stock)
```

### 6.5.12 Sync & Device Management

```
┌─────────────────────────────┐
│  ← Thiết bị & Đồng bộ      │
├─────────────────────────────┤
│                             │
│  📡 Trạng thái: Đang đồng bộ│
│  Mã cửa hàng: ZIN100        │
│                             │
│  Thiết bị đang kết nối:     │
│                             │
│  ┌─────────────────────┐    │
│  │ 📱 iPhone của Minh   │    │
│  │ ⭐ Owner │ 🟢 Online │    │
│  │ Sync: 2 phút trước   │    │
│  └─────────────────────┘    │
│  ┌─────────────────────┐    │
│  │ 📱 Samsung của Lan   │    │
│  │ Cashier │ 🟢 Online  │    │
│  │ Sync: Đang đồng bộ... │   │
│  │ ████████░░ 80%       │    │
│  └─────────────────────┘    │
│  ┌─────────────────────┐    │
│  │ 📱 iPad của Tùng     │    │
│  │ Manager │ 🔴 Offline │    │
│  │ Sync: 2 giờ trước    │    │
│  └─────────────────────┘    │
│                             │
│  Yêu cầu kết nối:           │
│  ┌─────────────────────┐    │
│  │ 📱 Oppo của Hương    │    │
│  │ [✅ Chấp nhận]       │    │
│  │ [❌ Từ chối]         │    │
│  └─────────────────────┘    │
│                             │
└─────────────────────────────┘
```

### 6.5.13 Backup & Restore — Màn hình chính

```
┌─────────────────────────────┐
│  ← Backup & Restore         │
├─────────────────────────────┤
│                             │
│  ┌─────────────────────┐    │
│  │ 🟢 Google Drive      │    │
│  │ minipos@gmail.com    │    │
│  │ Đã kết nối           │    │
│  │          [Ngắt kết nối]│   │
│  └─────────────────────┘    │
│                             │
│  ── Backup gần nhất ──────  │
│                             │
│  ┌─────────────────────┐    │
│  │ 📅 25/03/2026 14:30  │    │
│  │ 📦 Kích thước: 12.5 MB│   │
│  │ ✅ Thành công         │    │
│  └─────────────────────┘    │
│                             │
│  ── Tự động backup ───────  │
│                             │
│  ┌─────────────────────┐    │
│  │ Auto Backup    [🔵ON]│    │
│  │ Hàng ngày lúc 02:00  │    │
│  │ Chỉ qua Wi-Fi  [🔵] │    │
│  │           [Tùy chỉnh]│    │
│  └─────────────────────┘    │
│                             │
│  ┌─────────────────────┐    │
│  │ ☁️ Backup ngay       │    │
│  └─────────────────────┘    │
│  ┌─────────────────────┐    │
│  │ 📥 Khôi phục dữ liệu │    │
│  └─────────────────────┘    │
│                             │
│  📋 Xem lịch sử backup     │
│                             │
└─────────────────────────────┘

Trạng thái Google Drive:
🟢 Đã kết nối — hiển thị email + nút Ngắt
🔴 Chưa kết nối — hiển thị nút [Kết nối Google Drive]
```

### 6.5.14 Backup — Tạo bản backup

```
┌─────────────────────────────┐
│  ← Tạo bản backup           │
├─────────────────────────────┤
│                             │
│  Chọn dữ liệu backup:       │
│                             │
│  ┌─────────────────────┐    │
│  │ ☑️ Sản phẩm (150)    │    │
│  │ ☑️ Danh mục (12)     │    │
│  │ ☑️ Nhà cung cấp (8)  │    │
│  │ ☑️ Đơn hàng (1,234)  │    │
│  │ ☑️ Kho hàng          │    │
│  │ ☑️ Cài đặt cửa hàng  │    │
│  │ ☑️ Người dùng (4)    │    │
│  │ ☐ Hình ảnh sản phẩm  │    │
│  └─────────────────────┘    │
│                             │
│  Kích thước ước tính: ~12 MB │
│                             │
│  ── Mã hóa backup ────────  │
│                             │
│  ┌─────────────────────┐    │
│  │ 🔒 Mật khẩu backup   │    │
│  │ ┌─────────────────┐  │    │
│  │ │ ••••••••       👁️│  │    │
│  │ └─────────────────┘  │    │
│  │ Xác nhận mật khẩu    │    │
│  │ ┌─────────────────┐  │    │
│  │ │ ••••••••       👁️│  │    │
│  │ └─────────────────┘  │    │
│  │ ⚠️ Không thể khôi     │    │
│  │ phục nếu quên mật khẩu│   │
│  └─────────────────────┘    │
│                             │
│  ┌─────────────────────┐    │
│  │  ☁️ Bắt đầu Backup  │    │
│  └─────────────────────┘    │
│                             │
└─────────────────────────────┘
```

### 6.5.15 Backup — Tiến trình upload

```
┌─────────────────────────────┐
│         Đang backup...       │
├─────────────────────────────┤
│                             │
│                             │
│         ☁️                   │
│        ↑↑↑↑                 │
│      📦📦📦📦               │
│                             │
│                             │
│  Bước 3/4: Đang tải lên     │
│  Google Drive...             │
│                             │
│  ████████████░░░░░ 65%      │
│                             │
│  8.1 MB / 12.5 MB           │
│  Tốc độ: 2.3 MB/s           │
│  Còn lại: ~2 giây           │
│                             │
│                             │
│  Các bước:                   │
│  ✅ 1. Thu thập dữ liệu     │
│  ✅ 2. Nén & mã hóa         │
│  🔄 3. Tải lên Google Drive  │
│  ⬜ 4. Xác nhận hoàn tất    │
│                             │
│                             │
│  ┌─────────────────────┐    │
│  │    ❌ Hủy backup     │    │
│  └─────────────────────┘    │
│                             │
└─────────────────────────────┘
```

### 6.5.16 Backup — Lịch sử backup

```
┌─────────────────────────────┐
│  ← Lịch sử backup           │
├─────────────────────────────┤
│                             │
│  Google Drive: 38.2 MB / ∞   │
│  ┌─────────────────────┐    │
│  │ ████████░░░ 3 bản    │    │
│  └─────────────────────┘    │
│                             │
│  ┌─────────────────────┐    │
│  │ 📅 25/03/2026 14:30  │    │
│  │ 📦 12.5 MB │ ✅ OK    │    │
│  │ Auto backup           │    │
│  │        [📥 Restore] [🗑️]│  │
│  └─────────────────────┘    │
│  ┌─────────────────────┐    │
│  │ 📅 24/03/2026 02:00  │    │
│  │ 📦 12.3 MB │ ✅ OK    │    │
│  │ Auto backup           │    │
│  │        [📥 Restore] [🗑️]│  │
│  └─────────────────────┘    │
│  ┌─────────────────────┐    │
│  │ 📅 20/03/2026 10:15  │    │
│  │ 📦 13.4 MB │ ✅ OK    │    │
│  │ Manual backup         │    │
│  │        [📥 Restore] [🗑️]│  │
│  └─────────────────────┘    │
│                             │
│  ⚠️ Backup cũ hơn 90 ngày   │
│  sẽ bị tự động xóa          │
│                             │
└─────────────────────────────┘
```

### 6.5.17 Restore — Chọn bản backup & nhập mật khẩu

```
┌─────────────────────────────┐
│  ← Khôi phục dữ liệu        │
├─────────────────────────────┤
│                             │
│  ⚠️ Lưu ý: Dữ liệu hiện    │
│  tại sẽ được thay thế bởi   │
│  bản backup bạn chọn.       │
│                             │
│  ── Chọn bản backup ──────  │
│                             │
│  ┌─────────────────────┐    │
│  │ ○ 25/03/2026 14:30   │    │
│  │   12.5 MB │ Auto      │    │
│  └─────────────────────┘    │
│  ┌─────────────────────┐    │
│  │ ● 24/03/2026 02:00   │    │
│  │   12.3 MB │ Auto      │    │
│  └─────────────────────┘    │
│  ┌─────────────────────┐    │
│  │ ○ 20/03/2026 10:15   │    │
│  │   13.4 MB │ Manual    │    │
│  └─────────────────────┘    │
│                             │
│  ── Mật khẩu backup ──────  │
│                             │
│  ┌─────────────────────┐    │
│  │ 🔒 Nhập mật khẩu     │    │
│  │ ┌─────────────────┐  │    │
│  │ │                 👁️│  │    │
│  │ └─────────────────┘  │    │
│  └─────────────────────┘    │
│                             │
│  ── Xem trước dữ liệu ───  │
│                             │
│  ┌─────────────────────┐    │
│  │ Sản phẩm:       148  │    │
│  │ Danh mục:        12  │    │
│  │ Nhà cung cấp:     8  │    │
│  │ Đơn hàng:     1,200  │    │
│  │ Người dùng:       4  │    │
│  └─────────────────────┘    │
│                             │
│  ┌─────────────────────┐    │
│  │ 🔄 Bắt đầu khôi phục│    │
│  └─────────────────────┘    │
│                             │
└─────────────────────────────┘
```

### 6.5.18 Restore — Tiến trình khôi phục

```
┌─────────────────────────────┐
│       Đang khôi phục...      │
├─────────────────────────────┤
│                             │
│                             │
│         📥                   │
│        ↓↓↓↓                 │
│      📦📦📦📦               │
│                             │
│                             │
│  Bước 3/5: Đang ghi dữ liệu │
│  vào cơ sở dữ liệu...       │
│                             │
│  ████████████████░░ 85%     │
│                             │
│                             │
│  Các bước:                   │
│  ✅ 1. Tải xuống từ Drive    │
│  ✅ 2. Giải mã & giải nén   │
│  🔄 3. Ghi dữ liệu (85%)   │
│  ⬜ 4. Kiểm tra tính toàn vẹn│
│  ⬜ 5. Khởi động lại app     │
│                             │
│                             │
│  ┌─────────────────────┐    │
│  │    ❌ Hủy khôi phục  │    │
│  └─────────────────────┘    │
│                             │
│  ⚠️ Không tắt app trong      │
│  quá trình khôi phục!        │
│                             │
└─────────────────────────────┘
```

### 6.5.19 Backup — Kết nối Google Drive (chưa đăng nhập)

```
┌─────────────────────────────┐
│  ← Backup & Restore         │
├─────────────────────────────┤
│                             │
│                             │
│          ☁️                  │
│      Google Drive            │
│                             │
│  Kết nối Google Drive để     │
│  backup dữ liệu cửa hàng   │
│  lên đám mây an toàn.       │
│                             │
│  ✅ Mã hóa AES-256          │
│  ✅ Chỉ bạn đọc được        │
│  ✅ Tự động backup hàng ngày │
│  ✅ Khôi phục mọi lúc       │
│                             │
│                             │
│  ┌─────────────────────┐    │
│  │ 🔵 Kết nối với       │    │
│  │    Google Drive      │    │
│  └─────────────────────┘    │
│                             │
│  Bằng cách kết nối, bạn     │
│  cho phép miniPOS lưu trữ   │
│  dữ liệu trong thư mục ẩn  │
│  trên Google Drive.          │
│                             │
│                             │
└─────────────────────────────┘
```

## 6.6 Navigation Pattern

### Home-centric Navigation (Phone)

```
Home Screen (Card Launcher)
    │
    ├── 🛒 Bán hàng (POS) → Step 1 → 2 → 3 → 4 → 5
    ├── 📦 Quản lý kho → Stock Overview, Stock Check, Movements
    ├── 📥 Nhập hàng → Purchase Order List → Create PO
    ├── 🏷️ Sản phẩm → Product List → Detail / Form
    ├── 🏭 Nhà cung cấp → Supplier List → Detail / Form
    ├── 👥 Khách hàng → Customer List → Detail / Form
    ├── 📊 Báo cáo → Sales / Product / Staff Report
    ├── 📜 Lịch sử đơn → Order List → Detail → Refund
    ├── ⚙️ Cài đặt → Settings subpages
    └── 📡 Thiết bị → Device Management
```

- **Home** là màn hình mặc định sau đăng nhập cho mọi role
- Mỗi card chức năng mở **full-screen page** mới (push navigation)
- Nút **← Back** hoặc **swipe back** để quay về Home
- Không dùng bottom tab bar → tập trung vào card launcher
- POS flow: **stepper navigation** (Next/Back), không back về Home giữa chừng
- Badge đỏ trên card "Quản lý kho" khi có SP sắp hết hàng
- Badge đỏ trên card "Cài đặt" khi có device request pending

### Home Screen (Tablet — Landscape)

```
Tablet giữ nguyên Home card-based layout nhưng 4 cột:
- Nhấn vào card → mở content trong main area (master-detail)
- Hoặc full-screen nếu là POS flow

┌──────┬──────────────────────────────────────┐
│ ≡    │                                      │
│      │                                      │
│ 🏠   │         Main Content Area            │
│ Home │         (mở từ Home card)            │
│      │                                      │
│ 🛒   │                                      │
│ POS  │         Hoặc hiển thị Home           │
│      │         card grid ở đây              │
│ 📦   │                                      │
│Stock │                                      │
│      │                                      │
│ �   │                                      │
│Import│                                      │
│      │                                      │
│ �️   │                                      │
│Prods │                                      │
│      │                                      │
│ �   │                                      │
│Cust. │                                      │
│      │                                      │
│ 📊   │                                      │
│Report│                                      │
│      │                                      │
│ ⚙️   │                                      │
│ Set  │                                      │
└──────┴──────────────────────────────────────┘

Tablet có thêm Side Rail (compact navigation) bên trái:
- Icon-only khi collapsed, icon + label khi expanded
- Home card grid vẫn là default view ở main area
- POS step-by-step sử dụng full main area
```

## 6.7 Animations & Transitions

| Thao tác | Animation |
|----------|-----------|
| Home card tap | Ripple + scale down 0.97 → push page |
| Home card long press | Subtle scale + tooltip info |
| POS step transition | Slide left/right (300ms ease) |
| POS stepper progress | Line fill animation (green) |
| Chọn SP (POS Step 1) | Badge bounce + checkmark appear |
| Xóa SP khỏi giỏ | Slide left + fade out |
| Edit giá bán | Field highlight glow (amber) |
| Thanh toán thành công | Checkmark confetti |
| Sync đang chạy | Rotating sync icon |
| Pull to refresh | Standard material pull |
| Page transition | Shared element / Slide |
| Error shake | Horizontal shake (PIN sai) |
| Low stock alert | Pulse animation on badge |
| Backup upload | Cloud fly-up + progress ring |
| Backup complete | Cloud checkmark bounce |
| Restore download | Cloud fly-down + progress ring |
| Restore complete | Refresh spin + success checkmark |
