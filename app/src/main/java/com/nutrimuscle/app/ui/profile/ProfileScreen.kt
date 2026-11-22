package com.nutrimuscle.app.ui.profile

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutrimuscle.app.R
import com.nutrimuscle.app.data.local.prefs.GenderOption
import com.nutrimuscle.app.data.local.prefs.UnitSystem
import com.nutrimuscle.app.domain.model.ActivityLevel
import com.nutrimuscle.app.domain.model.AppLanguage
import com.nutrimuscle.app.domain.model.BmiCategory
import com.nutrimuscle.app.domain.model.FitnessGoal
import com.nutrimuscle.app.domain.model.MealType
import com.nutrimuscle.app.domain.model.NutritionTargets
import com.nutrimuscle.app.domain.model.ThemeMode
import com.nutrimuscle.app.notifications.MealReminderSlot
import com.nutrimuscle.app.notifications.ReminderSettings
import com.nutrimuscle.app.ui.components.PastelCard
import com.nutrimuscle.app.ui.components.PillGauge
import com.nutrimuscle.app.ui.haptics.rememberHaptics
import com.nutrimuscle.app.ui.theme.CardCornerRadius
import com.nutrimuscle.app.ui.theme.LocalAppColors
import com.nutrimuscle.app.ui.theme.ThemePreset
import com.nutrimuscle.app.ui.theme.ThemeViewModel
import com.nutrimuscle.app.ui.theme.previewSwatches
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

private enum class BodyMetricEditor {
    WEIGHT,
    HEIGHT,
    AGE
}

private enum class SavedAnchor {
    NAME,
    TARGETS,
    WEIGHT,
    HEIGHT,
    AGE,
    GOAL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val themeState by themeViewModel.themeState.collectAsStateWithLifecycle()
    val profile = state.profile
    val extras = state.extras
    val targets = state.targets
    val autoTargets = state.autoTargets
    val reminders = state.reminders
    val appColors = LocalAppColors.current
    val haptics = rememberHaptics()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val fmt = remember { NumberFormat.getIntegerInstance(Locale.US) }
    val isBurmese = profile.language == AppLanguage.MM
    val isImperial = extras.unitSystem == UnitSystem.IMPERIAL
    val isDarkPreview = themeState.preset.forcesDark || when (themeState.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    var showNameDialog by remember { mutableStateOf(false) }
    var showTargetsSheet by remember { mutableStateOf(false) }
    var activeBodyEditor by remember { mutableStateOf<BodyMetricEditor?>(null) }
    var showRemindersSheet by remember { mutableStateOf(false) }
    var editingTimeSlot by remember { mutableStateOf<MealReminderSlot?>(null) }
    var notificationDenied by remember { mutableStateOf(false) }
    var pendingTestAfterGrant by remember { mutableStateOf(false) }
    var savedPulse by remember { mutableStateOf<Pair<SavedAnchor, Int>?>(null) }

    fun flashSaved(anchor: SavedAnchor) {
        savedPulse = anchor to ((savedPulse?.second ?: 0) + 1)
    }

    LaunchedEffect(savedPulse) {
        if (savedPulse != null) {
            delay(1300L)
            savedPulse = null
        }
    }

    val exportedMsg = stringResource(R.string.backup_exported_msg)
    val importedMsg = stringResource(R.string.backup_imported_msg)
    val errorMsg = stringResource(R.string.backup_error_msg)

    LaunchedEffect(state.backupEvent) {
        state.backupEvent?.let { (_, ok) ->
            snackbarHostState.showSnackbar(if (ok) importedMsg else errorMsg)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationDenied = !granted
        if (granted) {
            viewModel.setMasterRemindersEnabled(true)
            if (pendingTestAfterGrant) {
                pendingTestAfterGrant = false
                viewModel.sendTestNotification { ok ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (ok) {
                                if (isBurmese) "စမ်းသပ်အသိပေးချက် ပို့ပြီးပါပြီ" else "Test notification sent"
                            } else {
                                if (isBurmese) "အသိပေးချက် ခွင့်ပြုချက် လိုအပ်ပါသည်" else "Notification permission required"
                            }
                        )
                    }
                }
            }
        } else {
            pendingTestAfterGrant = false
            viewModel.setMasterRemindersEnabled(false)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = viewModel.exportBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                }.onSuccess {
                    haptics.confirm()
                    snackbarHostState.showSnackbar(exportedMsg)
                }.onFailure {
                    haptics.reject()
                    snackbarHostState.showSnackbar(errorMsg)
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (raw != null) {
                    haptics.confirm()
                    viewModel.importBackupJson(raw)
                } else {
                    haptics.reject()
                    snackbarHostState.showSnackbar(errorMsg)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header: round avatar with initial (tap to edit name), name, and goal chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(appColors.card1)
                        .clickable {
                            haptics.click()
                            showNameDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = extras.displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "A",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
                        color = appColors.onCard
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            haptics.click()
                            showNameDialog = true
                        },
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = extras.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = appColors.mutedText,
                            modifier = Modifier.size(15.dp)
                        )
                        SavedCheckPill(visible = savedPulse?.first == SavedAnchor.NAME)
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(appColors.accent)
                            .padding(horizontal = 11.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = goalLabel(profile.goal, isBurmese),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = appColors.onCard
                        )
                    }
                }
            }

            // 2. Daily targets card: 4 PillGauges + Edit button
            PastelCard(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isBurmese) "နေ့စဉ်ပစ်မှတ်များ" else "Daily targets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (extras.targetOverrides.hasAnyOverride) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(appColors.card1.copy(alpha = 0.3f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isBurmese) "ကိုယ်တိုင်" else "Custom",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        SavedCheckPill(visible = savedPulse?.first == SavedAnchor.TARGETS)
                    }

                    TextButton(
                        onClick = {
                            haptics.click()
                            showTargetsSheet = true
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.edit_action),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val calVal = targets.calorieTarget.roundToInt()
                val calDisplay = if (calVal >= 1000) String.format(Locale.US, "%.1fk", calVal / 1000.0) else "$calVal"
                val calProg = (targets.calorieTarget / 3200.0).toFloat().coerceIn(0.25f, 1f)
                val macroKcalSum = (targets.proteinGrams * 4.0 + targets.carbsGrams * 4.0 + targets.fatGrams * 9.0)
                    .coerceAtLeast(1.0)
                val protProg = ((targets.proteinGrams * 4.0 / macroKcalSum) * 1.9).toFloat().coerceIn(0.22f, 1f)
                val carbProg = ((targets.carbsGrams * 4.0 / macroKcalSum) * 1.6).toFloat().coerceIn(0.22f, 1f)
                val fatProg = ((targets.fatGrams * 9.0 / macroKcalSum) * 2.1).toFloat().coerceIn(0.22f, 1f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    PillGauge(
                        label = if (isBurmese) "ကယ်လို" else "Cal",
                        value = calDisplay,
                        progress = calProg,
                        fillColor = appColors.ring,
                        pillWidth = 54.dp,
                        pillHeight = 112.dp
                    )
                    PillGauge(
                        label = if (isBurmese) "ပရိုတင်း" else "Protein",
                        value = "${targets.proteinGrams.roundToInt()}g",
                        progress = protProg,
                        fillColor = appColors.card1,
                        pillWidth = 54.dp,
                        pillHeight = 112.dp
                    )
                    PillGauge(
                        label = if (isBurmese) "ကာဗို" else "Carbs",
                        value = "${targets.carbsGrams.roundToInt()}g",
                        progress = carbProg,
                        fillColor = appColors.card3,
                        pillWidth = 54.dp,
                        pillHeight = 112.dp
                    )
                    PillGauge(
                        label = if (isBurmese) "အဆီ" else "Fat",
                        value = "${targets.fatGrams.roundToInt()}g",
                        progress = fatProg,
                        fillColor = appColors.card2,
                        pillWidth = 54.dp,
                        pillHeight = 112.dp
                    )
                }
            }

            // 3. Body section
            SectionHeader(text = stringResource(R.string.section_body))

            val weightDisplay = if (isImperial) {
                String.format(Locale.US, "%.0f", profile.weightKg * 2.20462)
            } else {
                formatCleanNum(profile.weightKg)
            }
            val weightUnit = if (isImperial) "lb" else "kg"

            val heightDisplay = if (isImperial) {
                val totalInches = (profile.heightCm / 2.54).roundToInt()
                "${totalInches / 12}'${totalInches % 12}\""
            } else {
                formatCleanNum(profile.heightCm)
            }
            val heightUnit = if (isImperial) "ft" else "cm"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BodyStatTile(
                    label = if (isBurmese) "အလေးချိန်" else "Weight",
                    value = weightDisplay,
                    unit = weightUnit,
                    tint = appColors.card1.copy(alpha = 0.35f),
                    saved = savedPulse?.first == SavedAnchor.WEIGHT,
                    onClick = {
                        haptics.click()
                        activeBodyEditor = BodyMetricEditor.WEIGHT
                    },
                    modifier = Modifier.weight(1f)
                )
                BodyStatTile(
                    label = if (isBurmese) "အရပ်" else "Height",
                    value = heightDisplay,
                    unit = heightUnit,
                    tint = appColors.card2.copy(alpha = 0.35f),
                    saved = savedPulse?.first == SavedAnchor.HEIGHT,
                    onClick = {
                        haptics.click()
                        activeBodyEditor = BodyMetricEditor.HEIGHT
                    },
                    modifier = Modifier.weight(1f)
                )
                BodyStatTile(
                    label = if (isBurmese) "အသက်" else "Age",
                    value = profile.age.toString(),
                    unit = if (isBurmese) "နှစ်" else "yrs",
                    tint = appColors.card3.copy(alpha = 0.45f),
                    saved = savedPulse?.first == SavedAnchor.AGE,
                    onClick = {
                        haptics.click()
                        activeBodyEditor = BodyMetricEditor.AGE
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Gender 3-option segmented control + BMI chip
            PastelCard(color = MaterialTheme.colorScheme.surface) {
                PillSegmentedRow(
                    modifier = Modifier.fillMaxWidth(),
                    options = listOf(
                        stringResource(R.string.gender_male) to (extras.genderOption == GenderOption.MALE),
                        stringResource(R.string.gender_female) to (extras.genderOption == GenderOption.FEMALE),
                        (if (isBurmese) "အခြား" else "Other") to (extras.genderOption == GenderOption.OTHER)
                    ),
                    onSelect = { idx ->
                        viewModel.selectGenderOption(GenderOption.entries[idx])
                        flashSaved(SavedAnchor.TARGETS)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                val bmiChipBg = when (targets.bmiCategory) {
                    BmiCategory.NORMAL -> appColors.success.copy(alpha = 0.45f)
                    BmiCategory.UNDERWEIGHT -> appColors.card1.copy(alpha = 0.45f)
                    BmiCategory.OVERWEIGHT -> appColors.warning.copy(alpha = 0.45f)
                    BmiCategory.OBESE -> MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BMI",
                        style = MaterialTheme.typography.labelMedium,
                        color = appColors.mutedText
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(bmiChipBg)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.1f", targets.bmi)} · ${bmiCategoryLabel(targets.bmiCategory)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 4. Goal section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(text = stringResource(R.string.section_goal))
                SavedCheckPill(visible = savedPulse?.first == SavedAnchor.GOAL)
            }

            PastelCard(color = MaterialTheme.colorScheme.surface) {
                PillSegmentedRow(
                    modifier = Modifier.fillMaxWidth(),
                    options = listOf(
                        goalLabel(FitnessGoal.MUSCLE_GAIN, isBurmese) to (profile.goal == FitnessGoal.MUSCLE_GAIN),
                        goalLabel(FitnessGoal.FAT_LOSS, isBurmese) to (profile.goal == FitnessGoal.FAT_LOSS),
                        goalLabel(FitnessGoal.MAINTAIN, isBurmese) to (profile.goal == FitnessGoal.MAINTAIN)
                    ),
                    onSelect = { idx ->
                        val picked = listOf(FitnessGoal.MUSCLE_GAIN, FitnessGoal.FAT_LOSS, FitnessGoal.MAINTAIN)[idx]
                        viewModel.selectGoal(picked)
                        flashSaved(SavedAnchor.GOAL)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                val activityIndex = ActivityLevel.entries.indexOf(profile.activityLevel).coerceIn(0, 4)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activityLabel(profile.activityLevel),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "×${profile.activityLevel.factor}",
                        style = MaterialTheme.typography.labelMedium,
                        color = appColors.mutedText
                    )
                }

                Slider(
                    value = activityIndex.toFloat(),
                    onValueChange = { raw ->
                        val nextIdx = raw.roundToInt().coerceIn(0, 4)
                        val nextLevel = ActivityLevel.entries[nextIdx]
                        if (nextLevel != profile.activityLevel) {
                            haptics.tick()
                            viewModel.selectActivity(nextLevel)
                            flashSaved(SavedAnchor.GOAL)
                        }
                    },
                    valueRange = 0f..4f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = appColors.ring,
                        activeTrackColor = appColors.card1,
                        inactiveTrackColor = appColors.ringTrack,
                        activeTickColor = appColors.onCard,
                        inactiveTickColor = appColors.mutedText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = activityDescription(profile.activityLevel, isBurmese),
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 5. Appearance section
            SectionHeader(text = if (isBurmese) "အပြင်အဆင်" else "Appearance")

            PastelCard(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    themeViewModel.availablePresets.forEach { preset ->
                        val selected = themeState.preset == preset
                        val swatches = remember(preset, isDarkPreview) {
                            preset.previewSwatches(isDarkPreview)
                        }
                        Column(
                            modifier = Modifier
                                .width(84.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .then(
                                    if (selected) {
                                        Modifier.border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable {
                                    if (!selected) {
                                        haptics.tick()
                                        themeViewModel.setPreset(preset)
                                    }
                                }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(swatches[0])
                                    .padding(5.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    swatches.drop(1).forEach { swatch ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(26.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(swatch)
                                        )
                                    }
                                }
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onSurface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (isBurmese) preset.labelMm else preset.labelEn,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                PillSegmentedRow(
                    modifier = Modifier.fillMaxWidth(),
                    options = listOf(
                        stringResource(R.string.theme_light) to (!themeState.preset.forcesDark && themeState.mode == ThemeMode.LIGHT),
                        stringResource(R.string.theme_dark) to (themeState.preset.forcesDark || themeState.mode == ThemeMode.DARK),
                        stringResource(R.string.theme_system) to (!themeState.preset.forcesDark && themeState.mode == ThemeMode.SYSTEM)
                    ),
                    onSelect = { idx ->
                        val picked = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)[idx]
                        themeViewModel.setMode(picked)
                        viewModel.selectThemeMode(picked)
                    }
                )
            }

            // 6. App section
            SectionHeader(text = stringResource(R.string.section_app))

            PastelCard(color = MaterialTheme.colorScheme.surface) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Language
                    AppSettingRow(
                        icon = Icons.Outlined.Translate,
                        title = if (isBurmese) "ဘာသာစကား" else "Language",
                        trailing = {
                            CompactPillToggle(
                                options = listOf("EN", "မြန်မာ"),
                                selectedIndex = if (profile.language == AppLanguage.EN) 0 else 1,
                                onSelect = { idx ->
                                    viewModel.selectLanguage(if (idx == 0) AppLanguage.EN else AppLanguage.MM)
                                }
                            )
                        }
                    )

                    // Units
                    AppSettingRow(
                        icon = Icons.Outlined.Straighten,
                        title = if (isBurmese) "ယူနစ်" else "Units",
                        trailing = {
                            CompactPillToggle(
                                options = listOf("kg·cm", "lb·ft"),
                                selectedIndex = if (extras.unitSystem == UnitSystem.METRIC) 0 else 1,
                                onSelect = { idx ->
                                    viewModel.selectUnitSystem(if (idx == 0) UnitSystem.METRIC else UnitSystem.IMPERIAL)
                                }
                            )
                        }
                    )

                    // Reminders (tap row to open meal times sheet; switch toggles master)
                    val activeTimesSummary = reminders.allSlots
                        .filter { it.enabled }
                        .joinToString(" · ") { String.format(Locale.US, "%02d:%02d", it.hour, it.minute) }
                        .ifEmpty { if (isBurmese) "အချိန်သတ်မှတ်ရန် နှိပ်ပါ" else "Tap to set meal times" }

                    AppSettingRow(
                        icon = Icons.Outlined.Notifications,
                        title = if (isBurmese) "အသိပေးချက်များ" else "Reminders",
                        subtitle = if (reminders.masterEnabled) activeTimesSummary else (if (isBurmese) "ပိတ်ထားသည် · အချိန်ပြင်ရန် နှိပ်ပါ" else "Off · Tap to set meal times"),
                        onRowClick = {
                            haptics.click()
                            showRemindersSheet = true
                        },
                        trailing = {
                            Switch(
                                checked = reminders.masterEnabled,
                                onCheckedChange = { wantEnable ->
                                    haptics.tick()
                                    if (!wantEnable) {
                                        viewModel.setMasterRemindersEnabled(false)
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !viewModel.hasNotificationPermission()) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        notificationDenied = false
                                        viewModel.setMasterRemindersEnabled(true)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                                    checkedTrackColor = appColors.success
                                )
                            )
                        }
                    )

                    // Haptic feedback
                    AppSettingRow(
                        icon = Icons.Outlined.Vibration,
                        title = if (isBurmese) "တုန်ခါမှု တုံ့ပြန်ချက်" else "Haptic feedback",
                        trailing = {
                            Switch(
                                checked = state.hapticFeedbackEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) haptics.tick()
                                    viewModel.setHapticFeedbackEnabled(enabled)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                                    checkedTrackColor = appColors.success
                                )
                            )
                        }
                    )

                    // Backup and Restore
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.background)
                                .clickable {
                                    haptics.click()
                                    exportLauncher.launch("nutrimuscle-backup.json")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Upload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.backup_export),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.background)
                                .clickable {
                                    haptics.click()
                                    importLauncher.launch(arrayOf("application/json"))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.backup_import),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // About row
                    AppSettingRow(
                        icon = Icons.Outlined.Info,
                        title = if (isBurmese) "အကြောင်းအရာ · v1.0" else "About · v1.0",
                        subtitle = stringResource(R.string.estimates_only),
                        trailing = {}
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 76.dp)
        )
    }

    // Small dialog to edit display name
    if (showNameDialog) {
        EditNameDialog(
            initialName = extras.displayName,
            isBurmese = isBurmese,
            onDismiss = { showNameDialog = false },
            onSave = { updatedName ->
                haptics.tick()
                viewModel.updateDisplayName(updatedName)
                flashSaved(SavedAnchor.NAME)
                showNameDialog = false
            }
        )
    }

    // Daily targets manual override bottom sheet
    if (showTargetsSheet) {
        EditTargetsBottomSheet(
            targets = targets,
            autoTargets = autoTargets,
            hasOverride = extras.targetOverrides.hasAnyOverride,
            isBurmese = isBurmese,
            onDismiss = { showTargetsSheet = false },
            onUpdate = { cal, prot, carb, fat ->
                viewModel.saveTargetOverrides(cal, prot, carb, fat)
                flashSaved(SavedAnchor.TARGETS)
            },
            onResetAuto = {
                haptics.tick()
                viewModel.resetTargetsToAuto()
                flashSaved(SavedAnchor.TARGETS)
            }
        )
    }

    // Body metric ruler-slider bottom sheet (Weight / Height / Age)
    activeBodyEditor?.let { metric ->
        BodyMetricBottomSheet(
            metric = metric,
            weightKg = profile.weightKg,
            heightCm = profile.heightCm,
            age = profile.age,
            isImperial = isImperial,
            isBurmese = isBurmese,
            onDismiss = { activeBodyEditor = null },
            onWeightChanged = { kg ->
                if (viewModel.updateWeightKg(kg)) {
                    flashSaved(SavedAnchor.WEIGHT)
                    true
                } else false
            },
            onHeightChanged = { cm ->
                if (viewModel.updateHeightCm(cm)) {
                    flashSaved(SavedAnchor.HEIGHT)
                    true
                } else false
            },
            onAgeChanged = { years ->
                if (viewModel.updateAge(years)) {
                    flashSaved(SavedAnchor.AGE)
                    true
                } else false
            }
        )
    }

    // Meal reminders times bottom sheet
    if (showRemindersSheet) {
        RemindersBottomSheet(
            reminders = reminders,
            isBurmese = isBurmese,
            notificationDenied = notificationDenied,
            hasNotificationPermission = viewModel.hasNotificationPermission(),
            canScheduleExactAlarms = viewModel.canScheduleExactAlarms(),
            onDismiss = { showRemindersSheet = false },
            onToggleMaster = { wantEnable ->
                haptics.tick()
                if (!wantEnable) {
                    viewModel.setMasterRemindersEnabled(false)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !viewModel.hasNotificationPermission()) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    notificationDenied = false
                    viewModel.setMasterRemindersEnabled(true)
                }
            },
            onToggleSlot = { mealType, on ->
                haptics.tick()
                viewModel.setMealReminderEnabled(mealType, on)
            },
            onEditTime = { slot ->
                haptics.click()
                editingTimeSlot = slot
            },
            onOpenNotificationSettings = {
                runCatching { context.startActivity(viewModel.appNotificationSettingsIntent()) }
            },
            onOpenExactAlarmSettings = {
                runCatching { context.startActivity(viewModel.exactAlarmSettingsIntent()) }
            },
            onSendTest = {
                haptics.click()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !viewModel.hasNotificationPermission()) {
                    pendingTestAfterGrant = true
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.sendTestNotification { ok ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (ok) {
                                    if (isBurmese) "စမ်းသပ်အသိပေးချက် ပို့ပြီးပါပြီ" else "Test notification sent"
                                } else {
                                    if (isBurmese) "အသိပေးချက် ခွင့်ပြုချက် လိုအပ်ပါသည်" else "Notification permission required"
                                }
                            )
                        }
                    }
                }
            }
        )
    }

    // Styled 28dp pastel Time Picker dialog
    editingTimeSlot?.let { slot ->
        val pickerState = rememberTimePickerState(
            initialHour = slot.hour,
            initialMinute = slot.minute,
            is24Hour = true
        )
        Dialog(
            onDismissRequest = { editingTimeSlot = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            PastelCard(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = reminderMealLabel(slot.mealType, isBurmese),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state = pickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = MaterialTheme.colorScheme.background,
                            selectorColor = appColors.card1,
                            containerColor = MaterialTheme.colorScheme.surface,
                            timeSelectorSelectedContainerColor = appColors.card1,
                            timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.background,
                            timeSelectorSelectedContentColor = appColors.onCard,
                            timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { editingTimeSlot = null }) {
                        Text(stringResource(R.string.cancel_action), color = appColors.mutedText)
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(
                        shape = CircleShape,
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = appColors.accent,
                            contentColor = appColors.onCard
                        ),
                        onClick = {
                            haptics.tick()
                            viewModel.setMealReminderTime(slot.mealType, pickerState.hour, pickerState.minute)
                            editingTimeSlot = null
                        }
                    ) {
                        Text(stringResource(R.string.save_action), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    val appColors = LocalAppColors.current
    Text(
        text = text.uppercase(Locale.US),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = appColors.mutedText,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun SavedCheckPill(visible: Boolean) {
    val appColors = LocalAppColors.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(appColors.success.copy(alpha = 0.55f))
                .padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = appColors.onCard,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = "Saved",
                style = MaterialTheme.typography.labelSmall,
                color = appColors.onCard
            )
        }
    }
}

@Composable
private fun BodyStatTile(
    label: String,
    value: String,
    unit: String,
    tint: Color,
    saved: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    PastelCard(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = appColors.mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            AnimatedVisibility(visible = saved, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(appColors.success),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = appColors.onCard,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(tint)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AppSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onRowClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .let { if (onRowClick != null) it.clickable(onClick = onRowClick) else it }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = appColors.mutedText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        trailing()
    }
}

@Composable
private fun CompactPillToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val appColors = LocalAppColors.current
    val haptics = rememberHaptics()
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { idx, label ->
            val selected = idx == selectedIndex
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selected) appColors.card1 else Color.Transparent)
                    .clickable {
                        if (!selected) {
                            haptics.tick()
                            onSelect(idx)
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) appColors.onCard else appColors.mutedText
                )
            }
        }
    }
}

@Composable
private fun EditNameDialog(
    initialName: String,
    isBurmese: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val appColors = LocalAppColors.current
    val haptics = rememberHaptics()
    var text by remember { mutableStateOf(initialName) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PastelCard(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .imePadding()
        ) {
            Text(
                text = if (isBurmese) "အမည်ပြင်ရန်" else "Edit name",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 32) text = it },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedBorderColor = appColors.card1,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel_action), color = appColors.mutedText)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    shape = CircleShape,
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = appColors.accent,
                        contentColor = appColors.onCard
                    ),
                    onClick = {
                        if (text.isBlank()) {
                            haptics.reject()
                        } else {
                            onSave(text.trim())
                        }
                    }
                ) {
                    Text(stringResource(R.string.save_action), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BodyMetricBottomSheet(
    metric: BodyMetricEditor,
    weightKg: Double,
    heightCm: Double,
    age: Int,
    isImperial: Boolean,
    isBurmese: Boolean,
    onDismiss: () -> Unit,
    onWeightChanged: (Double) -> Boolean,
    onHeightChanged: (Double) -> Boolean,
    onAgeChanged: (Int) -> Boolean
) {
    val appColors = LocalAppColors.current
    val haptics = rememberHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val minVal = when (metric) {
        BodyMetricEditor.WEIGHT -> 20f
        BodyMetricEditor.HEIGHT -> 100f
        BodyMetricEditor.AGE -> 10f
    }
    val maxVal = when (metric) {
        BodyMetricEditor.WEIGHT -> 300f
        BodyMetricEditor.HEIGHT -> 250f
        BodyMetricEditor.AGE -> 100f
    }
    val stepVal = when (metric) {
        BodyMetricEditor.WEIGHT -> 0.5f
        BodyMetricEditor.HEIGHT -> 1f
        BodyMetricEditor.AGE -> 1f
    }

    var sliderVal by remember(metric) {
        mutableFloatStateOf(
            when (metric) {
                BodyMetricEditor.WEIGHT -> weightKg.toFloat().coerceIn(minVal, maxVal)
                BodyMetricEditor.HEIGHT -> heightCm.toFloat().coerceIn(minVal, maxVal)
                BodyMetricEditor.AGE -> age.toFloat().coerceIn(minVal, maxVal)
            }
        )
    }

    fun applyNewValue(raw: Float) {
        val clamped = raw.coerceIn(minVal, maxVal)
        val snapped = when (metric) {
            BodyMetricEditor.WEIGHT -> (clamped * 2f).roundToInt() / 2f
            BodyMetricEditor.HEIGHT -> clamped.roundToInt().toFloat()
            BodyMetricEditor.AGE -> clamped.roundToInt().toFloat()
        }
        if (raw < minVal || raw > maxVal) {
            haptics.reject()
            return
        }
        if (snapped != sliderVal) {
            sliderVal = snapped
            haptics.tick()
            when (metric) {
                BodyMetricEditor.WEIGHT -> onWeightChanged(snapped.toDouble())
                BodyMetricEditor.HEIGHT -> onHeightChanged(snapped.toDouble())
                BodyMetricEditor.AGE -> onAgeChanged(snapped.roundToInt())
            }
        }
    }

    val title = when (metric) {
        BodyMetricEditor.WEIGHT -> if (isBurmese) "အလေးချိန်" else "Weight"
        BodyMetricEditor.HEIGHT -> if (isBurmese) "အရပ်" else "Height"
        BodyMetricEditor.AGE -> if (isBurmese) "အသက်" else "Age"
    }
    val primaryDisplay = when (metric) {
        BodyMetricEditor.WEIGHT -> if (isImperial) {
            "${(sliderVal * 2.20462f).roundToInt()}"
        } else {
            formatCleanNum(sliderVal.toDouble())
        }
        BodyMetricEditor.HEIGHT -> if (isImperial) {
            val inches = (sliderVal / 2.54f).roundToInt()
            "${inches / 12}'${inches % 12}\""
        } else {
            sliderVal.roundToInt().toString()
        }
        BodyMetricEditor.AGE -> sliderVal.roundToInt().toString()
    }
    val unitDisplay = when (metric) {
        BodyMetricEditor.WEIGHT -> if (isImperial) "lb (${formatCleanNum(sliderVal.toDouble())} kg)" else "kg"
        BodyMetricEditor.HEIGHT -> if (isImperial) "ft (${sliderVal.roundToInt()} cm)" else "cm"
        BodyMetricEditor.AGE -> if (isBurmese) "နှစ်" else "years"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = CardCornerRadius, topEnd = CardCornerRadius),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Large display + +/- steppers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .clickable { applyNewValue(sliderVal - stepVal) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Remove,
                        contentDescription = "-",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = primaryDisplay,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = unitDisplay,
                        style = MaterialTheme.typography.labelMedium,
                        color = appColors.mutedText
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(appColors.accent)
                        .clickable { applyNewValue(sliderVal + stepVal) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "+",
                        tint = appColors.onCard
                    )
                }
            }

            // Horizontal ruler-style slider
            Column(modifier = Modifier.fillMaxWidth()) {
                val tickMajor = appColors.mutedText
                val tickMinor = appColors.ringTrack
                val activeIndicator = appColors.ring
                val ratio = ((sliderVal - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .padding(horizontal = 10.dp)
                ) {
                    val tickCount = 40
                    val stepX = size.width / tickCount
                    for (i in 0..tickCount) {
                        val x = i * stepX
                        val isMajor = i % 5 == 0
                        val h = if (isMajor) size.height * 0.78f else size.height * 0.42f
                        drawLine(
                            color = if (isMajor) tickMajor else tickMinor,
                            start = Offset(x, size.height - h),
                            end = Offset(x, size.height),
                            strokeWidth = if (isMajor) 2.dp.toPx() else 1.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    val markerX = ratio * size.width
                    drawLine(
                        color = activeIndicator,
                        start = Offset(markerX, 0f),
                        end = Offset(markerX, size.height),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                Slider(
                    value = sliderVal,
                    onValueChange = { applyNewValue(it) },
                    valueRange = minVal..maxVal,
                    colors = SliderDefaults.colors(
                        thumbColor = appColors.ring,
                        activeTrackColor = appColors.card1,
                        inactiveTrackColor = appColors.ringTrack
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${minVal.roundToInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = appColors.mutedText
                    )
                    Text(
                        text = "${maxVal.roundToInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = appColors.mutedText
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTargetsBottomSheet(
    targets: NutritionTargets,
    autoTargets: NutritionTargets,
    hasOverride: Boolean,
    isBurmese: Boolean,
    onDismiss: () -> Unit,
    onUpdate: (Double?, Double?, Double?, Double?) -> Unit,
    onResetAuto: () -> Unit
) {
    val appColors = LocalAppColors.current
    val haptics = rememberHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var calText by remember(targets.calorieTarget) { mutableStateOf(targets.calorieTarget.roundToInt().toString()) }
    var protText by remember(targets.proteinGrams) { mutableStateOf(targets.proteinGrams.roundToInt().toString()) }
    var carbText by remember(targets.carbsGrams) { mutableStateOf(targets.carbsGrams.roundToInt().toString()) }
    var fatText by remember(targets.fatGrams) { mutableStateOf(targets.fatGrams.roundToInt().toString()) }

    val pillColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.background,
        unfocusedContainerColor = MaterialTheme.colorScheme.background,
        focusedBorderColor = appColors.card1,
        unfocusedBorderColor = Color.Transparent
    )

    fun persistAuto() {
        val c = calText.toDoubleOrNull()
        val p = protText.toDoubleOrNull()
        val cb = carbText.toDoubleOrNull()
        val f = fatText.toDoubleOrNull()
        if (c != null && p != null && cb != null && f != null) {
            onUpdate(c, p, cb, f)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = CardCornerRadius, topEnd = CardCornerRadius),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBurmese) "နေ့စဉ်ပစ်မှတ် ပြင်ရန်" else "Daily targets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (hasOverride) {
                    TextButton(
                        onClick = {
                            onResetAuto()
                            calText = autoTargets.calorieTarget.roundToInt().toString()
                            protText = autoTargets.proteinGrams.roundToInt().toString()
                            carbText = autoTargets.carbsGrams.roundToInt().toString()
                            fatText = autoTargets.fatGrams.roundToInt().toString()
                        }
                    ) {
                        Text(
                            text = if (isBurmese) "Auto ပြန်ထား" else "Reset to auto",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = calText,
                    onValueChange = {
                        calText = it.filter { ch -> ch.isDigit() }
                        haptics.tick()
                        persistAuto()
                    },
                    label = { Text("Calories (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = CircleShape,
                    colors = pillColors,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = protText,
                    onValueChange = {
                        protText = it.filter { ch -> ch.isDigit() }
                        haptics.tick()
                        persistAuto()
                    },
                    label = { Text("Protein (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = CircleShape,
                    colors = pillColors,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = carbText,
                    onValueChange = {
                        carbText = it.filter { ch -> ch.isDigit() }
                        haptics.tick()
                        persistAuto()
                    },
                    label = { Text("Carbs (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = CircleShape,
                    colors = pillColors,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = fatText,
                    onValueChange = {
                        fatText = it.filter { ch -> ch.isDigit() }
                        haptics.tick()
                        persistAuto()
                    },
                    label = { Text("Fat (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = CircleShape,
                    colors = pillColors,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = if (isBurmese) {
                    "Auto တွက်ချက်မှု: ${autoTargets.calorieTarget.roundToInt()} kcal · ${autoTargets.proteinGrams.roundToInt()}g P · ${autoTargets.carbsGrams.roundToInt()}g C · ${autoTargets.fatGrams.roundToInt()}g F"
                } else {
                    "Auto calculated: ${autoTargets.calorieTarget.roundToInt()} kcal · ${autoTargets.proteinGrams.roundToInt()}g P · ${autoTargets.carbsGrams.roundToInt()}g C · ${autoTargets.fatGrams.roundToInt()}g F"
                },
                style = MaterialTheme.typography.labelSmall,
                color = appColors.mutedText
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemindersBottomSheet(
    reminders: ReminderSettings,
    isBurmese: Boolean,
    notificationDenied: Boolean,
    hasNotificationPermission: Boolean,
    canScheduleExactAlarms: Boolean,
    onDismiss: () -> Unit,
    onToggleMaster: (Boolean) -> Unit,
    onToggleSlot: (MealType, Boolean) -> Unit,
    onEditTime: (MealReminderSlot) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onSendTest: () -> Unit
) {
    val appColors = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = CardCornerRadius, topEnd = CardCornerRadius),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBurmese) "အစားအချိန် သတိပေးချက်" else "Meal reminders",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = reminders.masterEnabled,
                    onCheckedChange = onToggleMaster,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.surface,
                        checkedTrackColor = appColors.success
                    )
                )
            }

            if (notificationDenied || (reminders.masterEnabled && !hasNotificationPermission)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(appColors.warning.copy(alpha = 0.25f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBurmese) "အသိပေးချက် ပိတ်ထားသည်" else "Notifications blocked",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onOpenNotificationSettings) {
                        Text(
                            text = if (isBurmese) "Settings ဖွင့်" else "Open settings",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (reminders.masterEnabled && !canScheduleExactAlarms) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(appColors.accent.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBurmese) "အချိန်တိကျ Alarm ခွင့်ပြုပါ" else "Allow exact alarms for on-time alerts",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onOpenExactAlarmSettings) {
                        Text(
                            text = if (isBurmese) "ခွင့်ပြု" else "Allow",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            reminders.allSlots.forEach { slot ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = reminderMealLabel(slot.mealType, isBurmese),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (reminders.masterEnabled) MaterialTheme.colorScheme.onSurface else appColors.mutedText,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (reminders.masterEnabled && slot.enabled) appColors.card1.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable(enabled = reminders.masterEnabled) { onEditTime(slot) }
                            .padding(horizontal = 11.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", slot.hour, slot.minute),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (reminders.masterEnabled) MaterialTheme.colorScheme.onSurface else appColors.mutedText
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Switch(
                        checked = slot.enabled,
                        enabled = reminders.masterEnabled,
                        onCheckedChange = { on -> onToggleSlot(slot.mealType, on) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.surface,
                            checkedTrackColor = appColors.success
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(appColors.accent)
                    .clickable(onClick = onSendTest),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isBurmese) "စမ်းသပ်အသိပေးချက် ပို့မည်" else "Send test notification",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = appColors.onCard
                )
            }
        }
    }
}

@Composable
private fun PillSegmentedRow(
    options: List<Pair<String, Boolean>>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    val haptics = rememberHaptics()
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { idx, (label, selected) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(CircleShape)
                    .background(if (selected) appColors.card1 else Color.Transparent)
                    .clickable {
                        if (!selected) haptics.tick()
                        onSelect(idx)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) appColors.onCard else appColors.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatCleanNum(value: Double): String =
    if (floor(value) == value) value.toInt().toString() else String.format(Locale.US, "%.1f", value)

private fun reminderMealLabel(mealType: MealType, isBurmese: Boolean): String = if (isBurmese) {
    when (mealType) {
        MealType.BREAKFAST -> "မနက်စာ"
        MealType.LUNCH -> "နေ့လယ်စာ"
        MealType.DINNER -> "ညနေစာ"
        MealType.SNACK -> "ညနေသရေစာ"
    }
} else {
    when (mealType) {
        MealType.BREAKFAST -> "Breakfast"
        MealType.LUNCH -> "Lunch"
        MealType.DINNER -> "Dinner"
        MealType.SNACK -> "Evening snack"
    }
}

private fun goalLabel(goal: FitnessGoal, isBurmese: Boolean): String = if (isBurmese) {
    when (goal) {
        FitnessGoal.MUSCLE_GAIN -> "ကြွက်သားတည်ဆောက်"
        FitnessGoal.FAT_LOSS -> "အဆီချ"
        FitnessGoal.MAINTAIN -> "ထိန်းသိမ်း"
    }
} else {
    when (goal) {
        FitnessGoal.MUSCLE_GAIN -> "Build muscle"
        FitnessGoal.FAT_LOSS -> "Lose fat"
        FitnessGoal.MAINTAIN -> "Maintain"
    }
}

private fun activityDescription(level: ActivityLevel, isBurmese: Boolean): String = if (isBurmese) {
    when (level) {
        ActivityLevel.SEDENTARY -> "ရုံးထိုင်အလုပ်၊ လေ့ကျင့်ခန်းနည်းသည်"
        ActivityLevel.LIGHT -> "တစ်ပတ် ၁-၃ ရက် ပေါ့ပါးစွာ လေ့ကျင့်သည်"
        ActivityLevel.MODERATE -> "တစ်ပတ် ၃-၅ ရက် ပုံမှန်လေ့ကျင့်သည်"
        ActivityLevel.ACTIVE -> "တစ်ပတ် ၆-၇ ရက် ပြင်းထန်စွာ လေ့ကျင့်သည်"
        ActivityLevel.VERY_ACTIVE -> "တစ်နေ့ ၂ ကြိမ် သို့မဟုတ် ကိုယ်ကာယအလုပ်ကြမ်း"
    }
} else {
    when (level) {
        ActivityLevel.SEDENTARY -> "Desk job with little or no weekly exercise"
        ActivityLevel.LIGHT -> "Light training or walks 1–3 days per week"
        ActivityLevel.MODERATE -> "Moderate gym sessions 3–5 days per week"
        ActivityLevel.ACTIVE -> "Hard training 6–7 days per week"
        ActivityLevel.VERY_ACTIVE -> "Physical job or twice-daily intense training"
    }
}

@Composable
private fun activityLabel(level: ActivityLevel): String = when (level) {
    ActivityLevel.SEDENTARY -> stringResource(R.string.activity_sedentary)
    ActivityLevel.LIGHT -> stringResource(R.string.activity_light)
    ActivityLevel.MODERATE -> stringResource(R.string.activity_moderate)
    ActivityLevel.ACTIVE -> stringResource(R.string.activity_active)
    ActivityLevel.VERY_ACTIVE -> stringResource(R.string.activity_very_active)
}

@Composable
private fun bmiCategoryLabel(category: BmiCategory): String = when (category) {
    BmiCategory.UNDERWEIGHT -> stringResource(R.string.bmi_underweight)
    BmiCategory.NORMAL -> stringResource(R.string.bmi_normal)
    BmiCategory.OVERWEIGHT -> stringResource(R.string.bmi_overweight)
    BmiCategory.OBESE -> stringResource(R.string.bmi_obese)
}
