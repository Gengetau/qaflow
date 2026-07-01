package com.gengetau.qaflow.modules.auth;

import com.gengetau.qaflow.modules.auth.dto.AuthResponse;
import com.gengetau.qaflow.modules.auth.dto.CurrentUserResponse;
import com.gengetau.qaflow.modules.auth.dto.LoginRequest;
import com.gengetau.qaflow.modules.auth.dto.RegisterRequest;
import com.gengetau.qaflow.modules.auth.dto.UserSummaryResponse;
import com.gengetau.qaflow.modules.auth.dto.WorkspaceMembershipResponse;
import com.gengetau.qaflow.modules.users.User;
import com.gengetau.qaflow.modules.users.UserRepository;
import com.gengetau.qaflow.modules.workspaces.Workspace;
import com.gengetau.qaflow.modules.workspaces.WorkspaceMember;
import com.gengetau.qaflow.modules.workspaces.WorkspaceMemberRepository;
import com.gengetau.qaflow.modules.workspaces.WorkspaceRepository;
import com.gengetau.qaflow.modules.workspaces.WorkspaceRole;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final SecureRandom secureRandom = new SecureRandom();
  private final long refreshTokenDays;

  public AuthService(
      UserRepository userRepository,
      WorkspaceRepository workspaceRepository,
      WorkspaceMemberRepository workspaceMemberRepository,
      RefreshTokenRepository refreshTokenRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      @Value("${qaflow.security.refresh-token-days}") long refreshTokenDays) {
    this.userRepository = userRepository;
    this.workspaceRepository = workspaceRepository;
    this.workspaceMemberRepository = workspaceMemberRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.refreshTokenDays = refreshTokenDays;
  }

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    String email = normalizeEmail(request.email());
    if (userRepository.existsByEmail(email)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
    }

    User user =
        userRepository.save(
            new User(email, passwordEncoder.encode(request.password()), request.displayName().trim()));
    Workspace workspace =
        workspaceRepository.save(new Workspace(request.workspaceName().trim(), uniqueSlug(request.workspaceName()), user));
    workspaceMemberRepository.save(new WorkspaceMember(workspace, user, WorkspaceRole.OWNER));

    return authResponse(user, issueRefreshToken(user));
  }

  @Transactional
  public AuthResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByEmail(normalizeEmail(request.email()))
            .filter(User::isActive)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    return authResponse(user, issueRefreshToken(user));
  }

  @Transactional
  public AuthResponse refresh(String rawRefreshToken) {
    RefreshToken existing =
        refreshTokenRepository
            .findByTokenHashAndRevokedAtIsNull(hash(rawRefreshToken))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

    if (existing.isExpired() || !existing.getUser().isActive()) {
      existing.revoke();
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
    }

    existing.revoke();
    return authResponse(existing.getUser(), issueRefreshToken(existing.getUser()));
  }

  @Transactional
  public void logout(String rawRefreshToken) {
    refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash(rawRefreshToken)).ifPresent(RefreshToken::revoke);
  }

  @Transactional(readOnly = true)
  public CurrentUserResponse currentUser(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .filter(User::isActive)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not active"));
    return new CurrentUserResponse(
        user.getId(), user.getEmail(), user.getDisplayName(), user.getAvatarUrl(), memberships(user));
  }

  @Transactional(readOnly = true)
  public User requireActiveUser(UUID userId) {
    return userRepository
        .findById(userId)
        .filter(User::isActive)
        .orElseThrow(() -> new InvalidTokenException("User is not active"));
  }

  private AuthResponse authResponse(User user, String refreshToken) {
    return new AuthResponse(
        jwtService.issueAccessToken(user),
        refreshToken,
        "Bearer",
        jwtService.expiresInSeconds(),
        new UserSummaryResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getAvatarUrl()),
        memberships(user));
  }

  private List<WorkspaceMembershipResponse> memberships(User user) {
    return workspaceMemberRepository.findByUser_Id(user.getId()).stream()
        .map(this::toMembershipResponse)
        .toList();
  }

  private WorkspaceMembershipResponse toMembershipResponse(WorkspaceMember member) {
    Workspace workspace = member.getWorkspace();
    return new WorkspaceMembershipResponse(
        workspace.getId(), workspace.getName(), workspace.getSlug(), member.getRole().name());
  }

  private String issueRefreshToken(User user) {
    byte[] bytes = new byte[48];
    secureRandom.nextBytes(bytes);
    String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    refreshTokenRepository.save(
        new RefreshToken(user, hash(rawToken), OffsetDateTime.now().plusDays(refreshTokenDays)));
    return rawToken;
  }

  private String uniqueSlug(String workspaceName) {
    String base = slugify(workspaceName);
    String candidate = base;
    int suffix = 2;
    while (workspaceRepository.existsBySlug(candidate)) {
      candidate = base + "-" + suffix;
      suffix++;
    }
    return candidate;
  }

  private String slugify(String input) {
    String slug = input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    return slug.isBlank() ? "workspace" : slug;
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private String hash(String rawToken) {
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}