package repositories;

import entities.FunctionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Collection;

public interface FunctionRepository extends JpaRepository<FunctionEntity, Long> {
    List<FunctionEntity> findByUserId(Long userId);

    List<FunctionEntity> findByUserIdAndNameContainingIgnoreCase(Long userId, String name, Sort sort);

    List<FunctionEntity> findByIdIn(Collection<Long> ids, Sort sort);
    
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
    
    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);
}

