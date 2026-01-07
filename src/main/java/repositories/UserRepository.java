package repositories;

import entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;

import java.util.Optional;
import java.util.List;
import java.util.Collection;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);

    List<UserEntity> findByUsernameIn(Collection<String> usernames, Sort sort);
}

