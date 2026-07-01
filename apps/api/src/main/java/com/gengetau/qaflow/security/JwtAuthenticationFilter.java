package com.gengetau.qaflow.security;

import com.gengetau.qaflow.modules.auth.AuthService;
import com.gengetau.qaflow.modules.auth.InvalidTokenException;
import com.gengetau.qaflow.modules.auth.JwtService;
import com.gengetau.qaflow.modules.users.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final AuthService authService;

  public JwtAuthenticationFilter(JwtService jwtService, AuthService authService) {
    this.jwtService = jwtService;
    this.authService = authService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      UUID userId = jwtService.parseUserId(authorization.substring(7));
      User user = authService.requireActiveUser(userId);
      CurrentUser currentUser = new CurrentUser(user.getId(), user.getEmail(), user.getDisplayName());
      var authentication = new UsernamePasswordAuthenticationToken(currentUser, null, List.of());
      SecurityContextHolder.getContext().setAuthentication(authentication);
      filterChain.doFilter(request, response);
    } catch (InvalidTokenException exception) {
      SecurityContextHolder.clearContext();
      response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid access token");
    }
  }
}