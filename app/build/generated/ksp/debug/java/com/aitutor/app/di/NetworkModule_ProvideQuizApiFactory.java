package com.aitutor.app.di;

import com.aitutor.app.data.remote.api.QuizApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

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
public final class NetworkModule_ProvideQuizApiFactory implements Factory<QuizApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideQuizApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public QuizApi get() {
    return provideQuizApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideQuizApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideQuizApiFactory(retrofitProvider);
  }

  public static QuizApi provideQuizApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideQuizApi(retrofit));
  }
}
