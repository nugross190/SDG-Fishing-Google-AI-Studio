package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.RunHistoryEntity
import com.example.data.model.StageProgressEntity
import com.example.data.repository.SimulationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

// Structure to hold the current parameter state
data class SimParams(
    val P: Double = 1500000.0,      // Price per ton
    val c: Double = 600000.0,       // Cost per unit effort
    val r: Double = 0.4,            // Growth rate
    val K: Double = 450000.0        // Carrying capacity
)

// Current UI state of the simulation
data class SimulationState(
    val activeScenario: String = "normal",
    val currentN: Double = 450000.0,
    val r: Double = 0.4,
    val K: Double = 450000.0,
    val P: Double = 1500000.0,
    val c: Double = 600000.0,
    val fleet: Int = 4,
    val portBudget: Double = 250000000.0,
    val cumulativeProfit: Double = 0.0,
    val npv: Double = 0.0,
    val currentTick: Int = 0,
    val isRunning: Boolean = false,
    val isYearEndPaused: Boolean = false,
    val forceYearPause: Boolean = true,
    val adminSalaryRatio: Double = 1.0,
    
    // Last tick metrics
    val lastRevenue: Double = 0.0,
    val lastFuel: Double = 0.0,
    val lastSupplies: Double = 0.0,
    val lastMaint: Double = 0.0,
    val lastOpex: Double = 0.0,
    val lastNetProfit: Double = 0.0,
    val lastDeltaN: Double = 0.0,
    val lastMonthlyFisherWage: Double = 0.0,
    val lastMonthlyAdminWage: Double = 0.0,

    // Charts series
    val popHistory: List<Double> = emptyList(),
    val fisherWageHistory: List<Double> = emptyList(),
    val adminWageHistory: List<Double> = emptyList(),

    // Game variables
    val peakFleet: Int = 4,
    val runEvents: List<String> = emptyList(),
    val runEventsDetailed: List<Map<String, Any>> = emptyList(),

    // Pedagogy dialog state
    val activePedagogyTrigger: Map<String, Any>? = null,
    val isPedagogyActive: Boolean = false,

    // Tutorial state
    val isTutorialMode: Boolean = false,
    val tutorialStep: Int = 0,
    val inputSentence: List<String> = emptyList(), // words chosen by student

    // Admin override params
    val overrideActive: Boolean = false
)

// Metadata for Scenarios
data class ScenarioInfo(
    val key: String,
    val label: String,
    val banner: String,
    val twist: String,
    val params: SimParams,
    val initFleet: Int,
    val initBudget: Double,
    val startNRatio: Double
)

class SimulationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SimulationRepository
    private var simJob: Job? = null
    private var tickSpeedMs: Long = 300L

    // Constants matches HTML exactly
    val J_FISHERMEN = 1500
    val UMR_MONTHLY_DEFAULT = 2500000.0
    val J_ADMIN = 20
    val ADMIN_SALARY_DEFAULT = 2500000.0
    val MGMT_CUT = 0.07
    val MAX_FLEET = 200
    val SHIP_COST = 100000000.0
    val SHIP_SELL = 40000000.0
    val SHIP_MAINT_ANNUAL = 20000000.0
    val HARBOR_FEE_PER_SHIP_MONTHLY = 7841667.0
    val q = 0.00008
    val EFFORT_PER_SHIP_ANNUAL = 1000.0
    val SUPPLIES_RATE = 240000.0
    val delta = 0.05
    val maxTicks = 120

    private val firedTriggers = mutableSetOf<String>()

    val scenarios = mapOf(
        "normal" to ScenarioInfo(
            key = "normal",
            label = "Kondisi Normal",
            banner = "Laut sehat, harga ikan normal, biaya operasional wajar. Mulai dengan 4 kapal — eksplorasi bebas, temukan polanya.",
            twist = "Baseline — semua parameter default",
            params = SimParams(P = 1500000.0, c = 600000.0, r = 0.4, K = 450000.0),
            initFleet = 4,
            initBudget = 250000000.0,
            startNRatio = 1.0
        ),
        "bbm" to ScenarioInfo(
            key = "bbm",
            label = "Krisis BBM",
            banner = "Harga solar melonjak. Biaya melaut hampir dua kali lipat. Cari titik di mana melaut masih menguntungkan.",
            twist = "c naik: Rp 600rb → Rp 1.1jt per unit effort",
            params = SimParams(P = 1500000.0, c = 1100000.0, r = 0.4, K = 450000.0),
            initFleet = 4,
            initBudget = 250000000.0,
            startNRatio = 1.0
        ),
        "elnino_bbm" to ScenarioInfo(
            key = "elnino_bbm",
            label = "El Niño + Krisis BBM",
            banner = "Dua tekanan sekaligus: harga solar masih tinggi, dan anomali iklim El Niño membuat ikan berkembang biak jauh lebih lambat.",
            twist = "r turun ke 0.2 (El Niño) + c naik ke Rp 1.1jt (BBM)",
            params = SimParams(P = 1500000.0, c = 1100000.0, r = 0.2, K = 450000.0),
            initFleet = 4,
            initBudget = 250000000.0,
            startNRatio = 1.0
        ),
        "overfished" to ScenarioInfo(
            key = "overfished",
            label = "Warisan Overfishing",
            banner = "Pendahulu meninggalkan pelabuhan dengan 50 kapal di laut yang hanya tersisa 20% populasinya. Pulihkan, atau panen sebelum habis?",
            twist = "Mulai N = 20% K, armada awal 50 kapal, kas terbatas",
            params = SimParams(P = 1500000.0, c = 600000.0, r = 0.4, K = 450000.0),
            initFleet = 50,
            initBudget = 100000000.0,
            startNRatio = 0.2
        )
    )

    private val _uiState = MutableStateFlow(SimulationState())
    val uiState: StateFlow<SimulationState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = SimulationRepository(database.runHistoryDao(), database.stageProgressDao())
        
        // Load initial state for normal scenario
        resetSimulation()
    }

    // Combine Room tables with our StateFlow to present data reactively
    val allRuns: StateFlow<List<RunHistoryEntity>> = repository.allRuns.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val stageProgressList: StateFlow<List<StageProgressEntity>> = repository.allProgress.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun changeScenario(scenarioKey: String) {
        _uiState.update { it.copy(activeScenario = scenarioKey) }
        resetSimulation()
    }

    fun startPlaying() {
        val state = _uiState.value
        if (state.isRunning) return
        
        if (state.isTutorialMode && state.tutorialStep == 15) {
            _uiState.update { it.copy(tutorialStep = 2) }
        }
        
        _uiState.update { it.copy(isRunning = true, isYearEndPaused = false) }
        simJob = viewModelScope.launch(Dispatchers.Default) {
            while (_uiState.value.isRunning) {
                delay(tickSpeedMs)
                // Execute on main thread to perform UI additions safely
                viewModelScope.launch(Dispatchers.Main) {
                    stepMonth()
                }
            }
        }
    }

    fun pausePlaying() {
        _uiState.update { it.copy(isRunning = false) }
        simJob?.cancel()
    }

    fun toggleYearPause(isEnabled: Boolean) {
        _uiState.update { it.copy(forceYearPause = isEnabled) }
    }

    fun resetSimulation() {
        simJob?.cancel()
        firedTriggers.clear()
        
        val currentScenarioKey = _uiState.value.activeScenario
        val config = scenarios[currentScenarioKey] ?: scenarios["normal"]!!
        val startN = config.params.K * config.startNRatio

        _uiState.update { state ->
            state.copy(
                currentN = startN,
                r = config.params.r,
                K = config.params.K,
                P = config.params.P,
                c = config.params.c,
                fleet = config.initFleet,
                portBudget = config.initBudget,
                cumulativeProfit = 0.0,
                npv = 0.0,
                currentTick = 0,
                isRunning = false,
                isYearEndPaused = false,
                adminSalaryRatio = 1.0,
                
                lastRevenue = 0.0,
                lastFuel = 0.0,
                lastSupplies = 0.0,
                lastMaint = 0.0,
                lastOpex = 0.0,
                lastNetProfit = 0.0,
                lastDeltaN = 0.0,
                lastMonthlyFisherWage = 0.0,
                lastMonthlyAdminWage = 0.0,
                
                popHistory = listOf(startN),
                fisherWageHistory = listOf(0.0),
                adminWageHistory = listOf(0.0),
                
                peakFleet = config.initFleet,
                runEvents = emptyList(),
                runEventsDetailed = emptyList(),
                activePedagogyTrigger = null,
                isPedagogyActive = false,
                isTutorialMode = false,
                tutorialStep = 0,
                inputSentence = emptyList(),
                overrideActive = false
            )
        }
        tickSpeedMs = 300L
    }

    // Admin direct override tuning
    fun applyAdminOverrides(
        r: Double, K: Double, q_override: Double, P: Double, c: Double,
        initFleet: Int, initBudget: Double, startNRatio: Double,
        harborFee: Double, shipCost: Double, shipSell: Double, shipMaint: Double, effortPerShip: Double,
        salaryRatio: Double
    ) {
        simJob?.cancel()
        _uiState.update { state ->
            val finalStartN = K * startNRatio
            state.copy(
                r = r,
                K = K,
                P = P,
                c = c,
                fleet = initFleet,
                portBudget = initBudget,
                currentN = finalStartN,
                cumulativeProfit = 0.0,
                npv = 0.0,
                currentTick = 0,
                isRunning = false,
                isYearEndPaused = false,
                adminSalaryRatio = salaryRatio,
                
                popHistory = listOf(finalStartN),
                fisherWageHistory = listOf(0.0),
                adminWageHistory = listOf(0.0),
                overrideActive = true
            )
        }
    }

    fun buyShips(qty: Int) {
        val state = _uiState.value
        val maxByBudget = (state.portBudget / SHIP_COST).toInt()
        val maxByCap = MAX_FLEET - state.fleet
        val finalQty = qty.coerceAtMost(maxByBudget).coerceAtMost(maxByCap)
        
        if (finalQty <= 0) return

        val cost = finalQty * SHIP_COST
        val isTutorialStep1 = state.isTutorialMode && state.tutorialStep == 1

        _uiState.update { current ->
            val updatedFleet = current.fleet + finalQty
            val detailedEvent = mapOf(
                "tick" to current.currentTick,
                "year" to (current.currentTick / 12.0),
                "action" to "buy",
                "qty" to finalQty,
                "fleetAfter" to updatedFleet,
                "popTon" to current.currentN.toInt(),
                "popPct" to (current.currentN / current.K * 100.0).toInt(),
                "cumProfit" to current.cumulativeProfit.toInt()
            )
            val newTutorialStep = if (isTutorialStep1 && updatedFleet > 15) 15 else current.tutorialStep
            current.copy(
                fleet = updatedFleet,
                portBudget = current.portBudget - cost,
                peakFleet = current.peakFleet.coerceAtLeast(updatedFleet),
                runEvents = current.runEvents + "Bulan ${current.currentTick}: Beli $finalQty Kapal (Total: $updatedFleet)",
                runEventsDetailed = current.runEventsDetailed + detailedEvent,
                tutorialStep = newTutorialStep
            )
        }
    }

    fun sellShips(qty: Int) {
        val state = _uiState.value
        val finalQty = qty.coerceAtMost(state.fleet)
        
        if (finalQty <= 0) return

        val proceeds = finalQty * SHIP_SELL
        _uiState.update { current ->
            val updatedFleet = current.fleet - finalQty
            val detailedEvent = mapOf(
                "tick" to current.currentTick,
                "year" to (current.currentTick / 12.0),
                "action" to "sell",
                "qty" to finalQty,
                "fleetAfter" to updatedFleet,
                "popTon" to current.currentN.toInt(),
                "popPct" to (current.currentN / current.K * 100.0).toInt(),
                "cumProfit" to current.cumulativeProfit.toInt()
            )
            current.copy(
                fleet = updatedFleet,
                portBudget = current.portBudget + proceeds,
                runEvents = current.runEvents + "Bulan ${current.currentTick}: Jual $finalQty Kapal (Sisa: $updatedFleet)",
                runEventsDetailed = current.runEventsDetailed + detailedEvent
            )
        }
    }

    fun changeAdminSalaryRatio(ratio: Double) {
        _uiState.update { it.copy(adminSalaryRatio = ratio) }
    }

    private fun stepMonth() {
        val state = _uiState.value
        if (!state.isRunning) return

        val nextTick = state.currentTick + 1
        val currentYear = nextTick / 12.0

        val rMonthly = state.r / 12.0
        val qMonthly = q / 12.0
        val shipMaintMonthly = SHIP_MAINT_ANNUAL / 12.0

        val E = state.fleet * (EFFORT_PER_SHIP_ANNUAL / 12.0)
        val growth = rMonthly * state.currentN * (1.0 - state.currentN / state.K)
        val yield_t = qMonthly * E * state.currentN

        val updatedN = (state.currentN + growth - yield_t).coerceAtLeast(0.0)

        // Financial mechanics
        val revenue = state.P * yield_t
        val supRate = SUPPLIES_RATE.coerceAtMost(state.c)
        val fuelRate = state.c - supRate
        val fuelCost = fuelRate * E
        val supCost = supRate * E
        val maintCost = state.fleet * shipMaintMonthly
        val opex = fuelCost + supCost + maintCost
        val profit = revenue - opex
        val discounted = profit / Math.pow(1.0 + delta, currentYear)

        val updatedCumulativeProfit = state.cumulativeProfit + profit
        val updatedNpv = state.npv + discounted

        val tax = if (profit > 0.0) profit * MGMT_CUT else 0.0
        val adminCost = J_ADMIN * ADMIN_SALARY_DEFAULT * state.adminSalaryRatio
        val harborCost = state.fleet * HARBOR_FEE_PER_SHIP_MONTHLY
        val fleetFund = (tax - adminCost - harborCost).coerceAtLeast(0.0)

        val updatedPortBudget = state.portBudget + fleetFund

        // Welfare
        val perFisherWage = if (profit > 0.0) (profit * (1.0 - MGMT_CUT)) / J_FISHERMEN else 0.0
        val perAdminWage = if (tax > harborCost) {
            (ADMIN_SALARY_DEFAULT * state.adminSalaryRatio).coerceAtMost((tax - harborCost) / J_ADMIN)
        } else {
            0.0
        }

        val updatedPops = state.popHistory + updatedN
        val updatedFisherWages = state.fisherWageHistory + perFisherWage
        val updatedAdminWages = state.adminWageHistory + perAdminWage

        _uiState.update { current ->
            current.copy(
                currentN = updatedN,
                currentTick = nextTick,
                cumulativeProfit = updatedCumulativeProfit,
                npv = updatedNpv,
                portBudget = updatedPortBudget,
                
                lastRevenue = revenue,
                lastFuel = fuelCost,
                lastSupplies = supCost,
                lastMaint = maintCost,
                lastOpex = opex,
                lastNetProfit = profit,
                lastDeltaN = growth - yield_t,
                lastMonthlyFisherWage = perFisherWage,
                lastMonthlyAdminWage = perAdminWage,
                
                popHistory = updatedPops,
                fisherWageHistory = updatedFisherWages,
                adminWageHistory = updatedAdminWages
            )
        }

        // Check if we are in tutorial story mode
        if (state.isTutorialMode) {
            checkTutorialStoryProgress()
        } else {
            // Standard pedagogy checkpoints
            checkPedagogicalTriggers(prevPopRatio = state.currentN / state.K, prevFleet = state.fleet, prevProfit = state.lastNetProfit, prevAdminWage = state.lastMonthlyAdminWage)
        }

        // Check for run end conditions or year end pause
        if (nextTick >= maxTicks || updatedN <= 0.0) {
            endSimulationRun()
        } else if (nextTick % 12 == 0 && state.forceYearPause) {
            pausePlaying()
            _uiState.update { it.copy(isYearEndPaused = true) }
        }
    }

    fun dismissYearEndProgress() {
        val state = _uiState.value
        _uiState.update { it.copy(isYearEndPaused = false) }
        // User must manually hit PLAY again, giving them a chance to buy ships at year-end.
    }

    private fun endSimulationRun() {
        pausePlaying()
        // Open the reflection sheet
        _uiState.update { it.copy(isPedagogyActive = false) }
    }

    fun submitReflectionAndSave(text: String) {
        val state = _uiState.value
        val popPct = (state.currentN / state.K * 100.0).toInt()
        
        // Classify the outcome style
        val style = classifyRunArchetype(
            finalPopPct = popPct,
            peakFleet = state.peakFleet,
            finalFleet = state.fleet,
            cumProfit = state.cumulativeProfit,
            isCollapsed = state.currentN <= 0.0,
            eventsCount = state.runEventsDetailed.size,
            scenarioKey = state.activeScenario
        )

        // Convert series to quick structures
        val yearlyPopSeries = mutableListOf<Map<String, Int>>()
        state.popHistory.forEachIndexed { index, value ->
            if (index % 12 == 0) {
                yearlyPopSeries.add(mapOf("year" to (index / 12), "popTon" to value.toInt()))
            }
        }
        val seriesJson = JSONArray(yearlyPopSeries.map { JSONObject(it) }).toString()
        val eventsJson = JSONArray(state.runEventsDetailed.map { JSONObject(it) }).toString()

        val run = RunHistoryEntity(
            scenarioKey = state.activeScenario,
            scenarioLabel = scenarios[state.activeScenario]?.label ?: "Unknown",
            durationMonths = state.currentTick,
            finalPopPct = popPct,
            finalPopTon = state.currentN.toInt(),
            cumulativeProfit = state.cumulativeProfit,
            npv = state.npv,
            adminRatio = state.adminSalaryRatio,
            reflection = text,
            peakFleet = state.peakFleet,
            finalFleet = state.fleet,
            collapsed = state.currentN <= 0.0 || state.currentTick < maxTicks,
            popSeriesJson = seriesJson,
            eventsJson = eventsJson,
            style = style
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.insertRun(run)
            
            // Mark stage completed
            repository.completeStage(state.activeScenario)

            // Unlock next stage if any
            val STAGE_ORDER = listOf("normal", "bbm", "elnino_bbm", "overfished")
            val index = STAGE_ORDER.indexOf(state.activeScenario)
            if (index != -1 && index < STAGE_ORDER.size - 1) {
                val nextStage = STAGE_ORDER[index + 1]
                repository.unlockStage(nextStage)
            }
        }
        resetSimulation()
    }

    private fun classifyRunArchetype(
        finalPopPct: Int, peakFleet: Int, finalFleet: Int,
        cumProfit: Double, isCollapsed: Boolean, eventsCount: Int, scenarioKey: String
    ): String {
        val initFleet = scenarios[scenarioKey]?.initFleet ?: 4
        if (isCollapsed || finalPopPct < 25) return "greedy"
        if (eventsCount >= 8) return "chaotic"
        if (finalPopPct > 70 && peakFleet <= initFleet + 3) return "overconserving"
        if (finalPopPct in 40..65 && cumProfit > 0) return "balanced"
        if (finalPopPct < 40) return "greedy"
        return "overconserving"
    }

    private fun checkPedagogicalTriggers(prevPopRatio: Double, prevFleet: Int, prevProfit: Double, prevAdminWage: Double) {
        val state = _uiState.value
        val popRatio = state.currentN / state.K

        // Trigger 1: Low population (<30% or <75%)
        if (popRatio < 0.75 && prevPopRatio >= 0.75 && !firedTriggers.contains("pop_declining")) {
            firedTriggers.add("pop_declining")
            triggerPedagogyPrompt(
                id = "pop_declining",
                badge = "Pengamatan · Populasi Ikan",
                emoji = "📉",
                title = "Populasi Ikan Mulai Turun",
                body = "Populasi ikan baru saja melewati batas 75% kapasitas laut — sekarang di <b>${(popRatio * 100).toInt()}%</b>. Apa yang paling mungkin menjadi penyebab utama penurunan ini?",
                type = "mc",
                options = listOf(
                    "Tangkapan melebihi kemampuan laut untuk memulihkan diri" to true,
                    "Harga ikan turun sehingga nelayan menangkap lebih banyak" to false,
                    "Biaya operasional terlalu tinggi sehingga ada insentif over-fishing" to false,
                    "Gaji admin menguras kas sehingga tidak ada dana perawatan laut" to false
                ),
                feedbackCorrect = "✓ Tepat. Populasi turun ketika tangkapan (yield) lebih besar dari pertumbuhan alami. Coba kurangi armada.",
                feedbackWrong = "× Salah. Populasi turun karena tangkapan total melebihi laju biomassa alami laut."
            )
        }

        // Trigger 2: MSY Zone (around 50% capacity)
        if (popRatio in 0.44..0.55 && prevPopRatio > 0.55 && !firedTriggers.contains("msy_zone")) {
            firedTriggers.add("msy_zone")
            triggerPedagogyPrompt(
                id = "msy_zone",
                badge = "Konsep Kunci · MSY",
                emoji = "🐟",
                title = "Zona Maximum Sustainable Yield",
                body = "Populasi ikan kini sekitar 50% kapasitas laut. Para ilmuwan menyebut titik ini Maximum Sustainable Yield (MSY). Mengapa titik ini penting?",
                type = "mc",
                options = listOf(
                    "Di titik ini ikan berkembang biak paling cepat — panen bisa berkelanjutan" to true,
                    "Di titik ini kapal paling efisien menangkap ikan" to false,
                    "Di titik ini profit pelabuhan otomatis paling tinggi" to false,
                    "Di titik ini biaya operasional per ton ikan paling rendah" to false
                ),
                feedbackCorrect = "✓ Tepat sekali! Pertumbuhan alami r * N * (1 - N/K) mencapai puncaknya pada N = K/2.",
                feedbackWrong = "× Salah. Di titik 50% biomassa ketersediaan regenerasi laut adalah yang tertinggi."
            )
        }

        // Trigger 3: Collapsed Zone (<30% population)
        if (popRatio < 0.30 && prevPopRatio >= 0.30 && !firedTriggers.contains("critical_pop")) {
            firedTriggers.add("critical_pop")
            triggerPedagogyPrompt(
                id = "critical_pop",
                badge = "Peringatan Kritis · Kolaps",
                emoji = "🚨",
                title = "Populasi Di Bawah 30% — Zona Kritis!",
                body = "Laut kita berada di zona kritis. Regenerasi melambat secara ekstrem. Tindakan darurat apa yang harus diambil?",
                type = "mc",
                options = listOf(
                    "Kurangi atau hentikan sementara operasi kapal agar laut bisa pulih" to true,
                    "Tambah kapal agar pendapatan cukup menutupi kerugian" to false,
                    "Pertahankan armada — tunggu harga ikan naik" to false,
                    "Biarkan saja karena alam pasti memulihkan dirinya sendiri" to false
                ),
                feedbackCorrect = "✓ Sangat tepat. Menurunkan upaya penangkapan adalah satu-satunya cara penyelamatan stok saat kritis.",
                feedbackWrong = "× Menambah atau membiarkan armada berisiko merusak ekosistem secara permanen!"
            )
        }

        // Trigger 4: First Loss
        if (state.lastNetProfit < 0.0 && prevProfit >= 0.0 && !firedTriggers.contains("first_loss")) {
            firedTriggers.add("first_loss")
            triggerPedagogyPrompt(
                id = "first_loss",
                badge = "Kondisi Keuangan · Defisit",
                emoji = "💸",
                title = "Operasional Merugi Bulan Ini",
                body = "Biaya operasional (BBM, logistik, perawatan) bulan ini melebihi hasil penjualan. Siapa yang paling berdampak dan apa tindakanmu?",
                type = "open"
            )
        }
    }

    private fun triggerPedagogyPrompt(
        id: String, badge: String, emoji: String, title: String, body: String,
        type: String, options: List<Pair<String, Boolean>> = emptyList(),
        feedbackCorrect: String = "", feedbackWrong: String = ""
    ) {
        pausePlaying()
        val m = mapOf(
            "id" to id,
            "badge" to badge,
            "emoji" to emoji,
            "title" to title,
            "body" to body,
            "type" to type,
            "options" to options,
            "feedbackCorrect" to feedbackCorrect,
            "feedbackWrong" to feedbackWrong
        )
        _uiState.update { it.copy(activePedagogyTrigger = m, isPedagogyActive = true) }
    }

    fun submitPedagogyAnswer(choiceIndex: Int?, openText: String?) {
        val currentTrigger = _uiState.value.activePedagogyTrigger ?: return
        val type = currentTrigger["type"] as String
        val id = currentTrigger["id"] as String

        _uiState.update { current ->
            val detailedEvent = mapOf(
                "tick" to current.currentTick,
                "type" to "pedagogy_answered",
                "id" to id,
                "choice" to (choiceIndex ?: -1),
                "openAnswer" to (openText ?: "")
            )
            current.copy(
                isPedagogyActive = false,
                activePedagogyTrigger = null,
                runEventsDetailed = current.runEventsDetailed + detailedEvent
            )
        }
    }

    // ── TUTORIAL MODE STORY ──
    fun startTutorialStory() {
        resetSimulation()
        _uiState.update { state ->
            state.copy(
                isTutorialMode = true,
                tutorialStep = 1,
                fleet = 4,
                peakFleet = 4,
                isRunning = false,
                forceYearPause = true
            )
        }
        tickSpeedMs = 250L
    }

    private fun checkTutorialStoryProgress() {
        val state = _uiState.value
        val popRatio = state.currentN / state.K

        if (state.tutorialStep == 20 && popRatio < 0.70) {
            pausePlaying()
            _uiState.update { it.copy(tutorialStep = 3) }
        } else if ((state.tutorialStep == 3 || state.tutorialStep == 20 || state.tutorialStep == 21) && state.lastNetProfit < 0.0) {
            pausePlaying()
            _uiState.update { it.copy(tutorialStep = 4) }
        }
    }

    fun advanceTutorialStep() {
        val step = _uiState.value.tutorialStep
        if (step == 2) {
            _uiState.update { it.copy(tutorialStep = 20) }
        } else if (step == 3) {
            _uiState.update { it.copy(tutorialStep = 21, isRunning = true) }
            startPlaying()
        } else if (step == 4) {
            // Transition to mentor dialogue
            _uiState.update { it.copy(tutorialStep = 5) }
        } else if (step == 5) {
            // Complete tutorial and unlock sandbox
            _uiState.update { it.copy(isTutorialMode = false, tutorialStep = 0) }
            resetSimulation()
        }
    }


    // Cohort Data Generation for Sandbox Analysis (Uji Agent feature)
    fun generateCohorts(count: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            val historySeries = mutableListOf<RunHistoryEntity>()
            
            val styles = listOf("Greedy — Terlalu Agresif", "Balanced — Berkelanjutan", "Over-Conserving — Terlalu Hati-hati")
            val scenariosKeys = listOf("normal", "bbm", "elnino_bbm", "overfished")
            
            for (i in 1..count) {
                scenariosKeys.forEach { scKey ->
                    styles.forEach { style ->
                        val duration = 120
                        val valPop = if (style.contains("Greedy")) 50000 else if (style.contains("Balanced")) 210000 else 400000
                        val finalPopPct = (valPop / 450000.0 * 100.0).toInt()
                        val profit = if (style.contains("Greedy")) 450000000.0 else if (style.contains("Balanced")) 1500000000.0 else 200000000.0
                        
                        historySeries.add(
                            RunHistoryEntity(
                                scenarioKey = scKey,
                                scenarioLabel = scenarios[scKey]?.label ?: "",
                                durationMonths = duration,
                                finalPopPct = finalPopPct,
                                finalPopTon = valPop,
                                cumulativeProfit = profit,
                                npv = profit * 0.8,
                                adminRatio = 1.0,
                                reflection = "Hasil simulasi cohort dummy ($style) untuk analisis data pemetaan.",
                                peakFleet = if (style.contains("Greedy")) 60 else if (style.contains("Balanced")) 15 else 5,
                                finalFleet = if (style.contains("Greedy")) 50 else if (style.contains("Balanced")) 12 else 4,
                                collapsed = style.contains("Greedy"),
                                popSeriesJson = "[]",
                                eventsJson = "[]",
                                style = if (style.contains("Greedy")) "greedy" else if (style.contains("Balanced")) "balanced" else "overconserving"
                            )
                        )
                    }
                }
            }
            
            historySeries.forEach { repository.insertRun(it) }
        }
    }

    fun clearAllRuns() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllRuns()
        }
    }

    fun resetWholeAppProgress() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllRuns()
            repository.resetProgress()
        }
        resetSimulation()
    }
}
