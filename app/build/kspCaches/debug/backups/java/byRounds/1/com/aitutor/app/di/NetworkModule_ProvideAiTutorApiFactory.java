package com.aitutor.app.di;

import com.aitutor.app.data.remote.api.AiTutorApi;
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
public final class NetworkModule_ProvideAiTutorApiFactory implements Factory<AiTutorApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideAiTutorApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public AiTutorApi get() {
    return provideAiTutorApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideAiTutorApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideAiTutorApiFactory(retrofitProvider);
  }

  public static AiTutorApi provideAiTutorApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideAiTutorApi(retrofit));
  }
}
