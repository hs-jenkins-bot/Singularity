package com.hubspot.singularity.auth.authenticator;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.net.HttpHeaders;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityManagedThreadPoolFactory;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.auth.SingularityAuthModule;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.WebhookAuthConfiguration;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularityWebhookAuthenticator implements SingularityAuthenticator {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityWebhookAuthenticator.class
  );

  private final AsyncHttpClient asyncHttpClient;
  private final WebhookAuthConfiguration webhookAuthConfiguration;
  private final WebhookResponseParser responseParser;
  private final LoadingCache<String, SingularityUserPermissionsResponse> permissionsCache;

  @Inject
  public SingularityWebhookAuthenticator(
    @Named(
      SingularityAuthModule.WEBHOOK_AUTH_HTTP_CLIENT
    ) AsyncHttpClient asyncHttpClient,
    SingularityConfiguration configuration,
    WebhookResponseParser responseParser,
    SingularityManagedThreadPoolFactory managedThreadPoolFactory
  ) {
    this.asyncHttpClient = asyncHttpClient;
    this.webhookAuthConfiguration = configuration.getWebhookAuthConfiguration();
    this.responseParser = responseParser;
    ExecutorService authRefresh = managedThreadPoolFactory.get("auth-refresh", 5);
    this.permissionsCache =
      CacheBuilder
        .newBuilder()
        .refreshAfterWrite(
          webhookAuthConfiguration.getCacheValidationMs(),
          TimeUnit.MILLISECONDS
        )
        .build(
          new CacheLoader<String, SingularityUserPermissionsResponse>() {

            @Override
            public SingularityUserPermissionsResponse load(String authHeaderValue) {
              return verifyUncached(authHeaderValue);
            }

            @Override
            public ListenableFuture<SingularityUserPermissionsResponse> reload(
              String authHeaderVaule,
              SingularityUserPermissionsResponse oldVaule
            ) {
              ListenableFutureTask<SingularityUserPermissionsResponse> task = ListenableFutureTask.create(
                () -> {
                  try {
                    return verifyUncached(authHeaderVaule);
                  } catch (Throwable t) {
                    LOG.warn("Unable to refresh user information", t);
                    return oldVaule;
                  }
                }
              );
              authRefresh.execute(task);
              return task;
            }
          }
        );
  }

  @Override
  public Optional<SingularityUser> getUser(ContainerRequestContext context) {
    String authHeaderValue = extractAuthHeader(context);

    SingularityUserPermissionsResponse permissionsResponse = verify(authHeaderValue);

    return permissionsResponse.getUser();
  }

  private String extractAuthHeader(ContainerRequestContext context) {
    final String authHeaderValue = context.getHeaderString(HttpHeaders.AUTHORIZATION);

    if (Strings.isNullOrEmpty(authHeaderValue)) {
      throw WebExceptions.unauthorized(
        "No Authorization header present, please log in first"
      );
    } else {
      return authHeaderValue;
    }
  }

  private SingularityUserPermissionsResponse verify(String authHeaderValue) {
    try {
      return permissionsCache.get(authHeaderValue);
    } catch (Throwable t) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Exception verifying token", t);
      }
      throw WebExceptions.unauthorized(
        String.format("Exception while verifying token: %s", t.getMessage())
      );
    }
  }

  private SingularityUserPermissionsResponse verifyUncached(String authHeaderValue) {
    try {
      Response response = asyncHttpClient
        .prepareGet(webhookAuthConfiguration.getAuthVerificationUrl())
        .addHeader("Authorization", authHeaderValue)
        .execute()
        .get();
      SingularityUserPermissionsResponse permissionsResponse = responseParser.parse(
        response
      );
      permissionsCache.put(authHeaderValue, permissionsResponse);
      return permissionsResponse;
    } catch (Throwable t) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Exception verifying token", t);
      }
      if (t instanceof WebApplicationException) {
        throw (WebApplicationException) t;
      } else {
        throw new RuntimeException(t);
      }
    }
  }
}
