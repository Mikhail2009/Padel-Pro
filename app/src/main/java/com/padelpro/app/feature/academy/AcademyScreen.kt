package com.padelpro.app.feature.academy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SportsBaseball
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.components.PpBottomSheet
import com.padelpro.app.core.designsystem.components.PpCard
import com.padelpro.app.core.designsystem.components.PpCourseCard
import com.padelpro.app.core.designsystem.components.PpFilterChip
import com.padelpro.app.core.designsystem.components.PpPrimaryButton
import com.padelpro.app.core.designsystem.components.PpProgressBar
import com.padelpro.app.core.designsystem.components.PpSecondaryButton
import com.padelpro.app.core.designsystem.components.PpStreakFlame
import com.padelpro.app.core.designsystem.components.PpXpRing
import com.padelpro.app.core.designsystem.components.levelTitleRes
import com.padelpro.app.core.designsystem.theme.PpAmberLed
import com.padelpro.app.core.designsystem.theme.PpBallTrail
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextMid
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.domain.model.CourseCategory

@Composable
fun AcademyScreen(
    viewModel: AcademyViewModel,
    listState: LazyListState,
    onOpenCourse: (String) -> Unit,
    onOpenRulebook: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showLockedSheet by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(PpSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PpSpacing.md)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.academy_title),
                        style = PpTextStyles.H1,
                        color = PpTextHigh,
                        modifier = Modifier.weight(1f)
                    )
                    PpSecondaryButton(
                        text = stringResource(R.string.academy_rulebook_btn),
                        leadingIcon = Icons.Rounded.MenuBook,
                        onClick = onOpenRulebook,
                        modifier = Modifier.testTag("academy_rulebook_button")
                    )
                }
            }

            item {
                val academyFraction = if (state.totalLessons == 0) 0f
                else state.completedLessons.toFloat() / state.totalLessons.toFloat()
                PpCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PpXpRing(
                            progress = academyFraction,
                            center = "${state.academyPercent}%",
                            caption = stringResource(levelTitleRes(state.level))
                        )
                        Spacer(Modifier.width(PpSpacing.lg))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(
                                    R.string.level_badge_format,
                                    state.level,
                                    stringResource(levelTitleRes(state.level))
                                ),
                                style = PpTextStyles.Micro,
                                color = PpGlassCyan
                            )
                            Spacer(Modifier.height(PpSpacing.xs))
                            Text(
                                text = stringResource(R.string.xp_total_format, state.totalXp),
                                style = PpTextStyles.Numeral,
                                color = PpAmberLed
                            )
                            Spacer(Modifier.height(PpSpacing.sm))
                            PpProgressBar(
                                progress = academyFraction,
                                brush = PpBallTrail,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(PpSpacing.md))
                            PpStreakFlame(streakDays = state.streakDays)
                        }
                    }
                }
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PpSpacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(CourseCategory.entries) { category ->
                        PpFilterChip(
                            label = stringResource(categoryLabelRes(category)),
                            selected = state.selectedCategory == category,
                            onClick = { viewModel.selectCategory(category) },
                            modifier = Modifier.testTag("academy_filter_${category.name.lowercase()}")
                        )
                    }
                }
            }

            items(state.filteredCourses, key = { it.id }) { course ->
                val statusLabel = when {
                    !course.isUnlocked -> stringResource(R.string.academy_status_locked)
                    course.isCompleted -> stringResource(R.string.academy_status_completed)
                    course.completedLessons > 0 -> stringResource(R.string.academy_status_continue)
                    else -> stringResource(R.string.academy_status_start)
                }
                val progressPercent = (course.progressFraction * 100f).toInt()
                PpCourseCard(
                    title = course.title.ifBlank { courseTitle(course.order) },
                    meta = pluralStringResource(
                        R.plurals.academy_course_meta_format,
                        course.lessonCount,
                        course.lessonCount,
                        course.estimatedMinutes
                    ),
                    progressFraction = course.progressFraction,
                    progressText = "$progressPercent%",
                    statusLabel = statusLabel,
                    icon = courseIcon(course.order),
                    isUnlocked = course.isUnlocked,
                    isCompleted = course.isCompleted,
                    isHighlighted = course.id == state.highlightedCourseId,
                    onClick = {
                        if (course.isUnlocked) {
                            onOpenCourse(course.id)
                        } else {
                            showLockedSheet = true
                        }
                    },
                    modifier = Modifier.testTag("course_card_${course.id}")
                )
            }
        }
    }

    if (showLockedSheet) {
        PpBottomSheet(
            onDismissRequest = { showLockedSheet = false },
            title = stringResource(R.string.academy_locked_sheet_title)
        ) {
            Text(
                text = stringResource(R.string.academy_locked_sheet_body),
                style = PpTextStyles.Body,
                color = PpTextMid
            )
            Spacer(Modifier.height(PpSpacing.xl))
            PpPrimaryButton(
                text = stringResource(R.string.academy_locked_sheet_ok),
                onClick = { showLockedSheet = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("academy_locked_ok_button")
            )
        }
    }
}

private fun categoryLabelRes(category: CourseCategory): Int = when (category) {
    CourseCategory.ALL -> R.string.academy_chip_all
    CourseCategory.FUNDAMENTALS -> R.string.academy_chip_fundamentals
    CourseCategory.RULES -> R.string.academy_chip_rules
    CourseCategory.SERVING -> R.string.academy_chip_serving
    CourseCategory.TECHNIQUE -> R.string.academy_chip_technique
    CourseCategory.TACTICS -> R.string.academy_chip_tactics
}

@Composable
internal fun courseTitle(order: Int): String = when (order) {
    1 -> stringResource(R.string.course_1_title)
    2 -> stringResource(R.string.course_2_title)
    3 -> stringResource(R.string.course_3_title)
    4 -> stringResource(R.string.course_4_title)
    5 -> stringResource(R.string.course_5_title)
    6 -> stringResource(R.string.course_6_title)
    7 -> stringResource(R.string.course_7_title)
    else -> stringResource(R.string.course_8_title)
}

@Composable
internal fun courseDescription(order: Int): String = when (order) {
    1 -> stringResource(R.string.course_1_desc)
    2 -> stringResource(R.string.course_2_desc)
    3 -> stringResource(R.string.course_3_desc)
    4 -> stringResource(R.string.course_4_desc)
    5 -> stringResource(R.string.course_5_desc)
    6 -> stringResource(R.string.course_6_desc)
    7 -> stringResource(R.string.course_7_desc)
    else -> stringResource(R.string.course_8_desc)
}

private fun courseIcon(order: Int): ImageVector = when (order) {
    1 -> Icons.Rounded.School
    2 -> Icons.Rounded.GridView
    3 -> Icons.Rounded.Rule
    4 -> Icons.Rounded.SportsBaseball
    5 -> Icons.Rounded.SportsTennis
    6 -> Icons.Rounded.Timeline
    7 -> Icons.Rounded.AutoStories
    else -> Icons.Rounded.Psychology
}
