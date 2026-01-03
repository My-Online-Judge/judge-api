package vn.thanhtuanle.submission;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.thanhtuanle.entity.Submission;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
}
