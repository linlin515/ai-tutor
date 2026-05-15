package com.aitutor.app.data.repository;

import com.aitutor.app.data.local.dao.ConversationDao;
import com.aitutor.app.data.local.dao.MessageDao;
import com.aitutor.app.data.remote.api.ChatStreamApi;
import com.aitutor.app.data.remote.interceptor.TokenManager;
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
public final class ChatRepositoryImpl_Factory implements Factory<ChatRepositoryImpl> {
  private final Provider<ConversationDao> conversationDaoProvider;

  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<ChatStreamApi> chatStreamApiProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  public ChatRepositoryImpl_Factory(Provider<ConversationDao> conversationDaoProvider,
      Provider<MessageDao> messageDaoProvider, Provider<ChatStreamApi> chatStreamApiProvider,
      Provider<TokenManager> tokenManagerProvider) {
    this.conversationDaoProvider = conversationDaoProvider;
    this.messageDaoProvider = messageDaoProvider;
    this.chatStreamApiProvider = chatStreamApiProvider;
    this.tokenManagerProvider = tokenManagerProvider;
  }

  @Override
  public ChatRepositoryImpl get() {
    return newInstance(conversationDaoProvider.get(), messageDaoProvider.get(), chatStreamApiProvider.get(), tokenManagerProvider.get());
  }

  public static ChatRepositoryImpl_Factory create(Provider<ConversationDao> conversationDaoProvider,
      Provider<MessageDao> messageDaoProvider, Provider<ChatStreamApi> chatStreamApiProvider,
      Provider<TokenManager> tokenManagerProvider) {
    return new ChatRepositoryImpl_Factory(conversationDaoProvider, messageDaoProvider, chatStreamApiProvider, tokenManagerProvider);
  }

  public static ChatRepositoryImpl newInstance(ConversationDao conversationDao,
      MessageDao messageDao, ChatStreamApi chatStreamApi, TokenManager tokenManager) {
    return new ChatRepositoryImpl(conversationDao, messageDao, chatStreamApi, tokenManager);
  }
}
