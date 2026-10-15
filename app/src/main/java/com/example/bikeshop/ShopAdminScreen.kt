package com.example.bikeshop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ShopAdminScreen(modifier: Modifier, onCatalogChanged: () -> Unit) {
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf<ShopAdminSession?>(null) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("تسجيل دخول المالك مطلوب. الحساب يجب أن يكون معتمدًا في Supabase.") }
    var busy by remember { mutableStateOf(false) }
    var products by remember { mutableStateOf<List<AdminProduct>>(emptyList()) }
    var editingId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var image by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("parts") }
    var bikeType by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(true) }

    fun loadProducts(current: ShopAdminSession) {
        scope.launch {
            busy = true
            try {
                products = withContext(Dispatchers.IO) { ShopAdminApi.listProducts(current) }
                message = "عدد المنتجات: ${products.size}"
            } catch (e: Exception) {
                message = e.message ?: "تعذر تحميل المنتجات"
            } finally { busy = false }
        }
    }

    Column(modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(message, color = Color(0xFF1677E8), fontSize = 13.sp)
        if (session == null) {
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("البريد الإلكتروني للمالك") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("كلمة المرور") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        try {
                            val signedIn = withContext(Dispatchers.IO) { ShopAdminApi.signIn(email, password) }
                            session = signedIn
                            password = ""
                            message = "تم التحقق من صلاحية المالك"
                            loadProducts(signedIn)
                        } catch (e: Exception) {
                            message = e.message ?: "فشل تسجيل الدخول"
                        } finally { busy = false }
                    }
                },
                enabled = !busy && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "جارٍ التحقق..." else "تسجيل الدخول") }
        } else {
            Text("إضافة منتج أو تعديله", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم المنتج") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = image, onValueChange = { image = it }, label = { Text("رابط الصورة أو اسم ملف SVG") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = section == "parts", onClick = { section = "parts" }, label = { Text("قطع غيار") })
                FilterChip(selected = section == "accessories", onClick = { section = "accessories" }, label = { Text("لوازم وزينة") })
            }
            if (section == "parts") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = bikeType == "large", onClick = { bikeType = "large" }, label = { Text("كبيرة") })
                    FilterChip(selected = bikeType == "small", onClick = { bikeType = "small" }, label = { Text("صغيرة") })
                    FilterChip(selected = bikeType.isBlank(), onClick = { bikeType = "" }, label = { Text("الكل") })
                }
            } else bikeType = ""
            OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("السعر (اختياري)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = active, onCheckedChange = { active = it })
                Text("ظاهر للعملاء")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val current = session ?: return@Button
                        scope.launch {
                            busy = true
                            try {
                                withContext(Dispatchers.IO) {
                                    ShopAdminApi.saveProduct(current, AdminProduct(
                                        id = editingId, name = name, image = image, section = section,
                                        bikeType = bikeType, price = price, active = active,
                                        sortOrder = products.firstOrNull { it.id == editingId }?.sortOrder ?: products.size
                                    ))
                                }
                                message = "تم حفظ المنتج"
                                editingId = ""; name = ""; image = ""; price = ""; bikeType = ""; active = true
                                loadProducts(current)
                                onCatalogChanged()
                            } catch (e: Exception) { message = e.message ?: "تعذر حفظ المنتج" }
                            finally { busy = false }
                        }
                    },
                    enabled = !busy && name.isNotBlank() && image.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) { Text(if (editingId.isBlank()) "إضافة المنتج" else "حفظ التعديل") }
                OutlinedButton(onClick = {
                    editingId = ""; name = ""; image = ""; price = ""; bikeType = ""; section = "parts"; active = true
                }) { Text("جديد") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("المنتجات", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { session?.let { loadProducts(it) } }, enabled = !busy) { Text("تحديث") }
                TextButton(onClick = { session = null; products = emptyList(); password = ""; message = "تم تسجيل الخروج" }) { Text("خروج") }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(products, key = { it.id }) { product ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(product.name, fontWeight = FontWeight.Bold)
                            Text("${if (product.section == "parts") "قطع غيار" else "لوازم وزينة"} • ${if (product.active) "ظاهر" else "مخفي"} • ${product.price.ifBlank { "بلا سعر" }}", fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = {
                                    editingId = product.id; name = product.name; image = product.image
                                    section = product.section; bikeType = product.bikeType; price = product.price; active = product.active
                                }) { Text("تعديل") }
                                TextButton(onClick = {
                                    val current = session ?: return@TextButton
                                    scope.launch {
                                        busy = true
                                        try {
                                            withContext(Dispatchers.IO) { ShopAdminApi.saveProduct(current, product.copy(active = !product.active)) }
                                            loadProducts(current); onCatalogChanged()
                                            message = if (product.active) "تم إخفاء المنتج" else "تم إظهار المنتج"
                                        } catch (e: Exception) { message = e.message ?: "تعذر تغيير الحالة" }
                                        finally { busy = false }
                                    }
                                }, enabled = !busy) { Text(if (product.active) "إخفاء" else "إظهار") }
                                TextButton(onClick = {
                                    val current = session ?: return@TextButton
                                    scope.launch {
                                        busy = true
                                        try {
                                            withContext(Dispatchers.IO) { ShopAdminApi.deleteProduct(current, product.id) }
                                            products = products.filterNot { it.id == product.id }
                                            message = "تم حذف المنتج"
                                            onCatalogChanged()
                                        } catch (e: Exception) { message = e.message ?: "تعذر حذف المنتج" }
                                        finally { busy = false }
                                    }
                                }, enabled = !busy) { Text("حذف") }
                            }
                        }
                    }
                }
            }
        }
    }
}
