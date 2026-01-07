package repositories;

import entities.CompositeFunctionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompositeFunctionRepository extends JpaRepository<CompositeFunctionEntity, Long> {
    List<CompositeFunctionEntity> findByUserId(Long userId);
    
    Optional<CompositeFunctionEntity> findByUserIdAndNameIgnoreCase(Long userId, String name);
    
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
}

