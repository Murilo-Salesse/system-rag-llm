package github.salessew.notebooklm.service;

import github.salessew.notebooklm.domain.entity.User;
import github.salessew.notebooklm.domain.repository.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User getOrCreateCurrentUser(Jwt jwt) {
        if (jwt == null) {
            throw new IllegalArgumentException("JWT não pode ser nulo");
        }

        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new IllegalArgumentException("JWT subject ('sub') não pode ser nulo ou vazio");
        }

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = sub + "@cognito.local";
        }

        String name = Optional.ofNullable(jwt.getClaimAsString("name"))
                .orElse(jwt.getClaimAsString("cognito:username"));

        Optional<User> existingUserOpt = userRepository.findByCognitoSub(sub);

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            boolean dirty = false;

            if (!Objects.equals(existingUser.getEmail(), email)) {
                existingUser.setEmail(email);
                dirty = true;
            }

            if (!Objects.equals(existingUser.getName(), name)) {
                existingUser.setName(name);
                dirty = true;
            }

            return dirty ? userRepository.save(existingUser) : existingUser;
        }

        User newUser = new User(sub, email, name);
        return userRepository.save(newUser);
    }
}
