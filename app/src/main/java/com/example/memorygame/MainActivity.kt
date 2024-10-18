package com.example.memorygame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memorygame.ui.theme.MemoryGameTheme
import androidx.compose.ui.platform.LocalConfiguration
import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MemoryGameTheme {
                MemoryGame()
            }
        }
    }
}

fun playSound(context: Context, soundResId: Int) {
    val mediaPlayer = MediaPlayer.create(context, soundResId)
    mediaPlayer.start()
    mediaPlayer.setOnCompletionListener {
        it.release()  // MediaPlayer nach dem Abspielen freigeben
    }
}

@Composable
fun MemoryGame(modifier: Modifier = Modifier) {
    val cards = remember { mutableStateListOf(*createMemoryDeck().toTypedArray()) }
    var flippedCards by remember { mutableStateOf(listOf<MemoryCard>()) }
    var attempts by remember { mutableStateOf(0) }
    var pairsFound by remember { mutableStateOf(0) }
    val context = LocalContext.current

    // Anzahl der Paare berechnen
    val totalPairs = cards.size / 2

    // Logik für das Umdrehen der Karten
    LaunchedEffect(flippedCards) {
        if (flippedCards.size == 2) {
            attempts += 1 // Jeder Versuch zählt, wenn zwei Karten aufgedeckt werden
            if (flippedCards[0].id != flippedCards[1].id) {
                // Fehlversuch: falsche Karten
                playSound(context, R.raw.fail)  // Sound für Fehlversuch abspielen
                kotlinx.coroutines.delay(10)
                flippedCards.forEach { it.isFlippedState = false }
            } else {
                // Erfolg: korrektes Paar
                playSound(context, R.raw.success)  // Sound für erfolgreiches Paar abspielen
                flippedCards[0].isMatchedState = true
                flippedCards[1].isMatchedState = true
                pairsFound += 1 // Paar gefunden
            }
            // Liste der umgedrehten Karten leeren
            flippedCards = listOf()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Spielfeld
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),  // Grid mit 4 Spalten
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Spielfeld nimmt den meisten Platz ein
            contentPadding = PaddingValues(16.dp)
        ) {
            items(cards.size) { index ->
                MemoryCardView(cards[index], onClick = {
                    if (flippedCards.size < 2 && !cards[index].isFlippedState && !cards[index].isMatchedState) {
                        // Karte umdrehen und zur Liste der umgedrehten Karten hinzufügen
                        cards[index].isFlippedState = true
                        flippedCards = flippedCards + cards[index]
                    }
                })
            }
        }

        // Informationen über Versuche und Paare
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Versuche: $attempts",
                style = MaterialTheme.typography.h6,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Gefundene Paare: $pairsFound / $totalPairs",
                style = MaterialTheme.typography.h6,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun MemoryCardView(card: MemoryCard, onClick: () -> Unit) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp  // Ermittelt die Bildschirmhöhe in dp
    val boxHeight = screenHeight / 5  // Hier kannst du anpassen, wie viel Platz eine Karte einnehmen soll

    Card(
        modifier = Modifier
            .padding(8.dp)
            .height(boxHeight) // Dynamische Höhe basierend auf der Bildschirmgröße
            .fillMaxWidth() // Karten nehmen die verfügbare Breite ein
            .clickable(enabled = !card.isMatchedState && !card.isFlippedState) { onClick() },
        backgroundColor = if (card.isFlippedState || card.isMatchedState) Color.White else Color.Gray,
        elevation = 8.dp
    ) {
        if (card.isFlippedState || card.isMatchedState) {
            Image(
                painter = painterResource(id = card.image),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(),
                contentScale = ContentScale.Fit // Bild passt sich an die Karte an
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "?", fontSize = 24.sp, color = Color.White)
            }
        }
    }
}