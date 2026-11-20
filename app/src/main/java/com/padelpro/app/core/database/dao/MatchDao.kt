package com.padelpro.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.padelpro.app.core.database.entity.MatchEntity
import com.padelpro.app.core.database.entity.MatchEventEntity
import com.padelpro.app.core.database.entity.MatchParticipantEntity
import com.padelpro.app.core.database.entity.SetResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches WHERE status = 'ACTIVE' LIMIT 1")
    fun observeActiveMatch(): Flow<MatchEntity?>

    @Query("SELECT * FROM matches WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveMatchOnce(): MatchEntity?

    @Query("SELECT * FROM matches WHERE status = 'COMPLETED' ORDER BY COALESCE(ended_at, started_at) DESC")
    fun observeCompletedMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :matchId LIMIT 1")
    fun observeMatchById(matchId: String): Flow<MatchEntity?>

    @Query("SELECT * FROM matches WHERE id = :matchId LIMIT 1")
    suspend fun getMatchByIdOnce(matchId: String): MatchEntity?

    @Query("SELECT * FROM match_participants WHERE match_id = :matchId ORDER BY team ASC, position ASC")
    suspend fun getParticipantsForMatch(matchId: String): List<MatchParticipantEntity>

    @Query("SELECT * FROM set_results WHERE match_id = :matchId ORDER BY set_index ASC")
    suspend fun getSetResultsForMatch(matchId: String): List<SetResultEntity>

    @Query("SELECT * FROM match_events WHERE match_id = :matchId ORDER BY seq ASC")
    suspend fun getEventsForMatch(matchId: String): List<MatchEventEntity>

    @Query("SELECT * FROM match_events WHERE match_id = :matchId ORDER BY seq ASC")
    fun observeEventsForMatch(matchId: String): Flow<List<MatchEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMatchStrict(match: MatchEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertParticipants(participants: List<MatchParticipantEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvent(event: MatchEventEntity): Long

    @Query("SELECT COALESCE(MAX(seq), 0) FROM match_events WHERE match_id = :matchId")
    suspend fun getMaxEventSeq(matchId: String): Int

    @Query("SELECT * FROM match_events WHERE match_id = :matchId AND type = 'POINT_WON' ORDER BY seq DESC LIMIT 1")
    suspend fun getLastPointWonEvent(matchId: String): MatchEventEntity?

    @Query("DELETE FROM match_events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: Long): Int

    @Query(
        "UPDATE matches SET status = 'COMPLETED', winner_team = :winnerTeam, ended_at = :endedAt, duration_ms = :durationMs " +
            "WHERE id = :matchId AND status = 'ACTIVE'"
    )
    suspend fun completeMatchIfActive(
        matchId: String,
        winnerTeam: String,
        endedAt: Long,
        durationMs: Long
    ): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetResults(results: List<SetResultEntity>)

    @Query("DELETE FROM matches WHERE id = :matchId AND status = 'ACTIVE'")
    suspend fun deleteActiveMatchById(matchId: String): Int

    @Query("DELETE FROM matches WHERE status = 'ACTIVE'")
    suspend fun deleteAllActiveMatches(): Int

    @Query("SELECT COUNT(*) FROM match_participants WHERE player_id = :playerId")
    suspend fun countParticipationsForPlayer(playerId: String): Int

    @Query("DELETE FROM matches")
    suspend fun deleteAllMatches()
}
