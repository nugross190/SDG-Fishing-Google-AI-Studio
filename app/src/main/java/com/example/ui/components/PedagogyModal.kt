package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedagogyModal(
    triggerMap: Map<String, Any>,
    onSubmit: (choiceIndex: Int?, openText: String?) -> Unit
) {
    val id = triggerMap["id"] as String
    val badge = triggerMap["badge"] as String
    val emoji = triggerMap["emoji"] as String
    val title = triggerMap["title"] as String
    val rawBody = triggerMap["body"] as String
    val type = triggerMap["type"] as String
    
    // Clean raw html bold tags simple parser (e.g. <b>Text</b> to Text)
    val body = rawBody.replace("<b>", "").replace("</b>", "")

    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, Color(0xFF3B82F6), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                // Header
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = emoji, fontSize = 32.sp)
                    Column {
                        Text(
                            text = badge.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = body,
                    fontSize = 13.sp,
                    color = Color(0xFF475569),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (type == "mc") {
                    val options = triggerMap["options"] as List<Pair<String, Boolean>>
                    var selectedIndex by remember { mutableStateOf<Int?>(null) }
                    var hasChecked by remember { mutableStateOf(false) }

                    options.forEachIndexed { idx, pair ->
                        val isCorrect = pair.second
                        val isSelected = selectedIndex == idx

                        val btnColor = when {
                            !hasChecked -> if (isSelected) Color(0xFFEFF6FF) else Color.White
                            isCorrect -> Color(0xFFDCFCE7) // highlight correct in green
                            isSelected -> Color(0xFFFEE2E2) // highlighted chosen-wrong in red
                            else -> Color.White
                        }

                        val borderColor = when {
                            !hasChecked -> if (isSelected) Color(0xFF3B82F6) else Color(0xFFE2E8F0)
                            isCorrect -> Color(0xFF4ADE80)
                            isSelected -> Color(0xFFF87171)
                            else -> Color(0xFFE2E8F0)
                        }

                        Button(
                            onClick = {
                                if (!hasChecked) {
                                    selectedIndex = idx
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = btnColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.5.dp, borderColor, RoundedCornerShape(10.dp)),
                            contentPadding = PaddingValues(12.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "${(65 + idx).toChar()}.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3B82F6),
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text(
                                    text = pair.first,
                                    fontSize = 12.sp,
                                    color = Color(0xFF334155),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (hasChecked) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val isChosenCorrect = selectedIndex != null && options[selectedIndex!!].second
                        val feedback = if (isChosenCorrect) {
                            triggerMap["feedbackCorrect"] as String
                        } else {
                            triggerMap["feedbackWrong"] as String
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChosenCorrect) Color(0xFFF0FDF4) else Color(0xFFFEF3C7)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (isChosenCorrect) Color(0xFFBBF7D0) else Color(0xFFFDE68A),
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Text(
                                text = feedback,
                                fontSize = 12.sp,
                                color = if (isChosenCorrect) Color(0xFF166534) else Color(0xFF92400E),
                                modifier = Modifier.padding(10.dp)
                                    .fillMaxWidth(),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (!hasChecked) {
                                if (selectedIndex != null) {
                                    hasChecked = true
                                }
                            } else {
                                onSubmit(selectedIndex, null)
                            }
                        },
                        enabled = selectedIndex != null,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (!hasChecked) "Periksa Jawaban" else "Mengerti, Lanjut ➔",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                } else {
                    // Open-ended answer text field
                    var openValue by remember { mutableStateOf("") }

                    OutlinedTextField(
                        value = openValue,
                        onValueChange = { openValue = it },
                        placeholder = { Text("Tulis pemikiranmu di sini...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onSubmit(null, openValue) },
                        enabled = openValue.trim().isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Lanjut ➔", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Text(
                    text = "Simulasi ditangguhkan sementara sampai kamu merespon.",
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp).fillMaxWidth(),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
