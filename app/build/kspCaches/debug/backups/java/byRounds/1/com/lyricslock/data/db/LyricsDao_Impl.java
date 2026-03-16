package com.lyricslock.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class LyricsDao_Impl implements LyricsDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CachedLyrics> __insertionAdapterOfCachedLyrics;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOlderThan;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOldest;

  public LyricsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCachedLyrics = new EntityInsertionAdapter<CachedLyrics>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `cached_lyrics` (`trackKey`,`lrcContent`,`fetchedAt`,`hasLyrics`,`title`,`artist`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CachedLyrics entity) {
        statement.bindString(1, entity.getTrackKey());
        statement.bindString(2, entity.getLrcContent());
        statement.bindLong(3, entity.getFetchedAt());
        final int _tmp = entity.getHasLyrics() ? 1 : 0;
        statement.bindLong(4, _tmp);
        statement.bindString(5, entity.getTitle());
        statement.bindString(6, entity.getArtist());
      }
    };
    this.__preparedStmtOfDeleteOlderThan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM cached_lyrics WHERE fetchedAt < ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteOldest = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        DELETE FROM cached_lyrics WHERE trackKey IN (\n"
                + "            SELECT trackKey FROM cached_lyrics \n"
                + "            ORDER BY fetchedAt ASC \n"
                + "            LIMIT ?\n"
                + "        )\n"
                + "    ";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final CachedLyrics entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCachedLyrics.insert(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOlderThan(final long cutoff, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOlderThan.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, cutoff);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteOlderThan.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOldest(final int count, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOldest.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, count);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteOldest.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getByKey(final String key, final Continuation<? super CachedLyrics> $completion) {
    final String _sql = "SELECT * FROM cached_lyrics WHERE trackKey = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, key);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CachedLyrics>() {
      @Override
      @Nullable
      public CachedLyrics call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTrackKey = CursorUtil.getColumnIndexOrThrow(_cursor, "trackKey");
          final int _cursorIndexOfLrcContent = CursorUtil.getColumnIndexOrThrow(_cursor, "lrcContent");
          final int _cursorIndexOfFetchedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "fetchedAt");
          final int _cursorIndexOfHasLyrics = CursorUtil.getColumnIndexOrThrow(_cursor, "hasLyrics");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfArtist = CursorUtil.getColumnIndexOrThrow(_cursor, "artist");
          final CachedLyrics _result;
          if (_cursor.moveToFirst()) {
            final String _tmpTrackKey;
            _tmpTrackKey = _cursor.getString(_cursorIndexOfTrackKey);
            final String _tmpLrcContent;
            _tmpLrcContent = _cursor.getString(_cursorIndexOfLrcContent);
            final long _tmpFetchedAt;
            _tmpFetchedAt = _cursor.getLong(_cursorIndexOfFetchedAt);
            final boolean _tmpHasLyrics;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasLyrics);
            _tmpHasLyrics = _tmp != 0;
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpArtist;
            _tmpArtist = _cursor.getString(_cursorIndexOfArtist);
            _result = new CachedLyrics(_tmpTrackKey,_tmpLrcContent,_tmpFetchedAt,_tmpHasLyrics,_tmpTitle,_tmpArtist);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object count(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM cached_lyrics";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
