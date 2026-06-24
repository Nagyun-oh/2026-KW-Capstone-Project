package com.example.security_log_system.controller;

import com.example.security_log_system.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Optional;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp(){
        AuthController authController = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    @DisplayName("로그인 성공 시 JWT 토큰과 200 OK를 반환한다")
    void login_whenSuccess_thenReturnToken() throws Exception{
        when(authService.login(any())).thenReturn(Optional.of("jwt-token"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                        "username": "admin",
                        "password": "1234"
                        }
                        """))
        .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));

    }

    @Test
    @DisplayName("로그인 실패 시 오류 메시지와 401 Unauthorized를 반환한다")
    void login_whenFailed_thenReturnUnauthorized() throws Exception {
        when(authService.login(any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Invalid username or password.")));
    }


    @Test
    @DisplayName("회원가입 성공 시 성공 메시지와 200 OK를 반환한다")
    void register_whenSuccess_thenReturnOk() throws Exception {
        when(authService.registerAdmin(any())).thenReturn(true);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "username" : "admin",
                            "password" : "1234"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Admin account created successfully.")));
    }

    @Test
    @DisplayName("중복된 아이디로 회원가입 시 오류 메시지와 400 Bad Request를 반환한다")
    void register_whenDuplicatedUsername_thenReturnBadRequest() throws Exception {
        when(authService.registerAdmin(any())).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "1234"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Username already exists.")));
    }
}

/*
    AuthControllerTest
    - 로그인 성공 → 200 + token
    - 로그인 실패 → 401
    - 회원가입 성공 → 200
    - 중복 아이디 → 400
* */
