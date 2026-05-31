package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TutorialOverlay(
    step: Int,
    currentTick: Int,
    onAdvance: () -> Unit,
    onSubmitMentorText: (String) -> Unit,
    onTutorialAction: (Int) -> Unit = {}
) {
    // Puzzle 2 data
    val puzzle2Words = listOf("kosong", "bahan bakar", "mahal", "ikan")
    val bank2 = listOf("kosong", "bahan bakar", "ikan", "mahal", "murah")

    // Steps that overlay the full screen with a dim background and a tutorial card.
    val showOverlay = step in setOf(1, 15, 2, 3, 6)

    if (showOverlay) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .pointerInput(Unit) { detectTapGestures() }
        ) {
            when (step) {
                1 -> {
                    BottomTutorialCard(
                        accent = Color(0xFFE0E7FF),
                        labelColor = Color(0xFF4338CA),
                        bodyColor = Color(0xFF1E3A8A),
                        label = "🧑‍✈️ KEPALA NELAYAN",
                        body = "Selamat datang, Manajer! Kas pelabuhan kita 550 juta rupiah. Mari mulai dengan membeli 5 kapal sebagai armada awal — tekan tombol di bawah ini!",
                        buttonText = "Beli 5 Kapal (Maksimal Anggaran)",
                        buttonColor = Color(0xFF4338CA),
                        onClick = { onTutorialAction(1) }
                    )
                }
                15 -> {
                    BottomTutorialCard(
                        accent = Color(0xFFE0E7FF),
                        labelColor = Color(0xFF4338CA),
                        bodyColor = Color(0xFF1E3A8A),
                        label = "🧑‍✈️ KEPALA NELAYAN",
                        body = "Armada sudah siap! Tekan MULAI untuk menjalankan simulasi. Setiap tahun simulasi akan otomatis di-PAUSE supaya kamu bisa evaluasi.",
                        buttonText = "▶ MULAI SIMULASI",
                        buttonColor = Color(0xFF4338CA),
                        onClick = { onTutorialAction(15) }
                    )
                }
                2 -> {
                    val year = currentTick / 12
                    BottomTutorialCard(
                        accent = Color(0xFFD1FAE5),
                        labelColor = Color(0xFF047857),
                        bodyColor = Color(0xFF064E3B),
                        label = "📅 AKHIR TAHUN $year — STRATEGI AGRESIF",
                        body = "Untuk memaksimalkan pendapatan, tambahkan kapal sebanyak yang anggaran kita izinkan tiap akhir tahun. Lihat grafiknya — mari amati apa yang terjadi pada laut.",
                        buttonText = "Tambah Kapal Maksimal & Lanjut",
                        buttonColor = Color(0xFF059669),
                        onClick = { onTutorialAction(2) }
                    )
                }
                3 -> {
                    BottomTutorialCard(
                        accent = Color(0xFFFEE2E2),
                        labelColor = Color(0xFFB91C1C),
                        bodyColor = Color(0xFF7F1D1D),
                        label = "⚠️ KRISIS POPULASI IKAN",
                        body = "Populasi ikan menurun signifikan! Perhatikan grafik: garis biru (ikan) anjlok, dan tak lama setelahnya garis hijau (kesejahteraan nelayan) ikut merosot. Inilah Overfishing — kapal terlalu banyak menangkap lebih cepat dari kemampuan laut memulihkan diri.",
                        buttonText = "Saya Mengerti, Lanjut",
                        buttonColor = Color(0xFFB91C1C),
                        onClick = { onAdvance() }
                    )
                }
                6 -> {
                    BottomTutorialCard(
                        accent = Color(0xFFE0E7FF),
                        labelColor = Color(0xFF1D4ED8),
                        bodyColor = Color(0xFF1E3A8A),
                        label = "🎯 MISI SESUNGGUHNYA",
                        body = "Di sandbox nanti, tugasmu bukan sekadar mengejar profit. Jaga keseimbangan antara populasi laut (jangan sampai kolaps) dan kesejahteraan ekonomi nelayan (upah ≥ UMR). Itulah inti pengelolaan perikanan yang berkelanjutan. Selamat bereksperimen!",
                        buttonText = "Masuk Sandbox",
                        buttonColor = Color(0xFF2563EB),
                        onClick = { onAdvance() }
                    )
                }
            }
        }
    }

    if (step == 4) {
        // Puzzle: Empty ocean / high opex costs
        SentencePuzzleDialog(
            badge = "Refleksi · Laut Kosong",
            question = "Kenapa kita tiba-tiba merugi padahal kapalnya banyak?",
            bankWords = bank2,
            correctOrder = puzzle2Words,
            sentenceTemplate = listOf(
                "Karena laut ", "[BLANK 0]", ", kapal memboroskan ", "[BLANK 1]",
                " yang ", "[BLANK 2]", " untuk mencari ", "[BLANK 3]", "."
            ),
            onSuccess = onAdvance
        )
    }

    if (step == 5) {
        // Mentor dialogue
        var textValue by remember { mutableStateOf("") }
        Dialog(onDismissRequest = {}) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🧑‍✈️", fontSize = 32.sp)
                        Column {
                            Text(text = "KEPALA NELAYAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                            Text(text = "Sebentar, Manajer…", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Kalau mengirim terlalu banyak kapal membuat laut kosong, dan terlalu sedikit kapal membuat kita merugi karena tidak sebanding dengan biaya operasional pelabuhan... menurutmu, apa strategi terbaik mengelola pelabuhan ini?",
                        fontSize = 13.sp,
                        color = Color(0xFF3B4F66),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        placeholder = { Text("Tulis pendapatmu di sini...", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Hanya untuk melatih pemahaman awalmu — tidak ada jawaban benar/salah.", fontSize = 9.sp, color = Color(0xFF94A3B8))

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onSubmitMentorText(textValue) },
                        enabled = textValue.trim().isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(text = "Lanjut ke Misi", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.BottomTutorialCard(
    accent: Color,
    labelColor: Color,
    bodyColor: Color,
    label: String,
    body: String,
    buttonText: String,
    buttonColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = accent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = labelColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(body, fontSize = 14.sp, color = bodyColor, fontWeight = FontWeight.Medium, lineHeight = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonText, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SentencePuzzleDialog(
    badge: String,
    question: String,
    bankWords: List<String>,
    correctOrder: List<String>,
    sentenceTemplate: List<String>,
    onSuccess: () -> Unit
) {
    // Use immutable List<String?> so reassignment always changes content -> Compose recomposes.
    var filledSlots by remember { mutableStateOf<List<String?>>(List(correctOrder.size) { null }) }
    var resultChecked by remember { mutableStateOf(false) }
    var isCorrectStatus by remember { mutableStateOf(false) }

    // Shuffled bank
    val bankList = remember { bankWords.shuffled() }

    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, Color(0xFF60A5FA), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = badge.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB),
                    letterSpacing = 1.sp
                )
                Text(
                    text = question,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sentence Builder Area
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                ) {
                    FlowRow(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sentenceTemplate.forEach { s ->
                            if (s.startsWith("[BLANK")) {
                                val blankIdx = s.substring(7, 8).toInt()
                                val filled = filledSlots[blankIdx]

                                if (filled != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (resultChecked) {
                                                    if (isCorrectStatus) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                                                } else Color(0xFFDBEAFE)
                                            )
                                            .border(
                                                1.5.dp,
                                                if (resultChecked) {
                                                    if (isCorrectStatus) Color(0xFF34D399) else Color(0xFFF87171)
                                                } else Color(0xFF60A5FA),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable(enabled = !resultChecked) {
                                                // Return word to the bank (new list reference -> recompose)
                                                filledSlots = filledSlots.toMutableList().apply { this[blankIdx] = null }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = filled,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (resultChecked) {
                                                if (isCorrectStatus) Color(0xFF065F46) else Color(0xFF991B1B)
                                            } else Color(0xFF1D4ED8)
                                        )
                                    }
                                } else {
                                    // Empty blank placeholder
                                    Box(
                                        modifier = Modifier
                                            .size(width = 80.dp, height = 24.dp)
                                            .background(Color.White, RoundedCornerShape(6.dp))
                                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                                    )
                                }
                            } else {
                                Text(
                                    text = s,
                                    fontSize = 13.sp,
                                    color = Color(0xFF334155),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Word Pool
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    bankList.forEach { word ->
                        val used = filledSlots.contains(word)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (used) Color(0xFFE2E8F0) else Color.White)
                                .border(
                                    1.5.dp,
                                    if (used) Color(0xFFE2E8F0) else Color(0xFF94A3B8),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable(enabled = !used && !resultChecked) {
                                    val emptyIdx = filledSlots.indexOf(null)
                                    if (emptyIdx != -1) {
                                        // New list reference -> Compose recomposes
                                        filledSlots = filledSlots.toMutableList().apply { this[emptyIdx] = word }
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = word,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (used) Color(0xFF94A3B8) else Color(0xFF1E293B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Feedback
                if (resultChecked) {
                    val msg = if (isCorrectStatus) "✓ Tepat sekali! Kerja Bagus." else "× Kurang tepat — dicoba kembali ya."
                    val clr = if (isCorrectStatus) Color(0xFF10B981) else Color(0xFFEF4444)
                    Text(text = msg, fontSize = 13.sp, color = clr, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val allFilled = filledSlots.none { it == null }

                Button(
                    onClick = {
                        if (!resultChecked) {
                            val isCorrect = filledSlots.zip(correctOrder).all { (a, b) -> a == b }
                            isCorrectStatus = isCorrect
                            resultChecked = true
                        } else {
                            if (isCorrectStatus) {
                                onSuccess()
                            } else {
                                // Reset for retry
                                filledSlots = List(correctOrder.size) { null }
                                resultChecked = false
                            }
                        }
                    },
                    enabled = allFilled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCorrectStatus && resultChecked) Color(0xFF039855) else Color(0xFF2563EB)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = when {
                            !resultChecked -> "Periksa"
                            isCorrectStatus -> "Selesai"
                            else -> "Ulangi Lagi"
                        },
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
