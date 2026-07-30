package com.whis.app.msg.storage;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Room DAO for MSG history storage and user corrections (MSG_PLAN.md Section 4.1 & 4.3).
 */
@Dao
public interface MsgHistoryDao {

    @Insert
    long insert(MsgHistoryEntry entry);

    @Update
    void update(MsgHistoryEntry entry);

    @Query("SELECT * FROM msg_history WHERE sender = :sender ORDER BY timestamp DESC LIMIT 50")
    List<MsgHistoryEntry> getHistoryForSender(String sender);

    @Query("SELECT * FROM msg_history WHERE body_hash = :bodyHash LIMIT 1")
    MsgHistoryEntry getByBodyHash(String bodyHash);

    @Query("UPDATE msg_history SET user_correction = :correction WHERE id = :id")
    void updateCorrection(int id, String correction);

    @Query("SELECT * FROM msg_history ORDER BY timestamp DESC LIMIT 100")
    List<MsgHistoryEntry> getRecentVerdicts();
}
