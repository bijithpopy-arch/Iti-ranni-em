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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HolidayAnnouncement
import com.example.data.NoticeItem
import com.example.ui.components.ActiveHolidayBanner
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors

@Composable
fun NoticesScreen(
    notices: List<NoticeItem>,
    todayHoliday: HolidayAnnouncement?,
    lastSeenNoticeId: String?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    var selectedCategory by remember { mutableStateOf("All") }

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

        // Active Holiday Banner if declared today
        if (todayHoliday != null) {
            item {
                ActiveHolidayBanner(holiday = todayHoliday)
            }
        }

        // Category filter chips
        if (categories.size > 1) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { category ->
                        val selected = selectedCategory == category
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
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
                NoticeCardItem(
                    notice = notice,
                    isUnread = isUnread
                )
            }
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
                            shape = RoundedCornerShape(8.dp),
                            color = colors.emeraldContainer
                        ) {
                            Text(
                                text = "NEW",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp
                                ),
                                color = colors.emeraldAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
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
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Posted by ${notice.postedBy}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted
            )
        }
    }
}
