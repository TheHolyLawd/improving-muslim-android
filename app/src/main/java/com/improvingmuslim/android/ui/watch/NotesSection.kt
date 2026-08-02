package com.improvingmuslim.android.ui.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.improvingmuslim.android.data.NotesStore
import com.improvingmuslim.android.ui.theme.Brand

/**
 * A per-lecture notes panel with easy tap-to-format buttons (H1/H2/H3, bullet, bold), an
 * Edit/Preview toggle, and automatic local saving. No markdown typing required.
 */
@Composable
fun NotesSection(videoId: String) {
    val brand = Brand.colors
    val context = LocalContext.current
    val shape = RoundedCornerShape(10.dp)

    var expanded by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(true) }
    var note by remember(videoId) {
        val initial = NotesStore.load(context, videoId)
        mutableStateOf(TextFieldValue(initial))
    }

    fun update(newValue: TextFieldValue) {
        note = newValue
        NotesStore.save(context, videoId, newValue.text)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, brand.line, shape)
            .background(brand.surface),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "My Notes",
                    color = brand.ink,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text(
                    text = if (note.text.isBlank()) "Add a note while you watch" else "Tap to open your notes",
                    color = brand.muted,
                    fontSize = 13.sp,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = brand.muted,
            )
        }

        if (expanded) {
            Column(
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                EditPreviewToggle(editing = editing, onChange = { editing = it })

                if (editing) {
                    FormatToolbar(
                        onHeading = { level -> update(applyLinePrefix(note, "#".repeat(level) + " ")) },
                        onBullet = { update(applyLinePrefix(note, "- ")) },
                        onBold = { update(applyBold(note)) },
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = ::update,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                        placeholder = { Text("Write what stood out to you…", color = brand.muted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = brand.ink,
                            unfocusedTextColor = brand.ink,
                            focusedBorderColor = brand.accent,
                            unfocusedBorderColor = brand.line,
                            cursorColor = brand.accent,
                        ),
                    )
                    if (note.text.isNotBlank()) {
                        Text(
                            text = "Clear notes",
                            color = brand.rose,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { update(TextFieldValue("")) }
                                .padding(vertical = 4.dp),
                        )
                    }
                } else {
                    NotePreview(note.text)
                }
            }
        }
    }
}

@Composable
private fun EditPreviewToggle(editing: Boolean, onChange: (Boolean) -> Unit) {
    val brand = Brand.colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(true to "Edit", false to "Preview").forEach { (isEdit, label) ->
            val selected = editing == isEdit
            Text(
                text = label,
                color = if (selected) brand.background else brand.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) brand.accent else brand.strongSurface)
                    .clickable { onChange(isEdit) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun FormatToolbar(
    onHeading: (Int) -> Unit,
    onBullet: () -> Unit,
    onBold: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ToolbarButton("H1") { onHeading(1) }
        ToolbarButton("H2") { onHeading(2) }
        ToolbarButton("H3") { onHeading(3) }
        ToolbarButton("•") { onBullet() }
        ToolbarButton("B") { onBold() }
    }
}

@Composable
private fun ToolbarButton(label: String, onClick: () -> Unit) {
    val brand = Brand.colors
    Text(
        text = label,
        color = brand.ink,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, brand.line, RoundedCornerShape(6.dp))
            .background(brand.strongSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun NotePreview(text: String) {
    val brand = Brand.colors
    if (text.isBlank()) {
        Text(
            text = "Nothing written yet. Switch to Edit to add notes.",
            color = brand.muted,
            fontSize = 14.sp,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        text.split("\n").forEach { line ->
            when {
                line.startsWith("### ") ->
                    Text(inlineBold(line.removePrefix("### ")), color = brand.ink, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                line.startsWith("## ") ->
                    Text(inlineBold(line.removePrefix("## ")), color = brand.ink, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                line.startsWith("# ") ->
                    Text(inlineBold(line.removePrefix("# ")), color = brand.ink, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                line.startsWith("- ") || line.startsWith("* ") ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", color = brand.accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(inlineBold(line.drop(2)), color = brand.ink, fontSize = 15.sp)
                    }
                line.isBlank() -> Box(modifier = Modifier.padding(2.dp))
                else -> Text(inlineBold(line), color = brand.ink, fontSize = 15.sp)
            }
        }
    }
}

/** Renders **bold** spans; everything else is plain. */
private fun inlineBold(source: String): AnnotatedString = buildAnnotatedString {
    source.split("**").forEachIndexed { index, part ->
        if (index % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) }
        else append(part)
    }
}

/** Adds [prefix] to the start of the line the cursor is on (e.g. "# ", "- "). */
private fun applyLinePrefix(value: TextFieldValue, prefix: String): TextFieldValue {
    val text = value.text
    val cursor = value.selection.start.coerceIn(0, text.length)
    val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let {
        if (it == -1) 0 else it + 1
    }
    val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
    val newCursor = cursor + prefix.length
    return value.copy(
        text = newText,
        selection = androidx.compose.ui.text.TextRange(newCursor),
    )
}

/** Wraps the selection in ** **, or inserts an empty pair at the cursor. */
private fun applyBold(value: TextFieldValue): TextFieldValue {
    val text = value.text
    val start = minOf(value.selection.start, value.selection.end)
    val end = maxOf(value.selection.start, value.selection.end)
    return if (start == end) {
        val newText = text.substring(0, start) + "****" + text.substring(start)
        value.copy(text = newText, selection = androidx.compose.ui.text.TextRange(start + 2))
    } else {
        val newText = text.substring(0, start) + "**" + text.substring(start, end) + "**" + text.substring(end)
        value.copy(text = newText, selection = androidx.compose.ui.text.TextRange(end + 4))
    }
}
