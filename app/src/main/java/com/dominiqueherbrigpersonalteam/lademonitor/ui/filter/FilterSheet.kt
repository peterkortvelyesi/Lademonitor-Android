package com.dominiqueherbrigpersonalteam.lademonitor.ui.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dominiqueherbrigpersonalteam.lademonitor.R
import com.dominiqueherbrigpersonalteam.lademonitor.ui.common.Fmt
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** Toolbar filter button; filled when a filter is active. */
@Composable
fun FilterIconButton(onClick: () -> Unit) {
    val range by SessionFilter.dateRange.collectAsStateWithLifecycle()
    IconButton(onClick = onClick) {
        Icon(
            if (range != null) Icons.Filled.FilterList else Icons.Outlined.FilterList,
            contentDescription = stringResource(R.string.filter_content_description)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val range by SessionFilter.dateRange.collectAsStateWithLifecycle()
    val zone = ZoneId.systemDefault()

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var customStart by remember {
        mutableStateOf(range?.first ?: System.currentTimeMillis() - 30L * 86_400_000)
    }
    var customEnd by remember { mutableStateOf(range?.last ?: System.currentTimeMillis()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text(stringResource(R.string.filter_sheet_title), style = MaterialTheme.typography.titleLarge)

            Text(
                stringResource(R.string.filter_date_range_section),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
            FilterPreset.entries.forEach { preset ->
                TextButton(
                    onClick = { SessionFilter.setDateRange(preset.range()); onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(preset.titleRes), modifier = Modifier.fillMaxWidth())
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            Text(
                stringResource(R.string.filter_custom_range_section),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.filter_from, Fmt.dateTimeShort(customStart).substringBefore(",")))
                }
                OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.filter_to, Fmt.dateTimeShort(customEnd).substringBefore(",")))
                }
            }
            Button(
                onClick = {
                    val start = Instant.ofEpochMilli(customStart).atZone(zone).toLocalDate()
                        .atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = Instant.ofEpochMilli(customEnd).atZone(zone).toLocalDate()
                        .atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
                    SessionFilter.setDateRange(minOf(start, end)..maxOf(start, end))
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) { Text(stringResource(R.string.action_apply)) }

            if (range != null) {
                TextButton(
                    onClick = { SessionFilter.clear(); onDismiss() },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text(stringResource(R.string.filter_clear), color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    if (showStartPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = customStart)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { customStart = it }
                    showStartPicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text(stringResource(R.string.action_cancel)) } }
        ) { DatePicker(state = state) }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = customEnd)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { customEnd = it }
                    showEndPicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text(stringResource(R.string.action_cancel)) } }
        ) { DatePicker(state = state) }
    }
}
