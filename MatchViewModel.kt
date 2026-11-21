package com.padelpro.app.feature.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.core.datastore.AppSettings
import com.padelpro.app.core.datastore.SettingsRepository
import com.padelpro.app.domain.model.DeleteFriendResult
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.FriendActionError
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchSummary
import com.padelpro.app.domain.model.Player
import com.padelpro.app.domain.model.StartMatchParticipant
import com.padelpro.app.domain.model.StartMatchRequest
import com.padelpro.app.domain.model.StartMatchResult
import com.padelpro.app.domain.model.TeamSide
import com.padelpro.app.domain.repository.MatchRepository
import com.padelpro.app.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MatchSetupNavEvent {
    data object NavigateToLiveMatch : MatchSetupNavEvent
}

data class MatchUiState(
    val userProfile: Player? = null,
    val friends: List<Player> = emptyList(),
    val activeMatch: MatchSummary? = null,
    val recentMatches: List<MatchSummary> = emptyList(),
    val selectedFormat: MatchFormat = MatchFormat.DOUBLES,
    val selectedDeuceMode: DeuceMode = DeuceMode.GOLDEN_POINT,
    val autoSideSwap: Boolean = true,
    val firstServerIndex: Int = 0,
    val slotPlayers: List<Player?> = listOf(null, null, null, null),
    val canStartMatch: Boolean = false,
    val isStartingMatch: Boolean = false,
    val showActiveConflictDialog: Boolean = false,
    val friendError: FriendActionError? = null
) {
    val requiredSlotCount: Int
        get() = if (selectedFormat == MatchFormat.SINGLES) 2 else 4

    fun playerForSlot(slotIndex: Int): Player? = slotPlayers.getOrNull(slotIndex)
}

private data class SetupDraftState(
    val formatOverride: MatchFormat? = null,
    val deuceOverride: DeuceMode? = null,
    val firstServerIndex: Int = 0,
    val slotIds: List<String?> = listOf(null, null, null, null),
    val slotA1Customized: Boolean = false,
    val isStartingMatch: Boolean = false,
    val showActiveConflictDialog: Boolean = false,
    val friendError: FriendActionError? = null
)

class MatchViewModel(
    private val playerRepository: PlayerRepository,
    private val matchRepository: MatchRepository,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val draft = MutableStateFlow(SetupDraftState())
    private val _navEvents = MutableSharedFlow<MatchSetupNavEvent>(extraBufferCapacity = 1)
    val navEvents: SharedFlow<MatchSetupNavEvent> = _navEvents.asSharedFlow()

    val uiState: StateFlow<MatchUiState> = combine(
        playerRepository.observeUserProfile(),
        playerRepository.observeFriends(),
        matchRepository.observeActiveMatch(),
        matchRepository.observeRecentMatches(limit = 3),
        settingsRepository.settings,
        draft
    ) { values ->
        val user = values[0] as? Player
        @Suppress("UNCHECKED_CAST")
        val friends = values[1] as List<Player>
        val active = values[2] as? MatchSummary
        @Suppress("UNCHECKED_CAST")
        val recent = values[3] as List<MatchSummary>
        val settings = values[4] as AppSettings
        val currentDraft = values[5] as SetupDraftState

        val allPlayersById = buildMap<String, Player> {
            if (user != null) put(user.id, user)
            friends.forEach { put(it.id, it) }
        }

        val format = currentDraft.formatOverride ?: settings.defaultFormat
        val deuce = currentDraft.deuceOverride ?: settings.defaultDeuceMode
        val requiredCount = if (format == MatchFormat.SINGLES) 2 else 4
        val serverIdx = currentDraft.firstServerIndex.coerceIn(0, requiredCount - 1)

        val resolvedSlots = (0 until 4).map { idx ->
            val explicitId = currentDraft.slotIds[idx]
            when {
                explicitId != null -> allPlayersById[explicitId]
                idx == 0 && !currentDraft.slotA1Customized -> user
                else -> null
            }
        }

        val activeSlotPlayers = if (format == MatchFormat.SINGLES) {
            listOf(resolvedSlots[0], resolvedSlots[2] ?: resolvedSlots[1])
        } else {
            resolvedSlots
        }

        val filledDistinct = activeSlotPlayers.filterNotNull().map { it.id }.distinct().size == requiredCount

        MatchUiState(
            userProfile = user,
            friends = friends,
            activeMatch = active,
            recentMatches = recent,
            selectedFormat = format,
            selectedDeuceMode = deuce,
            autoSideSwap = settings.autoSideSwap,
            firstServerIndex = serverIdx,
            slotPlayers = activeSlotPlayers.let { list ->
                if (list.size == 4) list else listOf(list[0], null, list[1], null)
            },
            canStartMatch = filledDistinct && !currentDraft.isStartingMatch,
            isStartingMatch = currentDraft.isStartingMatch,
            showActiveConflictDialog = currentDraft.showActiveConflictDialog,
            friendError = currentDraft.friendError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MatchUiState()
    )

    fun setFormat(format: MatchFormat) {
        val current = draft.value
        val maxServer = if (format == MatchFormat.SINGLES) 1 else 3
        draft.value = current.copy(
            formatOverride = format,
            firstServerIndex = current.firstServerIndex.coerceIn(0, maxServer)
        )
    }

    fun setDeuceMode(mode: DeuceMode) {
        draft.value = draft.value.copy(deuceOverride = mode)
    }

    fun setAutoSideSwap(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoSideSwap(enabled)
        }
    }

    fun setFirstServerIndex(index: Int) {
        val maxIndex = if (uiState.value.selectedFormat == MatchFormat.SINGLES) 1 else 3
        draft.value = draft.value.copy(firstServerIndex = index.coerceIn(0, maxIndex))
    }

    /**
     * Slot indices are normalized to 4 internal positions:
     * 0 = Team A player 1, 1 = Team A player 2 (doubles only),
     * 2 = Team B player 1, 3 = Team B player 2 (doubles only).
     */
    fun assignPlayerToSlot(internalSlotIndex: Int, player: Player) {
        if (internalSlotIndex !in 0..3) return
        val current = draft.value
        val user = uiState.value.userProfile
        val effectiveIds = current.slotIds.mapIndexed { idx, id ->
            if (idx == 0 && id == null && !current.slotA1Customized) user?.id else id
        }.toMutableList()

        val existingSlotOfPlayer = effectiveIds.indexOfFirst { it == player.id }
        val previousOccupant = effectiveIds[internalSlotIndex]
        if (existingSlotOfPlayer >= 0 && existingSlotOfPlayer != internalSlotIndex) {
            effectiveIds[existingSlotOfPlayer] = previousOccupant
        }
        effectiveIds[internalSlotIndex] = player.id

        draft.value = current.copy(
            slotIds = effectiveIds,
            slotA1Customized = current.slotA1Customized ||
                internalSlotIndex == 0 ||
                existingSlotOfPlayer == 0
        )
    }

    fun clearSlot(internalSlotIndex: Int) {
        if (internalSlotIndex !in 0..3) return
        val current = draft.value
        val updated = current.slotIds.toMutableList()
        updated[internalSlotIndex] = null
        draft.value = current.copy(
            slotIds = updated,
            slotA1Customized = current.slotA1Customized || internalSlotIndex == 0
        )
    }

    fun createAndAssignFriend(
        displayName: String,
        avatarId: Int,
        targetInternalSlotIndex: Int?
    ) {
        val trimmed = displayName.trim().take(20)
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val created = playerRepository.createFriend(trimmed, avatarId)
            if (targetInternalSlotIndex != null) {
                assignPlayerToSlot(targetInternalSlotIndex, created)
            }
        }
    }

    fun updateFriend(playerId: String, displayName: String, avatarId: Int) {
        val trimmed = displayName.trim().take(20)
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            playerRepository.updateFriend(playerId, trimmed, avatarId)
        }
    }

    fun deleteFriend(playerId: String) {
        viewModelScope.launch {
            when (playerRepository.deleteFriend(playerId)) {
                DeleteFriendResult.DELETED -> {
                    val current = draft.value
                    val updated = current.slotIds.map { if (it == playerId) null else it }
                    draft.value = current.copy(
                        slotIds = updated,
                        friendError = null
                    )
                }
                DeleteFriendResult.IN_USE_BY_MATCHES -> {
                    draft.value = draft.value.copy(friendError = FriendActionError.IN_USE_BY_MATCHES)
                }
                DeleteFriendResult.NOT_FOUND -> Unit
            }
        }
    }

    fun clearFriendError() {
        draft.value = draft.value.copy(friendError = null)
    }

    fun dismissActiveConflictDialog() {
        draft.value = draft.value.copy(showActiveConflictDialog = false)
    }

    fun startMatch(cancelExisting: Boolean = false) {
        val snapshot = uiState.value
        if (!snapshot.canStartMatch) return

        val participants = buildParticipants(snapshot) ?: return
        val firstServerTeam = when (snapshot.selectedFormat) {
            MatchFormat.SINGLES -> if (snapshot.firstServerIndex == 0) TeamSide.A else TeamSide.B
            MatchFormat.DOUBLES -> if (snapshot.firstServerIndex < 2) TeamSide.A else TeamSide.B
        }

        val request = StartMatchRequest(
            format = snapshot.selectedFormat,
            deuceMode = snapshot.selectedDeuceMode,
            bestOf = 3,
            firstServerTeam = firstServerTeam,
            participants = participants
        )

        draft.value = draft.value.copy(
            isStartingMatch = true,
            showActiveConflictDialog = false
        )

        viewModelScope.launch {
            when (matchRepository.startMatch(request, cancelExisting = cancelExisting)) {
                is StartMatchResult.Started -> {
                    draft.value = draft.value.copy(isStartingMatch = false)
                    _navEvents.emit(MatchSetupNavEvent.NavigateToLiveMatch)
                }
                is StartMatchResult.ActiveMatchExists -> {
                    draft.value = draft.value.copy(
                        isStartingMatch = false,
                        showActiveConflictDialog = true
                    )
                }
            }
        }
    }

    private fun buildParticipants(state: MatchUiState): List<StartMatchParticipant>? {
        return if (state.selectedFormat == MatchFormat.SINGLES) {
            val pA = state.slotPlayers[0] ?: return null
            val pB = state.slotPlayers[2] ?: return null
            listOf(
                StartMatchParticipant(playerId = pA.id, team = TeamSide.A, position = 0, isUser = pA.isUser),
                StartMatchParticipant(playerId = pB.id, team = TeamSide.B, position = 0, isUser = pB.isUser)
            )
        } else {
            val pA1 = state.slotPlayers[0] ?: return null
            val pA2 = state.slotPlayers[1] ?: return null
            val pB1 = state.slotPlayers[2] ?: return null
            val pB2 = state.slotPlayers[3] ?: return null

            // Put the designated first server at position 0 of their team so replay preserves exact server order.
            val teamAOrdered = if (state.firstServerIndex == 1) listOf(pA2, pA1) else listOf(pA1, pA2)
            val teamBOrdered = if (state.firstServerIndex == 3) listOf(pB2, pB1) else listOf(pB1, pB2)

            listOf(
                StartMatchParticipant(playerId = teamAOrdered[0].id, team = TeamSide.A, position = 0, isUser = teamAOrdered[0].isUser),
                StartMatchParticipant(playerId = teamAOrdered[1].id, team = TeamSide.A, position = 1, isUser = teamAOrdered[1].isUser),
                StartMatchParticipant(playerId = teamBOrdered[0].id, team = TeamSide.B, position = 0, isUser = teamBOrdered[0].isUser),
                StartMatchParticipant(playerId = teamBOrdered[1].id, team = TeamSide.B, position = 1, isUser = teamBOrdered[1].isUser)
            )
        }
    }

    fun formatMatchDate(epochMillis: Long): String = timeProvider.formatShortDate(epochMillis)
}
