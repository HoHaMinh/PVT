package com.hoaphat.pvt.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.site-name=Phòng vật tư Ống thép Hòa Phát Test")
@AutoConfigureMockMvc
class LoginFlowIntegrationTest {
    @Autowired private MockMvc mockMvc;

    @Test
    void doLoginPostIsHandledBySpringSecurityInsteadOfReturning405() throws Exception {
        mockMvc.perform(post("/doLogin")
                        .with(csrf())
                        .param("accountname", "account-does-not-exist")
                        .param("password", "invalid-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?error=true"));
    }
}
