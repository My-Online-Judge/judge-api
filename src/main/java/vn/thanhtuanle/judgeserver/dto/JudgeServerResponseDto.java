package vn.thanhtuanle.judgeserver.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class JudgeServerResponseDto {
    private String hostname;
    private String ip;
    private String judgerVersion;
    private Integer cpuCore;
    private Double cpuUsage;
    private Double memoryUsage;
    private String serviceUrl;
    private LocalDateTime lastHeartbeat;
    private Integer taskNumber;
    private boolean disabled;

    /** Derived: true when the last heartbeat is recent enough to consider the server up. */
    private boolean alive;
}
