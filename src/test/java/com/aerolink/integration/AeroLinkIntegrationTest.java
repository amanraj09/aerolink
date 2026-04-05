package com.aerolink.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for the AeroLink API.
 *
 * <p>Loads the full Spring context and makes real HTTP calls to the upstream Aviation Weather API.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AeroLinkIntegrationTest {

  private static final String BASE_URL = "/api/v1/airport";

  @Autowired private MockMvc mockMvc;
}
