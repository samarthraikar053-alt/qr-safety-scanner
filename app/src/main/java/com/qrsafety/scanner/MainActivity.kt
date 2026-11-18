package com.qrsafety.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.qrsafety.scanner.data.ScanHistoryRepository
import com.qrsafety.scanner.ui.screens.HistoryScreen
import com.qrsafety.scanner.ui.screens.ScannerScreen
import com.qrsafety.scanner.ui.screens.TestSamplesScreen
import com.qrsafety.scanner.ui.theme.DarkBackground
import com.qrsafety.scanner.ui.theme.QRSafetyScannerTheme

enum class AppDestination {
    SCANNER,
    HISTORY,
    TEST_SAMPLES
}

class MainActivity : ComponentActivity() {

    private lateinit var historyRepository: ScanHistoryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        historyRepository = ScanHistoryRepository(this)

        setContent {
            QRSafetyScannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    var currentDestination by remember { mutableStateOf(AppDestination.SCANNER) }
                    var preselectedUrl by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(Unit) {
                        historyRepository.refresh()
                    }

                    when (currentDestination) {
                        AppDestination.SCANNER -> {
                            ScannerScreen(
                                repository = historyRepository,
                                onNavigateToHistory = {
                                    currentDestination = AppDestination.HISTORY
                                },
                                onNavigateToTestSamples = {
                                    currentDestination = AppDestination.TEST_SAMPLES
                                },
                                preselectedTestUrl = preselectedUrl,
                                onClearPreselectedTestUrl = {
                                    preselectedUrl = null
                                }
                            )
                        }
                        AppDestination.HISTORY -> {
                            HistoryScreen(
                                repository = historyRepository,
                                onBack = { currentDestination = AppDestination.SCANNER },
                                onSelectRecord = { url ->
                                    preselectedUrl = url
                                    currentDestination = AppDestination.SCANNER
                                }
                            )
                        }
                        AppDestination.TEST_SAMPLES -> {
                            TestSamplesScreen(
                                onBack = { currentDestination = AppDestination.SCANNER },
                                onTestUrlSelected = { url ->
                                    preselectedUrl = url
                                    currentDestination = AppDestination.SCANNER
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
