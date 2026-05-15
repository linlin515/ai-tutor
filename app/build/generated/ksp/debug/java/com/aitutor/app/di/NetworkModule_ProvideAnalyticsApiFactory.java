package com.aitutor.app.di;

import com.aitutor.app.data.remote.api.AnalyticsApi;
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
public final class NetworkModule_ProvideAnalyticsApiFactory implements Factory<AnalyticsApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideAnalyticsApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public AnalyticsApi get() {
    return provideAnalyticsApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideAnalyticsApiFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideAnalyticsApiFactory(retrofitProvider);
  }

  public static AnalyticsApi provideAnalyticsApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideAnalyticsApi(retrofit));
  }
}
