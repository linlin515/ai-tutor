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
import com.aitutor.app.data.local.entity.KnowledgePointEntity;
import com.aitutor.app.data.local.entity.LearningRecordEntity;
import com.aitutor.app.domain.model.KnowledgeSummary;
import com.aitutor.app.domain.model.TodayStats;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
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
public final class AnalyticsDao_Impl implements AnalyticsDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LearningRecordEntity> __insertionAdapterOfLearningRecordEntity;

  private final EntityInsertionAdapter<KnowledgePointEntity> __insertionAdapterOfKnowledgePointEntity;

  public AnalyticsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLearningRecordEntity = new EntityInsertionAdapter<LearningRecordEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `learning_records` (`id`,`date`,`learnDurationMin`,`solveCount`,`correctCount`,`wrongCount`,`streakDays`,`totalKnowledgePoints`,`masteredPoints`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LearningRecordEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getDate());
        statement.bindLong(3, entity.getLearnDurationMin());
        statement.bindLong(4, entity.getSolveCount());
        statement.bindLong(5, entity.getCorrectCount());
        statement.bindLong(6, entity.getWrongCount());
        statement.bindLong(7, entity.getStreakDays());
        statement.bindLong(8, entity.getTotalKnowledgePoints());
        statement.bindLong(9, entity.getMasteredPoints());
        statement.bindLong(10, entity.getCreatedAt());
        statement.bindLong(11, entity.getUpdatedAt());
      }
    };
    this.__insertionAdapterOfKnowledgePointEntity = new EntityInsertionAdapter<KnowledgePointEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `knowledge_points` (`id`,`name`,`subject`,`parentId`,`status`,`confidence`,`lastReviewedAt`,`wrongCount`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final KnowledgePointEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getSubject());
        if (entity.getParentId() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getParentId());
        }
        statement.bindString(5, entity.getStatus());
        statement.bindDouble(6, entity.getConfidence());
        if (entity.getLastReviewedAt() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getLastReviewedAt());
        }
        statement.bindLong(8, entity.getWrongCount());
      }
    };
  }

  @Override
  public Object insertLearningRecord(final LearningRecordEntity record,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLearningRecordEntity.insert(record);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertKnowledgePoint(final KnowledgePointEntity point,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfKnowledgePointEntity.insert(point);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<TodayStats> getTodayStats(final String today) {
    final String _sql = "\n"
            + "        SELECT COALESCE(SUM(solveCount), 0) as solveCount,\n"
            + "               COALESCE(SUM(correctCount), 0) as correctCount,\n"
            + "               COALESCE(SUM(learnDurationMin), 0) as duration\n"
            + "        FROM learning_records WHERE date = ?\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, today);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"learning_records"}, new Callable<TodayStats>() {
      @Override
      @NonNull
      public TodayStats call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSolveCount = 0;
          final int _cursorIndexOfCorrectCount = 1;
          final int _cursorIndexOfDuration = 2;
          final TodayStats _result;
          if (_cursor.moveToFirst()) {
            final int _tmpSolveCount;
            _tmpSolveCount = _cursor.getInt(_cursorIndexOfSolveCount);
            final int _tmpCorrectCount;
            _tmpCorrectCount = _cursor.getInt(_cursorIndexOfCorrectCount);
            final int _tmpDuration;
            _tmpDuration = _cursor.getInt(_cursorIndexOfDuration);
            _result = new TodayStats(_tmpSolveCount,_tmpCorrectCount,_tmpDuration);
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
  public Flow<List<LearningRecordEntity>> getTrend(final String since) {
    final String _sql = "SELECT * FROM learning_records WHERE date >= ? ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, since);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"learning_records"}, new Callable<List<LearningRecordEntity>>() {
      @Override
      @NonNull
      public List<LearningRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfLearnDurationMin = CursorUtil.getColumnIndexOrThrow(_cursor, "learnDurationMin");
          final int _cursorIndexOfSolveCount = CursorUtil.getColumnIndexOrThrow(_cursor, "solveCount");
          final int _cursorIndexOfCorrectCount = CursorUtil.getColumnIndexOrThrow(_cursor, "correctCount");
          final int _cursorIndexOfWrongCount = CursorUtil.getColumnIndexOrThrow(_cursor, "wrongCount");
          final int _cursorIndexOfStreakDays = CursorUtil.getColumnIndexOrThrow(_cursor, "streakDays");
          final int _cursorIndexOfTotalKnowledgePoints = CursorUtil.getColumnIndexOrThrow(_cursor, "totalKnowledgePoints");
          final int _cursorIndexOfMasteredPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "masteredPoints");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<LearningRecordEntity> _result = new ArrayList<LearningRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LearningRecordEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final int _tmpLearnDurationMin;
            _tmpLearnDurationMin = _cursor.getInt(_cursorIndexOfLearnDurationMin);
            final int _tmpSolveCount;
            _tmpSolveCount = _cursor.getInt(_cursorIndexOfSolveCount);
            final int _tmpCorrectCount;
            _tmpCorrectCount = _cursor.getInt(_cursorIndexOfCorrectCount);
            final int _tmpWrongCount;
            _tmpWrongCount = _cursor.getInt(_cursorIndexOfWrongCount);
            final int _tmpStreakDays;
            _tmpStreakDays = _cursor.getInt(_cursorIndexOfStreakDays);
            final int _tmpTotalKnowledgePoints;
            _tmpTotalKnowledgePoints = _cursor.getInt(_cursorIndexOfTotalKnowledgePoints);
            final int _tmpMasteredPoints;
            _tmpMasteredPoints = _cursor.getInt(_cursorIndexOfMasteredPoints);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new LearningRecordEntity(_tmpId,_tmpDate,_tmpLearnDurationMin,_tmpSolveCount,_tmpCorrectCount,_tmpWrongCount,_tmpStreakDays,_tmpTotalKnowledgePoints,_tmpMasteredPoints,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Flow<List<KnowledgeSummary>> getKnowledgeSummary() {
    final String _sql = "SELECT status, COUNT(*) as count FROM knowledge_points GROUP BY status";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"knowledge_points"}, new Callable<List<KnowledgeSummary>>() {
      @Override
      @NonNull
      public List<KnowledgeSummary> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfStatus = 0;
          final int _cursorIndexOfCount = 1;
          final List<KnowledgeSummary> _result = new ArrayList<KnowledgeSummary>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final KnowledgeSummary _item;
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final int _tmpCount;
            _tmpCount = _cursor.getInt(_cursorIndexOfCount);
            _item = new KnowledgeSummary(_tmpStatus,_tmpCount);
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
  public Flow<List<KnowledgePointEntity>> getKnowledgeGraph(final String subject) {
    final String _sql = "SELECT * FROM knowledge_points WHERE subject = ? ORDER BY name";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, subject);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"knowledge_points"}, new Callable<List<KnowledgePointEntity>>() {
      @Override
      @NonNull
      public List<KnowledgePointEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfSubject = CursorUtil.getColumnIndexOrThrow(_cursor, "subject");
          final int _cursorIndexOfParentId = CursorUtil.getColumnIndexOrThrow(_cursor, "parentId");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfLastReviewedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastReviewedAt");
          final int _cursorIndexOfWrongCount = CursorUtil.getColumnIndexOrThrow(_cursor, "wrongCount");
          final List<KnowledgePointEntity> _result = new ArrayList<KnowledgePointEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final KnowledgePointEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpSubject;
            _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
            final String _tmpParentId;
            if (_cursor.isNull(_cursorIndexOfParentId)) {
              _tmpParentId = null;
            } else {
              _tmpParentId = _cursor.getString(_cursorIndexOfParentId);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final float _tmpConfidence;
            _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            final Long _tmpLastReviewedAt;
            if (_cursor.isNull(_cursorIndexOfLastReviewedAt)) {
              _tmpLastReviewedAt = null;
            } else {
              _tmpLastReviewedAt = _cursor.getLong(_cursorIndexOfLastReviewedAt);
            }
            final int _tmpWrongCount;
            _tmpWrongCount = _cursor.getInt(_cursorIndexOfWrongCount);
            _item = new KnowledgePointEntity(_tmpId,_tmpName,_tmpSubject,_tmpParentId,_tmpStatus,_tmpConfidence,_tmpLastReviewedAt,_tmpWrongCount);
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
  public Flow<Integer> getActiveDays(final String since) {
    final String _sql = "SELECT COUNT(DISTINCT date) FROM learning_records WHERE date >= ? AND solveCount > 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, since);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"learning_records"}, new Callable<Integer>() {
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
  public Flow<List<KnowledgePointEntity>> getAllKnowledgePoints() {
    final String _sql = "SELECT * FROM knowledge_points ORDER BY name";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"knowledge_points"}, new Callable<List<KnowledgePointEntity>>() {
      @Override
      @NonNull
      public List<KnowledgePointEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfSubject = CursorUtil.getColumnIndexOrThrow(_cursor, "subject");
          final int _cursorIndexOfParentId = CursorUtil.getColumnIndexOrThrow(_cursor, "parentId");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfLastReviewedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastReviewedAt");
          final int _cursorIndexOfWrongCount = CursorUtil.getColumnIndexOrThrow(_cursor, "wrongCount");
          final List<KnowledgePointEntity> _result = new ArrayList<KnowledgePointEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final KnowledgePointEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpSubject;
            _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
            final String _tmpParentId;
            if (_cursor.isNull(_cursorIndexOfParentId)) {
              _tmpParentId = null;
            } else {
              _tmpParentId = _cursor.getString(_cursorIndexOfParentId);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final float _tmpConfidence;
            _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            final Long _tmpLastReviewedAt;
            if (_cursor.isNull(_cursorIndexOfLastReviewedAt)) {
              _tmpLastReviewedAt = null;
            } else {
              _tmpLastReviewedAt = _cursor.getLong(_cursorIndexOfLastReviewedAt);
            }
            final int _tmpWrongCount;
            _tmpWrongCount = _cursor.getInt(_cursorIndexOfWrongCount);
            _item = new KnowledgePointEntity(_tmpId,_tmpName,_tmpSubject,_tmpParentId,_tmpStatus,_tmpConfidence,_tmpLastReviewedAt,_tmpWrongCount);
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
  public Object getLatestRecord(final Continuation<? super LearningRecordEntity> $completion) {
    final String _sql = "SELECT * FROM learning_records ORDER BY date DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<LearningRecordEntity>() {
      @Override
      @Nullable
      public LearningRecordEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfLearnDurationMin = CursorUtil.getColumnIndexOrThrow(_cursor, "learnDurationMin");
          final int _cursorIndexOfSolveCount = CursorUtil.getColumnIndexOrThrow(_cursor, "solveCount");
          final int _cursorIndexOfCorrectCount = CursorUtil.getColumnIndexOrThrow(_cursor, "correctCount");
          final int _cursorIndexOfWrongCount = CursorUtil.getColumnIndexOrThrow(_cursor, "wrongCount");
          final int _cursorIndexOfStreakDays = CursorUtil.getColumnIndexOrThrow(_cursor, "streakDays");
          final int _cursorIndexOfTotalKnowledgePoints = CursorUtil.getColumnIndexOrThrow(_cursor, "totalKnowledgePoints");
          final int _cursorIndexOfMasteredPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "masteredPoints");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final LearningRecordEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final int _tmpLearnDurationMin;
            _tmpLearnDurationMin = _cursor.getInt(_cursorIndexOfLearnDurationMin);
            final int _tmpSolveCount;
            _tmpSolveCount = _cursor.getInt(_cursorIndexOfSolveCount);
            final int _tmpCorrectCount;
            _tmpCorrectCount = _cursor.getInt(_cursorIndexOfCorrectCount);
            final int _tmpWrongCount;
            _tmpWrongCount = _cursor.getInt(_cursorIndexOfWrongCount);
            final int _tmpStreakDays;
            _tmpStreakDays = _cursor.getInt(_cursorIndexOfStreakDays);
            final int _tmpTotalKnowledgePoints;
            _tmpTotalKnowledgePoints = _cursor.getInt(_cursorIndexOfTotalKnowledgePoints);
            final int _tmpMasteredPoints;
            _tmpMasteredPoints = _cursor.getInt(_cursorIndexOfMasteredPoints);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new LearningRecordEntity(_tmpId,_tmpDate,_tmpLearnDurationMin,_tmpSolveCount,_tmpCorrectCount,_tmpWrongCount,_tmpStreakDays,_tmpTotalKnowledgePoints,_tmpMasteredPoints,_tmpCreatedAt,_tmpUpdatedAt);
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
