/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.application.commons.actor;

import io.camunda.application.commons.actor.ActorClockConfiguration.ActorClockControlled;
import io.camunda.zeebe.scheduler.clock.ActorClock;
import io.camunda.zeebe.scheduler.clock.ControlledActorClock;
import io.camunda.zeebe.shared.management.ActorClockService;
import io.camunda.zeebe.shared.management.ControlledActorClockService;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("unused")
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ActorClockControlled.class)
public final class ActorClockConfiguration {

  private final Optional<ActorClock> clock;
  private final ActorClockService service;

  @Autowired
  public ActorClockConfiguration(final ActorClockControlled controlledProperty) {
    this(controlledProperty.controlled());
  }

  public ActorClockConfiguration(final boolean controlled) {
    if (controlled) {
      final var controlledClock = new ControlledActorClock();
      service = new ControlledActorClockService(controlledClock);
      clock = Optional.of(controlledClock);
    } else {
      clock = Optional.empty();
      service = System::currentTimeMillis;
    }
  }

  @Bean
  public Optional<ActorClock> getClock() {
    return clock;
  }

  @Bean
  public ActorClockService getClockService() {
    return service;
  }

  @ConfigurationProperties(prefix = "zeebe.clock")
  public record ActorClockControlled(@DefaultValue("false") boolean controlled) {}
}
