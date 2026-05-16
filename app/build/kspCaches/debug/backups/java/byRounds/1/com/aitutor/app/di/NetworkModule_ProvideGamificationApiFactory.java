package com.aitutor.app.di;

import com.aitutor.app.data.remote.api.GamificationApi;
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
public final class NetworkModule_ProvideGamificationApiFactory implements Factory<GamificationApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideGamificationApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public GamificationApi get() {
    return provideGamificationApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideGamificationApiFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideGamificationApiFactory(retrofitProvider);
  }

  public static GamificationApi provideGamificationApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideGamificationApi(retrofit));
  }
}
