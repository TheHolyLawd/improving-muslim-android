package com.improvingmuslim.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.improvingmuslim.android.ui.theme.Brand

@Composable
fun TopicPill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = Brand.colors
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { this.selected = selected },
        shape = CircleShape,
        color = if (selected) brand.accent else brand.surface,
        border = BorderStroke(1.dp, if (selected) brand.accent else brand.line),
    ) {
        Text(
            text = title,
            color = if (selected) brand.background else brand.muted,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .defaultMinSize(minHeight = 42.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}
