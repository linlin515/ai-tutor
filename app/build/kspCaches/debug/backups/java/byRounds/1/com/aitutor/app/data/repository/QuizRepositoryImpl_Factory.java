package com.aitutor.app.data.repository;

import com.aitutor.app.data.local.dao.PendingSubmissionDao;
import com.aitutor.app.data.local.dao.QuizRecordDao;
import com.aitutor.app.data.remote.api.QuizApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class QuizRepositoryImpl_Factory implements Factory<QuizRepositoryImpl> {
  private final Provider<QuizApi> quizApiProvider;

  private final Provider<QuizRecordDao> quizRecordDaoProvider;

  private final Provider<PendingSubmissionDao> pendingSubmissionDaoProvider;

  public QuizRepositoryImpl_Factory(Provider<QuizApi> quizApiProvider,
      Provider<QuizRecordDao> quizRecordDaoProvider,
      Provider<PendingSubmissionDao> pendingSubmissionDaoProvider) {
    this.quizApiProvider = quizApiProvider;
    this.quizRecordDaoProvider = quizRecordDaoProvider;
    this.pendingSubmissionDaoProvider = pendingSubmissionDaoProvider;
  }

  @Override
  public QuizRepositoryImpl get() {
    return newInstance(quizApiProvider.get(), quizRecordDaoProvider.get(), pendingSubmissionDaoProvider.get());
  }

  public static QuizRepositoryImpl_Factory create(Provider<QuizApi> quizApiProvider,
      Provider<QuizRecordDao> quizRecordDaoProvider,
      Provider<PendingSubmissionDao> pendingSubmissionDaoProvider) {
    return new QuizRepositoryImpl_Factory(quizApiProvider, quizRecordDaoProvider, pendingSubmissionDaoProvider);
  }

  public static QuizRepositoryImpl newInstance(QuizApi quizApi, QuizRecordDao quizRecordDao,
      PendingSubmissionDao pendingSubmissionDao) {
    return new QuizRepositoryImpl(quizApi, quizRecordDao, pendingSubmissionDao);
  }
}
