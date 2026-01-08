package vn.thanhtuanle.problem;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.problem.dto.ProblemStatisticProjection;
import vn.thanhtuanle.problem.dto.ProblemStatisticsInfo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, UUID>, JpaSpecificationExecutor<Problem> {
    boolean existsByProblemSlug(String problemSlug);

    Page<Problem> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Optional<Problem> findByProblemSlug(String problemSlug);

    @Query(value = """
            SELECT
                p.*,
                COUNT(s.id) AS totalSubmission,
                COUNT(CASE WHEN s.status = 0 THEN 1 END) AS acceptedSubmission
            FROM t_problems p
            LEFT JOIN t_submissions s ON p.id = s.problem_id
            WHERE (:search IS NULL OR p.title ILIKE CONCAT('%', :search, '%')
                   OR p.description ILIKE CONCAT('%', :search, '%'))
              AND (:status IS NULL OR p.status = :status)
            GROUP BY p.id
            """, countQuery = """
            SELECT count(*) FROM t_problems p
            WHERE (:search IS NULL OR p.title ILIKE CONCAT('%', :search, '%'))
              AND (:status IS NULL OR p.status = :status)
            """, nativeQuery = true)
    Page<ProblemStatisticProjection> findProblemsWithStats(String search, Integer status, Pageable pageable);

    @Query(value = """
            SELECT
                p.*,
                COUNT(s.id) AS totalSubmission,
                COUNT(CASE WHEN s.status = 0 THEN 1 END) AS acceptedSubmission
            FROM t_problems p
            LEFT JOIN t_submissions s ON p.id = s.problem_id
            WHERE p.problem_slug = :slug
            GROUP BY p.id
            """, nativeQuery = true)
    Optional<ProblemStatisticProjection> findByProblemSlugWithStats(@Param("slug") String slug);

    @Query(value = "SELECT result, count(*) as count FROM t_submissions WHERE problem_id = :problemId GROUP BY result", nativeQuery = true)
    List<ProblemStatisticsInfo> countSubmissionsByResult(@Param("problemId") UUID problemId);
}
