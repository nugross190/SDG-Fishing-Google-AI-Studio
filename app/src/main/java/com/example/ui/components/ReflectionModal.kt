package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ReflectionModal(
    scenarioLabel: String,
    scenarioKey: String,
    finalPopTon: Int,
    finalPopPct: Int,
    durationMonths: Int,
    cumulativeProfit: Double,
    netProfit: Double,
    fisherWage: Double,
    peakFleet: Int,
    finalFleet: Int,
    onSaveReflection: (text: String) -> Unit
) {
    val scrollState = rememberScrollState()
    var reflectionText by remember { mutableStateOf("") }

    val rpFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 } }
    val formattedCumProfit = rpFormatter.format(cumulativeProfit)
    val formattedNetProfit = rpFormatter.format(netProfit)
    val formattedFisherWage = rpFormatter.format(fisherWage)

    // Calculate style archetype
    val styleArchetype = when {
        finalPopPct < 25 -> "Greedy — Terlalu Agresif 🔴"
        finalPopPct > 70 && peakFleet <= (if (scenarioKey == "overfished") 50 else 4) + 3 -> "Over-Conserving — Terlalu Hati-hati 🟡"
        finalPopPct in 40..65 && cumulativeProfit > 0 -> "Balanced — Berkelanjutan 🟢"
        else -> "Balanced/Over-Conserving"
    }

    // Welfare calculation
    val wagePct = (fisherWage / 2500000.0) * 100.0
    val (evalTitle, evalColor, evalText) = when {
        wagePct >= 95 -> Triple("Sejahtera (Upah memadai) ✓", Color(0xFF166534), "Mencapai standar layak penuh untuk menutupi kebutuhan pokok, pendidikan anak, serta jaring kesehatan nelayan.")
        wagePct >= 90 -> Triple("Cukup (Batas minimum) ⚠", Color(0xFF854D0E), "Kebutuhan harian terpenuhi, namun rentan apabila menghadapi pengeluran darurat mendadak.")
        wagePct >= 80 -> Triple("Rentan (Di bawah layak) ⚠", Color(0xFF9A3412), "Mulai mengalami tekanan ekonomi berat, memotong pengeluaran pokok seperti gizi harian.")
        else -> Triple("Krisis (Kelewat parah) ✗", Color(0xFF991B1B), "Jauh di bawah standar kemiskinan ekstrem. Keluarga tidak mampu mencukupi gizi primer.")
    }

    val reflectionPrompts = mapOf(
        "normal" to "Apa yang terjadi saat kita menambah perahu terlalu banyak? Seberapa besar armada optimal agar laba maksimal tanpa mengganggu populasi induk?",
        "bbm" to "Krisis solar melambungkan biaya. Bandingkan dengan Normal, strategi apa yang harus disesuaikan agar nelayan tetap mendapat upah di atas UMR?",
        "elnino_bbm" to "Anomali cuaca mengganggu regenerasi. Bagaimana cara terbaik mengimbangi krisis alamiah dan krisis energi secara bersamaan?",
        "overfished" to "Kamu mewarisi laut yang rusak parah dengan armada berlebih. Langkah darurat apa yang berhasil atau tidak berhasil memulihkan keseimbangan ekoregion?"
    )

    val prompt = reflectionPrompts[scenarioKey] ?: "Bagikan analisismu mengenai simulasi barusan."

    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFFDEE2EB), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = "Simulasi Selesai 🏁",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1F)
                )
                Text(
                    text = "Skenario: $scenarioLabel · $durationMonths Bulan",
                    fontSize = 12.sp,
                    color = Color(0xFF74777F),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Grid stats
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3F4F9), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFDEE2EB), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatRow(label = "Populasi Akhir", value = "$finalPopTon Ton ($finalPopPct%)", color = if (finalPopPct < 30) Color(0xFFB91C1C) else Color(0xFF3F5F90))
                    StatRow(label = "Upah Nelayan Akhir", value = "$formattedFisherWage / bln", color = evalColor)
                    StatRow(label = "Profit Bersih Akhir", value = formattedNetProfit, color = if (netProfit >= 0) Color(0xFF166534) else Color(0xFFB91C1C))
                    StatRow(label = "Total Keuntungan", value = formattedCumProfit, color = Color(0xFFDB2777))
                    StatRow(label = "Puncak Kapal", value = "$peakFleet Kapal (Akhir: $finalFleet)", color = Color(0xFF44474E))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Archetype Style
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFDEE2EB), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F9))
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Gaya Analisismu: ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF44474E)
                        )
                        Text(
                            text = styleArchetype,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1B1B1F)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Welfare Evaluation details
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFDEE2EB), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = evalColor.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Evaluasi Kesejahteraan: $evalTitle",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = evalColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = evalText,
                            fontSize = 11.sp,
                            color = Color(0xFF44474E),
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reflection box
                Text(
                    text = "Pertanyaan Refleksi",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF74777F),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = prompt,
                    fontSize = 13.sp,
                    color = Color(0xFF1B1B1F),
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = reflectionText,
                    onValueChange = { reflectionText = it },
                    placeholder = { Text("Tulis refleksimu di sini (minimal 10 karakter)...", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onSaveReflection(reflectionText) },
                    enabled = reflectionText.trim().length >= 10,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F5F90)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = "Simpan & Lanjut ke Sandbox", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun StatRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF64748B))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
