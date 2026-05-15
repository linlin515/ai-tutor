package com.aitutor.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.aitutor.app.data.local.entity.WrongAnswerEntity;
import java.lang.Class;
import java.lang.Exception;
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
public final class WrongAnswerDao_Impl implements WrongAnswerDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<WrongAnswerEntity> __insertionAdapterOfWrongAnswerEntity;

  private final EntityDeletionOrUpdateAdapter<WrongAnswerEntity> __deletionAdapterOfWrongAnswerEntity;

  private final EntityDeletionOrUpdateAdapter<WrongAnswerEntity> __updateAdapterOfWrongAnswerEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public WrongAnswerDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWrongAnswerEntity = new EntityInsertionAdapter<WrongAnswerEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `wrong_answers` (`id`,`question`,`correctAnswer`,`userAnswer`,`subject`,`knowledgePoint`,`source`,`intervalDays`,`consecutiveCorrect`,`isMastered`,`nextReviewAt`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WrongAnswerEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getQuestion());
        statement.bindString(3, entity.getCorrectAnswer());
        statement.bindString(4, entity.getUserAnswer());
        statement.bindString(5, entity.getSubject());
        statement.bindString(6, entity.getKnowledgePoint());
        statement.bindString(7, entity.getSource());
        statement.bindLong(8, entity.getIntervalDays());
        statement.bindLong(9, entity.getConsecutiveCorrect());
        final int _tmp = entity.isMastered() ? 1 : 0;
        statement.bindLong(10, _tmp);
        statement.bindLong(11, entity.getNextReviewAt());
        statement.bindLong(12, entity.getCreatedAt());
        statement.bindLong(13, entity.getUpdatedAt());
      }
    };
    this.__deletionAdapterOfWrongAnswerEntity = new EntityDeletionOrUpdateAdapter<WrongAnswerEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `wrong_answers` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WrongAnswerEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfWrongAnswerEntity = new EntityDeletionOrUpdateAdapter<WrongAnswerEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `wrong_answers` SET `id` = ?,`question` = ?,`correctAnswer` = ?,`userAnswer` = ?,`subject` = ?,`knowledgePoint` = ?,`source` = ?,`intervalDays` = ?,`consecutiveCorrect` = ?,`isMastered` = ?,`nextReviewAt` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WrongAnswerEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getQuestion());
        statement.bindString(3, entity.getCorrectAnswer());
        statement.bindString(4, entity.getUserAnswer());
        statement.bindString(5, entity.getSubject());
        statement.bindString(6, entity.getKnowledgePoint());
        statement.bindString(7, entity.getSource());
        statement.bindLong(8, entity.getIntervalDays());
        statement.bindLong(9, entity.getConsecutiveCorrect());
        final int _tmp = entity.isMastered() ? 1 : 0;
        statement.bindLong(10, _tmp);
        statement.bindLong(11, entity.getNextReviewAt());
        statement.bindLong(12, entity.getCreatedAt());
        statement.bindLong(13, entity.getUpdatedAt());
        statement.bindString(14, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM wrong_answers WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final WrongAnswerEntity answer,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfWrongAnswerEntity.insert(answer);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final WrongAnswerEntity answer,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfWrongAnswerEntity.handle(answer);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final WrongAnswerEntity answer,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfWrongAnswerEntity.handle(answer);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<WrongAnswerEntity>> getAll() {
    final String _sql = "SELECT * FROM wrong_answers ORDER BY nextReviewAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"wrong_answers"}, new Callable<List<WrongAnswerEntity>>() {
      @Override
      @NonNull
      public List<WrongAnswerEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfQuestion = CursorUtil.getColumnIndexOrThrow(_cursor, "question");
          final int _cursorIndexOfCorrectAnswer = CursorUtil.getColumnIndexOrThrow(_cursor, "correctAnswer");
          final int _cursorIndexOfUserAnswer = CursorUtil.getColumnIndexOrThrow(_cursor, "userAnswer");
          final int _cursorIndexOfSubject = CursorUtil.getColumnIndexOrThrow(_cursor, "subject");
          final int _cursorIndexOfKnowledgePoint = CursorUtil.getColumnIndexOrThrow(_cursor, "knowledgePoint");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfIntervalDays = CursorUtil.getColumnIndexOrThrow(_cursor, "intervalDays");
          final int _cursorIndexOfConsecutiveCorrect = CursorUtil.getColumnIndexOrThrow(_cursor, "consecutiveCorrect");
          final int _cursorIndexOfIsMastered = CursorUtil.getColumnIndexOrThrow(_cursor, "isMastered");
          final int _cursorIndexOfNextReviewAt = CursorUtil.getColumnIndexOrThrow(_cursor, "nextReviewAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<WrongAnswerEntity> _result = new ArrayList<WrongAnswerEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WrongAnswerEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpQuestion;
            _tmpQuestion = _cursor.getString(_cursorIndexOfQuestion);
            final String _tmpCorrectAnswer;
            _tmpCorrectAnswer = _cursor.getString(_cursorIndexOfCorrectAnswer);
            final String _tmpUserAnswer;
            _tmpUserAnswer = _cursor.getString(_cursorIndexOfUserAnswer);
            final String _tmpSubject;
            _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
            final String _tmpKnowledgePoint;
            _tmpKnowledgePoint = _cursor.getString(_cursorIndexOfKnowledgePoint);
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final int _tmpIntervalDays;
            _tmpIntervalDays = _cursor.getInt(_cursorIndexOfIntervalDays);
            final int _tmpConsecutiveCorrect;
            _tmpConsecutiveCorrect = _cursor.getInt(_cursorIndexOfConsecutiveCorrect);
            final boolean _tmpIsMastered;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsMastered);
            _tmpIsMastered = _tmp != 0;
            final long _tmpNextReviewAt;
            _tmpNextReviewAt = _cursor.getLong(_cursorIndexOfNextReviewAt);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new WrongAnswerEntity(_tmpId,_tmpQuestion,_tmpCorrectAnswer,_tmpUserAnswer,_tmpSubject,_tmpKnowledgePoint,_tmpSource,_tmpIntervalDays,_tmpConsecutiveCorrect,_tmpIsMastered,_tmpNextReviewAt,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Flow<List<WrongAnswerEntity>> getBySubject(final String subject) {
    final String _sql = "SELECT * FROM wrong_answers WHERE subject = ? ORDER BY nextReviewAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, subject);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"wrong_answers"}, new Callable<List<WrongAnswerEntity>>() {
      @Override
      @NonNull
      public List<WrongAnswerEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfQuestion = CursorUtil.getColumnIndexOrThrow(_cursor, "question");
          final int _cursorIndexOfCorrectAnswer = CursorUtil.getColumnIndexOrThrow(_cursor, "correctAnswer");
          final int _cursorIndexOfUserAnswer = CursorUtil.getColumnIndexOrThrow(_cursor, "userAnswer");
          final int _cursorIndexOfSubject = CursorUtil.getColumnIndexOrThrow(_cursor, "subject");
          final int _cursorIndexOfKnowledgePoint = CursorUtil.getColumnIndexOrThrow(_cursor, "knowledgePoint");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfIntervalDays = CursorUtil.getColumnIndexOrThrow(_cursor, "intervalDays");
          final int _cursorIndexOfConsecutiveCorrect = CursorUtil.getColumnIndexOrThrow(_cursor, "consecutiveCorrect");
          final int _cursorIndexOfIsMastered = CursorUtil.getColumnIndexOrThrow(_cursor, "isMastered");
          final int _cursorIndexOfNextReviewAt = CursorUtil.getColumnIndexOrThrow(_cursor, "nextReviewAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<WrongAnswerEntity> _result = new ArrayList<WrongAnswerEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WrongAnswerEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpQuestion;
            _tmpQuestion = _cursor.getString(_cursorIndexOfQuestion);
            final String _tmpCorrectAnswer;
            _tmpCorrectAnswer = _cursor.getString(_cursorIndexOfCorrectAnswer);
            final String _tmpUserAnswer;
            _tmpUserAnswer = _cursor.getString(_cursorIndexOfUserAnswer);
            final String _tmpSubject;
            _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
            final String _tmpKnowledgePoint;
            _tmpKnowledgePoint = _cursor.getString(_cursorIndexOfKnowledgePoint);
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final int _tmpIntervalDays;
            _tmpIntervalDays = _cursor.getInt(_cursorIndexOfIntervalDays);
            final int _tmpConsecutiveCorrect;
            _tmpConsecutiveCorrect = _cursor.getInt(_cursorIndexOfConsecutiveCorrect);
            final boolean _tmpIsMastered;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsMastered);
            _tmpIsMastered = _tmp != 0;
            final long _tmpNextReviewAt;
            _tmpNextReviewAt = _cursor.getLong(_cursorIndexOfNextReviewAt);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new WrongAnswerEntity(_tmpId,_tmpQuestion,_tmpCorrectAnswer,_tmpUserAnswer,_tmpSubject,_tmpKnowledgePoint,_tmpSource,_tmpIntervalDays,_tmpConsecutiveCorrect,_tmpIsMastered,_tmpNextReviewAt,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Flow<List<WrongAnswerEntity>> getDueReviews(final long now) {
    final String _sql = "SELECT * FROM wrong_answers WHERE nextReviewAt <= ? AND isMastered = 0 ORDER BY nextReviewAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, now);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"wrong_answers"}, new Callable<List<WrongAnswerEntity>>() {
      @Override
      @NonNull
      public List<WrongAnswerEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfQuestion = CursorUtil.getColumnIndexOrThrow(_cursor, "question");
          final int _cursorIndexOfCorrectAnswer = CursorUtil.getColumnIndexOrThrow(_cursor, "correctAnswer");
          final int _cursorIndexOfUserAnswer = CursorUtil.getColumnIndexOrThrow(_cursor, "userAnswer");
          final int _cursorIndexOfSubject = CursorUtil.getColumnIndexOrThrow(_cursor, "subject");
          final int _cursorIndexOfKnowledgePoint = CursorUtil.getColumnIndexOrThrow(_cursor, "knowledgePoint");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfIntervalDays = CursorUtil.getColumnIndexOrThrow(_cursor, "intervalDays");
          final int _cursorIndexOfConsecutiveCorrect = CursorUtil.getColumnIndexOrThrow(_cursor, "consecutiveCorrect");
          final int _cursorIndexOfIsMastered = CursorUtil.getColumnIndexOrThrow(_cursor, "isMastered");
          final int _cursorIndexOfNextReviewAt = CursorUtil.getColumnIndexOrThrow(_cursor, "nextReviewAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<WrongAnswerEntity> _result = new ArrayList<WrongAnswerEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WrongAnswerEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpQuestion;
            _tmpQuestion = _cursor.getString(_cursorIndexOfQuestion);
            final String _tmpCorrectAnswer;
            _tmpCorrectAnswer = _cursor.getString(_cursorIndexOfCorrectAnswer);
            final String _tmpUserAnswer;
            _tmpUserAnswer = _cursor.getString(_cursorIndexOfUserAnswer);
            final String _tmpSubject;
            _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
            final String _tmpKnowledgePoint;
            _tmpKnowledgePoint = _cursor.getString(_cursorIndexOfKnowledgePoint);
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final int _tmpIntervalDays;
            _tmpIntervalDays = _cursor.getInt(_cursorIndexOfIntervalDays);
            final int _tmpConsecutiveCorrect;
            _tmpConsecutiveCorrect = _cursor.getInt(_cursorIndexOfConsecutiveCorrect);
            final boolean _tmpIsMastered;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsMastered);
            _tmpIsMastered = _tmp != 0;
            final long _tmpNextReviewAt;
            _tmpNextReviewAt = _cursor.getLong(_cursorIndexOfNextReviewAt);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new WrongAnswerEntity(_tmpId,_tmpQuestion,_tmpCorrectAnswer,_tmpUserAnswer,_tmpSubject,_tmpKnowledgePoint,_tmpSource,_tmpIntervalDays,_tmpConsecutiveCorrect,_tmpIsMastered,_tmpNextReviewAt,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Flow<Integer> getDueCount(final long now) {
    final String _sql = "SELECT COUNT(*) FROM wrong_answers WHERE nextReviewAt <= ? AND isMastered = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, now);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"wrong_answers"}, new Callable<Integer>() {
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
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getById(final String id,
      final Continuation<? super WrongAnswerEntity> $completion) {
    final String _sql = "SELECT * FROM wrong_answers WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<WrongAnswerEntity>() {
      @Override
      @Nullable
      public WrongAnswerEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfQuestion = CursorUtil.getColumnIndexOrThrow(_cursor, "question");
          final int _cursorIndexOfCorrectAnswer = CursorUtil.getColumnIndexOrThrow(_cursor, "correctAnswer");
          final int _cursorIndexOfUserAnswer = CursorUtil.getColumnIndexOrThrow(_cursor, "userAnswer");
          final int _cursorIndexOfSubject = CursorUtil.getColumnIndexOrThrow(_cursor, "subject");
          final int _cursorIndexOfKnowledgePoint = CursorUtil.getColumnIndexOrThrow(_cursor, "knowledgePoint");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfIntervalDays = CursorUtil.getColumnIndexOrThrow(_cursor, "intervalDays");
          final int _cursorIndexOfConsecutiveCorrect = CursorUtil.getColumnIndexOrThrow(_cursor, "consecutiveCorrect");
          final int _cursorIndexOfIsMastered = CursorUtil.getColumnIndexOrThrow(_cursor, "isMastered");
          final int _cursorIndexOfNextReviewAt = CursorUtil.getColumnIndexOrThrow(_cursor, "nextReviewAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final WrongAnswerEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpQuestion;
            _tmpQuestion = _cursor.getString(_cursorIndexOfQuestion);
            final String _tmpCorrectAnswer;
            _tmpCorrectAnswer = _cursor.getString(_cursorIndexOfCorrectAnswer);
            final String _tmpUserAnswer;
            _tmpUserAnswer = _cursor.getString(_cursorIndexOfUserAnswer);
            final String _tmpSubject;
            _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
            final String _tmpKnowledgePoint;
            _tmpKnowledgePoint = _cursor.getString(_cursorIndexOfKnowledgePoint);
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final int _tmpIntervalDays;
            _tmpIntervalDays = _cursor.getInt(_cursorIndexOfIntervalDays);
            final int _tmpConsecutiveCorrect;
            _tmpConsecutiveCorrect = _cursor.getInt(_cursorIndexOfConsecutiveCorrect);
            final boolean _tmpIsMastered;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsMastered);
            _tmpIsMastered = _tmp != 0;
            final long _tmpNextReviewAt;
            _tmpNextReviewAt = _cursor.getLong(_cursorIndexOfNextReviewAt);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new WrongAnswerEntity(_tmpId,_tmpQuestion,_tmpCorrectAnswer,_tmpUserAnswer,_tmpSubject,_tmpKnowledgePoint,_tmpSource,_tmpIntervalDays,_tmpConsecutiveCorrect,_tmpIsMastered,_tmpNextReviewAt,_tmpCreatedAt,_tmpUpdatedAt);
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
