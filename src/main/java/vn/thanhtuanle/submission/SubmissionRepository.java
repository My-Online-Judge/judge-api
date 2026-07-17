package vn.thanhtuanle.submission;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.thanhtuanle.entity.Submission;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    /** Non-terminal submissions (PENDING/JUDGING) created before the given threshold — stuck. */
    @Query("SELECT s FROM Submission s WHERE s.status IN :statuses AND s.createdAt < :threshold")
    List<Submission> findStuck(@Param("statuses") Collection<Integer> statuses,
                               @Param("threshold") LocalDateTime threshold);
    @Query("SELECT s FROM Submission s WHERE s.problem.problemSlug = :slug ORDER BY s.createdAt DESC")
    Page<Submission> findByProblemSlugOrderByCreatedAtDesc(@Param("slug") String slug, Pageable pageable);

    @Query("""
                SELECT s
                FROM Submission s
                WHERE s.user.id = :userId
                ORDER BY s.createdAt DESC
            """)
    Page<Submission> findByUserIdOrderByCreatedAtDesc(
            @Param("userId") UUID userId,
            Pageable pageable);


    @Query("""
                SELECT s
                FROM Submission s
                WHERE s.user.id = :userId AND s.problem.problemSlug = :problemSlug
                ORDER BY s.createdAt DESC
            """)
    Page<Submission> findByUserIdAndProblemSlugOrderByCreatedAtDesc(
            @Param("userId") UUID userId,
            @Param("problemSlug") String problemSlug,
            Pageable pageable
    );
}
