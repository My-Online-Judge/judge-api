package vn.thanhtuanle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "t_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Submission extends BaseEntity {

    private String problemId;

    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String sourceCode;

    private String language;

    private int status;

    private String result;

    private String errorMessage;

    private Integer cpuTime;

    private int time;

    private Long memory;
}
