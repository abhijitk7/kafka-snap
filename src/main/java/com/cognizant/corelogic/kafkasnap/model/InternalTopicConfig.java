package com.cognizant.corelogic.kafkasnap.model;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Data;
import org.apache.kafka.clients.admin.ConfigEntry;

import com.cognizant.corelogic.kafkasnap.enums.ConfigSourceEnum;


@Data
@Builder
public class InternalTopicConfig {
  private final String name;
  private final String value;
  private final String defaultValue;
  private final ConfigEntry.ConfigSource source;
  private final boolean isSensitive;
  private final boolean isReadOnly;
  private final List<ConfigSynonymInfo> synonyms;
  private final String doc;

  public static InternalTopicConfig from(ConfigEntry configEntry) {
    InternalTopicConfig.InternalTopicConfigBuilder builder = InternalTopicConfig.builder()
        .name(configEntry.name())
        .value(configEntry.value())
        .source(configEntry.source())
        .isReadOnly(configEntry.isReadOnly())
        .isSensitive(configEntry.isSensitive())
        .synonyms(configEntry.synonyms()
        		.stream().map(synonym -> {
        			return ConfigSynonymInfo.builder()
        			.name(synonym.name())
        			.value(synonym.value())
        			.source(ConfigSourceEnum.fromValue(synonym.source().toString()))
        			.build();
        		}).collect(Collectors.toList()))
        .doc(configEntry.documentation());

    if (configEntry.source() == ConfigEntry.ConfigSource.DEFAULT_CONFIG) {
      // this is important case, because for some configs like "confluent.*" no synonyms returned, but
      // they are set by default and "source" == DEFAULT_CONFIG
      builder.defaultValue(configEntry.value());
    } else {
      // normally by default first entity of synonyms values will be used.
      configEntry.synonyms().stream()
          // skipping DYNAMIC_TOPIC_CONFIG value - which is explicitly set value when
          // topic was created (not default), see ConfigEntry.synonyms() doc
          .filter(s -> s.source() != ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG)
          .map(ConfigEntry.ConfigSynonym::value)
          .findFirst()
          .ifPresent(builder::defaultValue);
    }
    return builder.build();
  }
}
