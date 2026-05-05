/*
 * package com.system.log.config;
 *
 * import com.fasterxml.jackson.databind.ObjectMapper; import
 * com.fasterxml.jackson.core.json.JsonWriteFeature; import
 * org.springframework.context.annotation.Bean; import
 * org.springframework.context.annotation.Configuration;
 *
 * @Configuration public class JacksonConfig {
 *
 * @Bean public ObjectMapper objectMapper() { ObjectMapper mapper = new
 * ObjectMapper();
 *
 * mapper.getFactory()
 * .configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), false);
 *
 * return mapper; } }
 */