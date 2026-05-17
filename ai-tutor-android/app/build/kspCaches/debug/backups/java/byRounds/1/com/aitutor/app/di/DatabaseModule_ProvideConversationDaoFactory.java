package com.aitutor.app.di;

import com.aitutor.app.data.local.dao.ConversationDao;
import com.aitutor.app.data.local.db.AiTutorDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class DatabaseModule_ProvideConversationDaoFactory implements Factory<ConversationDao> {
  private final Provider<AiTutorDatabase> databaseProvider;

  public DatabaseModule_ProvideConversationDaoFactory(Provider<AiTutorDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ConversationDao get() {
    return provideConversationDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideConversationDaoFactory create(
      Provider<AiTutorDatabase> databaseProvider) {
    return new DatabaseModule_ProvideConversationDaoFactory(databaseProvider);
  }

  public static ConversationDao provideConversationDao(AiTutorDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideConversationDao(database));
  }
}
