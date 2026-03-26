# 5. Thiết kế đồng bộ dữ liệu P2P (Peer-to-Peer Sync)

## 5.1 Tổng quan

Đây là tính năng cốt lõi cho phép nhiều thiết bị trong cùng một cửa hàng chia sẻ dữ liệu mà **không cần server trung gian**. Tất cả dữ liệu được đồng bộ trực tiếp giữa các thiết bị qua mạng cục bộ (LAN/WiFi).

### Kịch bản ví dụ:

```
Cửa hàng "Zin100" có 3 thiết bị:

📱 Điện thoại A (Owner - Anh Minh) — Quầy thu ngân 1
📱 Điện thoại B (Cashier - Chị Lan) — Quầy thu ngân 2  
📱 Máy tính bảng C (Manager - Anh Tùng) — Quản lý kho

→ Khi Anh Minh thêm sản phẩm mới trên Điện thoại A
→ Sản phẩm tự động xuất hiện trên Điện thoại B và Máy tính bảng C
→ Khi Chị Lan bán hàng trên Điện thoại B
→ Tồn kho cập nhật trên tất cả thiết bị
```

## 5.2 Kiến trúc đồng bộ

### Mô hình: Mesh Network với Leader Election

```
┌──────────────────────────────────────────────────────┐
│                Store "Zin100" Network                 │
│                                                      │
│     ┌──────────┐         ┌──────────┐               │
│     │Device A  │◄───────►│Device B  │               │
│     │(Leader)  │         │(Follower)│               │
│     │  ⭐      │         │          │               │
│     └────┬─────┘         └────┬─────┘               │
│          │                    │                       │
│          │    ┌──────────┐    │                       │
│          └───►│Device C  │◄───┘                       │
│               │(Follower)│                            │
│               │          │                            │
│               └──────────┘                            │
│                                                      │
│  Kết nối: WiFi Direct / mDNS / Bonjour              │
│  Protocol: WebSocket over LAN                         │
└──────────────────────────────────────────────────────┘
```

### Leader Election Rules:
1. **Owner device** là leader ưu tiên
2. Nếu Owner offline → **Manager device** lâu nhất trở thành leader
3. Nếu không có Manager → **Cashier device** lâu nhất
4. Leader chịu trách nhiệm phân xử conflict

## 5.3 Cơ chế phát hiện thiết bị (Device Discovery)

### 5.3.1 Sử dụng mDNS / Bonjour / NSD

```
Service Name: _minipos._tcp.local.
TXT Records:
  - store_code: "ZIN100"
  - device_id: "uuid-of-device"
  - device_name: "iPhone của Minh"
  - role: "owner"
  - sync_version: "1234"        // Version mới nhất
  - app_version: "1.0.0"
```

### 5.3.2 Luồng Discovery

```
Thiết bị mới (Device B) muốn kết nối:

1. Device B broadcast: "Tôi muốn tham gia cửa hàng ZIN100"
   └─ Publish mDNS service: _minipos._tcp.local.
   
2. Device A (Owner) nhận được broadcast
   └─ Browse mDNS services, filter store_code = "ZIN100"

3. Device A hiển thị thông báo: 
   "📱 'Samsung của Lan' muốn tham gia cửa hàng"
   → [Chấp nhận] [Từ chối]

4. Owner chấp nhận → Thiết lập kết nối WebSocket
   └─ Device A gửi: approval + user credentials + full data sync

5. Device B nhận dữ liệu → Lưu vào local database
   └─ Hoàn tất quá trình tham gia
```

## 5.4 Cơ chế đồng bộ dữ liệu

### 5.4.1 Sync Strategy: Version Vector + Operation Log

Mỗi bản ghi trong database có:
- `sync_version`: Auto-increment number trên mỗi thiết bị
- `updated_at`: Timestamp chính xác
- `device_id`: Thiết bị thực hiện thay đổi cuối cùng

Mỗi thiết bị duy trì:
```
version_vector: {
    "device_A_id": 150,     // Biết dữ liệu từ Device A đến version 150
    "device_B_id": 89,      // Biết dữ liệu từ Device B đến version 89
    "device_C_id": 203      // Biết dữ liệu từ Device C đến version 203
}
```

### 5.4.2 Sync Types

#### Full Sync (Đồng bộ toàn bộ)
- Khi thiết bị mới tham gia cửa hàng
- Khi thiết bị offline quá lâu (> 7 ngày)
- Khi có lỗi inconsistency

```
Luồng Full Sync:

Device A (Leader)                    Device B (New)
     │                                    │
     │◄──── REQUEST_FULL_SYNC ───────────│
     │                                    │
     │ 1. Export toàn bộ dữ liệu        │
     │    (theo thứ tự dependency)        │
     │                                    │
     │──── SYNC_DATA [stores] ──────────►│
     │──── SYNC_DATA [users] ───────────►│
     │──── SYNC_DATA [categories] ──────►│
     │──── SYNC_DATA [suppliers] ───────►│
     │──── SYNC_DATA [customers] ───────►│
     │──── SYNC_DATA [products] ────────►│
     │──── SYNC_DATA [inventory] ───────►│
     │──── SYNC_DATA [orders] ──────────►│
     │──── SYNC_DATA [order_items] ─────►│
     │──── SYNC_COMPLETE ───────────────►│
     │                                    │
     │◄──── SYNC_ACK ───────────────────│
     │                                    │
```

#### Incremental Sync (Đồng bộ tăng dần)
- Đồng bộ thường xuyên khi các thiết bị đang kết nối
- Chỉ gửi dữ liệu thay đổi kể từ lần sync cuối

```
Luồng Incremental Sync:

Device A                              Device B
     │                                    │
     │ (Anh Minh thêm SP mới)           │
     │                                    │
     │──── SYNC_CHANGES ───────────────►│
     │     {                              │
     │       table: "products",           │
     │       operation: "INSERT",         │
     │       data: { ... product data },  │
     │       sync_version: 151,           │
     │       device_id: "device_A_id",    │
     │       timestamp: 1711353600000     │
     │     }                              │
     │                                    │
     │◄──── SYNC_ACK {version: 151} ────│
     │                                    │
```

### 5.4.3 Real-time Sync (Khi đang kết nối)

```
Khi có thay đổi trên bất kỳ thiết bị nào:

1. Thao tác trên DB local
   └─ Insert/Update/Delete record
   
2. Database trigger → Tạo sync event
   └─ SyncEvent { table, operation, record_id, data, version, timestamp }

3. SyncService broadcast event đến tất cả thiết bị đang kết nối
   └─ WebSocket message to all peers

4. Thiết bị nhận → Apply changes vào local DB
   └─ Kiểm tra conflict → Resolve → Apply
   
5. Gửi ACK về thiết bị gốc
   └─ Cập nhật version_vector
```

## 5.5 Xử lý xung đột (Conflict Resolution)

### 5.5.1 Các loại xung đột

| Loại | Ví dụ | Chiến lược |
|------|-------|-----------|
| **Update-Update** | 2 thiết bị sửa cùng 1 SP cùng lúc | Last Write Wins (LWW) |
| **Delete-Update** | 1 xóa, 1 sửa cùng SP | Delete wins |
| **Insert-Insert** | 2 thiết bị tạo SP cùng SKU | Merge + rename |
| **Inventory Conflict** | 2 thiết bị bán cùng SP, tồn kho không đủ | Accept both, cho phép âm kho + cảnh báo |

### 5.5.2 Last Write Wins (LWW) — Chiến lược chính

```
Khi nhận sync data cho record đã tồn tại:

incoming_record vs local_record:

IF incoming.updated_at > local.updated_at:
    → Ghi đè local bằng incoming
    
ELSE IF incoming.updated_at < local.updated_at:
    → Giữ nguyên local (local mới hơn)
    
ELSE IF incoming.updated_at == local.updated_at:
    → So sánh device_id (alphabetically) 
    → Device ID "nhỏ hơn" thắng (deterministic)
```

### 5.5.3 Đặc biệt: Xung đột tồn kho

Tồn kho là dữ liệu nhạy cảm nhất. Sử dụng **CRDT Counter** approach:

```
Thay vì đồng bộ giá trị tồn kho tuyệt đối (quantity = 50),
đồng bộ các THAO TÁC (operations):

Device A: Bán 2 SP → stock_movement: { type: 'sale_out', quantity: -2 }
Device B: Bán 1 SP → stock_movement: { type: 'sale_out', quantity: -1 }

Khi đồng bộ:
→ Cả 2 movements đều được apply
→ Tồn kho cuối = tồn kho đầu - 2 - 1
→ Không mất dữ liệu, không xung đột
```

**Quy trình:**
1. Mỗi thao tác kho tạo `stock_movement` record
2. Đồng bộ `stock_movements` (append-only, không bao giờ sửa/xóa)
3. `inventory.quantity` = SUM(all stock_movements cho product đó)
4. Periodically recalculate để đảm bảo chính xác

## 5.6 Protocol Messages

### 5.6.1 Message Format

```json
{
    "type": "SYNC_CHANGES",
    "id": "msg-uuid",
    "store_id": "store-uuid",
    "sender_device_id": "device-a-uuid",
    "timestamp": 1711353600000,
    "payload": {
        "changes": [
            {
                "table": "products",
                "operation": "INSERT",
                "record_id": "product-uuid",
                "data": { "...full record data..." },
                "sync_version": 151,
                "device_id": "device-a-uuid",
                "updated_at": 1711353600000
            }
        ]
    },
    "checksum": "sha256-hash"
}
```

### 5.6.2 Message Types

| Type | Mô tả |
|------|-------|
| `HELLO` | Handshake khi kết nối, trao đổi version_vector |
| `REQUEST_FULL_SYNC` | Yêu cầu đồng bộ toàn bộ |
| `SYNC_DATA` | Gửi batch dữ liệu (full sync) |
| `SYNC_CHANGES` | Gửi thay đổi incremental |
| `SYNC_ACK` | Xác nhận đã nhận và apply |
| `SYNC_REJECT` | Từ chối (conflict chưa resolve) |
| `SYNC_COMPLETE` | Full sync hoàn tất |
| `PING` | Heartbeat kiểm tra kết nối |
| `PONG` | Trả lời heartbeat |
| `DEVICE_JOIN` | Thiết bị mới yêu cầu tham gia |
| `DEVICE_APPROVED` | Owner chấp nhận thiết bị mới |
| `DEVICE_REJECTED` | Owner từ chối |
| `DEVICE_REMOVED` | Thiết bị bị xóa khỏi cửa hàng |

## 5.7 Trạng thái kết nối

```
┌─────────┐    discover    ┌────────────┐    connect    ┌───────────┐
│SEARCHING│───────────────►│  FOUND     │──────────────►│CONNECTING │
└─────────┘                └────────────┘               └─────┬─────┘
                                                              │
                           ┌────────────┐    handshake   ┌────▼──────┐
                           │  SYNCING   │◄──────────────│ CONNECTED │
                           └──────┬─────┘               └───────────┘
                                  │                           ▲
                           ┌──────▼─────┐                     │
                           │   READY    │─────────────────────┘
                           │ (realtime) │    ongoing sync
                           └──────┬─────┘
                                  │
                           ┌──────▼─────┐
                           │DISCONNECTED│──── auto reconnect (30s)
                           └────────────┘
```

## 5.8 Handling Offline Scenarios

### Kịch bản 1: Thiết bị offline → online

```
1. Device B offline 2 giờ
2. Trong 2 giờ: Device A tạo 5 SP mới, bán 10 đơn
3. Device B online → kết nối Device A
4. Trao đổi version_vector:
   - B biết A đến version 100
   - A hiện tại version 150
5. A gửi tất cả changes từ version 101 → 150
6. B apply changes → version_vector cập nhật
7. B gửi changes của mình (nếu có) cho A
```

### Kịch bản 2: Tất cả thiết bị offline (không có WiFi)

```
1. Mỗi thiết bị hoạt động độc lập với dữ liệu local
2. Khi có WiFi → các thiết bị tự tìm nhau (mDNS)
3. Đồng bộ incremental từ thời điểm mất kết nối
4. Resolve conflicts nếu có
```

### Kịch bản 3: Thiết bị mới khi không có Leader

```
1. Device D muốn tham gia nhưng Device A (Owner) offline
2. Device D đợi cho đến khi Device A online
3. Hoặc: Device B/C (nếu là Manager) có thể approve
   (cần cấu hình trong settings)
```

## 5.9 Bảo mật đồng bộ

| Biện pháp | Chi tiết |
|-----------|---------|
| **Mã hóa truyền tải** | TLS/SSL cho WebSocket connection |
| **Xác thực thiết bị** | Mỗi thiết bị phải được approve bởi Owner |
| **Store code verification** | Thiết bị phải biết đúng store_code mới kết nối được |
| **Message signing** | Mỗi message có checksum SHA-256 |
| **Data encryption** | Dữ liệu nhạy cảm (PIN, password) được hash trước khi sync |
| **Device revocation** | Owner có thể xóa thiết bị bất kỳ lúc nào → thiết bị bị xóa tự động ngắt kết nối và xóa dữ liệu |

## 5.10 Giới hạn & Lưu ý

| Giới hạn | Chi tiết |
|---------|---------|
| **Số thiết bị tối đa** | 10 thiết bị / cửa hàng (khuyến nghị ≤ 5) |
| **Khoảng cách** | Cùng mạng WiFi / LAN |
| **Dung lượng sync** | Batch tối đa 1000 records / message |
| **Timeout** | Heartbeat mỗi 10s, timeout 30s |
| **Retry** | Auto retry 3 lần khi sync fail, backoff exponential |
| **Sync order** | Phải sync theo thứ tự dependency: stores → users → categories → suppliers → customers → products → variants → inventory → orders |
