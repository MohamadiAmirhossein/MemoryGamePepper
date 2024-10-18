package com.example.memorygame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setzt die Medienlautstärke auf Maximum
        setMediaVolume(this)

        setContent {
            MemoryGameTheme {
                MemoryGame()
            }
        }
    }
}

fun setMediaVolume(context: Context) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    audioManager.setStreamVolume(
        AudioManager.STREAM_MUSIC,
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
        0
    )
}

fun playSound(context: Context, soundResId: Int) {
    val mediaPlayer = MediaPlayer.create(context, soundResId)
    mediaPlayer.setVolume(1.0f, 1.0f)  // Lautstärke für beide Kanäle auf 100% setzen (Links, Rechts)
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

    var showMenu by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    // Anzahl der Paare berechnen
    val totalPairs = cards.size / 2

    // Logik für das Umdrehen der Karten
    LaunchedEffect(flippedCards) {
        if (!isPaused && flippedCards.size == 2) {
            attempts += 1 // Jeder Versuch zählt, wenn zwei Karten aufgedeckt werden

            // Warte darauf, dass der flipCard-Sound abgespielt wird, bevor success/fail abgespielt werden
            kotlinx.coroutines.delay(300) // Kleine Verzögerung, um flipCard-Sound abzuspielen

            if (flippedCards[0].id != flippedCards[1].id) {
                // Fehlversuch: falsche Karten
                playSound(context, R.raw.fail)  // Sound für Fehlversuch abspielen
                kotlinx.coroutines.delay(1000)  // 1 Sekunde warten, bevor die Karten wieder umgedreht werden
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

    Box(modifier = modifier.fillMaxSize()) {
        // Hintergrundbild
        Image(
            painter = painterResource(id = R.drawable.jungle), // Das Hintergrundbild jungle.png
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop // Passt das Bild an den gesamten Bildschirm an
        )

        // Spielfeld
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),  // Grid mit 4 Spalten
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(), // Passt sich an die Höhe an
            contentPadding = PaddingValues(16.dp)
        ) {
            items(cards.size) { index ->
                MemoryCardView(cards[index], onClick = {
                    if (flippedCards.size < 2 && !cards[index].isFlippedState && !cards[index].isMatchedState && !isPaused) {
                        // Sound für das Umdrehen der Karte abspielen
                        playSound(context, R.raw.flipcard)  // flipCard.mp3 wird abgespielt

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
                .padding(16.dp)
                .align(Alignment.BottomCenter),
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

        // Kreiförmiger Button für Optionen
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Optionen"
                )
            }

            // Dropdown-Menü für Optionen
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(onClick = {
                    // Spiel pausieren
                    isPaused = !isPaused
                    showMenu = false
                }) {
                    Text(text = if (isPaused) "Spiel fortsetzen" else "Spiel pausieren")
                }
                DropdownMenuItem(onClick = {
                    // Spiel neustarten
                    attempts = 0
                    pairsFound = 0
                    flippedCards = listOf()
                    cards.clear()
                    cards.addAll(createMemoryDeck())
                    showMenu = false
                }) {
                    Text(text = "Spiel neustarten")
                }
            }
        }
    }
}


@Composable
fun MemoryCardView(card: MemoryCard, onClick: () -> Unit) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp  // Ermittelt die Bildschirmhöhe in dp
    val boxHeight = screenHeight / 5  // Höhe der Karte basierend auf der Bildschirmgröße

    Card(
        modifier = Modifier
            .padding(8.dp)
            .height(boxHeight) // Dynamische Höhe basierend auf der Bildschirmgröße
            .fillMaxWidth()    // Karten nehmen die verfügbare Breite ein
            .clickable(enabled = !card.isMatchedState && !card.isFlippedState) { onClick() },
        backgroundColor = Color.Transparent, // Hintergrundfarbe wird transparent, damit das Bild sichtbar ist
        elevation = 8.dp
    ) {
        if (card.isFlippedState || card.isMatchedState) {
            // Zeige das Bild der Karte an, wenn sie umgedreht oder gematcht ist
            Image(
                painter = painterResource(id = card.image),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(),
                contentScale = ContentScale.Fit // Bild passt sich an die Karte an
            )
        } else {
            // Zeige das Hintergrundbild an, wenn die Karte nicht umgedreht ist
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.question_mark),  // Verwende questionMark.png
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop // Hintergrundbild füllt die Karte aus
                )
            }
        }
    }
}