package dev.dimvlachos.moodboard.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.dimvlachos.moodboard.domain.PhotoFilter
import dev.dimvlachos.moodboard.domain.SortOrder

/** The gallery filter's body; iOS shows it in a detent sheet, Android in a bottom sheet. */
@Composable
fun FilterSheetContent(
    filter: PhotoFilter,
    allTags: List<String>,
    onFilterChange: (PhotoFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Scrolls, so a long tag list stays reachable at the iOS sheet's medium detent.
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Favorites only",
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            Switch(
                checked = filter.favoritesOnly,
                onCheckedChange = { onFilterChange(filter.copy(favoritesOnly = it)) },
                modifier = Modifier.testTag("favoritesOnly"),
            )
        }
        Text("Tags", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            allTags.forEach { tag ->
                val selected = tag in filter.tags
                FilterChip(
                    selected = selected,
                    onClick = {
                        onFilterChange(
                            filter.copy(
                                tags = if (selected) filter.tags - tag else filter.tags + tag
                            )
                        )
                    },
                    label = { Text(tag) },
                )
            }
        }
        Text("Sort", style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SortOrder.entries.forEachIndexed { index, sort ->
                SegmentedButton(
                    selected = filter.sort == sort,
                    onClick = { onFilterChange(filter.copy(sort = sort)) },
                    shape = SegmentedButtonDefaults.itemShape(index, SortOrder.entries.size),
                    label = { Text(sort.name) },
                )
            }
        }
    }
}
