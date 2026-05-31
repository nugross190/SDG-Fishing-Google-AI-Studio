package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.SimulationViewModel

@Composable
fun AdminPanel(
    modifier: Modifier = Modifier,
    viewModel: SimulationViewModel,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()
    val state by viewModel.uiState.collectAsState()

    // Working local states for all parameters
    var rVal by remember(state.activeScenario) { mutableStateOf(state.r.toString()) }
    var kVal by remember(state.activeScenario) { mutableStateOf(state.K.toString()) }
    var qVal by remember(state.activeScenario) { mutableStateOf(viewModel.q.toString()) }
    var pVal by remember(state.activeScenario) { mutableStateOf(state.P.toString()) }
    var cVal by remember(state.activeScenario) { mutableStateOf(state.c.toString()) }
    var deltaVal by remember(state.activeScenario) { mutableStateOf(viewModel.delta.toString()) }
    var mgmtCutVal by remember(state.activeScenario) { mutableStateOf(viewModel.MGMT_CUT.toString()) }
    
    var harborFeeVal by remember(state.activeScenario) { mutableStateOf(viewModel.HARBOR_FEE_PER_SHIP_MONTHLY.toString()) }
    var shipCostVal by remember(state.activeScenario) { mutableStateOf(viewModel.SHIP_COST.toString()) }
    var shipSellVal by remember(state.activeScenario) { mutableStateOf(viewModel.SHIP_SELL.toString()) }
    var shipMaintVal by remember(state.activeScenario) { mutableStateOf(viewModel.SHIP_MAINT_ANNUAL.toString()) }
    var effortPerShipVal by remember(state.activeScenario) { mutableStateOf(viewModel.EFFORT_PER_SHIP_ANNUAL.toString()) }
    
    var initFleetVal by remember(state.activeScenario) { mutableStateOf(state.fleet.toString()) }
    var initBudgetVal by remember(state.activeScenario) { mutableStateOf(state.portBudget.toString()) }
    
    var startNRatioVal by remember(state.activeScenario) { 
        val initConfig = viewModel.scenarios[state.activeScenario]
        mutableStateOf((initConfig?.startNRatio ?: 1.0).toString()) 
    }

    var cohortSize by remember { mutableStateOf("1") }
    var statusMessage by remember { mutableStateOf("") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(2.dp, Color(0xFF3F5F90), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.someOfSupaDoneTextBar(), // Custom alignment
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🔧 Admin Tuning Panel",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1B1F)
                    )
                    Text(
                        text = "Edit parameter lalu klik Apply & Reset Sim.",
                        fontSize = 11.sp,
                        color = Color(0xFF74777F)
                    )
                }
                IconButton(onClick = onClose) {
                    Text(text = "✕", fontWeight = FontWeight.Bold, color = Color(0xFF3F5F90), fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section 1: Schaefer Math Formulas
            AdminSectionHeader(title = "Populasi & Bio-Math (Schaefer)")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminInput(label = "r (Pertumbuhan)", value = rVal, onValueChange = { rVal = it }, modifier = Modifier.weight(1f))
                AdminInput(label = "K (Maks Ton)", value = kVal, onValueChange = { kVal = it }, modifier = Modifier.weight(1f))
                AdminInput(label = "q (Catchability)", value = qVal, onValueChange = { qVal = it }, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Section 2: Economics
            AdminSectionHeader(title = "Keuangan & Tarif")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminInput(label = "P (Harga Jual/Ton)", value = pVal, onValueChange = { pVal = it }, modifier = Modifier.weight(1.5f))
                AdminInput(label = "c (Effort Cost)", value = cVal, onValueChange = { cVal = it }, modifier = Modifier.weight(1f))
                AdminInput(label = "δ (Discount rate)", value = deltaVal, onValueChange = { deltaVal = it }, modifier = Modifier.weight(1f))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminInput(label = "Pajak Mgmt (0-1)", value = mgmtCutVal, onValueChange = { mgmtCutVal = it }, modifier = Modifier.weight(1f))
                AdminInput(label = "Sewa Pelabuhan/kap/bln", value = harborFeeVal, onValueChange = { harborFeeVal = it }, modifier = Modifier.weight(1.5f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Section 3: Ships Specs
            AdminSectionHeader(title = "Infrastruktur Armada")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminInput(label = "Harga Beli Kapal", value = shipCostVal, onValueChange = { shipCostVal = it }, modifier = Modifier.weight(1f))
                AdminInput(label = "Harga Jual Kapal", value = shipSellVal, onValueChange = { shipSellVal = it }, modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminInput(label = "Simpanan Rawat/thn", value = shipMaintVal, onValueChange = { shipMaintVal = it }, modifier = Modifier.weight(1f))
                AdminInput(label = "Total Effort/kap/thn", value = effortPerShipVal, onValueChange = { effortPerShipVal = it }, modifier = Modifier.weight(1.3f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Section 4: Initial Scenarios state
            AdminSectionHeader(title = "Skenario Keadaan Awal")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminInput(label = "Armada Awal", value = initFleetVal, onValueChange = { initFleetVal = it }, modifier = Modifier.weight(1f))
                AdminInput(label = "Giro Kas Utama", value = initBudgetVal, onValueChange = { initBudgetVal = it }, modifier = Modifier.weight(1.5f))
                AdminInput(label = "Start N/K (0-1)", value = startNRatioVal, onValueChange = { startNRatioVal = it }, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Apply parameter button strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        try {
                            viewModel.applyAdminOverrides(
                                r = rVal.toDouble(),
                                K = kVal.toDouble(),
                                q_override = qVal.toDouble(),
                                P = pVal.toDouble(),
                                c = cVal.toDouble(),
                                initFleet = initFleetVal.toInt(),
                                initBudget = initBudgetVal.toDouble(),
                                startNRatio = startNRatioVal.toDouble(),
                                harborFee = harborFeeVal.toDouble(),
                                shipCost = shipCostVal.toDouble(),
                                shipSell = shipSellVal.toDouble(),
                                shipMaint = shipMaintVal.toDouble(),
                                effortPerShip = effortPerShipVal.toDouble(),
                                salaryRatio = state.adminSalaryRatio
                            )
                            statusMessage = "✓ Applied Overrides!"
                        } catch (e: Exception) {
                            statusMessage = "✗ Gagal parsing angka"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F5F90)),
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Apply & Reset Sim", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        viewModel.resetWholeAppProgress()
                        statusMessage = "↺ Defaults Restored & Locked Stages Reset"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1E2E9)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Restore Defaults", fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1F), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFFDEE2EB))
            Spacer(modifier = Modifier.height(12.dp))

            // Uji Agent Cohort generator: precomputes full simulated paths across standard styles!
            Text(
                text = "🧪 Analisis Gaya Bermain Cohort",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B1B1F)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Cohort count:", fontSize = 11.sp, color = Color(0xFF44474E))
                OutlinedTextField(
                    value = cohortSize,
                    onValueChange = { cohortSize = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(60.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3F5F90),
                        unfocusedBorderColor = Color(0xFFDEE2EB)
                    )
                )
                Button(
                    onClick = {
                        val size = cohortSize.toIntOrNull() ?: 1
                        viewModel.generateCohorts(size.coerceIn(1, 5))
                        statusMessage = "✓ Generated ${size * 12} scenario archetypes!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F5F90)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Gen Cohorts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            if (statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = statusMessage,
                    fontSize = 11.sp,
                    color = Color(0xFF1E5D91),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun AdminSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF74777F),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun AdminInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 9.sp, color = Color(0xFF1B1B1F))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontSize = 11.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFF3F5F90),
                unfocusedBorderColor = Color(0xFFDEE2EB)
            )
        )
    }
}

// Custom Arrangement helper for formatting compatibility
private fun Arrangement.someOfSupaDoneTextBar() = Arrangement.SpaceBetween
