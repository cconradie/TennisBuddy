package com.tennisbuddy.android

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tennisbuddy.shared.MatchFormat
import com.tennisbuddy.shared.MatchRecord
import com.tennisbuddy.shared.Player
import com.tennisbuddy.shared.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

enum class Screen(val title: String) {
    HOME("TennisBuddy"),
    SETUP("New match"),
    MATCH("Match"),
    REGISTER("Match register"),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Green app bar behind the status bar, so keep its icons light.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT))
        setContent {
            TennisBuddyTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TennisBuddyApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TennisBuddyApp(vm: MatchViewModel = viewModel()) {
    val controller = vm.controller
    val data by controller.data.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun go(target: Screen) {
        screen = target
        scope.launch { drawerState.close() }
    }

    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    val current = data.current
    val resumable = current != null && !current.isFinished

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = TennisGreen, drawerContentColor = Color.White) {
                DrawerHeader()
                DrawerItem("Home", screen == Screen.HOME) { go(Screen.HOME) }
                DrawerItem("New match", screen == Screen.SETUP) { go(Screen.SETUP) }
                if (resumable) {
                    DrawerItem("Resume match", screen == Screen.MATCH) { go(Screen.MATCH) }
                }
                DrawerItem("Match register", screen == Screen.REGISTER) { go(Screen.REGISTER) }
            }
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(screen.title, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = TennisGreen,
                        titleContentColor = TennisYellow,
                        navigationIconContentColor = TennisYellow,
                    ),
                )
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        resumeLabel = if (resumable) {
                            "Resume: ${current!!.names(Player.A)} vs ${current.names(Player.B)}"
                        } else {
                            null
                        },
                        onNewMatch = { go(Screen.SETUP) },
                        onResume = { go(Screen.MATCH) },
                        onRegister = { go(Screen.REGISTER) },
                    )

                    Screen.SETUP -> SetupScreen(
                        onStart = { format, sideA, sideB ->
                            controller.startMatch(format, sideA, sideB)
                            go(Screen.MATCH)
                        },
                        onCancel = { go(Screen.HOME) },
                    )

                    Screen.MATCH -> if (current == null) {
                        LaunchedEffect(Unit) { screen = Screen.HOME }
                    } else {
                        MatchScreen(
                            record = current,
                            onPoint = controller::pointWonBy,
                            onUndo = controller::undo,
                            onClose = {
                                controller.closeMatch()
                                go(Screen.HOME)
                            },
                        )
                    }

                    Screen.REGISTER -> RegisterScreen(data.history, onDelete = controller::deleteRecord)
                }
            }
        }
    }
}

@Composable
private fun DrawerHeader() {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painterResource(R.drawable.tennisbuddy_logo),
            contentDescription = "TennisBuddy logo",
            modifier = Modifier.size(72.dp),
        )
        Text("TennisBuddy", style = MaterialTheme.typography.titleLarge, color = TennisYellow)
    }
}

@Composable
private fun DrawerItem(label: String, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = TennisYellow,
            selectedTextColor = TennisGreenDark,
            unselectedContainerColor = Color.Transparent,
            unselectedTextColor = Color.White,
        ),
    )
}

/** Yellow button with dark green text: the app's main action style. */
@Composable
private fun YellowButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = TennisYellow,
            contentColor = TennisGreenDark,
            disabledContainerColor = TennisGreenLight,
            disabledContentColor = TennisGreen,
        ),
    ) { Text(text, fontWeight = FontWeight.Bold) }
}

@Composable
fun HomeScreen(
    resumeLabel: String?,
    onNewMatch: () -> Unit,
    onResume: () -> Unit,
    onRegister: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painterResource(R.drawable.tennisbuddy_logo),
            contentDescription = "TennisBuddy logo",
            modifier = Modifier.size(140.dp),
        )
        if (resumeLabel != null) {
            YellowButton(resumeLabel, Modifier.fillMaxWidth(), onClick = onResume)
        }
        Button(onClick = onNewMatch, modifier = Modifier.fillMaxWidth()) { Text("New match") }
        OutlinedButton(onClick = onRegister, modifier = Modifier.fillMaxWidth()) { Text("Match register") }
    }
}

@Composable
fun RegisterScreen(history: List<MatchRecord>, onDelete: (String) -> Unit) {
    if (history.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No matches yet. Finished matches are saved here.", textAlign = TextAlign.Center)
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Box(Modifier.size(1.dp)) }
        items(history, key = { it.id }) { record ->
            HistoryCard(record, onDelete = { onDelete(record.id) })
        }
        item { Box(Modifier.size(16.dp)) }
    }
}

@Composable
fun HistoryCard(record: MatchRecord, onDelete: () -> Unit) {
    val whenText = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(record.startedAtMillis))
    val result = record.state.winner?.let { "Winner: ${record.names(it)}" } ?: "Ended early"
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TennisGreenLight, contentColor = TennisGreenDark),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${record.names(Player.A)} vs ${record.names(Player.B)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text("${record.format.label()} · $whenText")
            Text("Score (sets): ${record.scoreSummary()}", style = MaterialTheme.typography.bodyLarge)
            Text("$result · Time: ${formatDuration(record.elapsedMillis(0))}")
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}

private fun MatchFormat.label() = if (this == MatchFormat.SINGLES) "Singles" else "Doubles"

@Composable
fun SetupScreen(
    onStart: (MatchFormat, List<String>, List<String>) -> Unit,
    onCancel: () -> Unit,
) {
    var format by rememberSaveable { mutableStateOf<MatchFormat?>(null) }
    var a1 by rememberSaveable { mutableStateOf("") }
    var a2 by rememberSaveable { mutableStateOf("") }
    var b1 by rememberSaveable { mutableStateOf("") }
    var b2 by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val chosen = format
        if (chosen == null) {
            Text("Is it singles or doubles?", style = MaterialTheme.typography.titleLarge)
            YellowButton("Singles", Modifier.fillMaxWidth()) { format = MatchFormat.SINGLES }
            YellowButton("Doubles", Modifier.fillMaxWidth()) { format = MatchFormat.DOUBLES }
        } else {
            val doubles = chosen == MatchFormat.DOUBLES
            Text(
                if (doubles) "Doubles: enter the four players" else "Singles: enter the two players",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(if (doubles) "Team A" else "Player A", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = a1, onValueChange = { a1 = it }, singleLine = true,
                label = { Text(if (doubles) "Team A, player 1" else "Player A name") },
                modifier = Modifier.fillMaxWidth(),
            )
            if (doubles) {
                OutlinedTextField(
                    value = a2, onValueChange = { a2 = it }, singleLine = true,
                    label = { Text("Team A, player 2") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(if (doubles) "Team B" else "Player B", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = b1, onValueChange = { b1 = it }, singleLine = true,
                label = { Text(if (doubles) "Team B, player 1" else "Player B name") },
                modifier = Modifier.fillMaxWidth(),
            )
            if (doubles) {
                OutlinedTextField(
                    value = b2, onValueChange = { b2 = it }, singleLine = true,
                    label = { Text("Team B, player 2") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            YellowButton("Start match", Modifier.fillMaxWidth()) {
                val sideA = if (doubles) listOf(a1, a2) else listOf(a1)
                val sideB = if (doubles) listOf(b1, b2) else listOf(b1)
                onStart(chosen, sideA, sideB)
            }
            TextButton(onClick = { format = null }) { Text("Change singles/doubles") }
        }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

@Composable
fun MatchScreen(
    record: MatchRecord,
    onPoint: (Player) -> Unit,
    onUndo: () -> Unit,
    onClose: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(record.id, record.isFinished) {
        while (!record.isFinished) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val state = record.state

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "${record.format.label()} · ${formatDuration(record.elapsedMillis(now))}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        state.winner?.let {
            Text(
                "${record.names(it)} win${if (record.format == MatchFormat.SINGLES) "s" else ""}!",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
        }
        Text("Sets: ${record.scoreSummary()}")
        if (state.tiebreak) Text("Tiebreak", fontWeight = FontWeight.Bold)
        Player.entries.forEach { p ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TennisGreen, contentColor = Color.White),
            ) {
                Column(
                    Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        record.names(p),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        state.pointDisplay(p),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = TennisYellow,
                    )
                    Text(
                        "Sets ${state.setsWon(p)}   Games ${state.currentSet.games(p)}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    YellowButton("Point to ${record.names(p)}", enabled = !state.isOver) { onPoint(p) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onUndo) { Text("Undo") }
            if (state.isOver) {
                Button(onClick = onClose) { Text("Done") }
            } else {
                OutlinedButton(onClick = onClose) { Text("End match") }
            }
        }
    }
}
