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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tennisbuddy.shared.MatchFormat
import com.tennisbuddy.shared.MatchRecord
import com.tennisbuddy.shared.Player
import com.tennisbuddy.shared.PlayerStats
import com.tennisbuddy.shared.computeStats
import com.tennisbuddy.shared.formatDuration
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

/** The four bottom-bar tabs, plus the two screens used while playing a match. */
enum class Screen { HOME, MATCHES, STATS, PROFILE, SETUP, MATCH }

private data class Tab(val screen: Screen, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Screen.HOME, "Home", Icons.Filled.Home),
    Tab(Screen.MATCHES, "Matches", Icons.Filled.SportsTennis),
    Tab(Screen.STATS, "Statistics", Icons.Filled.BarChart),
    Tab(Screen.PROFILE, "Profile", Icons.Filled.Person),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dark green screens: keep status and navigation bar icons light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        setContent {
            TennisBuddyTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TennisBuddyApp()
                }
            }
        }
    }
}

@Composable
fun TennisBuddyApp(vm: MatchViewModel = viewModel()) {
    val controller = vm.controller
    val data by controller.data.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }

    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

    val current = data.current
    val resumable = current != null && !current.isFinished
    val stats = computeStats(data.history)
    // Setting up or playing a match counts as being on the Home tab.
    val selectedTab = if (screen == Screen.SETUP || screen == Screen.MATCH) Screen.HOME else screen

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = TennisCard, contentColor = Color.White) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab.screen,
                        onClick = { screen = tab.screen },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TennisYellow,
                            selectedTextColor = TennisYellow,
                            indicatorColor = TennisTile,
                            unselectedIconColor = Color.White,
                            unselectedTextColor = Color.White,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (screen) {
                Screen.HOME -> DashboardScreen(
                    resumeLabel = if (resumable) {
                        "Resume: ${current!!.names(Player.A)} vs ${current.names(Player.B)}"
                    } else {
                        null
                    },
                    recent = data.history.take(2),
                    stats = stats,
                    onNewMatch = { screen = Screen.SETUP },
                    onResume = { screen = Screen.MATCH },
                    onSeeAll = { screen = Screen.MATCHES },
                )

                Screen.MATCHES -> MatchesScreen(data.history, onDelete = controller::deleteRecord)

                Screen.STATS -> StatsScreen(stats)

                Screen.PROFILE -> ProfileScreen(
                    name = data.profileName,
                    onSave = controller::setProfileName,
                )

                Screen.SETUP -> SetupScreen(
                    myName = data.profileName,
                    onStart = { format, sideA, sideB ->
                        controller.startMatch(format, sideA, sideB)
                        screen = Screen.MATCH
                    },
                    onCancel = { screen = Screen.HOME },
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
                            screen = Screen.HOME
                        },
                    )
                }
            }
        }
    }
}

// ---------- shared building blocks ----------

@Composable
private fun cardColors() = CardDefaults.cardColors(containerColor = TennisCard, contentColor = Color.White)

@Composable
private fun ScreenTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
}

/** Yellow pill button with dark text: the app's main action style. */
@Composable
private fun YellowButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TennisYellow,
            contentColor = TennisDarkText,
            disabledContainerColor = TennisTile,
            disabledContentColor = Color.White,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Box(Modifier.size(8.dp))
        }
        Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TennisTile, contentColor = Color.White),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TennisYellow)
        }
    }
}

@Composable
private fun StatTiles(stats: PlayerStats) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Wins", "${stats.wins}/${stats.matches}", Modifier.weight(1f))
            StatTile("Sets won %", "${stats.setsWonPercent}%", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Time played", formatDuration(stats.totalMillis), Modifier.weight(1f))
            StatTile("Matches played", "${stats.matches}", Modifier.weight(1f))
        }
    }
}

private fun MatchFormat.label() = if (this == MatchFormat.SINGLES) "Singles" else "Doubles"

/** Side A is "you", so a win for A is a Win and a win for B is a Loss. */
private fun resultLabel(record: MatchRecord): Pair<String, Color> = when (record.state.winner) {
    Player.A -> "Win" to TennisWin
    Player.B -> "Loss" to TennisLoss
    null -> "Ended early" to Color.White
}

private fun dateText(record: MatchRecord): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(record.startedAtMillis))

@Composable
private fun MatchCard(record: MatchRecord, onDelete: (() -> Unit)? = null) {
    val (result, resultColor) = resultLabel(record)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = cardColors()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(dateText(record), style = MaterialTheme.typography.bodySmall)
                Text(result, color = resultColor, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    record.names(Player.A),
                    Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    record.scoreSummary(),
                    Modifier.weight(1f),
                    color = TennisYellow,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    record.names(Player.B),
                    Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "${record.format.label()} · ${formatDuration(record.elapsedMillis(0))}",
                style = MaterialTheme.typography.bodySmall,
            )
            if (onDelete != null) {
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

// ---------- screens ----------

@Composable
fun DashboardScreen(
    resumeLabel: String?,
    recent: List<MatchRecord>,
    stats: PlayerStats,
    onNewMatch: () -> Unit,
    onResume: () -> Unit,
    onSeeAll: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painterResource(R.drawable.tennisbuddy_logo),
                contentDescription = "TennisBuddy logo",
                modifier = Modifier.size(44.dp),
            )
            Box(Modifier.size(10.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White)) { append("Tennis") }
                    withStyle(SpanStyle(color = TennisYellow)) { append("Buddy") }
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        ScreenTitle("Dashboard")
        if (resumeLabel != null) {
            YellowButton(resumeLabel, Modifier.fillMaxWidth(), onClick = onResume)
        }
        YellowButton("Start New Match", Modifier.fillMaxWidth(), icon = Icons.Filled.Add, onClick = onNewMatch)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Recent Matches", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (recent.isNotEmpty()) TextButton(onClick = onSeeAll) { Text("See all") }
        }
        if (recent.isEmpty()) {
            Text("No matches yet. Finished matches are saved here.")
        } else {
            recent.forEach { MatchCard(it) }
        }

        Text("My Statistics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        StatTiles(stats)
    }
}

@Composable
fun MatchesScreen(history: List<MatchRecord>, onDelete: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle("Matches")
        if (history.isEmpty()) {
            Text("No matches yet. Finished matches are saved here.")
        } else {
            history.forEach { record -> MatchCard(record, onDelete = { onDelete(record.id) }) }
        }
    }
}

@Composable
fun StatsScreen(stats: PlayerStats) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle("Statistics")
        StatTiles(stats)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = cardColors()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Record", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Wins: ${stats.wins}")
                Text("Losses: ${stats.losses}")
                Text("Ended early: ${stats.endedEarly}")
                Text("Sets won: ${stats.setsWon} of ${stats.setsPlayed}")
            }
        }
        Text("You are always Player A, the first name when a match is set up.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ProfileScreen(name: String, onSave: (String) -> Unit) {
    var text by rememberSaveable(name) { mutableStateOf(name) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle("Profile")
        Text("Your name is filled in as Player A when you start a match, and your wins and losses are counted for Player A.")
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            label = { Text("Your name") },
            modifier = Modifier.fillMaxWidth(),
        )
        YellowButton("Save", Modifier.fillMaxWidth(), enabled = text.trim() != name) { onSave(text) }
    }
}

@Composable
fun SetupScreen(
    myName: String,
    onStart: (MatchFormat, List<String>, List<String>) -> Unit,
    onCancel: () -> Unit,
) {
    var format by rememberSaveable { mutableStateOf<MatchFormat?>(null) }
    var a1 by rememberSaveable { mutableStateOf(myName) }
    var a2 by rememberSaveable { mutableStateOf("") }
    var b1 by rememberSaveable { mutableStateOf("") }
    var b2 by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle("New match")
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
            Text(if (doubles) "Team A (you)" else "Player A (you)", fontWeight = FontWeight.Bold)
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
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
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
                color = TennisYellow,
                textAlign = TextAlign.Center,
            )
        }
        Text("Sets: ${record.scoreSummary()}")
        if (state.tiebreak) Text("Tiebreak", fontWeight = FontWeight.Bold, color = TennisYellow)
        Player.entries.forEach { p ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = cardColors()) {
                Column(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(record.names(p), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
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
                    YellowButton("Point to ${record.names(p)}", Modifier.fillMaxWidth(), enabled = !state.isOver) { onPoint(p) }
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
