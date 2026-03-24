package com.xorwns56.user.user;

import com.xorwns56.user.auth.AuthDTO;
import com.xorwns56.user.kafka.UserDeletedEvent;
import com.xorwns56.user.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, UserDeletedEvent> kafkaTemplate;

    // Redis key 형식: refresh:{userId}
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String KAFKA_TOPIC_USER_DELETED = "user-deleted";

    // 회원가입
    @Transactional
    public void register(AuthDTO.RegisterRequest request) {
        if (userRepository.existsByLoginId(request.getLoginId())) {
            log.warn("회원가입 실패 - 중복 아이디: loginId={}", request.getLoginId());
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        User user = User.builder()
                .loginId(request.getLoginId())
                .hp(request.getHp())
                .pwHash(passwordEncoder.encode(request.getPw()))
                .build();
        userRepository.save(user);
        log.info("회원가입 완료: userId={}, loginId={}", user.getId(), user.getLoginId());
    }

    // 로그인 - Access Token 반환, Refresh Token Redis 저장
    @Transactional(readOnly = true)
    public AuthDTO.TokenResponse login(AuthDTO.LoginRequest request) {
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPw(), user.getPwHash())) {
            log.warn("로그인 실패 - 비밀번호 불일치: loginId={}", request.getLoginId());
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        String userId = String.valueOf(user.getId());
        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        // Refresh Token을 Redis에 저장 (TTL: 24시간)
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userId,
                refreshToken,
                Duration.ofMillis(jwtTokenProvider.getRefreshExpirationMs())
        );

        log.info("로그인 성공: userId={}, loginId={}", userId, user.getLoginId());
        return new AuthDTO.TokenResponse(accessToken, refreshToken);
    }

    // Refresh Token으로 Access Token 재발급
    public String reissue(String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            log.warn("토큰 재발급 실패 - 유효하지 않은 Refresh Token");
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        String userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);

        // Redis에 저장된 Refresh Token과 비교 (탈취/로그아웃 여부 확인)
        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
        if (!refreshToken.equals(storedToken)) {
            log.warn("토큰 재발급 실패 - 만료/로그아웃된 토큰: userId={}", userId);
            throw new IllegalArgumentException("만료되거나 로그아웃된 토큰입니다.");
        }

        log.info("토큰 재발급 완료: userId={}", userId);
        return jwtTokenProvider.createAccessToken(userId);
    }

    // 로그아웃 - Redis에서 Refresh Token 삭제
    public void logout(String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            return;
        }
        String userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
        log.info("로그아웃 완료: userId={}", userId);
    }

    // 아이디 중복 확인
    @Transactional(readOnly = true)
    public boolean existsByLoginId(String loginId) {
        return userRepository.existsByLoginId(loginId);
    }

    // 사용자 조회 (internal)
    @Transactional(readOnly = true)
    public UserDTO.Response findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id: " + id));
        return UserDTO.Response.from(user);
    }

    // 전체 사용자 조회 (internal - report-service 알림 발송용)
    @Transactional(readOnly = true)
    public List<UserDTO.Response> findAll() {
        return userRepository.findAll().stream()
                .map(UserDTO.Response::from)
                .collect(Collectors.toList());
    }

    // 사용자 정보 수정
    @Transactional
    public void update(Long userId, UserDTO.UpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setHp(request.getHp());
        user.setPwHash(passwordEncoder.encode(request.getPw()));
        log.info("사용자 정보 수정 완료: userId={}", userId);
    }

    // 회원 탈퇴 - Redis 토큰 삭제 + Kafka 이벤트 발행
    @Transactional
    public void delete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        userRepository.delete(user);

        // Redis에서 Refresh Token 삭제
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);

        // report-service에서 관련 데이터(missing, report, notification) 삭제를 위한 이벤트 발행
        kafkaTemplate.send(KAFKA_TOPIC_USER_DELETED, new UserDeletedEvent(userId));
        log.info("회원 탈퇴 완료: userId={}, Kafka 이벤트 발행", userId);
    }

    // loginId로 사용자 조회 (로그인 처리용)
    @Transactional(readOnly = true)
    public Optional<User> findByLoginId(String loginId) {
        return userRepository.findByLoginId(loginId);
    }
}
