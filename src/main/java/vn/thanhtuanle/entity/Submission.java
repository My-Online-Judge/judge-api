package vn.thanhtuanle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.thanhtuanle.judge.dto.JudgeResultDto;

import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Table(name = "t_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Submission extends BaseEntity {
    @Column(columnDefinition = "TEXT")
    private String sourceCode;

    private int status;

    private Integer result;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Integer cpuTime;

    private Integer time;

    private Long memory;

    private Boolean shareSubmission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", referencedColumnName = "id", nullable = false)
    private Problem problem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", referencedColumnName = "id", nullable = false)
    private Language language;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    // Raw per-testcase results from the judge, stored verbatim. Read ONLY through
    // SubmissionDetailAssembler — it carries hidden test data that must be filtered.
    // No columnDefinition: the test profile is H2, which has no `jsonb`.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details")
    private List<JudgeResultDto> details;
}
