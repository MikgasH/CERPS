package com.example.cerpshashkin.integration.controller;

import com.example.cerpshashkin.integration.config.TestConfig;
import com.example.cerpshashkin.dto.CreateProviderKeyRequest;
import com.example.cerpshashkin.dto.UpdateProviderKeyRequest;
import com.example.cerpshashkin.repository.ApiProviderKeyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class ProviderKeyManagementControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApiProviderKeyRepository repository;

    private static final String BASE_URL = "/api/v1/admin/provider-keys";

    @AfterEach
    void cleanup() {
        repository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProviderKey_WithInvalidRequest_ShouldReturn400() throws Exception {
        CreateProviderKeyRequest request = new CreateProviderKeyRequest("", "");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createProviderKey_WithoutAdminRole_ShouldReturn403() throws Exception {
        CreateProviderKeyRequest request = new CreateProviderKeyRequest("fixer", "test_api_key");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isForbidden());
    }

    @Test
    void createProviderKey_WithoutAuthentication_ShouldReturn403() throws Exception {
        CreateProviderKeyRequest request = new CreateProviderKeyRequest("fixer", "test_api_key");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllActiveProviderKeys_ShouldReturnList() throws Exception {
        CreateProviderKeyRequest request1 = new CreateProviderKeyRequest("fixer", "key1");
        CreateProviderKeyRequest request2 = new CreateProviderKeyRequest("currencyapi", "key2");

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)));

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getProviderKey_WithValidId_ShouldReturn200() throws Exception {
        CreateProviderKeyRequest request = new CreateProviderKeyRequest("fixer", "test_key");

        String createResponse = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.providerName").value("fixer"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getProviderKey_WithInvalidId_ShouldReturn404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProviderKey_WithValidRequest_ShouldReturn200() throws Exception {
        CreateProviderKeyRequest createRequest = new CreateProviderKeyRequest("fixer", "old_key");

        String createResponse = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(createResponse).get("id").asLong();

        UpdateProviderKeyRequest updateRequest = new UpdateProviderKeyRequest("new_key_12345");

        mockMvc.perform(put(BASE_URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.providerName").value("fixer"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProviderKey_WithInvalidId_ShouldReturn404() throws Exception {
        UpdateProviderKeyRequest request = new UpdateProviderKeyRequest("new_key");

        mockMvc.perform(put(BASE_URL + "/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProviderKey_WithValidId_ShouldReturn204() throws Exception {
        CreateProviderKeyRequest request = new CreateProviderKeyRequest("fixer", "test_key");

        String createResponse = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(delete(BASE_URL + "/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProviderKey_WithInvalidId_ShouldReturn404() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rotateProviderKey_WithValidRequest_ShouldReturn200() throws Exception {
        CreateProviderKeyRequest createRequest = new CreateProviderKeyRequest("fixer", "old_key");

        String createResponse = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(createResponse).get("id").asLong();

        UpdateProviderKeyRequest rotateRequest = new UpdateProviderKeyRequest("rotated_key_67890");

        mockMvc.perform(post(BASE_URL + "/" + id + "/rotate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rotateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.providerName").value("fixer"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rotateProviderKey_WithInvalidId_ShouldReturn404() throws Exception {
        UpdateProviderKeyRequest request = new UpdateProviderKeyRequest("rotated_key");

        mockMvc.perform(post(BASE_URL + "/99999/rotate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
