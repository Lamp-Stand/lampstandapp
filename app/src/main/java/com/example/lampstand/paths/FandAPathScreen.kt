package com.example.lampstand.paths

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lampstand.R
import com.example.lampstand.dashboard.BottomNavBar
import com.example.lampstand.ui.theme.LampStandTheme

/** This screen *is* the Fear & Anxiety category (web /paths/1). */
private const val CATEGORY_TITLE = "Fear & Anxiety"

/**
 * Web IMAGE_MAP: title → thumbnail. Apostrophes are normalised on lookup so
 * the curly quote in Android's fallback titles still resolves.
 */
private val pathImageMap = mapOf(
    "Trust in Uncertainty" to R.drawable.shipsail,
    "When Anxiety Won't Stop" to R.drawable.bible,
    "Fear of Failure" to R.drawable.dove,
    "When the Worst Happens" to R.drawable.recommended,
    "Afraid of What People Think" to R.drawable.crown,
    "When You Don't Feel Safe" to R.drawable.shipsail
)

private fun imageFor(title: String): Int =
    pathImageMap[title.replace("\u2019", "'")] ?: R.drawable.shipsail

@Composable
fun FandAPathScreen(
    onBack: () -> Unit = {},
    onItemSelected: (Int) -> Unit = {},
    onCardClick: () -> Unit = {},
    onPathClick: (String) -> Unit = {},
    onComingSoonClick: (String, (Boolean) -> Unit) -> Unit = { _, onResult -> onResult(false) },
    discoveryItems: List<com.example.lampstand.data.remote.DiscoveryItem> = emptyList()
) {
    // "Search Topics..." filters by title/description (web parity: the web
    // search box filters the same discovery list once wired).
    var searchQuery by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        PathsTopBar(
            title = "",
            onBackClick = onBack
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Title: 217 x 24, top 118, left 21
        Text(
            text = "Fear & Anxiety",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 24.sp,
            letterSpacing = 0.sp,
            color = Color(0xFF335E78),
            maxLines = 1,
            modifier = Modifier
                .padding(start = 21.dp)
                .width(217.dp)
                .height(24.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Subtitle: 315 x 60, top 152, left 21
        Text(
            text = "Grow in understanding God’s character through these curated paths focused on Fear and Anxiety",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 20.sp,
            letterSpacing = 0.sp,
            color = Color(0xFF535353),
            modifier = Modifier
                .padding(start = 21.dp, end = 21.dp)
                .width(315.dp)
                .height(60.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // List container: 339 wide, left 19.
                // Gap: 32dp after search bar, 12dp between cards.
                Column(
                    modifier = Modifier
                        .padding(start = 19.dp, top = 8.dp, bottom = 16.dp)
                        .width(339.dp)
                ) {
                    PathSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it }
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    val fallbackTitles = listOf(
                        "Trust in Uncertainty",
                        "When Anxiety Won\u2019t Stop",
                        "Fear of Failure",
                        "When the Worst Happens",
                        "Afraid of What People Think",
                        "When You Don\u2019t Feel Safe"
                    )
                    val fallbackDescriptions = listOf(
                        "For when the future feels unclear and you\u2019re trying to trust God one step at a time.",
                        "For the moments when your mind won\u2019t slow down and you need calm and clarity.",
                        "For when the pressure to succeed makes you doubt yourself or hold back.",
                        "For the seasons when life breaks unexpectedly and you need strength to get through.",
                        "For when fear of judgment steals your confidence and you want to live freely again.",
                        "For the moments you feel vulnerable or overwhelmed and long for steady peace."
                    )
                    // Web: paths.filter(p => p.category === category.title) — only
                    // Fear & Anxiety paths, live ones first, coming-soon A-Z.
                    val categoryItems = discoveryItems.filter { it.category == CATEGORY_TITLE }
                    val ordered =
                        categoryItems.filter { it.isLive } +
                            categoryItems.filter { !it.isLive }.sortedBy { it.title }
                    // Fallback only before the first fetch (previews / offline);
                    // once loaded an empty category renders empty, like the web.
                    val allCards: List<Triple<String, String, com.example.lampstand.data.remote.DiscoveryItem?>> =
                        if (discoveryItems.isEmpty()) {
                            fallbackTitles.mapIndexed { i, t ->
                                Triple(t, fallbackDescriptions.getOrElse(i) { "" }, null)
                            }
                        } else {
                            ordered.map { Triple(it.title, it.description, it) }
                        }
                    // "Search Topics..." matches title or description, case-insensitive.
                    val query = searchQuery.trim()
                    val cards = if (query.isEmpty()) allCards else allCards.filter {
                        it.first.contains(query, ignoreCase = true) ||
                            it.second.contains(query, ignoreCase = true)
                    }
                    if (cards.isEmpty()) {
                        // Web only renders the empty message when a search is active.
                        if (query.isNotEmpty()) {
                            Text(
                                text = "No paths match \u201C$query\u201D.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 20.sp,
                                color = Color(0xFF535353),
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                        return@Column
                    }
                    cards.forEachIndexed { index, (cardTitle, cardDesc, item) ->
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        // Stable identity, like React's key= — keeps a card's
                        // tapped/confirmation state across the post-tap refresh.
                        key(item?.pathId ?: "fallback-$index") {
                            when {
                                item == null -> FeaturedPathCard(
                                    imageRes = imageFor(cardTitle),
                                    title = cardTitle,
                                    description = cardDesc,
                                    onClick = onCardClick
                                )
                                item.isLive -> FeaturedPathCard(
                                    imageRes = imageFor(cardTitle),
                                    title = cardTitle,
                                    description = cardDesc,
                                    availableBadge = true,
                                    onClick = { onPathClick(item.pathId) }
                                )
                                else -> ComingSoonPathCard(
                                    imageRes = imageFor(cardTitle),
                                    title = cardTitle,
                                    description = cardDesc,
                                    totalTaps = item.totalTaps.toInt(),
                                    userHasTapped = item.userHasTapped,
                                    onTap = { onResult -> onComingSoonClick(item.pathId, onResult) }
                                )
                            }
                        }
                    }
                }
            }
        }

        BottomNavBar(
            selectedIndex = 1,
            onItemSelected = onItemSelected
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun FandAPathScreenPreview() {
    LampStandTheme(dynamicColor = false) {
        FandAPathScreen()
    }
}
