package com.aitutor.app.ui.screen.camera.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Available subjects for photo-based problem solving.
 */
val solveSubjects = listOf(
    "auto" to "自动识别",
    "math" to "数学",
    "physics" to "物理",
    "chemistry" to "化学",
    "biology" to "生物",
    "chinese" to "语文",
    "english" to "英语"
)

/**
 * Subject selector component for the camera solve screen.
 *
 * Displays a horizontal row of filter chips so the user can hint at the
 * subject of the photographed problem before uploading.
 *
 * @param selectedSubject currently selected subject key (e.g. "auto", "math", …)
 * @param onSubjectChanged called when the user taps a different subject chip
 * @param modifier         optional [Modifier]
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectSelector(
    selectedSubject: String,
    onSubjectChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "选择科目",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            solveSubjects.forEach { (key, label) ->
                val isSelected = key == selectedSubject
                FilterChip(
                    selected = isSelected,
                    onClick = { onSubjectChanged(key) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = if (isSelected) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = MaterialTheme.colorScheme.outline,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    },
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    }
}
