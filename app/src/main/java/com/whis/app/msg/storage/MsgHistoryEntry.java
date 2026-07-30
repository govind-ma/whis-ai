package com.whis.app.msg.storage;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity for SMS detection history (MSG_PLAN.md Section 4.3).
 * <p>
 * Stores SHA-256 hash of message body rather than raw body text for DPDP privacy compliance.
 */
@Entity(tableName = "msg_history", indices = {@Index(value = {"sender"}), @Index(value = {"body_hash"})})
public class MsgHistoryEntry {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public long timestamp;
    public String sender;
    public String body_hash;       // SHA-256 hash of body (DPDP privacy compliance)
    public String category;
    public String threat_level;
    public float confidence;
    public String reason_text;
    public int layers_used;
    public String user_correction;  // null, "SAFE", or "SCAM"

    public MsgHistoryEntry() {}

    @Ignore
    public MsgHistoryEntry(long timestamp, String sender, String body_hash, String category,
                           String threat_level, float confidence, String reason_text,
                           int layers_used, String user_correction) {
        this.timestamp = timestamp;
        this.sender = sender;
        this.body_hash = body_hash;
        this.category = category;
        this.threat_level = threat_level;
        this.confidence = confidence;
        this.reason_text = reason_text;
        this.layers_used = layers_used;
        this.user_correction = user_correction;
    }
}
