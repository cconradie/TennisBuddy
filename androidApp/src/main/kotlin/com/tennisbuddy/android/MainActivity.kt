package com.tennisbuddy.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tennisbuddy.shared.MatchFormat
import com.tennisbuddy.shared.MatchRecord
import com.tennisbuddy.shared.Player
import com.tennisbuddy.shared.formatDuration
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

enum class Screen { HOME, SETUP, MATCH }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) { TennisBuddyApp() }
            }
        }
    }
}

@Composable
fun TennisBuddyApp(vm: MatchViewModel = viewModel()) {
    val controller = vm.controller
    val data by controller.data.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }

    when (screen) {
        Screen.HOME -> HomeScreen(
            current = data.current,
            history = data.history,
            onNewMatch = { screen = Screen.SETUP },
            onResume = { screen = Screen.MATCH },
            onDelete = controller::deleteRecord,
        )

        Screen.SETUP -> {
            BackHandler { screen = Screen.HOME }
            SetupScreen(
                onStart = { format, sideA, sideB ->
                    controller.startMatch(format, sideA, sideB)
                    screen = Screen.MATCH
                },
                onBack = { screen = Screen.HOME },
            )
        }

        Screen.MATCH -> {
            val record = data.current
            if (record == null) {
                LaunchedEffect(Unit) { screen = Screen.HOME }
            } else {
                BackHandler { screen = Screen.HOME } // leaves the match running; resume from Home
                MatchScreen(
                    record = record,
                    onPoint = controller::pointWonBy,
                    onUndo = controller::undo,
                    onClose = {
                        controller.closeMatch()
                        screen = Screen.HOME
                    },
                    onHome = { screen = Screen.HOME },
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    current: MatchRecord?,
    history: List<MatchRecord>,
    onNewMatch: () -> Unit,
    onResume: () -> Unit,
    onDelete: (String) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    painterResource(R.drawable.tennisbuddy_logo),
                    contentDescription = "TennisBuddy logo",
                    modifier = Modifier.size(120.dp),
                )
                Text("TennisBuddy", style = MaterialTheme.typography.headlineMedium)
                if (current != null && !current.isFinished) {
                    Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) {
                        Text("Resume: ${current.names(Player.A)} vs ${current.names(Player.B)}")
                    }
                }
                Button(onClick = onNewMatch, modifier = Modifier.fillMaxWidth()) { Text("New match") }
                Text("Match register", style = MaterialTheme.typography.titleMedium)
                if (history.isEmpty()) {
                    Text("No matches yet. Finished matches are saved here.")
                }
            }
        }
        items(history, key = { it.id }) { record ->
            HistoryCard(record, onDelete = { onDelete(record.id) })
        }
        item { Text("", Modifier.padding(16.dp)) }
    }
}

@Composable
fun HistoryCard(record: MatchRecord, onDelete: () -> Unit) {
    val whenText = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(record.startedAtMillis))
    val result = record.state.winner?.let { "Winner: ${record.names(it)}" } ?: "Ended early"
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${record.names(Player.A)} vs ${record.names(Player.B)}",
                style = MaterialTheme.typography.titleMedium,
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
    onBack: () -> Unit,
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
        Text("New match", style = MaterialTheme.typography.headlineMedium)
        val chosen = format
        if (chosen == null) {
            Text("Is it singles or doubles?", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { format = MatchFormat.SINGLES }, modifier = Modifier.fillMaxWidth()) {
                Text("Singles")
            }
            Button(onClick = { format = MatchFormat.DOUBLES }, modifier = Modifier.fillMaxWidth()) {
                Text("Doubles")
            }
        } else {
            val doubles = chosen == MatchFormat.DOUBLES
            Text(
                if (doubles) "Doubles: enter the four players" else "Singles: enter the two players",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(if (doubles) "Team A" else "Player A")
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
            Text(if (doubles) "Team B" else "Player B")
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
            Button(
                onClick = {
                    val sideA = if (doubles) listOf(a1, a2) else listOf(a1)
                    val sideB = if (doubles) listOf(b1, b2) else listOf(b1)
                    onStart(chosen, sideA, sideB)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Start match") }
            TextButton(onClick = { format = null }) { Text("Change singles/doubles") }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

@Composable
fun MatchScreen(
    record: MatchRecord,
    onPoint: (Player) -> Unit,
    onUndo: () -> Unit,
    onClose: () -> Unit,
    onHome: () -> Unit,
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
        Text(record.format.label(), style = MaterialTheme.typography.titleMedium)
        Text(
            "Time ${formatDuration(record.elapsedMillis(now))}",
            style = MaterialTheme.typography.headlineMedium,
        )
        state.winner?.let {
            Text(
                "${record.names(it)} win${if (record.format == MatchFormat.SINGLES) "s" else ""}!",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
        }
        Text("Sets: ${record.scoreSummary()}")
        Player.entries.forEach { p ->
            Card(Modifier.fillMaxWidth()) {
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
                        "Sets ${state.setsWon(p)}   Games ${state.currentSet.games(p)}   " +
                            "Points ${state.pointDisplay(p)}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(onClick = { onPoint(p) }, enabled = !state.isOver) { Text("Point to ${record.names(p)}") }
                }
            }
        }
        if (state.tiebreak) Text("Tiebreak")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onUndo) { Text("Undo") }
            OutlinedButton(onClick = onHome) { Text("Home") }
            if (state.isOver) {
                Button(onClick = onClose) { Text("Done") }
            } else {
                OutlinedButton(onClick = onClose) { Text("End match") }
            }
        }
    }
}
