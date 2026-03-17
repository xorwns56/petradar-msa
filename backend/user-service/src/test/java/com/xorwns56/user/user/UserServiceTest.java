package com.xorwns56.user.user;

import com.xorwns56.user.auth.AuthDTO;
import com.xorwns56.user.kafka.UserDeletedEvent;
import com.xorwns56.user.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private KafkaTemplate<String, UserDeletedEvent> kafkaTemplate;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .loginId("testuser")
                .hp("010-1234-5678")
                .pwHash("encoded_password")
                .build();
    }

    // ========== 회원가입 테스트 ==========

    @Nested
    @DisplayName("회원가입")
    class Register {

        @Test
        @DisplayName("정상 회원가입")
        void register_success() {
            // given
            AuthDTO.RegisterRequest request = new AuthDTO.RegisterRequest("newuser", "Password1!", "010-1111-2222");
            given(userRepository.existsByLoginId("newuser")).willReturn(false);
            given(passwordEncoder.encode("Password1!")).willReturn("encoded");

            // when
            userService.register(request);

            // then
            then(userRepository).should().save(any(User.class));
        }

        @Test
        @DisplayName("중복 아이디로 회원가입 시 예외 발생")
        void register_duplicateLoginId_throwsException() {
            // given
            AuthDTO.RegisterRequest request = new AuthDTO.RegisterRequest("testuser", "Password1!", "010-1111-2222");
            given(userRepository.existsByLoginId("testuser")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.register(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 사용 중인 아이디입니다.");
        }
    }

    // ========== 로그인 테스트 ==========

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("정상 로그인 - Access Token + Refresh Token 반환")
        void login_success() {
            // given
            AuthDTO.LoginRequest request = new AuthDTO.LoginRequest("testuser", "password");
            given(userRepository.findByLoginId("testuser")).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("password", "encoded_password")).willReturn(true);
            given(jwtTokenProvider.createAccessToken("1")).willReturn("access_token");
            given(jwtTokenProvider.createRefreshToken("1")).willReturn("refresh_token");
            given(jwtTokenProvider.getRefreshExpirationMs()).willReturn(86400000L);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // when
            AuthDTO.TokenResponse response = userService.login(request);

            // then
            assertThat(response.getAccessToken()).isEqualTo("access_token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh_token");
            // Redis에 Refresh Token 저장 확인
            then(valueOperations).should().set(eq("refresh:1"), eq("refresh_token"), any());
        }

        @Test
        @DisplayName("존재하지 않는 아이디로 로그인 시 예외 발생")
        void login_userNotFound_throwsException() {
            // given
            AuthDTO.LoginRequest request = new AuthDTO.LoginRequest("unknown", "password");
            given(userRepository.findByLoginId("unknown")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        @Test
        @DisplayName("비밀번호 불일치 시 예외 발생")
        void login_wrongPassword_throwsException() {
            // given
            AuthDTO.LoginRequest request = new AuthDTO.LoginRequest("testuser", "wrongpw");
            given(userRepository.findByLoginId("testuser")).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("wrongpw", "encoded_password")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
    }

    // ========== 토큰 재발급 테스트 ==========

    @Nested
    @DisplayName("토큰 재발급")
    class Reissue {

        @Test
        @DisplayName("정상 재발급")
        void reissue_success() {
            // given
            String refreshToken = "valid_refresh_token";
            given(jwtTokenProvider.validateRefreshToken(refreshToken)).willReturn(true);
            given(jwtTokenProvider.getUserIdFromRefreshToken(refreshToken)).willReturn("1");
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("refresh:1")).willReturn(refreshToken);
            given(jwtTokenProvider.createAccessToken("1")).willReturn("new_access_token");

            // when
            String result = userService.reissue(refreshToken);

            // then
            assertThat(result).isEqualTo("new_access_token");
        }

        @Test
        @DisplayName("유효하지 않은 Refresh Token 시 예외 발생")
        void reissue_invalidToken_throwsException() {
            // given
            given(jwtTokenProvider.validateRefreshToken("invalid")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.reissue("invalid"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("유효하지 않은 Refresh Token입니다.");
        }

        @Test
        @DisplayName("Redis에 저장된 토큰과 불일치 시 예외 발생 (로그아웃/탈취)")
        void reissue_tokenMismatch_throwsException() {
            // given
            String refreshToken = "valid_refresh_token";
            given(jwtTokenProvider.validateRefreshToken(refreshToken)).willReturn(true);
            given(jwtTokenProvider.getUserIdFromRefreshToken(refreshToken)).willReturn("1");
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("refresh:1")).willReturn("different_token");

            // when & then
            assertThatThrownBy(() -> userService.reissue(refreshToken))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("만료되거나 로그아웃된 토큰입니다.");
        }
    }

    // ========== 로그아웃 테스트 ==========

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("정상 로그아웃 - Redis에서 Refresh Token 삭제")
        void logout_success() {
            // given
            String refreshToken = "valid_refresh_token";
            given(jwtTokenProvider.validateRefreshToken(refreshToken)).willReturn(true);
            given(jwtTokenProvider.getUserIdFromRefreshToken(refreshToken)).willReturn("1");

            // when
            userService.logout(refreshToken);

            // then
            then(redisTemplate).should().delete("refresh:1");
        }

        @Test
        @DisplayName("유효하지 않은 토큰으로 로그아웃 시 무시")
        void logout_invalidToken_ignored() {
            // given
            given(jwtTokenProvider.validateRefreshToken("invalid")).willReturn(false);

            // when
            userService.logout("invalid");

            // then - Redis 삭제 호출 없음
            then(redisTemplate).should(never()).delete(anyString());
        }
    }

    // ========== 사용자 조회 테스트 ==========

    @Nested
    @DisplayName("사용자 조회")
    class FindUser {

        @Test
        @DisplayName("ID로 사용자 조회 성공")
        void findById_success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when
            UserDTO.Response response = userService.findById(1L);

            // then
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getLoginId()).isEqualTo("testuser");
            assertThat(response.getHp()).isEqualTo("010-1234-5678");
        }

        @Test
        @DisplayName("존재하지 않는 ID 조회 시 예외 발생")
        void findById_notFound_throwsException() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.findById(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("전체 사용자 조회")
        void findAll_success() {
            // given
            User user2 = User.builder().id(2L).loginId("user2").hp("010-0000-0000").build();
            given(userRepository.findAll()).willReturn(List.of(testUser, user2));

            // when
            List<UserDTO.Response> result = userService.findAll();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getLoginId()).isEqualTo("testuser");
            assertThat(result.get(1).getLoginId()).isEqualTo("user2");
        }

        @Test
        @DisplayName("아이디 중복 확인")
        void existsByLoginId() {
            // given
            given(userRepository.existsByLoginId("testuser")).willReturn(true);
            given(userRepository.existsByLoginId("newuser")).willReturn(false);

            // when & then
            assertThat(userService.existsByLoginId("testuser")).isTrue();
            assertThat(userService.existsByLoginId("newuser")).isFalse();
        }
    }

    // ========== 사용자 정보 수정 테스트 ==========

    @Nested
    @DisplayName("사용자 정보 수정")
    class Update {

        @Test
        @DisplayName("정상 수정")
        void update_success() {
            // given
            UserDTO.UpdateRequest request = new UserDTO.UpdateRequest("NewPass1!", "010-9999-8888");
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(passwordEncoder.encode("NewPass1!")).willReturn("new_encoded");

            // when
            userService.update(1L, request);

            // then
            assertThat(testUser.getHp()).isEqualTo("010-9999-8888");
            assertThat(testUser.getPwHash()).isEqualTo("new_encoded");
        }

        @Test
        @DisplayName("존재하지 않는 사용자 수정 시 예외 발생")
        void update_notFound_throwsException() {
            // given
            UserDTO.UpdateRequest request = new UserDTO.UpdateRequest("NewPass1!", "010-9999-8888");
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.update(999L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("사용자를 찾을 수 없습니다.");
        }
    }

    // ========== 회원 탈퇴 테스트 ==========

    @Nested
    @DisplayName("회원 탈퇴")
    class Delete {

        @Test
        @DisplayName("정상 탈퇴 - DB 삭제 + Redis 삭제 + Kafka 이벤트 발행")
        void delete_success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when
            userService.delete(1L);

            // then
            then(userRepository).should().delete(testUser);
            then(redisTemplate).should().delete("refresh:1");
            then(kafkaTemplate).should().send(eq("user-deleted"), any(UserDeletedEvent.class));
        }

        @Test
        @DisplayName("존재하지 않는 사용자 탈퇴 시 예외 발생")
        void delete_notFound_throwsException() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.delete(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("사용자를 찾을 수 없습니다.");
        }
    }
}
