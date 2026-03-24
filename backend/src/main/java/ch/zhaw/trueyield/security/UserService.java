package ch.zhaw.trueyield.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    public String getCurrentUserId() {
        try {
            Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return jwt.getSubject();
        } catch (Exception e) {
            return "anonymous";
        }
    }

    public String getCurrentUserEmail() {
        try {
            Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return jwt.getClaimAsString("email");
        } catch (Exception e) {
            return null;
        }
    }

    public boolean userHasRole(String role) {
        try {
            Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            List<String> roles = jwt.getClaimAsStringList("user_roles");
            return roles != null && roles.contains(role);
        } catch (Exception e) {
            return false;
        }
    }
}
