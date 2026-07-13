package vn.thanhtuanle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * A registered judge_server, kept fresh by its periodic heartbeat
 * (POST /api/judge_server_heartbeat/). Mirrors QingdaoU's JudgeServer model.
 * {@code hostname} is the natural key used to upsert on each heartbeat.
 */
@Entity
@Table(name = "t_judge_servers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class JudgeServer extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String hostname;

    private String ip;

    private String judgerVersion;

    private Integer cpuCore;

    private Double cpuUsage;

    private Double memoryUsage;

    private String serviceUrl;

    private LocalDateTime lastHeartbeat;

    @Builder.Default
    private Integer taskNumber = 0;

    @Builder.Default
    private boolean isDisabled = false;
}
