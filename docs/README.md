# miniPOS — Documentation

> **Version 1.1.0** · Android POS (Point of Sale) application  
> Last updated: April 14, 2026

---

## Table of Contents

1. [Overview](#1-overview)
2. [Tech Stack](#2-tech-stack)
3. [Architecture](#3-architecture)
4. [Project Structure](#4-project-structure)
5. [Database Schema](#5-database-schema)
6. [Feature Modules](#6-feature-modules)
7. [Authentication & Authorization](#7-authentication--authorization)
8. [POS Flow](#8-pos-flow)
9. [Inventory Management](#9-inventory-management)
10. [Receipt & Printing](#10-receipt--printing)
11. [Barcode System](#11-barcode-system)
12. [Backup & Restore](#12-backup--restore)
13. [Wi-Fi P2P Sync](#13-wi-fi-p2p-sync)
14. [Theming & Localization](#14-theming--localization)
15. [App Rating System](#15-app-rating-system)
16. [Navigation](#16-navigation)
17. [Build & Release](#17-build--release)
18. [Dependencies](#18-dependencies)

---

## 1. Overview

**miniPOS** is a fully offline-first Android POS application designed for small to medium retail businesses. The app runs entirely on-device with a local SQLite database — no cloud server required. Data can be shared between devices using Wi-Fi P2P sync.

### Key Capabilities

| Feature | Description |
|---|---|
| **Point of Sale** | Full POS flow with cart, discounts, tax, multiple payment methods |
| **Product Management** | Products with variants, categories, suppliers, barcode, images |
| **Inventory** | Real-time stock tracking, purchase orders, stock audits, movement history |
| **Customer Management** | Customer profiles, purchase history, debt tracking |
| **Order Management** | Order history, detail view, refund/cancel support |
| **Reports** | Revenue, profit, order analytics with chart visualizations |
| **Receipt** | Text, bitmap, PDF generation; Bluetooth thermal printing; share/print via system |
| **Backup** | AES-256-GCM encrypted local backups (.mpos files) |
| **Wi-Fi Sync** | Peer-to-peer data sync between devices via NSD + raw sockets |
| **Multi-user** | Owner / Manager / Cashier roles with granular permissions |
| **Barcode** | EAN-13 generation, QR code, ML Kit camera scanning |
| **Theming** | Light / Dark / System theme; English & Vietnamese languages |

---

## 2. Tech Stack

| Layer | Technology |
|---|---|
| Language | **Kotlin** (JVM 17) |
| UI Framework | **Jetpack Compose** (Material 3) with Compose BOM 2024.02.02 |
| Architecture | **MVVM** + Clean Architecture (domain/data/ui layers) |
| DI | **Hilt** (Dagger) 2.50 |
| Database | **Room** 2.6.1 (SQLite) — schema version 6 |
| Preferences | **DataStore** (Preferences) |
| Navigation | **Navigation Compose** 2.7.6 |
| Async | **Kotlin Coroutines** + Flow |
| Camera | **CameraX** 1.4.1 |
| Barcode Scan | **ML Kit** Barcode Scanning 17.3.0 |
| Image Loading | **Coil** 2.5.0 |
| Charts | **Vico** (compose-m3) 1.13.1 |
| Auth | **Google Sign-In** (Play Services Auth 20.7.0) |
| Cloud Storage | **Google Drive API** v3 |
| P2P Sync | **Nearby Connections** 19.1.0 + NSD |
| Security | **BCrypt** (at.favre.lib) 0.10.2, Android Keystore AES-256-GCM |
| Biometric | **AndroidX Biometric** 1.2.0-alpha05 |
| Background | **WorkManager** 2.9.0 |
| In-App Review | **Google Play Review** 2.0.1 |
| Testing | JUnit 4, MockK, Espresso, Compose UI Test |
| Build Tool | **Gradle** 8.13.2, KSP 1.9.22 |
| Min SDK | **26** (Android 8.0) |
| Target SDK | **35** |

---

## 3. Architecture

The app follows **Clean Architecture** with three main layers:

```
┌──────────────────────────────────────────────────┐
│                    UI Layer                       │
│  (Compose Screens, ViewModels, Components)        │
│  com.minipos.ui.*                                │
├──────────────────────────────────────────────────┤
│                 Domain Layer                      │
│  (Models, Repository Interfaces, Use Cases)       │
│  com.minipos.domain.*                            │
├──────────────────────────────────────────────────┤
│                  Data Layer                       │
│  (Room DB, DAOs, Repository Impls, Preferences)   │
│  com.minipos.data.*                              │
├──────────────────────────────────────────────────┤
│                  Core Layer                       │
│  (Auth, Backup, Barcode, DI, Rating, Receipt,     │
│   Sync, Theme, Utils)                            │
│  com.minipos.core.*                              │
└──────────────────────────────────────────────────┘
```

### Data Flow

```
Screen (Compose) → ViewModel → UseCase → Repository (interface)
                                              ↓
                                    RepositoryImpl (data layer)
                                      ↓           ↓
                                    Room DAO    AppPreferences
```

- **Unidirectional data flow**: UI observes `StateFlow`/`Flow` from ViewModels
- **Repository pattern**: Domain layer defines interfaces; data layer implements them
- **Hilt DI**: All dependencies provided via `AppModule` singleton scope

---

## 4. Project Structure

```
app/src/main/java/com/minipos/
├── MiniPosApplication.kt          # @HiltAndroidApp entry point
│
├── core/                           # Cross-cutting concerns
│   ├── auth/                       # Session, Permission, PermissionGuard
│   │   ├── PermissionChecker.kt    # Central permission logic (role-based)
│   │   ├── PermissionGuard.kt      # Composable helpers for permission-gated UI
│   │   └── SessionManager.kt       # Singleton session + permission queries
│   ├── backup/
│   │   └── BackupManager.kt        # AES-256-GCM encrypted backup/restore
│   ├── barcode/
│   │   ├── BarcodeGenerator.kt     # EAN-13 + QR Code bitmap generation
│   │   └── BarcodePrintHelper.kt   # Bluetooth barcode label printing
│   ├── constants/
│   │   └── AppConstants.kt         # App-wide constants (DB name, PIN config, etc.)
│   ├── di/
│   │   └── AppModule.kt            # Hilt module — DB, DAOs, repositories, managers
│   ├── rating/
│   │   ├── AppRatingDialog.kt      # Animated star-picker rating dialog
│   │   └── RatingManager.kt        # Smart rating prompt strategy
│   ├── receipt/
│   │   ├── ReceiptGenerator.kt     # Plain text receipt (for thermal printers)
│   │   ├── ReceiptBitmapGenerator.kt # Bitmap receipt
│   │   ├── ReceiptPdfGenerator.kt  # PDF receipt (Android PdfDocument)
│   │   ├── ReceiptPreviewDialog.kt # In-app receipt preview
│   │   ├── ReceiptPrintHelper.kt   # Bluetooth ESC/POS thermal printing
│   │   ├── ReceiptShareHelper.kt   # Share receipt via Intent
│   │   └── ReceiptSystemPrintHelper.kt # Android system print service
│   ├── sync/
│   │   └── WifiSyncManager.kt      # NSD-based P2P sync (server + client)
│   ├── theme/
│   │   ├── AppColors.kt            # Custom color tokens
│   │   ├── Theme.kt                # MiniPosTheme Compose theme
│   │   ├── ThemeManager.kt         # Theme mode + language manager
│   │   └── Typography.kt           # Custom typography
│   └── utils/
│       ├── CurrencyFormatter.kt    # VND formatting + compact notation
│       ├── DateUtils.kt            # Date/time formatting helpers
│       ├── HashUtils.kt            # BCrypt hashing
│       ├── ImageHelper.kt          # Image compression/resize
│       ├── UuidGenerator.kt        # UUID generation
│       └── Validators.kt           # Input validation helpers
│
├── data/                           # Data layer
│   ├── database/
│   │   ├── MiniPosDatabase.kt      # Room DB (14 entities, version 6, 6 migrations)
│   │   ├── dao/
│   │   │   └── Daos.kt             # All DAO interfaces (10 DAOs)
│   │   ├── entity/
│   │   │   └── Entities.kt         # All Room entities (14 tables)
│   │   ├── converter/              # (Type converters)
│   │   └── migration/              # (Future migrations)
│   ├── preferences/
│   │   └── AppPreferences.kt       # DataStore preferences (session, theme, rating, etc.)
│   └── repository/
│       ├── AuthRepositoryImpl.kt   # Auth implementation
│       └── RepositoryImpls.kt      # All other repository implementations
│
├── domain/                         # Domain layer (pure Kotlin, no Android deps)
│   ├── model/
│   │   ├── Cart.kt                 # In-memory cart + CartItem + Discount
│   │   ├── Category.kt             # Category model
│   │   ├── Customer.kt             # Customer model (with debt, initials)
│   │   ├── Inventory.kt            # InventoryItem, StockMovement, PurchaseOrder, etc.
│   │   ├── Order.kt                # Order, OrderItem, OrderPayment, PaymentMethod
│   │   ├── Permission.kt           # Permission enum (60+ granular permissions)
│   │   ├── Product.kt              # Product, ProductVariant, ProductWithStock
│   │   ├── Result.kt               # Result<T> sealed class + ErrorCode enum
│   │   ├── Store.kt                # Store, StoreSettings, CashierPermissions
│   │   ├── Supplier.kt             # Supplier model
│   │   └── User.kt                 # User, UserRole, AuthSession
│   ├── repository/
│   │   └── Repositories.kt         # All repository interfaces (8 repos)
│   └── usecase/
│       ├── auth/                    # Auth use cases
│       ├── category/                # Category use cases
│       ├── customer/                # Customer use cases
│       ├── inventory/               # Inventory use cases
│       ├── order/                   # Order use cases
│       ├── product/                 # Product use cases
│       ├── report/                  # Report use cases
│       └── supplier/                # Supplier use cases
│
└── ui/                             # UI layer (Compose)
    ├── MainActivity.kt             # @AndroidEntryPoint, theme + app state router
    ├── MainViewModel.kt            # Root ViewModel (AppState, rating, auth flow)
    ├── navigation/
    │   ├── Screen.kt               # Sealed class defining all routes
    │   └── NavGraph.kt             # Navigation graph with all composable destinations
    ├── components/
    │   ├── MiniPosComponents.kt    # Shared UI components (buttons, inputs, cards)
    │   ├── MiniPosPopups.kt        # Dialogs, bottom sheets, popups
    │   ├── CategoryIconMapper.kt   # Category icon mapping
    │   └── ShimmerEffect.kt        # Loading shimmer animation
    ├── splash/                     # Animated splash screen
    ├── onboarding/                 # Onboarding, CreateStore, JoinStore
    ├── login/                      # User selection + PIN/password login
    ├── pinlock/                    # PIN lock screen (background return)
    ├── home/                       # Home/Dashboard, StoreManagement hub, SetupGuide
    ├── pos/                        # POS flow (Step4=Payment, Step5=Complete)
    ├── product/                    # Product list + form (CRUD)
    ├── category/                   # Category list + form (CRUD, hierarchical)
    ├── supplier/                   # Supplier list + form (CRUD)
    ├── customer/                   # Customer list, form, detail
    ├── order/                      # Order list + detail
    ├── inventory/                  # Inventory hub + overview
    ├── purchase/                   # Purchase orders (nhập hàng)
    ├── stockmanagement/            # Stock management dashboard
    ├── stockaudit/                 # Stock audit / adjustment (kiểm kho)
    ├── report/                     # Reports with charts
    ├── settings/                   # App settings + Store settings
    ├── barcode/                    # Barcode management + preview
    ├── scan/                       # Scan-to-POS (camera barcode → cart)
    ├── scanner/                    # Barcode scanner camera, image viewer/picker
    └── sync/                       # Wi-Fi sync UI
```

---

## 5. Database Schema

**Room Database**: `minipos.db` — Version 6, 14 entities

### Entity Relationship Diagram

```
stores (1) ──┬──< users (N)
              ├──< categories (N) ──< categories (self-ref: parent_id)
              ├──< suppliers (N)
              ├──< products (N) ──┬──< product_variants (N)
              │                    └──< inventory (N, per variant)
              ├──< customers (N)
              ├──< orders (N) ──┬──< order_items (N)
              │                  └──< order_payments (N)
              ├──< stock_movements (N)
              └──< purchase_orders (N) ──< purchase_order_items (N)
```

### Tables

| Table | Description | Key Relations |
|---|---|---|
| `stores` | Store info, settings (JSON), currency | Root entity |
| `users` | Staff accounts (owner/manager/cashier), PIN hash, password hash | → stores |
| `categories` | Hierarchical categories with icon/color/sort | → stores, self-ref parent |
| `suppliers` | Supplier profiles with bank info | → stores |
| `products` | Products with SKU, barcode, pricing, tax, images | → stores, categories, suppliers |
| `product_variants` | Product variants with own SKU, barcode, pricing | → stores, products |
| `customers` | Customer profiles with spending/visit tracking | → stores |
| `inventory` | Stock levels per product/variant (unique per store+product+variant) | → stores, products |
| `orders` | Completed/refunded orders with totals | → stores |
| `order_items` | Line items per order | → orders |
| `order_payments` | Payment records per order (cash, transfer, e-wallet) | → orders |
| `stock_movements` | Full audit trail of stock changes | → products, suppliers |
| `purchase_orders` | Purchase/import orders (nhập hàng) | → stores |
| `purchase_order_items` | Line items per purchase order | → purchase_orders |

### Migrations

| Migration | Changes |
|---|---|
| 1 → 2 | Added `supplier_id` to `stock_movements` |
| 2 → 3 | Added `additional_images` to `products` |
| 3 → 4 | Created `product_variants` table |
| 4 → 5 | Added `mobile`, `payment_term`, `bank_name`, `bank_account`, `bank_account_holder` to `suppliers` |
| 5 → 6 | Created `purchase_orders` and `purchase_order_items` tables |

### Soft Delete Pattern

All major entities use soft delete (`is_deleted`, `deleted_at`) — records are never physically removed. Queries filter `WHERE is_deleted = 0`.

### Sync Support

Every entity includes `sync_version` and `device_id` columns for multi-device sync conflict resolution.

---

## 6. Feature Modules

### 6.1 Products

- CRUD with image capture (camera + gallery, multiple images)
- SKU auto-generation (`SP001`, `SP002`, ...)
- Barcode assignment (manual or auto-generated EAN-13)
- **Product Variants**: optional variants per product (e.g., Size S/M/L) with own SKU, barcode, pricing
- Unit selection (pcs, kg, box, etc.)
- Cost price / Selling price / Tax rate
- Min/max stock thresholds
- Category + Supplier linking
- Active/Inactive toggle

### 6.2 Categories

- Hierarchical (parent → child) with unlimited nesting
- Custom icons (from 100+ mapped Material icons) and colors
- Sort order support
- Product count per category

### 6.3 Suppliers

- Full contact info (phone, mobile, email, address)
- Tax code, payment terms
- Bank account details
- Link to products and purchase orders

### 6.4 Customers

- Profile with phone, email, address, notes
- Auto-tracked: total spent, visit count, last visit
- Debt tracking (`debtAmount`)
- Search by name/phone
- Initials avatar auto-generated
- Linkable to orders during POS checkout

### 6.5 Orders

- Order code auto-generated (`HD001`, `HD002`, ...)
- Statuses: Completed, Refunded, Partially Refunded, Cancelled
- Item-level and order-level discounts (percent or fixed)
- Tax calculation
- Multiple payment methods: Cash, Transfer, E-wallet, Other
- Change calculation for cash payments
- Customer linking

### 6.6 Reports

- Revenue, profit, order count analytics
- Configurable date ranges
- Chart visualizations (Vico library)

### 6.7 Stock Management

- Real-time stock overview with low-stock alerts
- Stock movement history with full audit trail
- Movement types: purchase_in, sale_out, return_in/out, adjustment_in/out, damage_out, transfer
- Stock audit / adjustment (kiểm kho)

### 6.8 Purchase Orders (Nhập hàng)

- Create purchase orders with supplier selection
- Auto code generation (`NK001`, `NK002`, ...)
- Multiple product line items
- Auto stock-in on confirmation
- Purchase history with detail view

---

## 7. Authentication & Authorization

### 7.1 User Roles

| Role | Description |
|---|---|
| **Owner** | Full access to everything. Creates the store. Cannot be deleted. |
| **Manager** | Business management (products, inventory, orders, reports). Cannot change system settings or manage other Managers/Owner. |
| **Cashier** | POS-only by default. Extended permissions configurable by Owner. |

### 7.2 Auth Flow

```
App Launch → Splash → Check Onboarded?
  ├── No  → Onboarding → CreateStore / JoinStore
  └── Yes → Check LoggedIn?
        ├── No  → Login (User Selection)
        └── Yes → Has PIN? 
              ├── Yes → PinLock
              └── No  → Home
```

- **PIN**: 4-6 digit PIN for quick login (BCrypt hashed)
- **Password**: Optional backup authentication; used for PIN reset
- **Biometric**: Optional fingerprint/face unlock
- **Account Lock**: After 5 failed attempts, locked for 5 minutes
- **Session Timeout**: Cashier=5min, Manager=15min, Owner=30min (configurable)

### 7.3 Permission System

60+ granular permissions organized by module (Store, Users, Categories, Products, Suppliers, Customers, Inventory, Orders, Reports, Devices, Data).

```
PermissionChecker (central logic)
    ├── ownerPermissions    → ALL permissions
    ├── managerPermissions  → Business ops subset
    └── cashierPermissions  → Base set + CashierPermissions overrides
```

**CashierPermissions** (configurable by Owner in Settings):

| Permission | Default | Description |
|---|---|---|
| `canApplyDiscount` | ❌ | Allow cashier to apply discounts |
| `maxDiscountPercent` | 10% | Maximum discount % for cashier |
| `canEditPrice` | ❌ | Allow cashier to edit selling prices |
| `canViewStock` | ❌ | Allow cashier to view inventory |
| `canViewCostPrice` | ❌ | Allow cashier to view cost prices |
| `canCancelOrder` | ❌ | Allow cashier to cancel orders |
| `requireApprovalForRefund` | ✅ | Require manager/owner approval for refunds |
| `canViewAllOrders` | ❌ | Allow viewing all orders (not just own) |

**UI Guards**: `PermissionGuard`, `PermissionGate`, `PermissionGuardAll`, `PermissionGuardAny` Composables + `UserRole.can()` extensions.

---

## 8. POS Flow

The POS flow is integrated into the HomeScreen with separate screens for Payment and Completion:

```
HomeScreen (POS Steps 1-3)
├── Step 1: Product selection (grid/list, category filter, search, barcode scan)
├── Step 2: Cart review (quantity edit, item discount, notes)
├── Step 3: Customer selection + Order discount
│
├── PosStep4Screen: Payment
│   ├── Payment method selection (Cash / Transfer / E-wallet / Other)
│   ├── Split payment support
│   ├── Cash received → change calculation
│   └── Confirm payment → create order
│
└── PosStep5Screen: Completion
    ├── Order confirmation summary
    ├── Receipt preview / print / share
    ├── New order / Go home
    └── Triggers rating check (onSuccessAction)
```

### Cart Model

```kotlin
Cart(
    items: List<CartItem>,   // product + variant + quantity + price + discount
    orderDiscount: Discount?, // order-level discount (percent/fixed)
    customer: Customer?,      // linked customer
    notes: String?,
)
```

- `PosCartHolder`: In-memory cart singleton shared across POS ViewModels
- Supports item-level and order-level discounts
- Tax calculated per-item based on product tax rate

---

## 9. Inventory Management

### Stock Tracking

- `inventory` table: quantity per product (or product+variant)
- Unique constraint: `(store_id, product_id, variant_id)`
- Variant-aware: products with variants track stock at variant level

### Stock Movements

Every stock change is recorded in `stock_movements` with:
- Type (purchase_in, sale_out, return_in/out, adjustment_in/out, damage_out, transfer)
- Quantity before/after
- Unit cost
- Reference ID (order, purchase order, etc.)
- Created by (user ID)

### Stock Audit

`StockAuditScreen` allows:
- Physical count entry
- Auto-calculate adjustment (counted − expected)
- Bulk adjustment with notes

### Low Stock Alerts

- Each product has `min_stock` threshold
- `getLowStockCount()` query counts products below threshold
- Dashboard shows alert badge

---

## 10. Receipt & Printing

### Generation Formats

| Format | Class | Description |
|---|---|---|
| **Plain Text** | `ReceiptGenerator` | 32-char width for 58mm thermal |
| **Bitmap** | `ReceiptBitmapGenerator` | Image receipt |
| **PDF** | `ReceiptPdfGenerator` | A4 PDF via Android `PdfDocument` |

### Printing Options

| Method | Class | Description |
|---|---|---|
| **Bluetooth Thermal** | `ReceiptPrintHelper` | ESC/POS protocol, 58mm/80mm |
| **System Print** | `ReceiptSystemPrintHelper` | Android print framework |
| **Share** | `ReceiptShareHelper` | Share via Intent (image/PDF) |

### Receipt Content

- Store header (name, address, phone, logo, custom header)
- Order info (code, date, customer)
- Item details (name, variant, quantity × price, discounts)
- Totals (subtotal, discounts, tax, grand total)
- Payment breakdown (method, received, change)
- Custom footer + "Thank you" message

---

## 11. Barcode System

### Generation

- **EAN-13**: Standard retail barcode using prefix `200` (GS1 in-store use range)
- **QR Code**: Minimal Reed-Solomon encoder (version 1-4)
- Auto-assignment during product creation
- Bulk barcode label generation and preview

### Scanning

- **ML Kit Barcode Scanning**: Real-time camera scanning via CameraX
- **Scan-to-POS**: Scan barcode → auto-add to cart (products + variants)
- Supports EAN-13, QR Code, and other formats

### Barcode Management

- `BarcodeScreen`: List products with barcodes, select for printing
- `BarcodePreviewScreen`: Print preview, Bluetooth thermal printing
- Shared ViewModel via navigation graph scoping

---

## 12. Backup & Restore

### Local Backup

- **Format**: `.mpos` file (Base64-encoded AES-256-GCM encrypted GZipped JSON)
- **Encryption**: Android Keystore (key never leaves secure hardware)
- **Content**: Full database snapshot (all 14 tables)
- **Location**: App private directory (`files/backups/`)

### API

```kotlin
BackupManager.createBackup(): BackupResult    // create encrypted backup
BackupManager.restoreBackup(path): RestoreResult  // restore (REPLACES all data)
BackupManager.listBackups(): List<BackupFileInfo>  // list available backups
BackupManager.deleteBackup(path): Boolean     // delete a backup file
```

### Backup Version

Current backup version: **2** — includes all tables through migration 5→6.

---

## 13. Wi-Fi P2P Sync

### How It Works

Uses **NSD (Network Service Discovery)** over local Wi-Fi:

1. **Server** device registers `_vipos._tcp.` service on port 54321
2. **Client** discovers nearby devices with matching store code
3. Handshake → validate store code
4. Bidirectional data exchange via JSON payloads over raw TCP sockets
5. Conflict resolution via `sync_version` + `device_id`

### Operations

| Command | Description |
|---|---|
| `HANDSHAKE` | Verify connectivity + store code |
| `SYNC_REQUEST` | Request full data payload from server |
| `SYNC_DATA` | Send data payload to server |
| `JOIN_REQUEST` | New device joining store — receives full snapshot |

### Security

- Store code verification on every connection
- Protocol version check
- Data encrypted at rest (via backup encryption)

---

## 14. Theming & Localization

### Theme

Managed by `ThemeManager` (singleton, Hilt-injected):

| Mode | Behavior |
|---|---|
| `SYSTEM` | Follow device dark mode setting |
| `LIGHT` | Force light theme |
| `DARK` | Force dark theme |

Custom design system:
- `AppColors`: Custom color tokens (Primary, Surface, Text, Success, Warning, etc.)
- `MiniPosTokens`: Radius, spacing, elevation tokens
- `Typography.kt`: Custom text styles

### Localization

| Language | Code | Status |
|---|---|---|
| English | `en` | ✅ Default |
| Vietnamese | `vi` | ✅ Full translation |

- Managed by `ThemeManager.setLanguage()`
- Uses `AppCompatDelegate.setApplicationLocales()`
- Resource files: `values/strings.xml` (en), `values-vi/strings.xml` (vi)
- Locale config: `xml/locales_config.xml`

---

## 15. App Rating System

### Strategy (`RatingManager`)

Smart, non-intrusive rating prompts:

1. Count successful actions (order complete, product saved, etc.)
2. Show dialog when:
   - User has **not** already rated
   - Action count ≥ threshold (3 first time, 10 after dismiss)
   - ≥ 3 days since last dismiss
   - Total dismissals < 3
3. Star picker (1-5):
   - **1-3★** → Open email to `zenixhq.com@gmail.com` with device info
   - **4-5★** → Google Play In-App Review API
4. "Maybe Later" → dismiss with cooldown

### Dialog (`AppRatingDialog`)

- Animated star picker with bounce effects
- Dynamic subtitle per star count
- Gradient submit button (gold for 4-5★, orange for 1-3★)
- Thank-you animation with heartbeat effect
- Auto-dismiss after 2.5 seconds

---

## 16. Navigation

### App State Machine

```
Splash → Onboarding → Login → PinLock → Home
           ↑                     ↑
           └── first launch      └── background return
```

`MainViewModel` manages `AppState` enum: `Splash`, `Onboarding`, `Locked`, `Login`, `Home`.

### Screen Routes

| Screen | Route | Description |
|---|---|---|
| Home | `home` | Dashboard + POS (steps 1-3) |
| POS Payment | `pos/step4` | Payment screen |
| POS Complete | `pos/step5` | Order completion |
| Products | `products` | Product list |
| Product Form | `products/form?id={id}` | Create/edit product |
| Categories | `categories` | Category list |
| Category Form | `categories/form?id={id}&parentId={parentId}` | Create/edit category |
| Suppliers | `suppliers` | Supplier list |
| Supplier Form | `suppliers/form?id={id}` | Create/edit supplier |
| Customers | `customers` | Customer list |
| Customer Form | `customers/form?id={id}` | Create/edit customer |
| Customer Detail | `customers/detail/{id}` | Customer profile |
| Orders | `orders` | Order list |
| Order Detail | `orders/{id}` | Order detail |
| Inventory Hub | `inventory_hub` | Inventory overview hub |
| Inventory | `inventory` | Stock overview |
| Purchase Order | `inventory/purchase` | Create purchase order |
| Purchase Detail | `inventory/purchase/{id}` | Purchase order detail |
| Reports | `reports` | Analytics & charts |
| Settings | `settings` | App settings |
| Store Settings | `settings/store` | Store configuration |
| Barcode Mgmt | `barcode` | Barcode management |
| Barcode Preview | `barcode/preview` | Barcode print preview |
| Stock Mgmt | `stock_management` | Stock dashboard |
| Stock Audit | `stock_audit` | Stock audit/adjustment |
| Scan to POS | `scan_to_pos` | Camera scan → cart |
| Store Mgmt Hub | `store_management` | Store management menu |
| Wi-Fi Sync | `wifi_sync` | P2P sync UI |
| Login | `login` | User selection |
| Onboarding | `onboarding` | Welcome screen |
| Create Store | `create_store` | New store setup |
| Join Store | `join_store` | Join existing store |

### Transitions

- Forward: fade + slide from right
- Back: fade + slide from left
- Home: fade only (no slide)
- App state changes: crossfade with scale

---

## 17. Build & Release

### Build Variants

| Variant | App ID | Debug | Minify |
|---|---|---|---|
| `debug` | `com.minipos.debug` | ✅ | ❌ |
| `release` | `com.minipos` | ❌ | ✅ (R8 + shrink resources) |

### Signing

Release signing configured via `local.properties`:

```properties
signing.storeFile=../miniPOS.jks
signing.storePassword=***
signing.keyAlias=***
signing.keyPassword=***
```

### ProGuard

R8 minification + resource shrinking enabled for release builds. ProGuard rules in `app/proguard-rules.pro`.

### Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

---

## 18. Dependencies

### Production

| Category | Library | Version |
|---|---|---|
| **Compose** | compose-bom | 2024.02.02 |
| | material3 | BOM-managed |
| | material-icons-extended | BOM-managed |
| **Lifecycle** | activity-compose | 1.8.2 |
| | lifecycle-runtime-compose | 2.7.0 |
| | lifecycle-viewmodel-compose | 2.7.0 |
| **Navigation** | navigation-compose | 2.7.6 |
| **DI** | hilt-android | 2.50 |
| | hilt-navigation-compose | 1.1.0 |
| | hilt-work | 1.1.0 |
| **Database** | room-runtime + room-ktx | 2.6.1 |
| **Preferences** | datastore-preferences | 1.0.0 |
| **Coroutines** | kotlinx-coroutines-android | 1.7.3 |
| **JSON** | gson | 2.10.1 |
| **Security** | bcrypt (favre) | 0.10.2 |
| **Camera** | camera-camera2 + lifecycle + view | 1.4.1 |
| **Barcode** | mlkit-barcode-scanning | 17.3.0 |
| **Image** | coil-compose | 2.5.0 |
| | exifinterface | 1.3.7 |
| **Biometric** | biometric | 1.2.0-alpha05 |
| **Auth** | play-services-auth | 20.7.0 |
| **Drive** | google-api-services-drive | v3-rev20231128 |
| **P2P** | play-services-nearby | 19.1.0 |
| **Background** | work-runtime-ktx | 2.9.0 |
| **Review** | play-review + review-ktx | 2.0.1 |
| **Charts** | vico compose-m3 | 1.13.1 |
| **Splash** | core-splashscreen | 1.0.1 |

### Testing

| Library | Version |
|---|---|
| junit | 4.13.2 |
| kotlinx-coroutines-test | 1.7.3 |
| mockk | 1.13.9 |
| espresso-core | 3.5.1 |
| compose-ui-test-junit4 | BOM-managed |

---

## Appendix: Permissions Manifest

The app requires the following Android permissions:

| Permission | Purpose |
|---|---|
| `INTERNET` | P2P sync, Google Drive |
| `ACCESS_NETWORK_STATE` | Network availability check |
| `ACCESS_WIFI_STATE` / `CHANGE_WIFI_STATE` | Wi-Fi sync |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Nearby device discovery |
| `NEARBY_WIFI_DEVICES` | Wi-Fi Direct (Android 13+) |
| `BLUETOOTH` / `BLUETOOTH_ADMIN` | Thermal printer (≤ API 30) |
| `BLUETOOTH_CONNECT` / `BLUETOOTH_SCAN` | Thermal printer (API 31+) |
| `CAMERA` | Barcode scanning |
| `USE_BIOMETRIC` | Fingerprint/face unlock |
| `READ_EXTERNAL_STORAGE` | Backup restore (≤ API 32) |
| `FOREGROUND_SERVICE` | Background sync |
| `POST_NOTIFICATIONS` | Sync/backup notifications |
