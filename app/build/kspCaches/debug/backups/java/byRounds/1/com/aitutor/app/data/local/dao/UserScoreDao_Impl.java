package com.aitutor.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.aitutor.app.data.local.entity.UserScoreEntity;
import java.lang.Class;
import java.lang.Exception;
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
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class UserScoreDao_Impl implements UserScoreDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<UserScoreEntity> __insertionAdapterOfUserScoreEntity;

  public UserScoreDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfUserScoreEntity = new EntityInsertionAdapter<UserScoreEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `user_score` (`id`,`totalScore`,`currentStreak`,`longestStreak`,`lastLearningDate`,`updatedAt`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserScoreEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindLong(2, entity.getTotalScore());
        statement.bindLong(3, entity.getCurrentStreak());
        statement.bindLong(4, entity.getLongestStreak());
        statement.bindString(5, entity.getLastLearningDate());
        statement.bindLong(6, entity.getUpdatedAt());
      }
    };
  }

  @Override
  public Object upsertScore(final UserScoreEntity score,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfUserScoreEntity.insert(score);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<UserScoreEntity> getScore() {
    final String _sql = "SELECT * FROM user_score WHERE id = 'user_score'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"user_score"}, new Callable<UserScoreEntity>() {
      @Override
      @Nullable
      public UserScoreEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTotalScore = CursorUtil.getColumnIndexOrThrow(_cursor, "totalScore");
          final int _cursorIndexOfCurrentStreak = CursorUtil.getColumnIndexOrThrow(_cursor, "currentStreak");
          final int _cursorIndexOfLongestStreak = CursorUtil.getColumnIndexOrThrow(_cursor, "longestStreak");
          final int _cursorIndexOfLastLearningDate = CursorUtil.getColumnIndexOrThrow(_cursor, "lastLearningDate");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final UserScoreEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final int _tmpTotalScore;
            _tmpTotalScore = _cursor.getInt(_cursorIndexOfTotalScore);
            final int _tmpCurrentStreak;
            _tmpCurrentStreak = _cursor.getInt(_cursorIndexOfCurrentStreak);
            final int _tmpLongestStreak;
            _tmpLongestStreak = _cursor.getInt(_cursorIndexOfLongestStreak);
            final String _tmpLastLearningDate;
            _tmpLastLearningDate = _cursor.getString(_cursorIndexOfLastLearningDate);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new UserScoreEntity(_tmpId,_tmpTotalScore,_tmpCurrentStreak,_tmpLongestStreak,_tmpLastLearningDate,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getScoreOnce(final Continuation<? super UserScoreEntity> $completion) {
    final String _sql = "SELECT * FROM user_score WHERE id = 'user_score'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<UserScoreEntity>() {
      @Override
      @Nullable
      public UserScoreEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTotalScore = CursorUtil.getColumnIndexOrThrow(_cursor, "totalScore");
          final int _cursorIndexOfCurrentStreak = CursorUtil.getColumnIndexOrThrow(_cursor, "currentStreak");
          final int _cursorIndexOfLongestStreak = CursorUtil.getColumnIndexOrThrow(_cursor, "longestStreak");
          final int _cursorIndexOfLastLearningDate = CursorUtil.getColumnIndexOrThrow(_cursor, "lastLearningDate");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final UserScoreEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final int _tmpTotalScore;
            _tmpTotalScore = _cursor.getInt(_cursorIndexOfTotalScore);
            final int _tmpCurrentStreak;
            _tmpCurrentStreak = _cursor.getInt(_cursorIndexOfCurrentStreak);
            final int _tmpLongestStreak;
            _tmpLongestStreak = _cursor.getInt(_cursorIndexOfLongestStreak);
            final String _tmpLastLearningDate;
            _tmpLastLearningDate = _cursor.getString(_cursorIndexOfLastLearningDate);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new UserScoreEntity(_tmpId,_tmpTotalScore,_tmpCurrentStreak,_tmpLongestStreak,_tmpLastLearningDate,_tmpUpdatedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
