package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.RunHistoryEntity
import com.example.ui.components.*
import com.example.viewmodel.SimulationViewModel
import com.example.viewmodel.ScenarioInfo
import org.json.JSONArray
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    viewModel: SimulationViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val historyRuns by viewModel.allRuns.collectAsStateWithLifecycle()
    val stageProgressList by viewModel.stageProgressList.collectAsStateWithLifecycle()

    var showAdminPanel by remember { mutableStateOf(false) }
    var showWelcomeDialog by remember { mutableStateOf(true) }
    var isOpexExpanded by remember { mutableStateOf(false) }

    // Currencies, tons and percentages formatter helpers
    val rpFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 } }
    val shortRpFormatter = remember {
        { rp: Double ->
            val absVal = Math.abs(rp)
            when {
                absVal >= 1_000_000_000_000.0 -> "Rp ${(rp / 1_000_000_000_000.0).format(2)} T"
                absVal >= 1_000_000_000.0 -> "Rp ${(rp / 1_000_000_000.0).format(2)} M"
                absVal >= 1_000_000.0 -> "Rp ${(rp / 1_000_000.0).format(1)} Jt"
                absVal >= 1_000.0 -> "Rp ${(rp / 1_000.0).format(0)} rb"
                else -> rpFormatter.format(rp)
            }
        }
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(state.tutorialStep) {
        if (state.isTutorialMode) {
            // Give a 1-second delay before scrolling to let user perceive layout change
            kotlinx.coroutines.delay(1000)
            when (state.tutorialStep) {
                1 -> listState.animateScrollToItem(1) // FleetCardSection
                15 -> listState.animateScrollToItem(8) // Playback section
                3 -> listState.animateScrollToItem(1) // FleetCardSection
                4 -> listState.animateScrollToItem(5) // Population Gauge
            }
        }
    }

    Scaffold(
        topBar = {
            HeaderBar(
                currentTick = state.currentTick,
                onOpenAdmin = { showAdminPanel = !showAdminPanel }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF3F4F9))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Skenario drop banner / detail header
                item {
                    ScenarioBannerSection(
                        activeScenario = state.activeScenario,
                        scenarios = viewModel.scenarios,
                        unlockedStagesList = (stageProgressList.filter { it.unlocked }.map { it.scenarioKey } + "normal").distinct(),
                        onScenarioSelected = { viewModel.changeScenario(it) }
                    )
                }

                // Main Dashboard Panel: grid splitting details on horizontal, stack on vertical
                val displayNRatio = if (state.K > 0) state.currentN / state.K else 0.0
                val isFisherProsperous = state.lastMonthlyFisherWage >= viewModel.UMR_MONTHLY_DEFAULT

                item {
                    // Fleet Controllers & harbor fund
                        FleetCardSection(
                            fleetCount = state.fleet,
                            maxFleet = viewModel.MAX_FLEET,
                            portBudget = state.portBudget,
                            shipCost = viewModel.SHIP_COST,
                            shipSell = viewModel.SHIP_SELL,
                            onBuy = { viewModel.buyShips(it) },
                            onSell = { viewModel.sellShips(it) },
                            shortRpFormatter = shortRpFormatter,
                            rpFormatter = rpFormatter
                        )
                }

                item {
                        // Salary Ratios
                        SalaryControlSection(
                            currentRatio = state.adminSalaryRatio,
                            onRatioSelected = { viewModel.changeAdminSalaryRatio(it) }
                        )
                }

                item {
                        // Laba-Rugi Metrics
                        PLMetricsSection(
                            revenue = state.lastRevenue,
                            fuel = state.lastFuel,
                            supplies = state.lastSupplies,
                            maint = state.lastMaint,
                            opex = state.lastOpex,
                            netProfit = state.lastNetProfit,
                            cumProfit = state.cumulativeProfit,
                            expanded = isOpexExpanded,
                            onToggleExpand = { isOpexExpanded = !isOpexExpanded },
                            shortRpFormatter = shortRpFormatter
                        )
                }

                item {
                        // Animated Sea visualizer
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.border(1.dp, Color(0xFFDEE2EB), RoundedCornerShape(24.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "🌊 VISUALISASI LAUT REAL-TIME",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF74777F),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                OceanScene(
                                    fleetCount = state.fleet,
                                    populationRatio = displayNRatio.toFloat(),
                                    isProsperous = isFisherProsperous
                                )
                            }
                        }
                }

                item {
                        // Population & Biomass Meter
                        PopulationGaugeSection(
                            currentN = state.currentN,
                            carryingCapacity = state.K,
                            deltaN = state.lastDeltaN,
                            percentN = (displayNRatio * 100.0).toInt()
                        )
                }

                item {
                        // Fisher / Staf Welfare Status
                        WelfareMetricsSection(
                            fisherWage = state.lastMonthlyFisherWage,
                            adminWage = state.lastMonthlyAdminWage,
                            umr = viewModel.UMR_MONTHLY_DEFAULT,
                            adminSalaryTarget = viewModel.ADMIN_SALARY_DEFAULT,
                            adminSalaryRatio = state.adminSalaryRatio,
                            rpFormatter = rpFormatter
                        )
                }

                // Admin overlay card inside dashboard
                if (showAdminPanel) {
                    item {
                        AdminPanel(
                            viewModel = viewModel,
                            onClose = { showAdminPanel = false }
                        )
                    }
                }

                // Live dynamic charts
                item {
                    val finalMaxPop = state.K.coerceAtLeast(450000.0)
                    SimulationChart(
                        modifier = Modifier.height(300.dp),
                        popHistory = state.popHistory,
                        fisherWageHistory = state.fisherWageHistory,
                        adminWageHistory = state.adminWageHistory,
                        carryingCapacity = finalMaxPop
                    )
                }

                // Play / Pause controllers
                item {
                    PlaybackSection(
                        isRunning = state.isRunning,
                        onPlay = { viewModel.startPlaying() },
                        onPause = { viewModel.pausePlaying() },
                        onReset = { viewModel.resetSimulation() },
                        onDemo = { viewModel.startTutorialStory() }
                    )
                }

                // saved simulation history registry
                item {
                    HistoryTableSection(
                        runs = historyRuns,
                        carryingCapacity = state.K,
                        onClearAll = { viewModel.clearAllRuns() },
                        rpShort = shortRpFormatter
                    )
                }
            }

            // Welcome popup overlay on first boot
            if (showWelcomeDialog && !state.isTutorialMode && historyRuns.isEmpty()) {
                WelcomePopup(
                    onStartTour = {
                        showWelcomeDialog = false
                        viewModel.startTutorialStory()
                    },
                    onSkipTour = {
                        showWelcomeDialog = false
                    }
                )
            }

            // Year end summary pause overlay card
            if (state.isYearEndPaused) {
                val year = state.currentTick / 12
                YearEndSummaryPopup(
                    year = year,
                    finalPop = state.currentN.toInt(),
                    fleet = state.fleet,
                    netProfit = state.lastNetProfit,
                    portBudget = state.portBudget,
                    fisherWage = state.lastMonthlyFisherWage,
                    adminWage = state.lastMonthlyAdminWage,
                    rpFormatter = rpFormatter,
                    onDismiss = { viewModel.dismissYearEndProgress() }
                )
            }

            // Pedagogy dialog prompts popup
            if (state.isPedagogyActive && state.activePedagogyTrigger != null) {
                PedagogyModal(
                    triggerMap = state.activePedagogyTrigger!!,
                    onSubmit = { choice, open ->
                        viewModel.submitPedagogyAnswer(choice, open)
                    }
                )
            }

            // Reflexive / Reflection details at play end
            val isRunComplete = state.currentTick >= viewModel.maxTicks || state.currentN <= 0.0
            if (isRunComplete && state.currentTick > 0 && !state.isRunning && !state.isTutorialMode) {
                val config = viewModel.scenarios[state.activeScenario]
                ReflectionModal(
                    scenarioLabel = config?.label ?: "Simulator Normal",
                    scenarioKey = state.activeScenario,
                    finalPopTon = state.currentN.toInt(),
                    finalPopPct = (if (state.K > 0) state.currentN / state.K * 100.0 else 0.0).toInt(),
                    durationMonths = state.currentTick,
                    cumulativeProfit = state.cumulativeProfit,
                    netProfit = state.lastNetProfit,
                    fisherWage = state.lastMonthlyFisherWage,
                    peakFleet = state.peakFleet,
                    finalFleet = state.fleet,
                    onSaveReflection = { text ->
                        viewModel.submitReflectionAndSave(text)
                    }
                )
            }

            // Scripted tutorial wizard overlay. Steps 20/21 are pass-through
            // waiting states (let the sim run and observe) — no overlay, so
            // the user can dismiss the year-end popup and press Play.
            val overlayStep = state.tutorialStep
            val isInteractiveOverlayStep = overlayStep in setOf(1, 2, 3, 4, 5, 15)
            if (state.isTutorialMode && isInteractiveOverlayStep) {
                TutorialOverlay(
                    step = state.tutorialStep,
                    onAdvance = { viewModel.advanceTutorialStep() },
                    onSubmitMentorText = { text ->
                        viewModel.advanceTutorialStep()
                    },
                    onTutorialAction = { step ->
                        when(step) {
                            1 -> viewModel.buyShips(viewModel.MAX_FLEET)
                            15 -> viewModel.startPlaying()
                        }
                    }
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// HEADER
// ──────────────────────────────────────────────
@Composable
fun HeaderBar(
    currentTick: Int,
    onOpenAdmin: () -> Unit
) {
    val displayYear = currentTick / 12
    val displayMonth = (currentTick % 12) + 1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF3F4F9))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF3F5F90), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🌊", fontSize = 18.sp)
            }
            Column {
                Text(
                    text = "Knovera SDG-14 v5",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1F)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFF22C55E), RoundedCornerShape(3.dp))
                    )
                    Text(
                        text = "Backend Synchronized",
                        fontSize = 11.sp,
                        color = Color(0xFF44474E),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFD9E2FF), RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFFADC6FF).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Tahun $displayYear Bln $displayMonth",
                    color = Color(0xFF001945),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = onOpenAdmin,
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFDEE2EB), RoundedCornerShape(16.dp))
            ) {
                Text(text = "⚙️", color = Color(0xFF44474E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ──────────────────────────────────────────────
// SCENARIOS BANNER & DROPDOWN
// ──────────────────────────────────────────────
@Composable
fun ScenarioBannerSection(
    activeScenario: String,
    scenarios: Map<String, ScenarioInfo>,
    unlockedStagesList: List<String>,
    onScenarioSelected: (String) -> Unit
) {
    val activeInfo = scenarios[activeScenario] ?: scenarios["normal"]!!

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, Color(0xFFDEE2EB), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "PILIH SKENARIO",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF74777F),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Scenario Tabs/Select list
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                scenarios.forEach { (key, info) ->
                    val unlocked = unlockedStagesList.contains(key)
                    val selected = activeScenario == key

                    Button(
                        onClick = { if (unlocked) onScenarioSelected(key) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) Color(0xFFD9E2FF) else Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) Color(0xFF3F5F90) else if (unlocked) Color(0xFFDEE2EB) else Color(0xFFE1E2E9),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                        enabled = unlocked
                    ) {
                        Text(
                            text = (if (unlocked) "" else "🔒 ") + info.label.replace("Kondisi ", ""),
                            color = if (selected) Color(0xFF001945) else if (unlocked) Color(0xFF44474E) else Color(0xFF74777F).copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // active banner spec details
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD9E2FF)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.border(1.dp, Color(0xFFADC6FF).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎯 ${activeInfo.label.uppercase()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF001945)
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF001945), RoundedCornerShape(10.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "ACTIVE", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = activeInfo.banner,
                        fontSize = 12.sp,
                        color = Color(0xFF1E5D91),
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// FLEET MANAGEMENT
// ──────────────────────────────────────────────
@Composable
fun FleetCardSection(
    fleetCount: Int,
    maxFleet: Int,
    portBudget: Double,
    shipCost: Double,
    shipSell: Double,
    onBuy: (Int) -> Unit,
    onSell: (Int) -> Unit,
    shortRpFormatter: (Double) -> String,
    rpFormatter: NumberFormat
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, Color(0xFFDEE2EB), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MANAJEMEN ARMADA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF74777F),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "$fleetCount / $maxFleet Kapal",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3F5F90),
                    modifier = Modifier
                        .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Buy grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9FAFF), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFDEE2EB).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🚢+ BELI KAPAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3F5F90))
                    Text(text = "${shortRpFormatter(shipCost)} / unit", fontSize = 10.sp, color = Color(0xFF1E5D91), fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 5, 10).forEach { qty ->
                        Button(
                            onClick = { onBuy(qty) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F5F90)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            enabled = portBudget >= (qty * shipCost) && fleetCount < maxFleet
                        ) {
                            Text(text = "+$qty", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sell grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9FAFF), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFDEE2EB).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🚢− JUAL KAPAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF74777F))
                    Text(text = "${shortRpFormatter(shipSell)} / unit", fontSize = 10.sp, color = Color(0xFF44474E), fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 5, 10).forEach { qty ->
                        Button(
                            onClick = { onSell(qty) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1E2E9)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            enabled = fleetCount >= qty
                        ) {
                            Text(text = "−$qty", fontSize = 11.sp, color = Color(0xFF1B1B1F), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cash fund total
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFD9E2FF), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFADC6FF).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(text = "KAS OPERASIONAL PORT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E5D91))
                    Text(
                        text = rpFormatter.format(portBudget),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF001945)
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// SALARY COUPLER
// ──────────────────────────────────────────────
@Composable
fun SalaryControlSection(
    currentRatio: Double,
    onRatioSelected: (Double) -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, Color(0xFFDEE2EB), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "REGULASI BIROKRASI (GAJI STAF)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF74777F),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3F4F9), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(1.0 to "Standar", 0.8 to "Hemat", 0.6 to "Minimum").forEach { (ratio, label) ->
                    val isSelected = currentRatio == ratio
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .border(width = if (isSelected) 1.dp else 0.dp, color = if (isSelected) Color(0xFFDEE2EB) else Color.Transparent, shape = RoundedCornerShape(10.dp))
                            .clickable { onRatioSelected(ratio) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF3F5F90) else Color(0xFF74777F)
                            )
                            Text(
                                text = "${(ratio * 100).toInt()}% Gaji",
                                fontSize = 9.sp,
                                color = if (isSelected) Color(0xFF1E5D91) else Color(0xFF74777F).copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// PL METRICS (LABA-RUGI EXPOSURE)
// ──────────────────────────────────────────────
@Composable
fun PLMetricsSection(
    revenue: Double,
    fuel: Double,
    supplies: Double,
    maint: Double,
    opex: Double,
    netProfit: Double,
    cumProfit: Double,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    shortRpFormatter: (Double) -> String
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, Color(0xFFDEE2EB), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 LABA / RUGI BULANAN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF74777F)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Revenue
            PLItemRow(label = "Pendapatan Tangkapan", value = shortRpFormatter(revenue), color = Color(0xFF166534))

            // Extensible Opex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (if (expanded) "▼ " else "▶ ") + "Biaya Operasional (Opex)",
                    fontSize = 12.sp,
                    color = Color(0xFF44474E),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "- " + shortRpFormatter(opex),
                    fontSize = 12.sp,
                    color = Color(0xFFB91C1C),
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 2.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OpexSubRow(label = "⛽ BBM (Bahan Bakar Solar)", value = shortRpFormatter(fuel))
                    OpexSubRow(label = "🎣 Perbekalan / Air Logistik", value = shortRpFormatter(supplies))
                    OpexSubRow(label = "🔧 Rawatan Kapal", value = shortRpFormatter(maint))
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFDEE2EB))

            // Total profit summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Profit Bersih", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1F))
                Text(
                    text = shortRpFormatter(netProfit),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (netProfit >= 0.0) Color(0xFF3F5F90) else Color(0xFFB91C1C)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Total Akumulasi Terkumpul", fontSize = 10.sp, color = Color(0xFF74777F), fontWeight = FontWeight.Bold)
                Text(text = shortRpFormatter(cumProfit), fontSize = 11.sp, color = Color(0xFFDB2777), fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun PLItemRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF475569))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun OpexSubRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 10.sp, color = Color(0xFF94A3B8))
        Text(text = value, fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
    }
}

// ──────────────────────────────────────────────
// POPULATION GAUGE
// ──────────────────────────────────────────────
@Composable
fun PopulationGaugeSection(
    currentN: Double,
    carryingCapacity: Double,
    deltaN: Double,
    percentN: Int
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFD9E2FF)),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.border(1.dp, Color(0xFFADC6FF).copy(alpha = 0.5f), RoundedCornerShape(28.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🐟 BIOMASS KANDUNGAN LAUT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF001945),
                    letterSpacing = 1.sp
                )
                
                // Growth rate trends
                val trendText = when {
                    deltaN < -0.5 -> "▼ ${deltaN.format(1)} ton/bln"
                    deltaN > 0.5 -> "▲ ${deltaN.format(1)} ton/bln"
                    else -> "≈ stabil"
                }
                val trendColor = when {
                    deltaN < -0.5 -> Color(0xFFB91C1C)
                    deltaN > 0.5 -> Color(0xFF166534)
                    else -> Color(0xFF1E5D91)
                }

                Text(
                    text = trendText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = trendColor,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${currentN.format(0)} ton",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Light,
                    color = Color(0xFF001945),
                    letterSpacing = (-1.5).sp
                )
                Text(
                    text = "$percentN % Kapasitas",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E5D91),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Percentage bar: White/40 styled track, filled with primary #3F5F90
            val barFilledRatio = (percentN / 100f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = barFilledRatio)
                        .background(Color(0xFF3F5F90), RoundedCornerShape(3.dp))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Kapasitas Sehat Maksimal Daya Tampung Laut: ${carryingCapacity.format(0)} ton",
                fontSize = 11.sp,
                color = Color(0xFF1E5D91),
                fontWeight = FontWeight.Medium,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

// ──────────────────────────────────────────────
// WELFARE STATUS
// ──────────────────────────────────────────────
@Composable
fun WelfareMetricsSection(
    fisherWage: Double,
    adminWage: Double,
    umr: Double,
    adminSalaryTarget: Double,
    adminSalaryRatio: Double,
    rpFormatter: NumberFormat
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, Color(0xFFDEE2EB), RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Fishers Column
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "RATA UPAH NELAYAN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF74777F))
                Text(
                    text = rpFormatter.format(fisherWage),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (fisherWage >= umr) Color(0xFF166534) else Color(0xFFB91C1C)
                )
                Text(
                    text = if (fisherWage >= umr) "≥ UMR ✓" else "< UMR (Krisis) ✗",
                    fontSize = 9.sp,
                    color = if (fisherWage >= umr) Color(0xFF22C55E) else Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold
                )
            }

            // Vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(Color(0xFFDEE2EB))
            )

            // Admins Column
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "RATA GAJI STAF", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF74777F))
                Text(
                    text = rpFormatter.format(adminWage),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (adminWage >= (adminSalaryTarget * adminSalaryRatio)) Color(0xFF3F5F90) else Color(0xFFB91C1C)
                )
                Text(
                    text = if (adminWage >= adminSalaryTarget) "Lunas ✓" else "Terhutang ⚠",
                    fontSize = 9.sp,
                    color = if (adminWage >= adminSalaryTarget) Color(0xFF22C55E) else Color(0xFFF59E0B),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// PLAYBACK RUNNING ROW CONTROLS
// ──────────────────────────────────────────────
@Composable
fun PlaybackSection(
    isRunning: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onDemo: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!isRunning) {
            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F5F90)),
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "▶ MULAI", color = Color.White, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onPause,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1E2E9)),
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "⏸ PAUS", color = Color(0xFF1B1B1F), fontWeight = FontWeight.Bold)
            }
        }

        Button(
            onClick = onReset,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1E2E9)),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(text = "↺ RESET", color = Color(0xFF1B1B1F), fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onDemo,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD9E2FF)),
            modifier = Modifier.weight(1.2f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFADC6FF).copy(alpha = 0.5f))
        ) {
            Text(text = "👩‍🏫 TUTORIAL", color = Color(0xFF001945), fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
        }
    }
}

// ──────────────────────────────────────────────
// HISTORY SUMMARY TABLE
// ──────────────────────────────────────────────
@Composable
fun HistoryTableSection(
    runs: List<RunHistoryEntity>,
    carryingCapacity: Double,
    onClearAll: () -> Unit,
    rpShort: (Double) -> String
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, Color(0xFFDEE2EB), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "📋 RIWAYAT RUNS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "${runs.size} runs", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (runs.isNotEmpty()) {
                        Text(
                            text = "Hapus",
                            fontSize = 11.sp,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onClearAll() }
                                .padding(end = 12.dp)
                        )
                    }
                    Text(text = if (isExpanded) "▼" else "▶", fontSize = 12.sp, color = Color(0xFF94A3B8))
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                if (runs.isEmpty()) {
                    Text(
                        text = "Belum ada run tersimpan. Selesaikan setidaknya satu 120 bulan simulasi untuk merekam data ditiap skenario.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        runs.forEach { run ->
                            HistoryRowCard(run, carryingCapacity, rpShort)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryRowCard(
    run: RunHistoryEntity,
    carryingCapacity: Double,
    rpShort: (Double) -> String
) {
    var detailsOpen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { detailsOpen = !detailsOpen }
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(text = run.scenarioLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    Text(text = "${run.durationMonths} bln · Style: ${run.style.uppercase()}", fontSize = 9.sp, color = Color(0xFF94A3B8))
                }

                // Outcome pill badge
                val (label, bg, txtClr) = when {
                    run.collapsed -> Triple("KOLAPS", Color(0xFFFEE2E2), Color(0xFF991B1B))
                    run.finalPopPct in 40..65 -> Triple("OPTIMAL", Color(0xFFD1FAE5), Color(0xFF065F46))
                    else -> Triple("SUB-OPTIMAL", Color(0xFFFEF3C7), Color(0xFF92400E))
                }

                Box(
                    modifier = Modifier
                        .background(bg, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(text = label, fontSize = 8.sp, color = txtClr, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Biomass: ${run.finalPopTon} t (${run.finalPopPct}%)", fontSize = 10.sp, color = Color(0xFF475569), fontWeight = FontWeight.Medium)
                Text(text = "Profit: ${rpShort(run.cumulativeProfit)}", fontSize = 10.sp, color = Color(0xFFDB2777), fontWeight = FontWeight.Bold)
            }

            AnimatedVisibility(visible = detailsOpen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Text(text = "REFLEKSI SISWA:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text(
                        text = run.reflection,
                        fontSize = 11.sp,
                        color = Color(0xFF1E293B),
                        modifier = Modifier
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                            .fillMaxWidth(),
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "JEJAK KEPUTUSAN:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    
                    val eventsList = remember(run.eventsJson) {
                        try {
                            val arr = JSONArray(run.eventsJson)
                            (0 until arr.length()).mapNotNull { i ->
                                val obj = arr.getJSONObject(i)
                                val action = obj.optString("action", "")
                                if (action != "buy" && action != "sell") return@mapNotNull null
                                "Bulan ${obj.getInt("tick")}: ${action.uppercase()} ${obj.getInt("qty")} perahu (Sisa: ${obj.getInt("fleetAfter")})"
                            }
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }

                    if (eventsList.isEmpty()) {
                        Text(text = "Tidak ada jual/beli kapal yang tercatat.", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    } else {
                        eventsList.forEach { evt ->
                            Text(text = "• $evt", fontSize = 10.sp, color = Color(0xFF475569))
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// OVERLAYS & POPUPS
// ──────────────────────────────────────────────
@Composable
fun WelcomePopup(
    onStartTour: () -> Unit,
    onSkipTour: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "🎣", fontSize = 56.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Selamat datang di Knovera",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Kamu adalah manajer pelabuhan baru. 1.500 nelayan dan 20 staf administrasi bergantung pada keputusan regulasi armadamu. Mari jalani kelas pelatihan singkat terlebih dahulu.",
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onStartTour,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Mulai Pelatihan ➔", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onSkipTour) {
                    Text(text = "Langsung ke Sandbox", fontSize = 12.sp, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
fun YearEndSummaryPopup(
    year: Int,
    finalPop: Int,
    fleet: Int,
    netProfit: Double,
    portBudget: Double,
    fisherWage: Double,
    adminWage: Double,
    rpFormatter: NumberFormat,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A))
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)))
                    )
                    .padding(20.dp)
            ) {
                Text(
                    text = "📅 Akhir Tahun $year",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Atur kembali strategi armada baru anda lalu lanjutkan.",
                    fontSize = 11.sp,
                    color = Color(0xFF93C5FD),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Divider(color = Color(0xFF60A5FA).copy(alpha = 0.5f))

                // Stats breakdown
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PopupStatRow(label = "Biomass Ikan", value = "$finalPop Ton")
                    PopupStatRow(label = "Jumlah Armada", value = "$fleet Kapal")
                    PopupStatRow(label = "Laba Operasional", value = rpFormatter.format(netProfit))
                    PopupStatRow(label = "Sisa Kas Port", value = rpFormatter.format(portBudget))
                    PopupStatRow(label = "Upah Nelayan", value = rpFormatter.format(fisherWage))
                    PopupStatRow(label = "Upah Admin", value = rpFormatter.format(adminWage))
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "▶ Lanjutkan Langkah ke Tahun Depan", fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                }
            }
        }
    }
}

@Composable
fun PopupStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = Color(0xFFBFDBFE))
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

// Float formatting compatibility
private fun Double.format(decimals: Int): String {
    return String.format(Locale.US, "%.${decimals}f", this)
}
private fun Float.format(decimals: Int): String {
    return String.format(Locale.US, "%.${decimals}f", this)
}
