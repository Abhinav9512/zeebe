package org.camunda.optimize.rest.engine;

import org.camunda.optimize.rest.providers.OptimizeObjectMapperProvider;
import org.camunda.optimize.service.util.configuration.ConfigurationReloadable;
import org.camunda.optimize.service.util.configuration.ConfigurationService;
import org.camunda.optimize.service.util.configuration.EngineConfiguration;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Component
public class EngineContextFactory {

  protected Logger logger = LoggerFactory.getLogger(getClass());

  private List<EngineContext> configuredEngines;

  @Autowired
  private ConfigurationService configurationService;

  @Autowired
  protected OptimizeObjectMapperProvider optimizeObjectMapperProvider;

  @PostConstruct
  public void init() {
    configuredEngines = new ArrayList();
    for (Map.Entry<String, EngineConfiguration> config : configurationService.getConfiguredEngines().entrySet()) {
      configuredEngines.add(constructEngineContext(config));
    }
  }

  private EngineContext constructEngineContext(Map.Entry<String, EngineConfiguration> config) {
    return new EngineContext(config.getKey(), constructClient(config), configurationService);
  }

  private Client constructClient(Map.Entry<String, EngineConfiguration> config) {
    Client client = ClientBuilder.newClient();
    client.property(ClientProperties.CONNECT_TIMEOUT, configurationService.getEngineConnectTimeout());
    client.property(ClientProperties.READ_TIMEOUT, configurationService.getEngineReadTimeout());
    client.register(new LoggingFilter());
    if (config.getValue().getAuthentication().isEnabled()) {
      client.register(
          new BasicAccessAuthenticationFilter(
            configurationService.getDefaultEngineAuthenticationUser(config.getKey()),
            configurationService.getDefaultEngineAuthenticationPassword(config.getKey())
          )
      );
    }
    client.register(optimizeObjectMapperProvider);
    return client;
  }

  public class LoggingFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) {
      String body = requestContext.getEntity() != null ? requestContext.getEntity().toString() : "";
      logger.trace("sending request to [{}] with body [{}]", requestContext.getUri() , body);
    }
  }


  public List<EngineContext> getConfiguredEngines() {
    return configuredEngines;
  }

}
