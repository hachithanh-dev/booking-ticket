package vn.geekup.booking.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ID_ATTR = "currentUserId";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_ROLE = "X-Role";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // Admin endpoints require X-Role: ADMIN
        if (path.startsWith("/api/v1/admin")) {
            String role = request.getHeader(HEADER_ROLE);
            if (!"ADMIN".equals(role)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required");
                return false;
            }
            return true;
        }

        // Webhook endpoints — no auth required
        if (path.startsWith("/api/v1/webhooks")) {
            return true;
        }

        // Customer endpoints require X-User-Id
        String userIdHeader = request.getHeader(HEADER_USER_ID);
        if (userIdHeader == null || userIdHeader.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "X-User-Id header required");
            return false;
        }

        try {
            UUID userId = UUID.fromString(userIdHeader);
            request.setAttribute(USER_ID_ATTR, userId);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid X-User-Id format");
            return false;
        }

        return true;
    }
}
