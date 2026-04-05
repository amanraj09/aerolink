package com.aerolink.config;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.aerolink.client.AviationDataProvider;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AviationDataProviderRegistry {

  private final Map<String, AviationDataProvider> providerMap;

  public AviationDataProviderRegistry(List<AviationDataProvider> providers) {
    this.providerMap =
        providers.stream().collect(toMap(AviationDataProvider::getProviderName, identity()));
  }

  public AviationDataProvider getActiveProviderByName(String providerName) {
    return Optional.ofNullable(providerMap.get(providerName))
        .orElseThrow(
            () ->
                new IllegalArgumentException("No provider registered with name: '" + providerName));
  }
}
