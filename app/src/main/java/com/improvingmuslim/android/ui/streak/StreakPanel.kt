package com.improvingmuslim.android.ui.streak

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.improvingmuslim.android.data.StreakStore
import com.improvingmuslim.android.ui.theme.Brand
import java.util.Calendar

/** The daily-streak panel opened from the header flame: today's progress, current/best, and
 *  a month heatmap. Read-only — the streak fills from actual lecture playback. */
@Composable
fun StreakPanel(streak: StreakStore.State, onDismiss: () -> Unit) {
    val brand = Brand.colors
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(18.dp),
            color = brand.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("LEARNING RHYTHM", color = brand.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Daily streak",
                            color = brand.ink,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = brand.muted)
                    }
                }

                Summary(streak)
                Stats(streak)
                MonthHeatmap(streak.days)

                Text(
                    text = "Only actual lecture playback counts toward your ${StreakStore.TARGET_MINUTES}-minute daily goal — skipping ahead doesn't fill it.",
                    color = brand.muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
        }
    }
}

@Composable
private fun Summary(streak: StreakStore.State) {
    val brand = Brand.colors
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(50))
                .background(if (streak.current > 0) brand.accent.copy(alpha = 0.14f) else brand.strongSurface)
                .border(2.dp, if (streak.completedToday) brand.accent else brand.line, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Whatshot, contentDescription = null, tint = brand.gold, modifier = Modifier.size(20.dp))
                Text(streak.current.toString(), color = brand.ink, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (streak.completedToday) "Goal complete today" else "Today in progress",
                color = brand.muted,
                fontSize = 12.sp,
            )
            Text(
                text = if (streak.current > 0) "${streak.current}-day streak" else "Start your streak today",
                color = brand.ink,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            Text(
                text = "${streak.todayMinutes} of ${StreakStore.TARGET_MINUTES} min watched today" +
                    if (streak.completedToday) " · beautifully kept." else " · ${streak.minutesLeft} min left.",
                color = brand.muted,
                fontSize = 13.sp,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(brand.line),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(streak.progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(brand.accent),
                )
            }
        }
    }
}

@Composable
private fun Stats(streak: StreakStore.State) {
    val brand = Brand.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier.fillMaxWidth().clip(shape).border(1.dp, brand.line, shape).padding(vertical = 14.dp),
    ) {
        Stat(streak.current.toString(), "Current", Modifier.weight(1f))
        Box(modifier = Modifier.width(1.dp).height(34.dp).background(brand.line))
        Stat(streak.best.toString(), "Best", Modifier.weight(1f))
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    val brand = Brand.colors
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = brand.ink, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(label, color = brand.muted, fontSize = 12.sp)
    }
}

@Composable
private fun MonthHeatmap(days: Map<String, StreakStore.DayRecord>) {
    val brand = Brand.colors
    val cal = Calendar.getInstance()
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    val todayDay = cal.get(Calendar.DAY_OF_MONTH)
    val first = Calendar.getInstance().apply { set(year, month, 1) }
    val leadingBlanks = first.get(Calendar.DAY_OF_WEEK) - 1 // Sunday = 0
    val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cells: List<Int?> = List(leadingBlanks) { null } + (1..daysInMonth).toList()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            Text("This month", color = brand.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text("Filled days met your goal", color = brand.muted, fontSize = 11.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            cells.chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        if (day == null) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            val key = "%04d-%02d-%02d".format(year, month + 1, day)
                            DayCell(day = day, record = days[key], isFuture = day > todayDay, modifier = Modifier.weight(1f))
                        }
                    }
                    repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, record: StreakStore.DayRecord?, isFuture: Boolean, modifier: Modifier = Modifier) {
    val brand = Brand.colors
    val bg = when {
        isFuture -> brand.strongSurface.copy(alpha = 0.4f)
        record?.completed == true -> brand.accent
        (record?.seconds ?: 0) > 0 -> brand.accent.copy(alpha = 0.35f)
        else -> brand.strongSurface
    }
    val fg = if (record?.completed == true) brand.background else brand.muted
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(day.toString(), color = fg, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
