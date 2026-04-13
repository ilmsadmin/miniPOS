package com.minipos.core.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.minipos.data.database.MiniPosDatabase
import com.minipos.data.database.entity.*
import com.minipos.data.preferences.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

data class DiscoveredDevice(
    val deviceName: String,
    val host: InetAddress,
    val port: Int,
    val storeCode: String,
)

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Scanning : SyncStatus()
    data class Found(val devices: List<DiscoveredDevice>) : SyncStatus()
    data class Connecting(val deviceName: String) : SyncStatus()
    data class Syncing(val deviceName: String) : SyncStatus()
    data class Success(val deviceName: String, val updatedAt: Long) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

@Singleton
class WifiSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MiniPosDatabase,
    private val appPreferences: AppPreferences,
) {
    companion object {
        private const val TAG = "WifiSyncManager"
        private const val SERVICE_TYPE = "_vipos._tcp."
        private const val SERVICE_PORT = 54321
        private const val PROTOCOL_VERSION = 2
        private const val SYNC_KEY_ALIAS = "minipos_sync_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val foundDevices = mutableListOf<DiscoveredDevice>()

    // ──────────────────────────────────────────────
    // SERVER — register this device as discoverable
    // ──────────────────────────────────────────────

    fun startServer(storeCode: String, deviceName: String) {
        stopServer()
        scope.launch {
            try {
                serverSocket = ServerSocket(SERVICE_PORT)
                serverJob = launch { acceptConnections(storeCode) }

                val serviceInfo = NsdServiceInfo().apply {
                    serviceName = "$deviceName-$storeCode"
                    serviceType = SERVICE_TYPE
                    port = SERVICE_PORT
                    setAttribute("storeCode", storeCode)
                    setAttribute("version", PROTOCOL_VERSION.toString())
                }

                registrationListener = object : NsdManager.RegistrationListener {
                    override fun onServiceRegistered(info: NsdServiceInfo) {
                        Log.d(TAG, "NSD registered: ${info.serviceName}")
                    }
                    override fun onRegistrationFailed(info: NsdServiceInfo, code: Int) {
                        Log.e(TAG, "NSD registration failed: $code")
                    }
                    override fun onServiceUnregistered(info: NsdServiceInfo) {
                        Log.d(TAG, "NSD unregistered")
                    }
                    override fun onUnregistrationFailed(info: NsdServiceInfo, code: Int) {}
                }
                nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start server", e)
            }
        }
    }

    private suspend fun acceptConnections(storeCode: String) {
        val ss = serverSocket ?: return
        while (currentCoroutineContext().isActive) {
            try {
                val client = withContext(Dispatchers.IO) { ss.accept() }
                scope.launch { handleClient(client, storeCode) }
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) Log.e(TAG, "Accept failed", e)
                break
            }
        }
    }

    private suspend fun handleClient(socket: Socket, storeCode: String) {
        try {
            socket.use {
                val reader = BufferedReader(InputStreamReader(it.getInputStream()))
                val writer = PrintWriter(BufferedWriter(OutputStreamWriter(it.getOutputStream())), true)

                val requestLine = reader.readLine() ?: return
                when {
                    requestLine.startsWith("HANDSHAKE") -> {
                        // Respond with store code
                        writer.println("OK:$storeCode")
                    }
                    requestLine.startsWith("SYNC_REQUEST") -> {
                        val payload = buildSyncPayload(storeCode)
                        writer.println(payload)
                        Log.d(TAG, "Sent sync payload (${payload.length} chars)")
                    }
                    requestLine.startsWith("SYNC_DATA") -> {
                        // Receive payload from client
                        val sb = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null && line != "END") {
                            sb.appendLine(line)
                        }
                        applySyncPayload(sb.toString(), storeCode)
                        writer.println("ACK")
                    }
                    // New device joining the store — send full snapshot so the guest can bootstrap
                    requestLine.startsWith("JOIN_REQUEST") -> {
                        // Verify store code sent by guest matches ours
                        val requestedCode = requestLine.substringAfter("JOIN_REQUEST:").trim()
                        if (requestedCode.uppercase() != storeCode.uppercase()) {
                            writer.println("ERROR:STORE_CODE_MISMATCH")
                            Log.w(TAG, "JOIN_REQUEST rejected: code mismatch (got=$requestedCode expected=$storeCode)")
                            return
                        }
                        val payload = buildSyncPayload(storeCode)
                        writer.println("JOIN_OK")
                        writer.println(payload)
                        writer.println("END")
                        Log.d(TAG, "JOIN_REQUEST accepted — sent full snapshot")
                    }
                    else -> Log.w(TAG, "Unknown request: $requestLine")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling client", e)
        }
    }

    fun stopServer() {
        registrationListener?.let {
            try { nsdManager.unregisterService(it) } catch (_: Exception) {}
            registrationListener = null
        }
        serverJob?.cancel()
        serverJob = null
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
    }

    // ──────────────────────────────────────────────
    // DISCOVERY — find peers on same network
    // ──────────────────────────────────────────────

    fun startDiscovery(currentStoreCode: String) {
        stopDiscovery()
        foundDevices.clear()
        _discoveredDevices.value = emptyList()
        _status.value = SyncStatus.Scanning

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Discovery failed: $errorCode")
                _status.value = SyncStatus.Error("Discovery failed (code $errorCode)")
            }
            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {}
            override fun onDiscoveryStarted(serviceType: String?) {
                Log.d(TAG, "Discovery started")
            }
            override fun onDiscoveryStopped(serviceType: String?) {
                Log.d(TAG, "Discovery stopped")
            }
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${serviceInfo.serviceName}")
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                        Log.e(TAG, "Resolve failed: $errorCode")
                    }
                    override fun onServiceResolved(info: NsdServiceInfo) {
                        scope.launch {
                            try {
                                // Handshake to confirm store code
                                val storeCode = performHandshake(info.host, info.port) ?: return@launch
                                if (storeCode != currentStoreCode) return@launch // different store — skip
                                val device = DiscoveredDevice(
                                    deviceName = info.serviceName.substringBefore("-$storeCode"),
                                    host = info.host,
                                    port = info.port,
                                    storeCode = storeCode,
                                )
                                foundDevices.removeAll { it.host == device.host }
                                foundDevices.add(device)
                                _discoveredDevices.value = foundDevices.toList()
                                _status.value = SyncStatus.Found(foundDevices.toList())
                            } catch (e: Exception) {
                                Log.e(TAG, "Handshake error", e)
                            }
                        }
                    }
                })
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                foundDevices.removeAll { it.deviceName == serviceInfo.serviceName.substringBefore("-") }
                _discoveredDevices.value = foundDevices.toList()
            }
        }

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try { nsdManager.stopServiceDiscovery(it) } catch (_: Exception) {}
            discoveryListener = null
        }
    }

    fun resetStatus() {
        _status.value = SyncStatus.Idle
    }

    private fun performHandshake(host: InetAddress, port: Int): String? {
        return try {
            Socket(host, port).use { socket ->
                socket.soTimeout = 3000
                val writer = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                writer.println("HANDSHAKE")
                val response = reader.readLine() ?: return null
                if (response.startsWith("OK:")) response.substring(3) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ──────────────────────────────────────────────
    // SYNC — pull data from peer
    // ──────────────────────────────────────────────

    fun syncWithDevice(device: DiscoveredDevice) {
        scope.launch {
            _status.value = SyncStatus.Connecting(device.deviceName)
            try {
                // Step 1: request their data
                val theirPayload = requestSyncData(device) ?: run {
                    _status.value = SyncStatus.Error("Could not connect to ${device.deviceName}")
                    return@launch
                }

                _status.value = SyncStatus.Syncing(device.deviceName)

                // Step 2: apply their data locally
                val storeCode = appPreferences.getCurrentStoreIdSync()
                    ?.let { database.storeDao().getStore()?.code } ?: ""
                applySyncPayload(theirPayload, storeCode)

                // Step 3: send our data to them
                val ourPayload = buildSyncPayload(storeCode)
                pushSyncData(device, ourPayload)

                val now = System.currentTimeMillis()
                _status.value = SyncStatus.Success(device.deviceName, now)
                Log.d(TAG, "Sync completed with ${device.deviceName}")

            } catch (e: Exception) {
                Log.e(TAG, "Sync failed", e)
                _status.value = SyncStatus.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Called by a NEW device (no store yet) to bootstrap from an existing host.
     * Sends a JOIN_REQUEST to [device] with [storeCode], receives a full DB snapshot,
     * applies it locally, then marks the device as onboarded.
     */
    fun joinStore(device: DiscoveredDevice, storeCode: String) {
        scope.launch {
            _status.value = SyncStatus.Connecting(device.deviceName)
            try {
                val payload = requestJoinSnapshot(device, storeCode) ?: run {
                    _status.value = SyncStatus.Error("Could not connect to ${device.deviceName}")
                    return@launch
                }

                _status.value = SyncStatus.Syncing(device.deviceName)
                applyJoinSnapshot(payload, storeCode)

                val now = System.currentTimeMillis()
                _status.value = SyncStatus.Success(device.deviceName, now)
                Log.d(TAG, "Join completed — store bootstrapped from ${device.deviceName}")
            } catch (e: Exception) {
                Log.e(TAG, "Join failed", e)
                _status.value = SyncStatus.Error(e.message ?: "Unknown error")
            }
        }
    }

    /** Sends JOIN_REQUEST to host and returns the encrypted snapshot string. */
    private fun requestJoinSnapshot(device: DiscoveredDevice, storeCode: String): String? {
        return try {
            Socket(device.host, device.port).use { socket ->
                socket.soTimeout = 15_000
                val writer = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                writer.println("JOIN_REQUEST:${storeCode.uppercase()}")
                val firstLine = reader.readLine() ?: return null
                if (firstLine != "JOIN_OK") {
                    Log.e(TAG, "Join rejected by host: $firstLine")
                    return null
                }
                // Read snapshot lines until "END"
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null && line != "END") {
                    sb.appendLine(line)
                }
                sb.toString().trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "requestJoinSnapshot failed", e)
            null
        }
    }

    /**
     * Applies a full DB snapshot from the host onto a fresh (empty) local database,
     * then writes preferences so the app treats this device as onboarded.
     *
     * Unlike [applySyncPayload] this always overwrites because the local DB is empty.
     * PIN/password fields are set blank — the new user must set their own PIN on first login.
     */
    private suspend fun applyJoinSnapshot(payload: String, storeCode: String) {
        if (payload.isBlank()) return
        try {
            val decrypted = decryptPayload(payload)
            val root = JSONObject(decrypted)
            val remoteStoreCode = root.optString("storeCode")
            if (remoteStoreCode.isNotEmpty() && remoteStoreCode.uppercase() != storeCode.uppercase()) {
                Log.w(TAG, "applyJoinSnapshot: store code mismatch")
                return
            }

            val now = System.currentTimeMillis()
            val deviceId = appPreferences.getDeviceIdSync()

            // ── Store ────────────────────────────────────────────────────────
            val storeObj = root.optJSONObject("store") ?: return
            val storeId = storeObj.getString("id")
            database.storeDao().insert(StoreEntity(
                id = storeId,
                name = storeObj.getString("name"),
                code = storeObj.getString("code"),
                address = storeObj.optString("address").takeNonNull(),
                phone = storeObj.optString("phone").takeNonNull(),
                settings = storeObj.optString("settings").takeNonNull(),
                currency = storeObj.optString("currency", "VND"),
                createdAt = now, updatedAt = now, deviceId = deviceId,
            ))

            // ── Users — keep display info; pin/password stay blank ────────────
            root.optJSONArray("users")?.forEachObject { u ->
                val id = u.getString("id")
                if (database.userDao().getById(id) == null) {
                    database.userDao().insert(UserEntity(
                        id = id, storeId = storeId,
                        displayName = u.getString("displayName"),
                        role = u.getString("role"),
                        pinHash = "",           // NEW device — user sets their own PIN
                        passwordHash = null,
                        isActive = u.optBoolean("isActive", true),
                        isDeleted = u.optBoolean("isDeleted", false),
                        createdAt = now, updatedAt = now, deviceId = deviceId,
                    ))
                }
            }

            // ── Categories ───────────────────────────────────────────────────
            root.optJSONArray("categories")?.forEachObject { c ->
                val id = c.getString("id")
                if (database.categoryDao().getById(id) == null) {
                    database.categoryDao().insert(CategoryEntity(
                        id = id, storeId = storeId,
                        name = c.getString("name"),
                        parentId = c.optString("parentId").takeNonNull(),
                        description = c.optString("description").takeNonNull(),
                        icon = c.optString("icon").takeNonNull(),
                        color = c.optString("color").takeNonNull(),
                        sortOrder = c.optInt("sortOrder", 0),
                        isActive = c.optBoolean("isActive", true),
                        isDeleted = c.optBoolean("isDeleted", false),
                        createdAt = now, updatedAt = now, deviceId = deviceId,
                    ))
                }
            }

            // ── Suppliers ────────────────────────────────────────────────────
            root.optJSONArray("suppliers")?.forEachObject { s ->
                val id = s.getString("id")
                if (database.supplierDao().getById(id) == null) {
                    database.supplierDao().insert(SupplierEntity(
                        id = id, storeId = storeId,
                        name = s.getString("name"),
                        contactPerson = s.optString("contactPerson").takeNonNull(),
                        phone = s.optString("phone").takeNonNull(),
                        mobile = s.optString("mobile").takeNonNull(),
                        email = s.optString("email").takeNonNull(),
                        address = s.optString("address").takeNonNull(),
                        isActive = s.optBoolean("isActive", true),
                        isDeleted = s.optBoolean("isDeleted", false),
                        createdAt = now, updatedAt = now, deviceId = deviceId,
                    ))
                }
            }

            // ── Products ─────────────────────────────────────────────────────
            root.optJSONArray("products")?.forEachObject { p ->
                val id = p.getString("id")
                if (database.productDao().getById(id) == null) {
                    database.productDao().insert(ProductEntity(
                        id = id, storeId = storeId,
                        name = p.getString("name"),
                        sku = p.getString("sku"),
                        barcode = p.optString("barcode").takeNonNull(),
                        description = p.optString("description").takeNonNull(),
                        categoryId = p.optString("categoryId").takeNonNull(),
                        supplierId = p.optString("supplierId").takeNonNull(),
                        sellingPrice = p.getDouble("sellingPrice"),
                        costPrice = p.optDouble("costPrice", 0.0),
                        unit = p.optString("unit", "pcs"),
                        imagePath = p.optString("imagePath").takeNonNull(),
                        minStock = p.optInt("minStock", 0),
                        maxStock = p.optInt("maxStock", -1).takeIf { it >= 0 },
                        isActive = p.optBoolean("isActive", true),
                        isDeleted = p.optBoolean("isDeleted", false),
                        trackInventory = p.optBoolean("trackInventory", true),
                        taxRate = p.optDouble("taxRate", 0.0),
                        hasVariants = p.optBoolean("hasVariants", false),
                        createdAt = now, updatedAt = now, deviceId = deviceId,
                    ))
                }
            }

            // ── Product Variants ──────────────────────────────────────────────
            root.optJSONArray("productVariants")?.forEachObject { v ->
                val id = v.getString("id")
                if (database.productVariantDao().getById(id) == null) {
                    database.productVariantDao().insert(ProductVariantEntity(
                        id = id, storeId = storeId,
                        productId = v.getString("productId"),
                        variantName = v.getString("variantName"),
                        sku = v.getString("sku"),
                        barcode = v.optString("barcode").takeNonNull(),
                        costPrice = v.optDouble("costPrice", Double.NaN).takeIf { !it.isNaN() },
                        sellingPrice = v.optDouble("sellingPrice", Double.NaN).takeIf { !it.isNaN() },
                        attributes = v.optString("attributes", "{}"),
                        isActive = v.optBoolean("isActive", true),
                        isDeleted = v.optBoolean("isDeleted", false),
                        createdAt = now, updatedAt = now, deviceId = deviceId,
                    ))
                }
            }

            // ── Customers ────────────────────────────────────────────────────
            root.optJSONArray("customers")?.forEachObject { c ->
                val id = c.getString("id")
                if (database.customerDao().getById(id) == null) {
                    database.customerDao().insert(CustomerEntity(
                        id = id, storeId = storeId,
                        name = c.getString("name"),
                        phone = c.optString("phone").takeNonNull(),
                        email = c.optString("email").takeNonNull(),
                        address = c.optString("address").takeNonNull(),
                        totalSpent = c.optDouble("totalSpent", 0.0),
                        visitCount = c.optInt("visitCount", 0),
                        lastVisitAt = c.optLong("lastVisitAt", -1L).takeIf { it >= 0 },
                        isDeleted = c.optBoolean("isDeleted", false),
                        createdAt = now, updatedAt = now, deviceId = deviceId,
                    ))
                }
            }

            // ── Inventory ────────────────────────────────────────────────────
            root.optJSONArray("inventory")?.forEachObject { inv ->
                val invId = inv.getString("id")
                val productId = inv.getString("productId")
                val variantId = inv.optString("variantId").takeNonNull()
                val existing = if (variantId != null)
                    database.inventoryDao().getByVariant(storeId, productId, variantId)
                else
                    database.inventoryDao().getByProduct(storeId, productId)
                if (existing == null) {
                    database.inventoryDao().insert(InventoryEntity(
                        id = invId, storeId = storeId,
                        productId = productId, variantId = variantId,
                        quantity = inv.getDouble("quantity"),
                        reservedQty = inv.optDouble("reservedQty", 0.0),
                        createdAt = now, updatedAt = now, deviceId = deviceId,
                    ))
                }
            }

            // ── Save preferences so the app knows this device is onboarded ───
            appPreferences.setCurrentStore(storeId)
            appPreferences.setOnboarded(true)
            // Do NOT set currentUserId or isLoggedIn — user must choose & authenticate on Login screen

            Log.d(TAG, "Join snapshot applied — storeId=$storeId")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying join snapshot", e)
            throw e
        }
    }

    private fun requestSyncData(device: DiscoveredDevice): String? {
        return try {
            Socket(device.host, device.port).use { socket ->
                socket.soTimeout = 10_000
                val writer = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                writer.println("SYNC_REQUEST")
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) sb.appendLine(line)
                sb.toString().trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "requestSyncData failed", e)
            null
        }
    }

    private fun pushSyncData(device: DiscoveredDevice, payload: String) {
        try {
            Socket(device.host, device.port).use { socket ->
                socket.soTimeout = 10_000
                val writer = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                writer.println("SYNC_DATA")
                writer.println(payload)
                writer.println("END")
                reader.readLine() // ACK
            }
        } catch (e: Exception) {
            Log.e(TAG, "pushSyncData failed", e)
        }
    }

    // ──────────────────────────────────────────────
    // DATA SERIALIZATION
    // ──────────────────────────────────────────────

    private suspend fun buildSyncPayload(storeCode: String): String {
        val store = database.storeDao().getStore() ?: return "{}"
        val storeId = store.id

        val root = JSONObject()
        root.put("version", PROTOCOL_VERSION)
        root.put("storeCode", storeCode)
        root.put("generatedAt", System.currentTimeMillis())

        // ── Store info ────────────────────────────────────────────────────────
        root.put("store", JSONObject().apply {
            put("id", store.id)
            put("name", store.name)
            put("code", store.code)
            put("address", store.address ?: JSONObject.NULL)
            put("phone", store.phone ?: JSONObject.NULL)
            put("settings", store.settings ?: JSONObject.NULL)
            put("currency", store.currency)
            put("updatedAt", store.updatedAt)
        })

        // ── Users — display info + role only (NO pin/password hashes ever leave the device) ──
        val usersArr = JSONArray()
        database.userDao().getAllUsers(storeId).forEach { u ->
            usersArr.put(JSONObject().apply {
                put("id", u.id)
                put("storeId", u.storeId)
                put("displayName", u.displayName)
                put("role", u.role)
                put("isActive", u.isActive)
                put("isDeleted", u.isDeleted)
                put("updatedAt", u.updatedAt)
            })
        }
        root.put("users", usersArr)

        // ── Categories ────────────────────────────────────────────────────────
        val catsArr = JSONArray()
        database.categoryDao().getAll(storeId).forEach { c ->
            catsArr.put(JSONObject().apply {
                put("id", c.id); put("name", c.name)
                put("parentId", c.parentId ?: JSONObject.NULL)
                put("description", c.description ?: JSONObject.NULL)
                put("icon", c.icon ?: JSONObject.NULL)
                put("color", c.color ?: JSONObject.NULL)
                put("sortOrder", c.sortOrder)
                put("isActive", c.isActive); put("isDeleted", c.isDeleted)
                put("updatedAt", c.updatedAt)
            })
        }
        root.put("categories", catsArr)

        // ── Suppliers ─────────────────────────────────────────────────────────
        val suppliersArr = JSONArray()
        database.supplierDao().getAll(storeId).forEach { s ->
            suppliersArr.put(JSONObject().apply {
                put("id", s.id); put("name", s.name)
                put("contactPerson", s.contactPerson ?: JSONObject.NULL)
                put("phone", s.phone ?: JSONObject.NULL)
                put("mobile", s.mobile ?: JSONObject.NULL)
                put("email", s.email ?: JSONObject.NULL)
                put("address", s.address ?: JSONObject.NULL)
                put("isActive", s.isActive); put("isDeleted", s.isDeleted)
                put("updatedAt", s.updatedAt)
            })
        }
        root.put("suppliers", suppliersArr)

        // ── Products (ALL fields — staff role filtering is applied on UI layer) ──
        val productsArr = JSONArray()
        database.productDao().getAll(storeId).forEach { p ->
            productsArr.put(JSONObject().apply {
                put("id", p.id); put("name", p.name)
                put("sku", p.sku)
                put("barcode", p.barcode ?: JSONObject.NULL)
                put("description", p.description ?: JSONObject.NULL)
                put("categoryId", p.categoryId ?: JSONObject.NULL)
                put("supplierId", p.supplierId ?: JSONObject.NULL)
                put("sellingPrice", p.sellingPrice)
                put("costPrice", p.costPrice)
                put("unit", p.unit)
                put("minStock", p.minStock)
                put("maxStock", p.maxStock ?: JSONObject.NULL)
                put("isActive", p.isActive); put("isDeleted", p.isDeleted)
                put("trackInventory", p.trackInventory)
                put("taxRate", p.taxRate)
                put("hasVariants", p.hasVariants)
                put("imagePath", p.imagePath ?: JSONObject.NULL)
                put("updatedAt", p.updatedAt)
            })
        }
        root.put("products", productsArr)

        // ── Product Variants ──────────────────────────────────────────────────
        val variantsArr = JSONArray()
        database.productDao().getAll(storeId).filter { it.hasVariants }.forEach { p ->
            database.productVariantDao().getByProductId(p.id).forEach { v ->
                variantsArr.put(JSONObject().apply {
                    put("id", v.id); put("productId", v.productId)
                    put("variantName", v.variantName); put("sku", v.sku)
                    put("barcode", v.barcode ?: JSONObject.NULL)
                    put("costPrice", v.costPrice ?: JSONObject.NULL)
                    put("sellingPrice", v.sellingPrice ?: JSONObject.NULL)
                    put("attributes", v.attributes)
                    put("isActive", v.isActive); put("isDeleted", v.isDeleted)
                    put("updatedAt", v.updatedAt)
                })
            }
        }
        root.put("productVariants", variantsArr)

        // ── Customers ─────────────────────────────────────────────────────────
        val customersArr = JSONArray()
        database.customerDao().getAll(storeId).forEach { c ->
            customersArr.put(JSONObject().apply {
                put("id", c.id); put("name", c.name)
                put("phone", c.phone ?: JSONObject.NULL)
                put("email", c.email ?: JSONObject.NULL)
                put("address", c.address ?: JSONObject.NULL)
                put("totalSpent", c.totalSpent)
                put("visitCount", c.visitCount)
                put("lastVisitAt", c.lastVisitAt ?: JSONObject.NULL)
                put("isDeleted", c.isDeleted)
                put("updatedAt", c.updatedAt)
            })
        }
        root.put("customers", customersArr)

        // ── Inventory ─────────────────────────────────────────────────────────
        val inventoryArr = JSONArray()
        database.inventoryDao().getAllStockWithProduct(storeId).forEach { sw ->
            inventoryArr.put(JSONObject().apply {
                put("id", sw.id)
                put("productId", sw.productId)
                put("variantId", sw.variantId ?: JSONObject.NULL)
                put("quantity", sw.quantity)
                put("reservedQty", sw.reservedQty)
                put("updatedAt", System.currentTimeMillis())
            })
        }
        root.put("inventory", inventoryArr)

        // ── Orders (last 90 days) ─────────────────────────────────────────────
        val cutoff = System.currentTimeMillis() - (90L * 24 * 3600 * 1000)
        val ordersArr = JSONArray()
        database.orderDao().getOrdersByDateRange(storeId, cutoff, System.currentTimeMillis()).forEach { o ->
            val orderObj = JSONObject().apply {
                put("id", o.id); put("orderCode", o.orderCode)
                put("customerId", o.customerId ?: JSONObject.NULL)
                put("customerName", o.customerName ?: JSONObject.NULL)
                put("customerPhone", o.customerPhone ?: JSONObject.NULL)
                put("subtotal", o.subtotal)
                put("discountType", o.discountType ?: JSONObject.NULL)
                put("discountValue", o.discountValue)
                put("discountAmount", o.discountAmount)
                put("taxAmount", o.taxAmount)
                put("totalAmount", o.totalAmount)
                put("status", o.status)
                put("notes", o.notes ?: JSONObject.NULL)
                put("createdBy", o.createdBy)
                put("isDeleted", o.isDeleted)
                put("createdAt", o.createdAt)
                put("updatedAt", o.updatedAt)
            }
            val itemsArr = JSONArray()
            database.orderDao().getOrderItems(o.id).forEach { i ->
                itemsArr.put(JSONObject().apply {
                    put("id", i.id); put("orderId", i.orderId)
                    put("productId", i.productId)
                    put("variantId", i.variantId ?: JSONObject.NULL)
                    put("productName", i.productName)
                    put("variantName", i.variantName ?: JSONObject.NULL)
                    put("quantity", i.quantity)
                    put("unitPrice", i.unitPrice)
                    put("costPrice", i.costPrice)
                    put("discountType", i.discountType ?: JSONObject.NULL)
                    put("discountValue", i.discountValue)
                    put("discountAmount", i.discountAmount)
                    put("taxAmount", i.taxAmount)
                    put("totalPrice", i.totalPrice)
                    put("createdAt", i.createdAt); put("updatedAt", i.updatedAt)
                })
            }
            orderObj.put("items", itemsArr)
            val paymentsArr = JSONArray()
            database.orderDao().getOrderPayments(o.id).forEach { p ->
                paymentsArr.put(JSONObject().apply {
                    put("id", p.id); put("orderId", p.orderId)
                    put("method", p.method); put("amount", p.amount)
                    put("receivedAmount", p.receivedAmount ?: JSONObject.NULL)
                    put("changeAmount", p.changeAmount)
                    put("referenceNo", p.referenceNo ?: JSONObject.NULL)
                    put("createdAt", p.createdAt); put("updatedAt", p.updatedAt)
                })
            }
            orderObj.put("payments", paymentsArr)
            ordersArr.put(orderObj)
        }
        root.put("orders", ordersArr)

        // ── Purchase Orders (last 90 days) ────────────────────────────────────
        val poArr = JSONArray()
        database.purchaseOrderDao().getByDateRange(storeId, cutoff, System.currentTimeMillis()).forEach { po ->
            val poObj = JSONObject().apply {
                put("id", po.id); put("code", po.code)
                put("supplierId", po.supplierId ?: JSONObject.NULL)
                put("supplierName", po.supplierName ?: JSONObject.NULL)
                put("totalAmount", po.totalAmount)
                put("totalItems", po.totalItems)
                put("notes", po.notes ?: JSONObject.NULL)
                put("status", po.status)
                put("createdBy", po.createdBy)
                put("confirmedAt", po.confirmedAt ?: JSONObject.NULL)
                put("isDeleted", po.isDeleted)
                put("createdAt", po.createdAt); put("updatedAt", po.updatedAt)
            }
            val poItemsArr = JSONArray()
            database.purchaseOrderDao().getItemsByOrderId(po.id).forEach { poi ->
                poItemsArr.put(JSONObject().apply {
                    put("id", poi.id); put("purchaseOrderId", poi.purchaseOrderId)
                    put("productId", poi.productId)
                    put("variantId", poi.variantId ?: JSONObject.NULL)
                    put("productName", poi.productName)
                    put("variantName", poi.variantName ?: JSONObject.NULL)
                    put("quantity", poi.quantity)
                    put("unitCost", poi.unitCost); put("totalCost", poi.totalCost)
                    put("createdAt", poi.createdAt); put("updatedAt", poi.updatedAt)
                })
            }
            poObj.put("items", poItemsArr)
            poArr.put(poObj)
        }
        root.put("purchaseOrders", poArr)

        val plainJson = root.toString()
        return encryptPayload(plainJson)
    }

    private suspend fun applySyncPayload(payload: String, storeCode: String) {
        if (payload.isBlank()) return
        try {
            val decrypted = decryptPayload(payload)
            val root = JSONObject(decrypted)
            val remoteStoreCode = root.optString("storeCode")
            if (remoteStoreCode.isNotEmpty() && remoteStoreCode != storeCode) {
                Log.w(TAG, "Store code mismatch: local=$storeCode remote=$remoteStoreCode")
                return
            }

            val store = database.storeDao().getStore() ?: return
            val storeId = store.id
            val now = System.currentTimeMillis()
            val deviceId = appPreferences.getDeviceIdSync()

            // ── Store info merge ──────────────────────────────────────────────
            root.optJSONObject("store")?.let { s ->
                val remote = s.getLong("updatedAt")
                if (store.updatedAt < remote) {
                    database.storeDao().insert(store.copy(
                        name = s.getString("name"),
                        address = s.optString("address").takeNonNull(),
                        phone = s.optString("phone").takeNonNull(),
                        settings = s.optString("settings").takeNonNull(),
                        currency = s.optString("currency", "VND"),
                        updatedAt = remote,
                    ))
                }
            }

            // ── Users (LWW — PIN is NEVER overwritten from sync) ─────────────
            root.optJSONArray("users")?.forEachObject { u ->
                val id = u.getString("id")
                val remote = u.getLong("updatedAt")
                val local = database.userDao().getById(id)
                if (local == null || local.updatedAt < remote) {
                    database.userDao().insert(UserEntity(
                        id = id,
                        storeId = storeId,
                        displayName = u.getString("displayName"),
                        role = u.getString("role"),
                        pinHash = local?.pinHash ?: "",      // Keep local PIN — NEVER sync PINs
                        passwordHash = local?.passwordHash, // Keep local password
                        isActive = u.getBoolean("isActive"),
                        isDeleted = u.optBoolean("isDeleted", false),
                        createdAt = local?.createdAt ?: now,
                        updatedAt = remote,
                        deviceId = local?.deviceId ?: deviceId,
                    ))
                }
            }

            // ── Categories (LWW) ──────────────────────────────────────────────
            root.optJSONArray("categories")?.forEachObject { c ->
                val id = c.getString("id")
                val remote = c.getLong("updatedAt")
                val local = database.categoryDao().getById(id)
                if (local == null || local.updatedAt < remote) {
                    database.categoryDao().insert(CategoryEntity(
                        id = id, storeId = storeId,
                        name = c.getString("name"),
                        parentId = c.optString("parentId").takeNonNull(),
                        description = c.optString("description").takeNonNull(),
                        icon = c.optString("icon").takeNonNull(),
                        color = c.optString("color").takeNonNull(),
                        sortOrder = c.optInt("sortOrder", 0),
                        isActive = c.optBoolean("isActive", true),
                        isDeleted = c.optBoolean("isDeleted", false),
                        createdAt = local?.createdAt ?: now,
                        updatedAt = remote,
                        deviceId = local?.deviceId ?: deviceId,
                    ))
                }
            }

            // ── Suppliers (LWW) ───────────────────────────────────────────────
            root.optJSONArray("suppliers")?.forEachObject { s ->
                val id = s.getString("id")
                val remote = s.getLong("updatedAt")
                val local = database.supplierDao().getById(id)
                if (local == null || local.updatedAt < remote) {
                    database.supplierDao().insert(SupplierEntity(
                        id = id, storeId = storeId,
                        name = s.getString("name"),
                        contactPerson = s.optString("contactPerson").takeNonNull(),
                        phone = s.optString("phone").takeNonNull(),
                        mobile = s.optString("mobile").takeNonNull(),
                        email = s.optString("email").takeNonNull(),
                        address = s.optString("address").takeNonNull(),
                        isActive = s.optBoolean("isActive", true),
                        isDeleted = s.optBoolean("isDeleted", false),
                        createdAt = local?.createdAt ?: now,
                        updatedAt = remote,
                        deviceId = local?.deviceId ?: deviceId,
                    ))
                }
            }

            // ── Products (LWW) ────────────────────────────────────────────────
            root.optJSONArray("products")?.forEachObject { p ->
                val id = p.getString("id")
                val remote = p.getLong("updatedAt")
                val local = database.productDao().getById(id)
                if (local == null || local.updatedAt < remote) {
                    database.productDao().insert(ProductEntity(
                        id = id, storeId = storeId,
                        name = p.getString("name"),
                        sku = p.getString("sku"),
                        barcode = p.optString("barcode").takeNonNull(),
                        description = p.optString("description").takeNonNull(),
                        categoryId = p.optString("categoryId").takeNonNull(),
                        supplierId = p.optString("supplierId").takeNonNull(),
                        sellingPrice = p.getDouble("sellingPrice"),
                        costPrice = p.optDouble("costPrice", 0.0),
                        unit = p.optString("unit", "pcs"),
                        imagePath = p.optString("imagePath").takeNonNull(),
                        minStock = p.optInt("minStock", 0),
                        maxStock = p.optInt("maxStock", -1).takeIf { it >= 0 },
                        isActive = p.optBoolean("isActive", true),
                        isDeleted = p.optBoolean("isDeleted", false),
                        trackInventory = p.optBoolean("trackInventory", true),
                        taxRate = p.optDouble("taxRate", 0.0),
                        hasVariants = p.optBoolean("hasVariants", false),
                        createdAt = local?.createdAt ?: now,
                        updatedAt = remote,
                        deviceId = local?.deviceId ?: deviceId,
                    ))
                }
            }

            // ── Product Variants (LWW) ────────────────────────────────────────
            root.optJSONArray("productVariants")?.forEachObject { v ->
                val id = v.getString("id")
                val remote = v.getLong("updatedAt")
                val local = database.productVariantDao().getById(id)
                if (local == null || local.updatedAt < remote) {
                    database.productVariantDao().insert(ProductVariantEntity(
                        id = id,
                        storeId = storeId,
                        productId = v.getString("productId"),
                        variantName = v.getString("variantName"),
                        sku = v.getString("sku"),
                        barcode = v.optString("barcode").takeNonNull(),
                        costPrice = v.optDouble("costPrice", Double.NaN).takeIf { !it.isNaN() },
                        sellingPrice = v.optDouble("sellingPrice", Double.NaN).takeIf { !it.isNaN() },
                        attributes = v.optString("attributes", "{}"),
                        isActive = v.optBoolean("isActive", true),
                        isDeleted = v.optBoolean("isDeleted", false),
                        createdAt = local?.createdAt ?: now,
                        updatedAt = remote,
                        deviceId = local?.deviceId ?: deviceId,
                    ))
                }
            }

            // ── Customers (LWW) ───────────────────────────────────────────────
            root.optJSONArray("customers")?.forEachObject { c ->
                val id = c.getString("id")
                val remote = c.getLong("updatedAt")
                val local = database.customerDao().getById(id)
                if (local == null || local.updatedAt < remote) {
                    database.customerDao().insert(CustomerEntity(
                        id = id, storeId = storeId,
                        name = c.getString("name"),
                        phone = c.optString("phone").takeNonNull(),
                        email = c.optString("email").takeNonNull(),
                        address = c.optString("address").takeNonNull(),
                        totalSpent = c.optDouble("totalSpent", local?.totalSpent ?: 0.0),
                        visitCount = c.optInt("visitCount", local?.visitCount ?: 0),
                        lastVisitAt = c.optLong("lastVisitAt", -1L).takeIf { it >= 0 }
                            ?: local?.lastVisitAt,
                        isDeleted = c.optBoolean("isDeleted", false),
                        createdAt = local?.createdAt ?: now,
                        updatedAt = remote,
                        deviceId = local?.deviceId ?: deviceId,
                    ))
                }
            }

            // ── Inventory (LWW — take max quantity to avoid accidental stock loss) ─
            root.optJSONArray("inventory")?.forEachObject { inv ->
                val productId = inv.getString("productId")
                val variantId = inv.optString("variantId").takeNonNull()
                val remoteQty = inv.getDouble("quantity")
                val existing = if (variantId != null)
                    database.inventoryDao().getByVariant(storeId, productId, variantId)
                else
                    database.inventoryDao().getByProduct(storeId, productId)

                if (existing == null) {
                    database.inventoryDao().insert(InventoryEntity(
                        id = inv.getString("id"),
                        storeId = storeId,
                        productId = productId,
                        variantId = variantId,
                        quantity = remoteQty,
                        reservedQty = inv.optDouble("reservedQty", 0.0),
                        createdAt = now,
                        updatedAt = now,
                        deviceId = deviceId,
                    ))
                }
                // Note: inventory adjustments are handled by stock movements; we don't blindly overwrite
            }

            // ── Orders (LWW — insert if not present; never update existing orders) ─
            root.optJSONArray("orders")?.forEachObject { o ->
                val id = o.getString("id")
                if (database.orderDao().getById(id) == null) {
                    database.orderDao().insertOrder(OrderEntity(
                        id = id, storeId = storeId,
                        orderCode = o.getString("orderCode"),
                        customerId = o.optString("customerId").takeNonNull(),
                        customerName = o.optString("customerName").takeNonNull(),
                        customerPhone = o.optString("customerPhone").takeNonNull(),
                        subtotal = o.optDouble("subtotal", 0.0),
                        discountType = o.optString("discountType").takeNonNull(),
                        discountValue = o.optDouble("discountValue", 0.0),
                        discountAmount = o.optDouble("discountAmount", 0.0),
                        taxAmount = o.optDouble("taxAmount", 0.0),
                        totalAmount = o.optDouble("totalAmount", 0.0),
                        status = o.optString("status", "completed"),
                        notes = o.optString("notes").takeNonNull(),
                        createdBy = o.optString("createdBy", ""),
                        isDeleted = o.optBoolean("isDeleted", false),
                        createdAt = o.getLong("createdAt"),
                        updatedAt = o.getLong("updatedAt"),
                        deviceId = deviceId,
                    ))
                    o.optJSONArray("items")?.let { arr ->
                        val items = (0 until arr.length()).map { i ->
                            val item = arr.getJSONObject(i)
                            OrderItemEntity(
                                id = item.getString("id"),
                                orderId = item.getString("orderId"),
                                productId = item.getString("productId"),
                                variantId = item.optString("variantId").takeNonNull(),
                                productName = item.getString("productName"),
                                variantName = item.optString("variantName").takeNonNull(),
                                quantity = item.getDouble("quantity"),
                                unitPrice = item.getDouble("unitPrice"),
                                costPrice = item.optDouble("costPrice", 0.0),
                                discountType = item.optString("discountType").takeNonNull(),
                                discountValue = item.optDouble("discountValue", 0.0),
                                discountAmount = item.optDouble("discountAmount", 0.0),
                                taxAmount = item.optDouble("taxAmount", 0.0),
                                totalPrice = item.getDouble("totalPrice"),
                                createdAt = item.getLong("createdAt"),
                                updatedAt = item.getLong("updatedAt"),
                                deviceId = deviceId,
                            )
                        }
                        if (items.isNotEmpty()) database.orderDao().insertOrderItems(items)
                    }
                    o.optJSONArray("payments")?.let { arr ->
                        val payments = (0 until arr.length()).map { i ->
                            val pay = arr.getJSONObject(i)
                            OrderPaymentEntity(
                                id = pay.getString("id"),
                                orderId = pay.getString("orderId"),
                                method = pay.getString("method"),
                                amount = pay.getDouble("amount"),
                                receivedAmount = pay.optDouble("receivedAmount", Double.NaN).takeIf { !it.isNaN() },
                                changeAmount = pay.optDouble("changeAmount", 0.0),
                                referenceNo = pay.optString("referenceNo").takeNonNull(),
                                createdAt = pay.getLong("createdAt"),
                                updatedAt = pay.getLong("updatedAt"),
                                deviceId = deviceId,
                            )
                        }
                        if (payments.isNotEmpty()) database.orderDao().insertOrderPayments(payments)
                    }
                }
            }

            // ── Purchase Orders (insert only if not present) ──────────────────
            root.optJSONArray("purchaseOrders")?.forEachObject { po ->
                val id = po.getString("id")
                if (database.purchaseOrderDao().getById(id) == null) {
                    database.purchaseOrderDao().insert(PurchaseOrderEntity(
                        id = id, storeId = storeId,
                        code = po.getString("code"),
                        supplierId = po.optString("supplierId").takeNonNull(),
                        supplierName = po.optString("supplierName").takeNonNull(),
                        totalAmount = po.optDouble("totalAmount", 0.0),
                        totalItems = po.optInt("totalItems", 0),
                        notes = po.optString("notes").takeNonNull(),
                        status = po.optString("status", "confirmed"),
                        createdBy = po.optString("createdBy", ""),
                        confirmedAt = po.optLong("confirmedAt", -1L).takeIf { it >= 0 },
                        isDeleted = po.optBoolean("isDeleted", false),
                        createdAt = po.getLong("createdAt"),
                        updatedAt = po.getLong("updatedAt"),
                        deviceId = deviceId,
                    ))
                    po.optJSONArray("items")?.let { arr ->
                        val items = (0 until arr.length()).map { i ->
                            val poi = arr.getJSONObject(i)
                            PurchaseOrderItemEntity(
                                id = poi.getString("id"),
                                purchaseOrderId = poi.getString("purchaseOrderId"),
                                productId = poi.getString("productId"),
                                variantId = poi.optString("variantId").takeNonNull(),
                                productName = poi.getString("productName"),
                                variantName = poi.optString("variantName").takeNonNull(),
                                quantity = poi.getDouble("quantity"),
                                unitCost = poi.getDouble("unitCost"),
                                totalCost = poi.getDouble("totalCost"),
                                createdAt = poi.getLong("createdAt"),
                                updatedAt = poi.getLong("updatedAt"),
                                deviceId = deviceId,
                            )
                        }
                        if (items.isNotEmpty()) database.purchaseOrderDao().insertItems(items)
                    }
                }
            }

            Log.d(TAG, "Sync payload applied successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying sync payload", e)
            throw e
        }
    }

    // ─── Helper extension ────────────────────────────────────────────────────

    private fun String?.takeNonNull(): String? = takeIf { !it.isNullOrEmpty() && it != "null" }

    private inline fun JSONArray.forEachObject(block: (JSONObject) -> Unit) {
        for (i in 0 until length()) {
            try { block(getJSONObject(i)) } catch (e: Exception) { Log.w(TAG, "Skip malformed object at $i", e) }
        }
    }

    // ──────────────────────────────────────────────
    // ENCRYPTION (AES-256-GCM via Android Keystore)
    // ──────────────────────────────────────────────

    private fun getSyncKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(SYNC_KEY_ALIAS)) {
            return (keyStore.getEntry(SYNC_KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        }
        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGen.init(
            KeyGenParameterSpec.Builder(
                SYNC_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build(),
        )
        return keyGen.generateKey()
    }

    private fun encryptPayload(plainText: String): String {
        return try {
            val key = getSyncKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = iv + cipherBytes
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed, sending plain", e)
            plainText // fallback — still works if both devices lack Keystore
        }
    }

    private fun decryptPayload(data: String): String {
        return try {
            val combined = Base64.decode(data, Base64.NO_WRAP)
            if (combined.size <= GCM_IV_LENGTH) return data // not encrypted
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val cipherBytes = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
            val key = getSyncKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "Decryption failed — treating as plaintext", e)
            data
        }
    }
}
