package com.aitutor.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.DBUtil;
import java.lang.Class;
import java.lang.Double;
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
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class StudyReportDao_Impl implements StudyReportDao {
  private final RoomDatabase __db;

  public StudyReportDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
  }

  @Override
  public Object getOverviewStats(final Continuation<? super OverviewStats> $completion) {
    final String _sql = "\n"
            + "        SELECT \n"
            + "            COUNT(DISTINCT date(timestamp / 1000, 'unixepoch')) as totalActiveDays,\n"
            + "            COUNT(*) as totalMessages,\n"
            + "            (SELECT COUNT(*) FROM messages WHERE date(timestamp / 1000, 'unixepoch') = date('now')) as todayMessages,\n"
            + "            MIN(date(timestamp / 1000, 'unixepoch')) as firstUseDate\n"
            + "        FROM messages\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<OverviewStats>() {
      @Override
      @NonNull
      public OverviewStats call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTotalActiveDays = 0;
          final int _cursorIndexOfTotalMessages = 1;
          final int _cursorIndexOfTodayMessages = 2;
          final int _cursorIndexOfFirstUseDate = 3;
          final OverviewStats _result;
          if (_cursor.moveToFirst()) {
            final int _tmpTotalActiveDays;
            _tmpTotalActiveDays = _cursor.getInt(_cursorIndexOfTotalActiveDays);
            final int _tmpTotalMessages;
            _tmpTotalMessages = _cursor.getInt(_cursorIndexOfTotalMessages);
            final int _tmpTodayMessages;
            _tmpTodayMessages = _cursor.getInt(_cursorIndexOfTodayMessages);
            final String _tmpFirstUseDate;
            if (_cursor.isNull(_cursorIndexOfFirstUseDate)) {
              _tmpFirstUseDate = null;
            } else {
              _tmpFirstUseDate = _cursor.getString(_cursorIndexOfFirstUseDate);
            }
            _result = new OverviewStats(_tmpTotalActiveDays,_tmpTotalMessages,_tmpTodayMessages,_tmpFirstUseDate);
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
  public Object getDailyStatsLast7Days(final Continuation<? super List<DailyStats>> $completion) {
    final String _sql = "\n"
            + "        SELECT date(timestamp / 1000, 'unixepoch') as day,\n"
            + "               COUNT(*) as msgCount,\n"
            + "               COUNT(DISTINCT conversationId) as convCount\n"
            + "        FROM messages\n"
            + "        WHERE date(timestamp / 1000, 'unixepoch') >= date('now', '-7 days')\n"
            + "        GROUP BY day ORDER BY day ASC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DailyStats>>() {
      @Override
      @NonNull
      public List<DailyStats> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDay = 0;
          final int _cursorIndexOfMsgCount = 1;
          final int _cursorIndexOfConvCount = 2;
          final List<DailyStats> _result = new ArrayList<DailyStats>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyStats _item;
            final String _tmpDay;
            _tmpDay = _cursor.getString(_cursorIndexOfDay);
            final int _tmpMsgCount;
            _tmpMsgCount = _cursor.getInt(_cursorIndexOfMsgCount);
            final int _tmpConvCount;
            _tmpConvCount = _cursor.getInt(_cursorIndexOfConvCount);
            _item = new DailyStats(_tmpDay,_tmpMsgCount,_tmpConvCount);
            _result.add(_item);
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
  public Object getDailyStatsLast30Days(final Continuation<? super List<DailyStats>> $completion) {
    final String _sql = "\n"
            + "        SELECT date(timestamp / 1000, 'unixepoch') as day,\n"
            + "               COUNT(*) as msgCount,\n"
            + "               COUNT(DISTINCT conversationId) as convCount\n"
            + "        FROM messages\n"
            + "        WHERE date(timestamp / 1000, 'unixepoch') >= date('now', '-30 days')\n"
            + "        GROUP BY day ORDER BY day ASC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DailyStats>>() {
      @Override
      @NonNull
      public List<DailyStats> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDay = 0;
          final int _cursorIndexOfMsgCount = 1;
          final int _cursorIndexOfConvCount = 2;
          final List<DailyStats> _result = new ArrayList<DailyStats>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyStats _item;
            final String _tmpDay;
            _tmpDay = _cursor.getString(_cursorIndexOfDay);
            final int _tmpMsgCount;
            _tmpMsgCount = _cursor.getInt(_cursorIndexOfMsgCount);
            final int _tmpConvCount;
            _tmpConvCount = _cursor.getInt(_cursorIndexOfConvCount);
            _item = new DailyStats(_tmpDay,_tmpMsgCount,_tmpConvCount);
            _result.add(_item);
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
  public Object getAllActiveDays(final Continuation<? super List<String>> $completion) {
    final String _sql = "\n"
            + "        SELECT DISTINCT date(timestamp / 1000, 'unixepoch') as day\n"
            + "        FROM messages\n"
            + "        ORDER BY day ASC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            final String _tmp;
            _tmp = _cursor.getString(0);
            _item = _tmp;
            _result.add(_item);
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
  public Object getTotalConversations(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM conversations";
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

  @Override
  public Object getConversationTopics(
      final Continuation<? super List<ConversationTopic>> $completion) {
    final String _sql = "\n"
            + "        SELECT title, messageCount FROM conversations\n"
            + "        WHERE title != '' ORDER BY updatedAt DESC LIMIT 20\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ConversationTopic>>() {
      @Override
      @NonNull
      public List<ConversationTopic> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTitle = 0;
          final int _cursorIndexOfMessageCount = 1;
          final List<ConversationTopic> _result = new ArrayList<ConversationTopic>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ConversationTopic _item;
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final int _tmpMessageCount;
            _tmpMessageCount = _cursor.getInt(_cursorIndexOfMessageCount);
            _item = new ConversationTopic(_tmpTitle,_tmpMessageCount);
            _result.add(_item);
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
  public Object getAvgResponseLength(final Continuation<? super Double> $completion) {
    final String _sql = "SELECT AVG(LENGTH(content)) FROM messages WHERE isUser = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Double>() {
      @Override
      @Nullable
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final Double _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getDouble(0);
            }
            _result = _tmp;
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
  public Object getTopWrongAnswers(final Continuation<? super List<TopWrongAnswer>> $completion) {
    final String _sql = "\n"
            + "        SELECT subject, COUNT(*) as wrongCount, question\n"
            + "        FROM wrong_answers\n"
            + "        GROUP BY knowledgePoint \n"
            + "        ORDER BY wrongCount DESC \n"
            + "        LIMIT 5\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TopWrongAnswer>>() {
      @Override
      @NonNull
      public List<TopWrongAnswer> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSubject = 0;
          final int _cursorIndexOfWrongCount = 1;
          final int _cursorIndexOfQuestion = 2;
          final List<TopWrongAnswer> _result = new ArrayList<TopWrongAnswer>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TopWrongAnswer _item;
            final String _tmpSubject;
            _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
            final int _tmpWrongCount;
            _tmpWrongCount = _cursor.getInt(_cursorIndexOfWrongCount);
            final String _tmpQuestion;
            _tmpQuestion = _cursor.getString(_cursorIndexOfQuestion);
            _item = new TopWrongAnswer(_tmpSubject,_tmpWrongCount,_tmpQuestion);
            _result.add(_item);
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
  public Object getWrongAnswersBySubject(
      final Continuation<? super List<SubjectCount>> $completion) {
    final String _sql = "\n"
            + "        SELECT subject, COUNT(*) as count \n"
            + "        FROM wrong_answers \n"
            + "        GROUP BY subject \n"
            + "        ORDER BY count DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<SubjectCount>>() {
      @Override
      @NonNull
      public List<SubjectCount> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSubject = 0;
          final int _cursorIndexOfCount = 1;
          final List<SubjectCount> _result = new ArrayList<SubjectCount>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SubjectCount _item;
            final String _tmpSubject;
            _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
            final int _tmpCount;
            _tmpCount = _cursor.getInt(_cursorIndexOfCount);
            _item = new SubjectCount(_tmpSubject,_tmpCount);
            _result.add(_item);
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
  public Object getTotalLearningStats(final Continuation<? super TotalLearningStats> $completion) {
    final String _sql = "\n"
            + "        SELECT COALESCE(SUM(learnDurationMin), 0) as totalDuration,\n"
            + "               COALESCE(SUM(solveCount), 0) as totalSolved,\n"
            + "               COALESCE(SUM(correctCount), 0) as totalCorrect,\n"
            + "               COALESCE(SUM(wrongCount), 0) as totalWrong,\n"
            + "               COALESCE(SUM(totalKnowledgePoints), 0) as totalKp,\n"
            + "               COALESCE(SUM(masteredPoints), 0) as masteredKp\n"
            + "        FROM learning_records\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TotalLearningStats>() {
      @Override
      @NonNull
      public TotalLearningStats call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTotalDuration = 0;
          final int _cursorIndexOfTotalSolved = 1;
          final int _cursorIndexOfTotalCorrect = 2;
          final int _cursorIndexOfTotalWrong = 3;
          final int _cursorIndexOfTotalKp = 4;
          final int _cursorIndexOfMasteredKp = 5;
          final TotalLearningStats _result;
          if (_cursor.moveToFirst()) {
            final int _tmpTotalDuration;
            _tmpTotalDuration = _cursor.getInt(_cursorIndexOfTotalDuration);
            final int _tmpTotalSolved;
            _tmpTotalSolved = _cursor.getInt(_cursorIndexOfTotalSolved);
            final int _tmpTotalCorrect;
            _tmpTotalCorrect = _cursor.getInt(_cursorIndexOfTotalCorrect);
            final int _tmpTotalWrong;
            _tmpTotalWrong = _cursor.getInt(_cursorIndexOfTotalWrong);
            final int _tmpTotalKp;
            _tmpTotalKp = _cursor.getInt(_cursorIndexOfTotalKp);
            final int _tmpMasteredKp;
            _tmpMasteredKp = _cursor.getInt(_cursorIndexOfMasteredKp);
            _result = new TotalLearningStats(_tmpTotalDuration,_tmpTotalSolved,_tmpTotalCorrect,_tmpTotalWrong,_tmpTotalKp,_tmpMasteredKp);
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
  public Object getWeeklyLearningStats(final Continuation<? super TotalLearningStats> $completion) {
    final String _sql = "\n"
            + "        SELECT COALESCE(SUM(learnDurationMin), 0) as totalDuration,\n"
            + "               COALESCE(SUM(solveCount), 0) as totalSolved,\n"
            + "               COALESCE(SUM(correctCount), 0) as totalCorrect,\n"
            + "               COALESCE(SUM(wrongCount), 0) as totalWrong,\n"
            + "               COALESCE(SUM(totalKnowledgePoints), 0) as totalKp,\n"
            + "               COALESCE(SUM(masteredPoints), 0) as masteredKp\n"
            + "        FROM learning_records\n"
            + "        WHERE date >= date('now', 'weekday 1', '-7 days')\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TotalLearningStats>() {
      @Override
      @NonNull
      public TotalLearningStats call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTotalDuration = 0;
          final int _cursorIndexOfTotalSolved = 1;
          final int _cursorIndexOfTotalCorrect = 2;
          final int _cursorIndexOfTotalWrong = 3;
          final int _cursorIndexOfTotalKp = 4;
          final int _cursorIndexOfMasteredKp = 5;
          final TotalLearningStats _result;
          if (_cursor.moveToFirst()) {
            final int _tmpTotalDuration;
            _tmpTotalDuration = _cursor.getInt(_cursorIndexOfTotalDuration);
            final int _tmpTotalSolved;
            _tmpTotalSolved = _cursor.getInt(_cursorIndexOfTotalSolved);
            final int _tmpTotalCorrect;
            _tmpTotalCorrect = _cursor.getInt(_cursorIndexOfTotalCorrect);
            final int _tmpTotalWrong;
            _tmpTotalWrong = _cursor.getInt(_cursorIndexOfTotalWrong);
            final int _tmpTotalKp;
            _tmpTotalKp = _cursor.getInt(_cursorIndexOfTotalKp);
            final int _tmpMasteredKp;
            _tmpMasteredKp = _cursor.getInt(_cursorIndexOfMasteredKp);
            _result = new TotalLearningStats(_tmpTotalDuration,_tmpTotalSolved,_tmpTotalCorrect,_tmpTotalWrong,_tmpTotalKp,_tmpMasteredKp);
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
  public Object getMonthlyLearningStats(
      final Continuation<? super TotalLearningStats> $completion) {
    final String _sql = "\n"
            + "        SELECT COALESCE(SUM(learnDurationMin), 0) as totalDuration,\n"
            + "               COALESCE(SUM(solveCount), 0) as totalSolved,\n"
            + "               COALESCE(SUM(correctCount), 0) as totalCorrect,\n"
            + "               COALESCE(SUM(wrongCount), 0) as totalWrong,\n"
            + "               COALESCE(SUM(totalKnowledgePoints), 0) as totalKp,\n"
            + "               COALESCE(SUM(masteredPoints), 0) as masteredKp\n"
            + "        FROM learning_records\n"
            + "        WHERE strftime('%Y-%m', date) = strftime('%Y-%m', 'now')\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TotalLearningStats>() {
      @Override
      @NonNull
      public TotalLearningStats call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTotalDuration = 0;
          final int _cursorIndexOfTotalSolved = 1;
          final int _cursorIndexOfTotalCorrect = 2;
          final int _cursorIndexOfTotalWrong = 3;
          final int _cursorIndexOfTotalKp = 4;
          final int _cursorIndexOfMasteredKp = 5;
          final TotalLearningStats _result;
          if (_cursor.moveToFirst()) {
            final int _tmpTotalDuration;
            _tmpTotalDuration = _cursor.getInt(_cursorIndexOfTotalDuration);
            final int _tmpTotalSolved;
            _tmpTotalSolved = _cursor.getInt(_cursorIndexOfTotalSolved);
            final int _tmpTotalCorrect;
            _tmpTotalCorrect = _cursor.getInt(_cursorIndexOfTotalCorrect);
            final int _tmpTotalWrong;
            _tmpTotalWrong = _cursor.getInt(_cursorIndexOfTotalWrong);
            final int _tmpTotalKp;
            _tmpTotalKp = _cursor.getInt(_cursorIndexOfTotalKp);
            final int _tmpMasteredKp;
            _tmpMasteredKp = _cursor.getInt(_cursorIndexOfMasteredKp);
            _result = new TotalLearningStats(_tmpTotalDuration,_tmpTotalSolved,_tmpTotalCorrect,_tmpTotalWrong,_tmpTotalKp,_tmpMasteredKp);
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
