package vn.thanhtuanle.submission;

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
    @Query("SELECT s FROM Submission s JOIN FETCH s.problem WHERE s.problem.problemSlug = :slug ORDER BY s.createdAt DESC")
    List<Submission> findByProblemSlugOrderByCreatedAtDesc(@Param("slug") String slug);

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
