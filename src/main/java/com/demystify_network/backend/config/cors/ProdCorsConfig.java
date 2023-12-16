package com.demystify_network.backend.config.cors;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("production")
public class ProdCorsConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/address/threatIntel")
        .allowedOrigins("https://metamask.github.io", "null")
        .allowedMethods("POST")
        .allowedHeaders("*")
        .maxAge(3600);
  }
}
