package com.aitutor.app.data.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.aitutor.app.data.local.entity.QuizRecordEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Float;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class QuizRecordDao_Impl implements QuizRecordDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<QuizRecordEntity> __insertionAdapterOfQuizRecordEntity;

  public QuizRecordDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfQuizRecordEntity = new EntityInsertionAdapter<QuizRecordEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `quiz_records` (`quizId`,`subject`,`knowledgePoints`,`difficulty`,`questionCount`,`score`,`durationSeconds`,`createdAt`,`syncedToCloud`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final QuizRecordEntity entity) {
        statement.bindString(1, entity.getQuizId());
        statement.bindString(2, entity.getSubject());
        statement.bindString(3, entity.getKnowledgePoints());
        statement.bindString(4, entity.getDifficulty());
        statement.bindLong(5, entity.getQuestionCount());
        if (entity.getScore() == null) {
          statement.bindNull(6);
        } else {
          statement.bindDouble(6, entity.getScore());
        }
        if (entity.getDurationSeconds() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getDurationSeconds());
        }
        statement.bindLong(8, entity.getCreatedAt());
        final int _tmp = entity.getSyncedToCloud() ? 1 : 0;
        statement.bindLong(9, _tmp);
      }
    };
  }

  @Override
  public Object insertRecord(final QuizRecordEntity record,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfQuizRecordEntity.insert(record);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<QuizRecordEntity>> getAllRecords() {
    final String _sql = "SELECT * FROM quiz_records ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"quiz_records"}, new Callable<List<QuizRecordEntity>>() {
      @Override
      @NonNull
      public List<QuizRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfQuizId = CursorUtil.getColumnIndexOrThrow(_cursor, "quizId");
          final int _cursorIndexOfSubject = CursorUtil.getColumnIndexOrThrow(_cursor, "subject");
          final int _cursorIndexOfKnowledgePoints = CursorUtil.getColumnIndexOrThrow(_cursor, "knowledgePoints");
          final int _cursorIndexOfDifficulty = CursorUtil.getColumnIndexOrThrow(_cursor, "difficulty");
          final int _cursorIndexOfQuestionCount = CursorUtil.getColumnIndexOrThrow(_cursor, "questionCount");
          final int _cursorIndexOfScore = CursorUtil.getColumnIndexOrThrow(_cursor, "score");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfSyncedToCloud = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedToCloud");
          final List<QuizRecordEntity> _result = new ArrayList<QuizRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final QuizRecordEntity _item;
            final String _tmpQuizId;
            _tmpQuizId = _cursor.getString(_cursorIndexOfQuizId);
            final String _tmpSubject;
            _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
            final String _tmpKnowledgePoints;
            _tmpKnowledgePoints = _cursor.getString(_cursorIndexOfKnowledgePoints);
            final String _tmpDifficulty;
            _tmpDifficulty = _cursor.getString(_cursorIndexOfDifficulty);
            final int _tmpQuestionCount;
            _tmpQuestionCount = _cursor.getInt(_cursorIndexOfQuestionCount);
            final Float _tmpScore;
            if (_cursor.isNull(_cursorIndexOfScore)) {
              _tmpScore = null;
            } else {
              _tmpScore = _cursor.getFloat(_cursorIndexOfScore);
            }
            final Integer _tmpDurationSeconds;
            if (_cursor.isNull(_cursorIndexOfDurationSeconds)) {
              _tmpDurationSeconds = null;
            } else {
              _tmpDurationSeconds = _cursor.getInt(_cursorIndexOfDurationSeconds);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final boolean _tmpSyncedToCloud;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSyncedToCloud);
            _tmpSyncedToCloud = _tmp != 0;
            _item = new QuizRecordEntity(_tmpQuizId,_tmpSubject,_tmpKnowledgePoints,_tmpDifficulty,_tmpQuestionCount,_tmpScore,_tmpDurationSeconds,_tmpCreatedAt,_tmpSyncedToCloud);
            _result.add(_item);
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
  public Flow<List<QuizRecordEntity>> getRecordsBySubject(final String subject) {
    final String _sql = "SELECT * FROM quiz_records WHERE subject = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, subject);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"quiz_records"}, new Callable<List<QuizRecordEntity>>() {
      @Override
      @NonNull
      public List<QuizRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfQuizId = CursorUtil.getColumnIndexOrThrow(_cursor, "quizId");
          final int _cursorIndexOfSubject = CursorUtil.getColumnIndexOrThrow(_cursor, "subject");
          final int _cursorIndexOfKnowledgePoints = CursorUtil.getColumnIndexOrThrow(_cursor, "knowledgePoints");
          final int _cursorIndexOfDifficulty = CursorUtil.getColumnIndexOrThrow(_cursor, "difficulty");
          final int _cursorIndexOfQuestionCount = CursorUtil.getColumnIndexOrThrow(_cursor, "questionCount");
          final int _cursorIndexOfScore = CursorUtil.getColumnIndexOrThrow(_cursor, "score");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfSyncedToCloud = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedToCloud");
          final List<QuizRecordEntity> _result = new ArrayList<QuizRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final QuizRecordEntity _item;
            final String _tmpQuizId;
            _tmpQuizId = _cursor.getString(_cursorIndexOfQuizId);
            final String _tmpSubject;
            _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
            final String _tmpKnowledgePoints;
            _tmpKnowledgePoints = _cursor.getString(_cursorIndexOfKnowledgePoints);
            final String _tmpDifficulty;
            _tmpDifficulty = _cursor.getString(_cursorIndexOfDifficulty);
            final int _tmpQuestionCount;
            _tmpQuestionCount = _cursor.getInt(_cursorIndexOfQuestionCount);
            final Float _tmpScore;
            if (_cursor.isNull(_cursorIndexOfScore)) {
              _tmpScore = null;
            } else {
              _tmpScore = _cursor.getFloat(_cursorIndexOfScore);
            }
            final Integer _tmpDurationSeconds;
            if (_cursor.isNull(_cursorIndexOfDurationSeconds)) {
              _tmpDurationSeconds = null;
            } else {
              _tmpDurationSeconds = _cursor.getInt(_cursorIndexOfDurationSeconds);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final boolean _tmpSyncedToCloud;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSyncedToCloud);
            _tmpSyncedToCloud = _tmp != 0;
            _item = new QuizRecordEntity(_tmpQuizId,_tmpSubject,_tmpKnowledgePoints,_tmpDifficulty,_tmpQuestionCount,_tmpScore,_tmpDurationSeconds,_tmpCreatedAt,_tmpSyncedToCloud);
            _result.add(_item);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
