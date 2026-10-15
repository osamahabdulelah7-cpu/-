package com.example.bikeshop

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest

private val Navy = Color(0xFF102B46)
private val Blue = Color(0xFF1677E8)
private val Pale = Color(0xFFF3F7FB)

data class ShopProduct(val name: String, val image: String, val section: String, val bikeType: String = "")

private val bikeTypes = listOf(
    ShopProduct("الدراجات الكبيرة", "bicycle_large.svg", "parts", "large"),
    ShopProduct("الدراجات الصغيرة", "bicycle_small.svg", "parts", "small")
)

private val parts = listOf(
    ShopProduct("الإطارات والعجلات", "parts_wheels.svg", "parts"),
    ShopProduct("السلاسل والتروس", "parts_chain.svg", "parts"),
    ShopProduct("الفرامل", "parts_brakes.svg", "parts"),
    ShopProduct("الدواسات", "parts_pedals.svg", "parts"),
    ShopProduct("الأنابيب الداخلية", "parts_tube.svg", "parts"),
    ShopProduct("المقاعد", "parts_seat.svg", "parts")
)

private val accessories = listOf(
    ShopProduct("خوذات", "accessories_helmet.svg", "accessories"),
    ShopProduct("أضواء أمامية وخلفية", "accessories_lights.svg", "accessories"),
    ShopProduct("أجراس", "accessories_bell.svg", "accessories"),
    ShopProduct("سلال وحقائب", "accessories_basket.svg", "accessories"),
    ShopProduct("حوامل الهاتف", "accessories_phone.svg", "accessories"),
    ShopProduct("مقابض وزينة", "accessories_grips.svg", "accessories")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BicycleShopApp() }
    }
}

@Composable
private fun BicycleShopApp() {
    var page by remember { mutableStateOf("home") }
    var selectedBike by remember { mutableStateOf("") }

    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, background = Pale)) {
        Scaffold(containerColor = Pale, topBar = {
            TopAppBarBicycle(
                title = when (page) {
                    "types" -> "أنواع الدراجات الهوائية"
                    "parts" -> "قطع الغيار"
                    "accessories" -> "اللوازم والزينة"
                    else -> "دراجتك"
                },
                showBack = page != "home",
                onBack = { page = if (page == "parts") "types" else "home" }
            )
        }) { padding ->
            when (page) {
                "home" -> HomeScreen(
                    Modifier.padding(padding),
                    onParts = { page = "types" },
                    onAccessories = { page = "accessories" }
                )
                "types" -> CatalogScreen(
                    Modifier.padding(padding),
                    title = "اختر نوع الدراجة",
                    products = bikeTypes,
                    onClick = { selectedBike = it.bikeType; page = "parts" },
                    showOrder = false
                )
                "parts" -> CatalogScreen(
                    Modifier.padding(padding),
                    title = if (selectedBike == "small") "قطع غيار الدراجات الصغيرة" else "قطع غيار الدراجات الكبيرة",
                    products = parts,
                    onClick = {},
                    showOrder = true,
                    bikeLabel = if (selectedBike == "small") "دراجة صغيرة" else "دراجة كبيرة"
                )
                else -> CatalogScreen(
                    Modifier.padding(padding),
                    title = "كل ما يضاف للدراجة الهوائية",
                    products = accessories,
                    onClick = {},
                    showOrder = true
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBarBicycle(title: String, showBack: Boolean, onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold, color = Color.White) },
        navigationIcon = {
            if (showBack) IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Navy)
    )
}

@Composable
private fun HomeScreen(modifier: Modifier, onParts: () -> Unit, onAccessories: () -> Unit) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("مرحبًا بك في طبيب السياكل", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Navy)
        Text("دراجات هوائية وقطع غيار ولوازم متنوعة", color = Color.DarkGray)
        HomeTile("قطع غيار الدراجات الهوائية", "اختر الدراجات الكبيرة أو الصغيرة ثم تصفح القطع", "bicycle_large.svg", onParts)
        HomeTile("لوازم الدراجات الهوائية وزينتها", "خوذات وأضواء وأجراس وحقائب وإضافات", "accessories_helmet.svg", onAccessories)
        Spacer(Modifier.weight(1f))
        Text("الأسعار ستظهر بعد أن يضيفها صاحب المحل", color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun HomeTile(title: String, subtitle: String, image: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ProductImage(image, Modifier.size(116.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Navy)
                Text(subtitle, fontSize = 13.sp, color = Color.DarkGray)
                Text("استعرض الآن ←", color = Blue, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CatalogScreen(
    modifier: Modifier,
    title: String,
    products: List<ShopProduct>,
    onClick: (ShopProduct) -> Unit,
    showOrder: Boolean,
    bikeLabel: String = ""
) {
    Column(modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Text(title, modifier = Modifier.padding(vertical = 14.dp), fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Navy)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            items(products) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onClick(product) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProductImage(product.image, Modifier.size(112.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy)
                            if (showOrder) {
                                Text("السعر: __________", color = Color.Gray, fontSize = 14.sp)
                                Text("يحدده صاحب المحل", color = Color.Gray, fontSize = 12.sp)
                                WhatsAppOrderButton(product.name, bikeLabel)
                            } else {
                                Text("اضغط لعرض التفاصيل", color = Blue, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductImage(assetName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
    val request = remember(assetName) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/$assetName")
            .crossfade(true)
            .build()
    }
    AsyncImage(
        model = request,
        imageLoader = imageLoader,
        contentDescription = null,
        modifier = modifier.background(Color(0xFFF7FAFD), RoundedCornerShape(12.dp))
    )
}

@Composable
private fun WhatsAppOrderButton(productName: String, bikeLabel: String = "") {
    val context = LocalContext.current
    Button(
        onClick = {
            val number = ShopConfig.WHATSAPP_NUMBER
            val message = "السلام عليكم، أريد طلب: $productName" +
                if (bikeLabel.isNotBlank()) " ($bikeLabel)" else ""
            val url = if (number.isBlank()) {
                "https://wa.me/?text=" + Uri.encode("$message. يرجى ضبط رقم المحل في إعدادات التطبيق.")
            } else {
                "https://wa.me/$number?text=" + Uri.encode(message)
            }
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text("اطلب الآن")
    }
}
