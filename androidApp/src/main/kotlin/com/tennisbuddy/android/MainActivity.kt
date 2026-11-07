package com.tennisbuddy.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tennisbuddy.shared.MatchController
import com.tennisbuddy.shared.MatchState
import com.tennisbuddy.shared.Player

class MainActivity : ComponentActivity() {
    private val controller = MatchController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    val state by remember { controller.state }.collectAsState()
                    ScoreScreen(
                        state = state,
                        onPoint = controller::pointWonBy,
                        onUndo = controller::undo,
                        onReset = controller::reset,
                    )
                }
            }
        }
    }
}

@Composable
fun ScoreScreen(
    state: MatchState,
    onPoint: (Player) -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("TennisBuddy", style = MaterialTheme.typography.headlineMedium)
        state.winner?.let { Text("Player ${it.name} wins!", style = MaterialTheme.typography.titleLarge) }
        Player.entries.forEach { p ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Player ${p.name}", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Sets ${state.setsWon(p)}  Games ${state.currentSet.games(p)}  Points ${state.pointDisplay(p)}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = { onPoint(p) }, enabled = !state.isOver) { Text("Point ${p.name}") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onUndo) { Text("Undo") }
            OutlinedButton(onClick = onReset) { Text("New match") }
        }
    }
}
