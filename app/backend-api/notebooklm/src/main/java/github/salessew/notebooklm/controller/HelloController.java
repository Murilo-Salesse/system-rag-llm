package github.salessew.notebooklm.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/hello")
public class HelloController {

    private static final Logger log = LoggerFactory.getLogger(HelloController.class);

    @GetMapping
    public ResponseEntity<Map<String, Object>> hello(@AuthenticationPrincipal Jwt jwt) {
        String tokenValue = jwt != null ? jwt.getTokenValue() : "anonymous";
        String subject = jwt != null ? jwt.getSubject() : "none";
        String email = jwt != null ? jwt.getClaimAsString("email") : "none";

        log.info("==> [HelloController] Received authenticated request! Subject: {}, Email: {}, Token: {}",
                subject, email, tokenValue);

        return ResponseEntity.ok(Map.of(
                "message", "Hello authenticated world!",
                "subject", subject,
                "email", email,
                "token", tokenValue
        ));
    }
}
