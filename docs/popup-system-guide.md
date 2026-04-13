# Popup System — Hướng dẫn sử dụng

File gốc: `MiniPosPopups.kt`

Tất cả popup được thiết kế đồng nhất dựa trên mock `popup-system.html`, sử dụng `AppColors` + `MiniPosTokens`.

---

## 1. Toast — Thông báo nhẹ, tự ẩn

```kotlin
var showToast by remember { mutableStateOf(false) }

MiniPosToastStyled(
    message = "Đã lưu thành công!",
    visible = showToast,
    type = PopupType.SUCCESS,  // SUCCESS | ERROR | WARNING | INFO
    onDismiss = { showToast = false },
)
```

## 2. Snackbar — Thông báo có action

```kotlin
var showSnack by remember { mutableStateOf(false) }

MiniPosSnackbar(
    message = "Đã xóa 1 sản phẩm",
    visible = showSnack,
    type = PopupType.SUCCESS,
    actionText = "Hoàn tác",
    onAction = { /* undo logic */ },
    onDismiss = { showSnack = false },
)
```

## 3. Alert Dialog — Thông báo quan trọng

```kotlin
var showAlert by remember { mutableStateOf(false) }

MiniPosAlertDialog(
    visible = showAlert,
    type = PopupType.SUCCESS,
    icon = Icons.Rounded.CheckCircle,
    title = "Thành công!",
    message = "Đơn hàng đã được tạo thành công.",
    confirmText = "Đã hiểu",
    onConfirm = { showAlert = false },
)
```

## 4. Confirm Dialog — Xác nhận hành động

```kotlin
var showConfirm by remember { mutableStateOf(false) }

MiniPosConfirmDialog(
    visible = showConfirm,
    type = PopupType.DELETE,
    icon = Icons.Rounded.Delete,
    title = "Xóa khách hàng?",
    message = "Bạn có chắc muốn xóa Nguyễn Văn A? Không thể hoàn tác.",
    cancelText = "Hủy",
    confirmText = "Xóa",
    confirmStyle = ConfirmButtonStyle.DANGER,
    onCancel = { showConfirm = false },
    onConfirm = { showConfirm = false; /* delete logic */ },
)
```

## 5. Prompt Dialog — Nhập dữ liệu nhanh

```kotlin
var showPrompt by remember { mutableStateOf(false) }
var discountValue by remember { mutableStateOf("") }

MiniPosPromptDialog(
    visible = showPrompt,
    type = PopupType.INFO,
    icon = Icons.Rounded.Edit,
    title = "Nhập giảm giá",
    message = "Nhập phần trăm hoặc số tiền giảm giá",
    inputLabel = "Giảm giá (%)",
    inputPlaceholder = "VD: 10",
    inputHint = "Tối đa 50%",
    inputValue = discountValue,
    keyboardType = KeyboardType.Number,
    onInputChange = { discountValue = it },
    onCancel = { showPrompt = false },
    onConfirm = { value -> showPrompt = false; /* apply discount */ },
)
```

## 6. Stacked Dialog — 3+ buttons xếp dọc

```kotlin
var showStacked by remember { mutableStateOf(false) }

MiniPosStackedDialog(
    visible = showStacked,
    type = PopupType.WARNING,
    icon = Icons.Rounded.Warning,
    title = "Lưu thay đổi?",
    message = "Bạn có thay đổi chưa lưu.",
    buttons = listOf(
        StackedDialogButton("Lưu thay đổi", Icons.Rounded.Save, StackedButtonStyle.PRIMARY) { /* save */ },
        StackedDialogButton("Hủy thay đổi", style = StackedButtonStyle.DANGER) { /* discard */ },
        StackedDialogButton("Tiếp tục chỉnh sửa", style = StackedButtonStyle.CANCEL) { showStacked = false },
    ),
    onDismiss = { showStacked = false },
)
```

## 7. Loading Dialog

```kotlin
var showLoading by remember { mutableStateOf(false) }

MiniPosLoadingDialog(
    visible = showLoading,
    message = "Đang xử lý...",
)
```

## 8. Action Sheet — Menu hành động

```kotlin
var showActions by remember { mutableStateOf(false) }

MiniPosActionSheet(
    visible = showActions,
    title = "Tùy chọn sản phẩm",
    description = "Coca Cola 330ml",
    items = listOf(
        ActionSheetItem("Chỉnh sửa", Icons.Rounded.Edit) { /* edit */ },
        ActionSheetItem("Nhân bản", Icons.Rounded.ContentCopy) { /* clone */ },
        ActionSheetItem("Chia sẻ", Icons.Rounded.Share) { /* share */ },
        ActionSheetItem("Ẩn sản phẩm", Icons.Rounded.VisibilityOff, ActionSheetItemStyle.MUTED) { /* hide */ },
        ActionSheetItem("Xóa sản phẩm", Icons.Rounded.Delete, ActionSheetItemStyle.DANGER) { /* delete */ },
    ),
    onDismiss = { showActions = false },
)
```

## 9. Bottom Sheet — Select List

```kotlin
var showSelect by remember { mutableStateOf(false) }
var selectedCategory by remember { mutableStateOf("cat_1") }

MiniPosSelectSheet(
    visible = showSelect,
    title = "Chọn danh mục",
    selectedId = selectedCategory,
    items = listOf(
        SelectListItem("cat_1", "Đồ uống", "32 sản phẩm", Icons.Rounded.LocalCafe),
        SelectListItem("cat_2", "Thực phẩm", "18 sản phẩm", Icons.Rounded.LunchDining),
    ),
    onSelect = { item -> selectedCategory = item.id; showSelect = false },
    onDismiss = { showSelect = false },
)
```

## 10. Bottom Sheet — Image Picker

```kotlin
var showPicker by remember { mutableStateOf(false) }

MiniPosImagePickerSheet(
    visible = showPicker,
    onPick = { option ->
        showPicker = false
        when (option) {
            ImagePickerOption.CAMERA -> { /* launch camera */ }
            ImagePickerOption.GALLERY -> { /* open gallery */ }
            ImagePickerOption.FILE -> { /* open file picker */ }
            ImagePickerOption.REMOVE -> { /* remove image */ }
        }
    },
    onDismiss = { showPicker = false },
)
```

## 11. Bottom Sheet — Quantity Edit

```kotlin
var showQty by remember { mutableStateOf(false) }

MiniPosQuantitySheet(
    visible = showQty,
    itemName = "Coca Cola 330ml",
    itemSubtitle = "10,000đ",
    unitPrice = 10000,
    currentQuantity = 2,
    formatPrice = { CurrencyFormatter.format(it) },
    onQuantityChange = { /* live update if needed */ },
    onDelete = { showQty = false; /* remove from cart */ },
    onConfirm = { qty -> showQty = false; /* update cart */ },
    onDismiss = { showQty = false },
)
```

## 12. Centralized Popup Host (Optional)

Thay vì quản lý nhiều biến riêng lẻ, dùng `MiniPosPopupState`:

```kotlin
val popupState = remember { MiniPosPopupState() }

// Trigger from anywhere
popupState.showToast("Đã lưu!", PopupType.SUCCESS)
popupState.showSnackbar("Đã xóa sản phẩm", PopupType.ERROR, "Hoàn tác") { /* undo */ }
popupState.showLoading("Đang đồng bộ...")

// Place in screen
Box {
    // ... screen content ...
    MiniPosPopupHost(popupState)
}
```

## 13. Bottom Sheet Form — MiniPosBottomSheet wrapper

```kotlin
var showEditForm by remember { mutableStateOf(false) }
var costPrice by remember { mutableStateOf("6,500") }
var sellPrice by remember { mutableStateOf("10,000") }

MiniPosBottomSheet(
    visible = showEditForm,
    title = "Chỉnh giá bán",
    onDismiss = { showEditForm = false },
    footer = {
        BottomSheetPrimaryButton(
            text = "Lưu thay đổi",
            icon = Icons.Rounded.Check,
            onClick = { showEditForm = false; /* save */ },
        )
    },
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BottomSheetField(
            label = "Giá nhập",
            value = costPrice,
            onValueChange = { costPrice = it },
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f),
        )
        BottomSheetField(
            label = "Giá bán",
            value = sellPrice,
            onValueChange = { sellPrice = it },
            required = true,
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f),
            textColor = AppColors.PrimaryLight,
            fontWeight = FontWeight.Bold,
        )
    }
}
```

## 14. Select Box — Inline Select Field + Bottom Sheet

Thay thế `ExposedDropdownMenuBox` / `DropdownMenu` bằng thiết kế đồng nhất.
Khi tap vào field sẽ mở `MiniPosSelectSheet` bottom sheet.

```kotlin
var selectedSupplierId by remember { mutableStateOf<String?>(null) }

MiniPosSelectBox(
    label = "Nhà cung cấp",
    title = "Chọn nhà cung cấp",
    items = listOf(
        SelectListItem("__none__", "Không chọn", icon = Icons.Filled.Block, iconTint = AppColors.TextTertiary),
        SelectListItem("s1", "Nhà cung cấp A", icon = Icons.Filled.Business, iconTint = AppColors.Primary),
        SelectListItem("s2", "Nhà cung cấp B", icon = Icons.Filled.Business, iconTint = AppColors.Primary),
    ),
    selectedId = selectedSupplierId ?: "__none__",
    placeholder = "Chọn...",
    required = true,
    onSelect = { item ->
        selectedSupplierId = if (item.id == "__none__") null else item.id
    },
)
```

### Compact variant — cho bảng / danh sách chặt

```kotlin
MiniPosSelectBoxCompact(
    title = "Lý do chênh lệch",
    items = listOf(
        SelectListItem("LOSS", "Thất thoát"),
        SelectListItem("DAMAGED", "Hư hỏng"),
        SelectListItem("THEFT", "Trộm cắp"),
    ),
    selectedId = "LOSS",
    onSelect = { item -> /* handle */ },
    height = 40.dp,
)
```
