package vn.thanhtuanle.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "t_languages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Language extends BaseEntity {

    private String name;

    private String identifier;

    private String extension;

    private String compileCommand;

    private String runCommand;

    private String seccompRule;

    private String srcName;

    private String exeName;

    private Long compileMaxMemory;

    private Long maxMemory;

    @OneToMany(mappedBy = "language", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Submission> submissions = new ArrayList<>();

    @Builder.Default
    private boolean isDisabled = false;
}
