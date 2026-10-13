package feehch.dev.autoparts.infrastructure.security;

import feehch.dev.autoparts.application.port.out.TokenStore;
import feehch.dev.autoparts.application.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Извлекает JWT из заголовка {@code Authorization: Bearer ...}, проверяет его
 * (подпись, срок, чёрный список, массовый отзыв) и помещает пользователя в SecurityContext.
 * <p>
 * Полномочия формируются с учётом иерархии ролей: ADMIN получает также ROLE_MANAGER и ROLE_CUSTOMER.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final TokenStore tokenStore;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, TokenStore tokenStore) {
        this.tokenProvider = tokenProvider;
        this.tokenStore = tokenStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)) {
            tokenProvider.parse(header.substring(BEARER.length()).trim())
                    .filter(this::notRevoked)
                    .ifPresent(parsed -> authenticate(parsed.user(), request));
        }
        chain.doFilter(request, response);
    }

    private boolean notRevoked(JwtTokenProvider.ParsedToken token) {
        if (tokenStore.isAccessTokenBlacklisted(token.user().tokenId())) {
            return false;
        }
        return tokenStore.accessTokensRevokedBefore(token.user().id())
                .map(revokedBefore -> !token.issuedAt().isBefore(revokedBefore))
                .orElse(true);
    }

    private void authenticate(AuthenticatedUser user, HttpServletRequest request) {
        List<SimpleGrantedAuthority> authorities = user.roles().stream()
                .flatMap(role -> role.impliedRoles().stream())
                .distinct()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
        var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
        authentication.setDetails(request.getRemoteAddr());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
