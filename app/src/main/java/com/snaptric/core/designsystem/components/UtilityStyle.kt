package com.snaptric.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.snaptric.core.database.entity.UtilityType
import com.snaptric.core.designsystem.theme.ElectricityYellow
import com.snaptric.core.designsystem.theme.GasOrange
import com.snaptric.core.designsystem.theme.WaterBlue

/**
 * Accent colour used to identify a utility type across the app.
 */
val UtilityType.accentColor: Color
    get() = when (this) {
        UtilityType.GAS -> GasOrange
        UtilityType.ELECTRICITY -> ElectricityYellow
        UtilityType.WATER -> WaterBlue
    }

/**
 * Icon used to identify a utility type across the app.
 */
val UtilityType.icon: ImageVector
    get() = when (this) {
        UtilityType.GAS -> Icons.Default.LocalFireDepartment
        UtilityType.ELECTRICITY -> Icons.Default.Bolt
        UtilityType.WATER -> Icons.Default.WaterDrop
    }

/**
 * Human-readable label, e.g. "Electricity".
 */
val UtilityType.label: String
    get() = name.lowercase().replaceFirstChar { it.uppercase() }

/**
 * Round badge with the utility icon on a soft tint of its accent colour.
 */
@Composable
fun UtilityIconBadge(
    type: UtilityType,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(type.accentColor.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = type.icon,
            contentDescription = type.label,
            tint = type.accentColor,
            modifier = Modifier.size(size / 2)
        )
    }
}
