package vn.thanhtuanle.submission;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.thanhtuanle.entity.Submission;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    @Query("SELECT s FROM Submission s JOIN FETCH s.problem WHERE s.problem.problemSlug = :slug")
    List<Submission> findByProblemSlug(@Param("slug") String slug);
}
