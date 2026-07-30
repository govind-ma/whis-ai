package com.whis.app.msg.storage;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Room SQLite Database for MSG module history (MSG_PLAN.md Section 4.1 & 4.3).
 */
@Database(entities = {MsgHistoryEntry.class}, version = 1, exportSchema = false)
public abstract class LocalMsgDatabase extends RoomDatabase {

    private static final String DB_NAME = "whis_msg_history.db";
    private static volatile LocalMsgDatabase INSTANCE;

    public abstract MsgHistoryDao msgHistoryDao();

    public static LocalMsgDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (LocalMsgDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            LocalMsgDatabase.class,
                            DB_NAME
                    ).allowMainThreadQueries() // Fast SQLite lookup
                     .fallbackToDestructiveMigration()
                     .build();
                }
            }
        }
        return INSTANCE;
    }
}
