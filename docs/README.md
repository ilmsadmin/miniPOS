# 📱 miniPOS - Hệ thống bán hàng mini cho cửa hàng nhỏ

## Tổng quan dự án

**miniPOS** là ứng dụng bán hàng (Point of Sale) trên điện thoại di động, được thiết kế dành riêng cho các cửa hàng nhỏ và doanh nghiệp vừa. Toàn bộ dữ liệu được lưu trữ trực tiếp trên thiết bị (local-first), đồng thời hỗ trợ đồng bộ dữ liệu giữa nhiều thiết bị trong cùng một cửa hàng thông qua kết nối mạng nội bộ (P2P sync).

---

## 📚 Mục lục tài liệu

| # | Tài liệu | Mô tả |
|---|----------|-------|
| 1 | [Tổng quan hệ thống](./01-system-overview.md) | Kiến trúc, công nghệ, mô hình dữ liệu |
| 2 | [Yêu cầu chức năng](./02-functional-requirements.md) | Chi tiết các chức năng của hệ thống |
| 3 | [Thiết kế Database](./03-database-design.md) | Schema, bảng, quan hệ dữ liệu |
| 4 | [Thiết kế API / Services](./04-api-services-design.md) | Các service layer và business logic |
| 5 | [Đồng bộ dữ liệu P2P](./05-p2p-sync-design.md) | Cơ chế đồng bộ giữa các thiết bị |
| 6 | [Thiết kế UI/UX](./06-ui-ux-design.md) | Wireframe, luồng màn hình |
| 7 | [Bảo mật & Phân quyền](./07-security-authorization.md) | Xác thực, phân quyền, mã hóa |
| 8 | [Kế hoạch triển khai](./08-deployment-plan.md) | Roadmap, milestone, ước lượng |
| 9 | [Backup & Restore Google Drive](./09-backup-restore-gdrive.md) | Sao lưu/khôi phục dữ liệu lên cloud |

---

## 🎯 Mục tiêu sản phẩm

- **Đơn giản**: Home screen card-based, POS bán hàng 5 bước, dễ sử dụng
- **Offline-first**: Hoạt động hoàn toàn không cần internet
- **Đồng bộ thông minh**: Nhiều thiết bị cùng cửa hàng tự động đồng bộ dữ liệu
- **Cloud Backup**: Sao lưu dữ liệu lên Google Drive, khôi phục bất cứ lúc nào
- **Quản lý khách hàng**: Tạo, tìm kiếm khách hàng, gắn khách vào đơn hàng
- **Nhẹ & Nhanh**: Ứng dụng nhỏ gọn, khởi động nhanh
- **Miễn phí**: Không cần server, không phí hàng tháng

---

## 👥 Đối tượng sử dụng

| Vai trò | Mô tả |
|---------|-------|
| **Chủ cửa hàng (Owner)** | Quản lý toàn bộ hệ thống, tạo cửa hàng, thêm nhân viên |
| **Quản lý (Manager)** | Quản lý kho, sản phẩm, nhà cung cấp, xem báo cáo |
| **Nhân viên bán hàng (Cashier)** | Thực hiện bán hàng POS, xem sản phẩm |

---

## 🔧 Công nghệ đề xuất

| Thành phần | Công nghệ |
|-----------|-----------|
| Mobile Framework | React Native / Flutter |
| Local Database | SQLite (via Drift/sqflite) hoặc Realm |
| P2P Sync | Multipeer Connectivity (iOS) / Nearby Connections (Android) / WebSocket LAN |
| State Management | Riverpod (Flutter) / Zustand (React Native) |
| UI Components | Material Design 3 |

---

*Phiên bản tài liệu: 1.0.0*  
*Ngày tạo: 25/03/2026*  
*Cập nhật lần cuối: 25/03/2026*
