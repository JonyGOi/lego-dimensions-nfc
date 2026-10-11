package com.ldnfc.app

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ldnfc.app.ui.screens.AdvancedScreen
import com.ldnfc.app.ui.screens.CatalogScreen
import com.ldnfc.app.ui.screens.CreditsScreen
import com.ldnfc.app.ui.screens.HistoryScreen
import com.ldnfc.app.ui.screens.ReadBackupScreen
import com.ldnfc.app.ui.screens.WriteTagScreen
import com.ldnfc.app.ui.theme.LegoDimensionsNFCTheme
import com.ldnfc.app.ui.viewmodel.MainViewModel
import java.util.Locale

data class LanguageOption(val code: String, val displayName: String, val nativeName: String)

val SUPPORTED_LANGUAGES = listOf(
    LanguageOption("", "System Default", "Padrão do Sistema"),
    LanguageOption("en", "English", "English"),
    LanguageOption("pt", "Portuguese", "Português"),
    LanguageOption("es", "Spanish", "Español"),
    LanguageOption("fr", "French", "Français"),
    LanguageOption("de", "German", "Deutsch"),
    LanguageOption("it", "Italian", "Italiano"),
    LanguageOption("ru", "Russian", "Русский"),
    LanguageOption("ja", "Japanese", "日本語"),
    LanguageOption("zh", "Chinese (Simplified)", "简体中文"),
    LanguageOption("zh-rTW", "Chinese (Traditional)", "繁體中文"),
    LanguageOption("ko", "Korean", "한국어"),
    LanguageOption("nl", "Dutch", "Nederlands"),
    LanguageOption("pl", "Polish", "Polski"),
    LanguageOption("tr", "Turkish", "Türkçe"),
    LanguageOption("ar", "Arabic", "العربية"),
    LanguageOption("hi", "Hindi", "हिन्दी"),
    LanguageOption("uk", "Ukrainian", "Українська"),
    LanguageOption("vi", "Vietnamese", "Tiếng Việt"),
    LanguageOption("id", "Indonesian", "Bahasa Indonesia"),
    LanguageOption("cs", "Czech", "Čeština"),
    LanguageOption("sv", "Swedish", "Svenska"),
    LanguageOption("el", "Greek", "Ελληνικά"),
    LanguageOption("ro", "Romanian", "Română")
)

fun applyLocale(context: Context, langCode: String) {
    val locale = if (langCode.isEmpty()) {
        val sysLocales = androidx.core.os.ConfigurationCompat.getLocales(android.content.res.Resources.getSystem().configuration)
        if (!sysLocales.isEmpty) sysLocales[0] ?: Locale.getDefault() else Locale.getDefault()
    } else {
        if (langCode.contains("-r")) {
            val parts = langCode.split("-r")
            Locale(parts[0], parts[1])
        } else if (langCode.contains("-")) {
            val parts = langCode.split("-")
            Locale(parts[0], parts[1])
        } else {
            Locale(langCode)
        }
    }
    Locale.setDefault(locale)
    val resources = context.resources
    val config = resources.configuration
    config.setLocale(locale)
    context.createConfigurationContext(config)
    resources.updateConfiguration(config, resources.displayMetrics)
}

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private val viewModel: MainViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null
    private var currentRoute: String = "catalog"

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedLang = prefs.getString("app_language", "") ?: ""
        if (savedLang.isNotEmpty()) {
            applyLocale(this, savedLang)
        }

        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        viewModel.setNfcEnabledState(nfcAdapter?.isEnabled == true)

        setContent {
            LegoDimensionsNFCTheme {
                MainApp(
                    viewModel = viewModel,
                    onRouteChange = { route -> currentRoute = route },
                    onLanguageChanged = { newCode ->
                        prefs.edit().putString("app_language", newCode).apply()
                        applyLocale(this, newCode)
                        recreate()
                    }
                )
            }
        }

        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.setNfcEnabledState(nfcAdapter?.isEnabled == true)

        // Ativa o ReaderMode (método preferido e mais rápido no Android moderno)
        nfcAdapter?.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        tag?.let { onTagDiscovered(it) }
    }

    override fun onTagDiscovered(tag: Tag) {
        runOnUiThread {
            when (currentRoute) {
                "write" -> viewModel.onTagDiscoveredForWriting(tag)
                "read_backup" -> viewModel.onTagDiscoveredForReading(tag)
                else -> {
                    // Se estiver em outra tela, dispara leitura rápida ou notifica
                    viewModel.onTagDiscoveredForReading(tag)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: MainViewModel,
    onRouteChange: (String) -> Unit,
    onLanguageChanged: (String) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route ?: "catalog"
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val currentLang = remember { prefs.getString("app_language", "") ?: "" }

    onRouteChange(currentDestination)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "LEGO Dimensions NFC",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showLanguageDialog = true }) {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = androidx.compose.ui.res.stringResource(R.string.choose_language)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentDestination == "catalog",
                    onClick = { navController.navigate("catalog") { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text(androidx.compose.ui.res.stringResource(R.string.nav_catalog)) }
                )
                if (uiState.activeItemToWrite != null) {
                    NavigationBarItem(
                        selected = currentDestination == "write",
                        onClick = { navController.navigate("write") { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Nfc, contentDescription = null) },
                        label = { Text(androidx.compose.ui.res.stringResource(R.string.nav_write)) }
                    )
                }
                NavigationBarItem(
                    selected = currentDestination == "advanced",
                    onClick = { navController.navigate("advanced") { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.Build, contentDescription = null) },
                    label = { Text(androidx.compose.ui.res.stringResource(R.string.nav_advanced)) }
                )
                NavigationBarItem(
                    selected = currentDestination == "history",
                    onClick = { navController.navigate("history") { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text(androidx.compose.ui.res.stringResource(R.string.nav_history)) }
                )
                NavigationBarItem(
                    selected = currentDestination == "credits",
                    onClick = { navController.navigate("credits") { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text(androidx.compose.ui.res.stringResource(R.string.nav_credits)) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "catalog",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable("catalog") {
                CatalogScreen(
                    viewModel = viewModel,
                    onNavigateToWrite = {
                        navController.navigate("write")
                    }
                )
            }
            composable("write") {
                WriteTagScreen(
                    viewModel = viewModel,
                    onBackToCatalog = {
                        navController.navigate("catalog") {
                            popUpTo("catalog") { inclusive = true }
                        }
                    }
                )
            }
            composable("read_backup") {
                ReadBackupScreen(viewModel = viewModel)
            }
            composable("advanced") {
                AdvancedScreen(
                    viewModel = viewModel,
                    onNavigateToWrite = {
                        navController.navigate("write")
                    }
                )
            }
            composable("history") {
                HistoryScreen(viewModel = viewModel)
            }
            composable("credits") {
                CreditsScreen()
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.choose_language),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(SUPPORTED_LANGUAGES, key = { it.code }) { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showLanguageDialog = false
                                    onLanguageChanged(lang.code)
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentLang == lang.code,
                                onClick = {
                                    showLanguageDialog = false
                                    onLanguageChanged(lang.code)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = lang.nativeName,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = lang.displayName,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(androidx.compose.ui.res.stringResource(R.string.btn_close))
                }
            }
        )
    }
}
