package repositories;

import config.Application;
import entities.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class)
@TestPropertySource(locations = "classpath:application.properties")
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private FunctionRepository functionRepository;

    @Autowired
    private CompositeFunctionRepository compositeFunctionRepository;

    @BeforeEach
    void setUp() {
        // Удаляем в правильном порядке: точки -> компонентные функции -> функции -> пользователи (избегаем проблем с внешними ключами)
        pointRepository.deleteAll();
        compositeFunctionRepository.deleteAll();
        functionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testSimpleSaveAndFind() {
        // 1. Create
        UserEntity user = new UserEntity("test_user", "password123");
        UserEntity saved = userRepository.save(user);

        // 2. Read
        Optional<UserEntity> found = userRepository.findByUsername("test_user");

        // 3. Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("test_user");
        assertThat(found.get().getPasswordHash()).isEqualTo("password123");
    }

    @Test
    void testFindByUsernameNotFound() {
        Optional<UserEntity> found = userRepository.findByUsername("non_existent_user");
        assertThat(found).isEmpty();
    }

    @Test
    void testFindByUsernameInWithSorting() {
        // Given
        UserEntity u1 = userRepository.save(new UserEntity("alice", "hash1"));
        UserEntity u2 = userRepository.save(new UserEntity("bob", "hash2"));
        UserEntity u3 = userRepository.save(new UserEntity("charlie", "hash3"));

        // When - ищем только alice и charlie, сортируем по username по возрастанию
        List<UserEntity> users = userRepository.findByUsernameIn(
                List.of("charlie", "alice"),
                Sort.by(Sort.Direction.ASC, "username")
        );

        // Then
        assertThat(users).hasSize(2);
        assertThat(users.get(0).getUsername()).isEqualTo("alice");
        assertThat(users.get(1).getUsername()).isEqualTo("charlie");
        assertThat(users).extracting(UserEntity::getId)
                .containsExactlyInAnyOrder(u1.getId(), u3.getId());
    }

    @Test
    void testFindByUsernameInEmptyAndUnknown() {
        // Given
        userRepository.save(new UserEntity("user1", "hash1"));

        // When / Then
        assertThat(userRepository.findByUsernameIn(List.of(), Sort.unsorted()))
                .isEmpty();
        assertThat(userRepository.findByUsernameIn(List.of("unknown"), Sort.by("username")))
                .isEmpty();
    }

    @Test
    void testDeleteUserAndCount() {
        // Given
        UserEntity u1 = userRepository.save(new UserEntity("user1", "hash1"));
        UserEntity u2 = userRepository.save(new UserEntity("user2", "hash2"));

        assertThat(userRepository.count()).isEqualTo(2);

        // When
        userRepository.delete(u1);

        // Then
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(userRepository.findByUsername("user1")).isEmpty();
        assertThat(userRepository.findByUsername("user2")).isPresent();
    }
}