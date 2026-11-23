package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HolidayAnnouncement
import com.example.data.NoticeItem
import com.example.data.TechNewsItem
import com.example.ui.components.ActiveHolidayBanner
import com.example.ui.components.ExternalUrlHelper
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors

private enum class NoticesFeedMode(val label: String) {
    NOTICES("College Notices"),
    TECH_NEWS("Live Tech News")
}

private val TECH_NEWS_CATEGORIES = listOf("All", "Electronics", "Embedded", "Power & Industrial")

@Composable
fun NoticesScreen(
    notices: List<NoticeItem>,
    todayHoliday: HolidayAnnouncement?,
    lastSeenNoticeId: String?,
    onRefresh: () -> Unit,
    techNewsItems: List<TechNewsItem> = emptyList(),
    isLoadingTechNews: Boolean = false,
    techNewsError: String? = null,
    onRefreshTechNews: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val context = LocalContext.current
    var feedMode by remember { mutableStateOf(NoticesFeedMode.NOTICES) }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedNewsCategory by remember { mutableStateOf("All") }

    LaunchedEffect(feedMode) {
        if (feedMode == NoticesFeedMode.TECH_NEWS && techNewsItems.isEmpty() && !isLoadingTechNews) {
            onRefreshTechNews()
        }
    }

    val categories = remember(notices) {
        val dynamic = notices.map { it.category.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        listOf("All") + dynamic
    }

    val filteredNotices = remember(notices, selectedCategory) {
        if (selectedCategory == "All") {
            notices
        } else {
            notices.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    val filteredNews = remember(techNewsItems, selectedNewsCategory) {
        if (selectedNewsCategory == "All") {
            techNewsItems
        } else {
            techNewsItems.filter { it.category.equals(selectedNewsCategory, ignoreCase = true) }
        }
    }

    val lastSeenIndex = remember(notices, lastSeenNoticeId) {
        if (lastSeenNoticeId.isNullOrBlank()) -1
        else notices.indexOfFirst { it.id == lastSeenNoticeId }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("notices_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mode switcher: College Notices vs Live Technical News Feed
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NoticesFeedMode.entries.forEach { mode ->
                        val selected = feedMode == mode
                        Surface(
                            onClick = { feedMode = mode },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) colors.skyAccent else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("notices_mode_${mode.name.lowercase()}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (mode == NoticesFeedMode.NOTICES) Icons.Rounded.Campaign else Icons.Rounded.Memory,
                                    contentDescription = null,
                                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else colors.textMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        if (feedMode == NoticesFeedMode.NOTICES) {
            // Header card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(colors.skyContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Campaign,
                                    contentDescription = null,
                                    tint = colors.skyAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "College & Trade Notices",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Govt. ITI Ranni • Electronics Mechanic",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted
                                )
                            }
                        }

                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("notices_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh notices",
                                tint = colors.skyAccent
                            )
                        }
                    }
                }
            }

            if (todayHoliday != null) {
                item {
                    ActiveHolidayBanner(holiday = todayHoliday)
                }
            }

            if (categories.size > 1) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { category ->
                            val selected = selectedCategory == category
                            FilterChip(
                                selected = selected,
                                onClick = { selectedCategory = category },
                                label = { Text(text = category, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.skyContainer,
                                    selectedLabelColor = colors.skyAccent,
                                    containerColor = colors.cardSurface,
                                    labelColor = colors.textMuted
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = colors.borderSubtle,
                                    selectedBorderColor = colors.skyAccent
                                )
                            )
                        }
                    }
                }
            }

            if (filteredNotices.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.cardSurface,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(38.dp)
                            )
                            Text(
                                text = "No Notices Published Yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Official trade circulars, workshop schedules, and holiday notices (English / Manglish / മലയാളം) will appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            } else {
                items(filteredNotices, key = { it.id.ifBlank { "${it.title}_${it.date}_${it.hashCode()}" } }) { notice ->
                    val originalIndex = notices.indexOfFirst { it.id == notice.id }
                    val isUnread = lastSeenIndex == -1 || (originalIndex in 0 until lastSeenIndex)
                    NoticeCardItem(notice = notice, isUnread = isUnread)
                }
            }
        } else {
            // LIVE TECHNICAL NEWS FEED
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(1.dp, colors.emeraldAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(colors.emeraldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Memory,
                                    contentDescription = null,
                                    tint = colors.emeraldAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Live Electronics & Tech News",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Electronics, Embedded MCUs, Power & Industrial updates",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted
                                )
                            }
                        }

                        IconButton(
                            onClick = onRefreshTechNews,
                            enabled = !isLoadingTechNews,
                            modifier = Modifier.testTag("tech_news_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh technical news",
                                tint = colors.emeraldAccent
                            )
                        }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TECH_NEWS_CATEGORIES) { cat ->
                        val selected = selectedNewsCategory == cat
                        FilterChip(
                            selected = selected,
                            onClick = { selectedNewsCategory = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.emeraldContainer,
                                selectedLabelColor = colors.emeraldAccent,
                                containerColor = colors.cardSurface,
                                labelColor = colors.textMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = colors.borderSubtle,
                                selectedBorderColor = colors.emeraldAccent
                            )
                        )
                    }
                }
            }

            if (isLoadingTechNews && techNewsItems.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.cardSurface,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = colors.emeraldAccent)
                            Text(
                                text = "Fetching live Electronics & Embedded news...",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            } else if (techNewsError != null && techNewsItems.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.cardSurface,
                        border = BorderStroke(1.dp, colors.redAccent.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            InlineAlertCard(message = techNewsError, isError = true)
                            Button(
                                onClick = onRefreshTechNews,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.skyAccent,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Retry Feed")
                            }
                        }
                    }
                }
            } else if (filteredNews.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.cardSurface,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No articles found for \"$selectedNewsCategory\". Try switching category or tapping Refresh.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(filteredNews, key = { it.id }) { article ->
                    TechNewsArticleCard(
                        article = article,
                        onOpenLink = { ExternalUrlHelper.openUrl(context, article.link) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TechNewsArticleCard(
    article: TechNewsItem,
    onOpenLink: () -> Unit
) {
    val colors = LocalItiColors.current
    val (bg, fg) = when (article.category) {
        "Embedded" -> colors.violetContainer to colors.violetAccent
        "Power & Industrial" -> colors.amberContainer to colors.amberAccent
        "Electronics" -> colors.emeraldContainer to colors.emeraldAccent
        else -> colors.skyContainer to colors.skyAccent
    }

    Surface(
        onClick = onOpenLink,
        shape = RoundedCornerShape(16.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tech_news_card_${article.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(6.dp), color = bg) {
                        Text(
                            text = article.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp
                            ),
                            color = fg,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = article.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.skyAccent
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = "Open article",
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = article.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = article.summary,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = article.publishedAt,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                color = colors.textMuted
            )
        }
    }
}

@Composable
fun NoticeCardItem(
    notice: NoticeItem,
    isUnread: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val catUpper = notice.category.uppercase()
    val (tagBg, tagFg) = when {
        catUpper.contains("URGENT") || catUpper.contains("EXAM") -> colors.redContainer to colors.redAccent
        catUpper.contains("HOLIDAY") -> colors.amberContainer to colors.amberAccent
        catUpper.contains("WORKSHOP") || catUpper.contains("PROJECT") -> colors.emeraldContainer to colors.emeraldAccent
        catUpper.contains("DGT") || catUpper.contains("NCVT") -> colors.violetContainer to colors.violetAccent
        else -> colors.skyContainer to colors.skyAccent
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.cardSurface,
        border = BorderStroke(
            width = if (isUnread) 1.5.dp else 1.dp,
            color = if (isUnread) colors.skyAccent.copy(alpha = 0.65f) else colors.borderSubtle
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("notice_card_${notice.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = tagBg,
                        border = BorderStroke(1.dp, tagFg.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = notice.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = tagFg,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (isUnread) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.redContainer
                        ) {
                            Text(
                                text = "NEW",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp
                                ),
                                color = colors.redAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = notice.date,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = colors.textMuted
                )
            }

            Text(
                text = notice.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = notice.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Text(
                text = "Published by ${notice.postedBy}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted
            )
        }
    }
}
