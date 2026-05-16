package com.aitutor.app.data.local.dao;

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
import com.aitutor.app.data.local.entity.SubscriptionCacheEntity;
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
public final class SubscriptionCacheDao_Impl implements SubscriptionCacheDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SubscriptionCacheEntity> __insertionAdapterOfSubscriptionCacheEntity;

  private final SharedSQLiteStatement __preparedStmtOfClear;

  public SubscriptionCacheDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSubscriptionCacheEntity = new EntityInsertionAdapter<SubscriptionCacheEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `subscription_cache` (`id`,`planType`,`status`,`featuresJson`,`dailyQuotaTotal`,`dailyQuotaUsed`,`validUntil`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SubscriptionCacheEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getPlanType());
        statement.bindString(3, entity.getStatus());
        statement.bindString(4, entity.getFeaturesJson());
        statement.bindLong(5, entity.getDailyQuotaTotal());
        statement.bindLong(6, entity.getDailyQuotaUsed());
        if (entity.getValidUntil() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getValidUntil());
        }
        statement.bindLong(8, entity.getUpdatedAt());
      }
    };
    this.__preparedStmtOfClear = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM subscription_cache";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final SubscriptionCacheEntity cache,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSubscriptionCacheEntity.insert(cache);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clear(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClear.acquire();
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
          __preparedStmtOfClear.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<SubscriptionCacheEntity> observe() {
    final String _sql = "SELECT * FROM subscription_cache WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"subscription_cache"}, new Callable<SubscriptionCacheEntity>() {
      @Override
      @Nullable
      public SubscriptionCacheEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlanType = CursorUtil.getColumnIndexOrThrow(_cursor, "planType");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfFeaturesJson = CursorUtil.getColumnIndexOrThrow(_cursor, "featuresJson");
          final int _cursorIndexOfDailyQuotaTotal = CursorUtil.getColumnIndexOrThrow(_cursor, "dailyQuotaTotal");
          final int _cursorIndexOfDailyQuotaUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "dailyQuotaUsed");
          final int _cursorIndexOfValidUntil = CursorUtil.getColumnIndexOrThrow(_cursor, "validUntil");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final SubscriptionCacheEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpPlanType;
            _tmpPlanType = _cursor.getString(_cursorIndexOfPlanType);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpFeaturesJson;
            _tmpFeaturesJson = _cursor.getString(_cursorIndexOfFeaturesJson);
            final int _tmpDailyQuotaTotal;
            _tmpDailyQuotaTotal = _cursor.getInt(_cursorIndexOfDailyQuotaTotal);
            final int _tmpDailyQuotaUsed;
            _tmpDailyQuotaUsed = _cursor.getInt(_cursorIndexOfDailyQuotaUsed);
            final String _tmpValidUntil;
            if (_cursor.isNull(_cursorIndexOfValidUntil)) {
              _tmpValidUntil = null;
            } else {
              _tmpValidUntil = _cursor.getString(_cursorIndexOfValidUntil);
            }
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new SubscriptionCacheEntity(_tmpId,_tmpPlanType,_tmpStatus,_tmpFeaturesJson,_tmpDailyQuotaTotal,_tmpDailyQuotaUsed,_tmpValidUntil,_tmpUpdatedAt);
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
  public Object get(final Continuation<? super SubscriptionCacheEntity> $completion) {
    final String _sql = "SELECT * FROM subscription_cache WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SubscriptionCacheEntity>() {
      @Override
      @Nullable
      public SubscriptionCacheEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlanType = CursorUtil.getColumnIndexOrThrow(_cursor, "planType");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfFeaturesJson = CursorUtil.getColumnIndexOrThrow(_cursor, "featuresJson");
          final int _cursorIndexOfDailyQuotaTotal = CursorUtil.getColumnIndexOrThrow(_cursor, "dailyQuotaTotal");
          final int _cursorIndexOfDailyQuotaUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "dailyQuotaUsed");
          final int _cursorIndexOfValidUntil = CursorUtil.getColumnIndexOrThrow(_cursor, "validUntil");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final SubscriptionCacheEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpPlanType;
            _tmpPlanType = _cursor.getString(_cursorIndexOfPlanType);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpFeaturesJson;
            _tmpFeaturesJson = _cursor.getString(_cursorIndexOfFeaturesJson);
            final int _tmpDailyQuotaTotal;
            _tmpDailyQuotaTotal = _cursor.getInt(_cursorIndexOfDailyQuotaTotal);
            final int _tmpDailyQuotaUsed;
            _tmpDailyQuotaUsed = _cursor.getInt(_cursorIndexOfDailyQuotaUsed);
            final String _tmpValidUntil;
            if (_cursor.isNull(_cursorIndexOfValidUntil)) {
              _tmpValidUntil = null;
            } else {
              _tmpValidUntil = _cursor.getString(_cursorIndexOfValidUntil);
            }
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new SubscriptionCacheEntity(_tmpId,_tmpPlanType,_tmpStatus,_tmpFeaturesJson,_tmpDailyQuotaTotal,_tmpDailyQuotaUsed,_tmpValidUntil,_tmpUpdatedAt);
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
