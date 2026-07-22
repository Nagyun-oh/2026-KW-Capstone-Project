package com.example.security_log_system.service;


import com.example.security_log_system.dto.LogRequestDto;
import com.example.security_log_system.entity.AdminUser;
import com.example.security_log_system.repository.AdminUserRepository;
import com.example.security_log_system.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/*
    AuthServiceTest
    - 로그인 성공 시 JWT 반환
    - 유저 없음 → Optional.empty()
    - 비밀번호 불일치 → Optional.empty()
    - 관리자 등록 성공 시 저장
    - 중복 아이디면 저장하지 않음
* */

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("유효한 로그인 정보 입력 시 JWT 토큰을 반환한다")
    void login_whenValidCredentials_thenReturnToken() {
        LogRequestDto request = loginRequest("admin","1234");

        AdminUser user = AdminUser.builder()
                .username("admin")
                .password("encoded-password")
                .role("ROLE_ADMIN")
                .build();

        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("1234","encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken("admin")).thenReturn("jwt-token");

        Optional<String> result = authService.login(request);

        assertThat(result).contains("jwt-token");

        verify(adminUserRepository).findByUsername("admin");
        verify(passwordEncoder).matches("1234","encoded-password");
        verify(jwtUtil).generateToken("admin");
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 로그인 시 빈 Optional을 반환하고 인증을 중단한다")
    void login_whenUserNotFound_thenReurnEmpty() {
        LogRequestDto request = loginRequest("unknown","1234");

        when(adminUserRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        Optional<String>result = authService.login(request);

        assertThat(result).isEmpty();

        verify(adminUserRepository).findByUsername("unknown");
        verifyNoInteractions(passwordEncoder,jwtUtil);
    }

    @Test
    @DisplayName("로그인 시 비밀번호가 일치하지 않으면 빈 Optional을 반환하고 JWT를 생성하지 않는다")
    void login_whenPasswordMismatch_thenReturnEmpty(){
        LogRequestDto request = loginRequest("admin","wrong-password");

        AdminUser user = AdminUser.builder()
                .username("admin")
                .password("encoded-password")
                .role("ROLE_ADMIN")
                .build();

        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password","encoded-password")).thenReturn(false);

        Optional<String> result = authService.login(request);

        assertThat(result).isEmpty();

        verify(adminUserRepository).findByUsername("admin");
        verify(passwordEncoder).matches("wrong-password","encoded-password");
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("회원가입시 사용자 이름이 중복되지 않으면 비밀번호를 암호화하여 관리자를 저장하고 true를 반환한다")
    void registerAdmin_whenUsernameNotExists_thenSaveUserAndReturnTrue(){
        LogRequestDto request = loginRequest("admin","1234");

        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("1234")).thenReturn("encoded-password");

        boolean result = authService.registerAdmin(request);

        assertThat(result).isTrue();

        ArgumentCaptor<AdminUser> userCaptor = ArgumentCaptor.forClass(AdminUser.class);
        verify(adminUserRepository).save(userCaptor.capture());

        AdminUser savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("admin");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getRole()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("회원가입시 사용자 이름이 이미 존재하면 관리자를 저장하지 않고 false를 반환한다")
    void registerAdmin_whenUsernameExists_thenReturnFalse() {
        LogRequestDto request = loginRequest("admin","1234");

        AdminUser existingUser = AdminUser.builder()
                .username("admin")
                .password("encoded-password")
                .role("ROLE_ADMIN")
                .build();

        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(existingUser));

        boolean result = authService.registerAdmin(request);

        assertThat(result).isFalse();

        verify(adminUserRepository).findByUsername("admin");
        verify(adminUserRepository,never()).save(any());
        verifyNoInteractions(passwordEncoder);

    }

    private LogRequestDto loginRequest(String username, String password) {
        LogRequestDto request = new LogRequestDto();
        ReflectionTestUtils.setField(request, "username", username);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }
}
