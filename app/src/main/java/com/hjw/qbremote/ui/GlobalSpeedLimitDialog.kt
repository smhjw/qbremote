package com.hjw.qbremote.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hjw.qbremote.R
import com.hjw.qbremote.data.ServerProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GlobalSpeedLimitDialog(
    limits: com.hjw.qbremote.data.model.GlobalSpeedLimits?,
    loading: Boolean,
    serverProfiles: List<ServerProfile>,
    selectedProfileId: String?,
    onSelectServer: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        dlKb: String, ulKb: String, altDlKb: String, altUlKb: String,
        schedulerEnabled: Boolean, scheduleFromHour: Int, scheduleFromMin: Int,
        scheduleToHour: Int, scheduleToMin: Int, schedulerDays: Int,
    ) -> Unit,
) {
    fun Long.toKbDisplay(): String = if (this <= 0L) "0" else (this / 1024L).toString()

    var dlKb by rememberSaveable(limits) { mutableStateOf(limits?.downloadLimit?.toKbDisplay() ?: "") }
    var ulKb by rememberSaveable(limits) { mutableStateOf(limits?.uploadLimit?.toKbDisplay() ?: "") }
    var altDlKb by rememberSaveable(limits) { mutableStateOf(limits?.altDownloadLimit?.toKbDisplay() ?: "") }
    var altUlKb by rememberSaveable(limits) { mutableStateOf(limits?.altUploadLimit?.toKbDisplay() ?: "") }

    var schedulerEnabled by rememberSaveable(limits) { mutableStateOf(limits?.schedulerEnabled ?: false) }
    var fromHour by rememberSaveable(limits) { mutableStateOf(limits?.scheduleFromHour?.toString() ?: "8") }
    var fromMin by rememberSaveable(limits) { mutableStateOf(limits?.scheduleFromMin?.toString() ?: "0") }
    var toHour by rememberSaveable(limits) { mutableStateOf(limits?.scheduleToHour?.toString() ?: "20") }
    var toMin by rememberSaveable(limits) { mutableStateOf(limits?.scheduleToMin?.toString() ?: "0") }
    var schedulerDays by rememberSaveable(limits) { mutableStateOf(limits?.schedulerDays ?: 0) }

    val dayLabels = listOf(
        stringResource(R.string.schedule_days_every_day),
        stringResource(R.string.schedule_days_weekdays),
        stringResource(R.string.schedule_days_weekends),
        stringResource(R.string.schedule_days_monday),
        stringResource(R.string.schedule_days_tuesday),
        stringResource(R.string.schedule_days_wednesday),
        stringResource(R.string.schedule_days_thursday),
        stringResource(R.string.schedule_days_friday),
        stringResource(R.string.schedule_days_saturday),
        stringResource(R.string.schedule_days_sunday),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.global_speed_limit_title)) },
        text = {
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // 服务器选择器（多服务器时显示）
                    if (serverProfiles.size > 1) {
                        var serverExpanded by remember { mutableStateOf(false) }
                        val selectedProfile = serverProfiles.find { it.id == selectedProfileId }
                        fun profileDisplayName(p: ServerProfile): String =
                            p.name.ifBlank { "${p.host}:${p.port}" }
                        ExposedDropdownMenuBox(
                            expanded = serverExpanded,
                            onExpandedChange = { serverExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = selectedProfile?.let { profileDisplayName(it) } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.speed_limit_server_label)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serverExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                            )
                            ExposedDropdownMenu(
                                expanded = serverExpanded,
                                onDismissRequest = { serverExpanded = false },
                            ) {
                                serverProfiles.forEach { profile ->
                                    DropdownMenuItem(
                                        text = { Text(profileDisplayName(profile)) },
                                        onClick = {
                                            serverExpanded = false
                                            if (profile.id != selectedProfileId) {
                                                onSelectServer(profile.id)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                    }

                    Text(
                        text = stringResource(R.string.normal_speed_section),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    OutlinedTextField(
                        value = dlKb,
                        onValueChange = { dlKb = it },
                        label = { Text(stringResource(R.string.global_dl_limit_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = ulKb,
                        onValueChange = { ulKb = it },
                        label = { Text(stringResource(R.string.global_ul_limit_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    HorizontalDivider()

                    Text(
                        text = stringResource(R.string.alt_speed_section),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    OutlinedTextField(
                        value = altDlKb,
                        onValueChange = { altDlKb = it },
                        label = { Text(stringResource(R.string.global_alt_dl_limit_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = altUlKb,
                        onValueChange = { altUlKb = it },
                        label = { Text(stringResource(R.string.global_alt_ul_limit_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(
                        text = stringResource(R.string.speed_limit_hint_zero_unlimited),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.alt_speed_schedule_enabled),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(R.string.alt_speed_schedule_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = schedulerEnabled,
                            onCheckedChange = { schedulerEnabled = it },
                        )
                    }
                    AnimatedVisibility(visible = schedulerEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 时间段卡片：上下两行
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    // 开始时间行
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = stringResource(R.string.alt_speed_schedule_from),
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            OutlinedTextField(
                                                value = fromHour,
                                                onValueChange = { v ->
                                                    if (v.isEmpty()) { fromHour = v; return@OutlinedTextField }
                                                    val n = v.toIntOrNull()
                                                    if (n != null && n in 0..23 && v.length <= 2) fromHour = v
                                                },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.width(56.dp),
                                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                    textAlign = TextAlign.Center,
                                                ),
                                            )
                                            Text(":", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            OutlinedTextField(
                                                value = fromMin,
                                                onValueChange = { v ->
                                                    if (v.isEmpty()) { fromMin = v; return@OutlinedTextField }
                                                    val n = v.toIntOrNull()
                                                    if (n != null && n in 0..59 && v.length <= 2) fromMin = v
                                                },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.width(56.dp),
                                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                    textAlign = TextAlign.Center,
                                                ),
                                            )
                                        }
                                    }
                                    HorizontalDivider()
                                    // 结束时间行
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = stringResource(R.string.alt_speed_schedule_to),
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            OutlinedTextField(
                                                value = toHour,
                                                onValueChange = { v ->
                                                    if (v.isEmpty()) { toHour = v; return@OutlinedTextField }
                                                    val n = v.toIntOrNull()
                                                    if (n != null && n in 0..23 && v.length <= 2) toHour = v
                                                },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.width(56.dp),
                                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                    textAlign = TextAlign.Center,
                                                ),
                                            )
                                            Text(":", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            OutlinedTextField(
                                                value = toMin,
                                                onValueChange = { v ->
                                                    if (v.isEmpty()) { toMin = v; return@OutlinedTextField }
                                                    val n = v.toIntOrNull()
                                                    if (n != null && n in 0..59 && v.length <= 2) toMin = v
                                                },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.width(56.dp),
                                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                    textAlign = TextAlign.Center,
                                                ),
                                            )
                                        }
                                    }
                                }
                            }
                            // 生效日
                            var daysExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = daysExpanded,
                                onExpandedChange = { daysExpanded = it },
                            ) {
                                OutlinedTextField(
                                    value = dayLabels.getOrElse(schedulerDays) { dayLabels[0] },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.alt_speed_schedule_days)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = daysExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                )
                                ExposedDropdownMenu(
                                    expanded = daysExpanded,
                                    onDismissRequest = { daysExpanded = false },
                                ) {
                                    dayLabels.forEachIndexed { index, label ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                schedulerDays = index
                                                daysExpanded = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        dlKb, ulKb, altDlKb, altUlKb,
                        schedulerEnabled,
                        fromHour.toIntOrNull()?.coerceIn(0, 23) ?: 0,
                        fromMin.toIntOrNull()?.coerceIn(0, 59) ?: 0,
                        toHour.toIntOrNull()?.coerceIn(0, 23) ?: 0,
                        toMin.toIntOrNull()?.coerceIn(0, 59) ?: 0,
                        schedulerDays,
                    )
                },
                enabled = !loading,
            ) {
                Text(stringResource(R.string.global_speed_limit_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.global_speed_limit_cancel))
            }
        },
    )
}
