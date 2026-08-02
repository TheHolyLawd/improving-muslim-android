package com.improvingmuslim.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.improvingmuslim.android.ui.home.SortOption
import com.improvingmuslim.android.ui.theme.Brand

/** "Sort by" label plus a dropdown of sort options. */
@Composable
fun SortDropdown(
    selected: SortOption,
    onSelect: (SortOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = Brand.colors
    var expanded by remember { mutableStateOf(false) }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Sort by",
            color = brand.muted,
            fontSize = 13.sp,
            modifier = Modifier.padding(end = 6.dp),
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = brand.surface,
            border = BorderStroke(1.dp, brand.line),
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { expanded = true },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            ) {
                Text(
                    text = selected.label,
                    color = brand.ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = brand.muted,
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option.label,
                            fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
