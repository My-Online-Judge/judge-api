package vn.thanhtuanle.problem;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import vn.thanhtuanle.entity.Problem;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, UUID>, JpaSpecificationExecutor<Problem> {
    boolean existsByProblemSlug(String problemSlug);

    Page<Problem> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Optional<Problem> findByProblemSlug(String problemSlug);
}
