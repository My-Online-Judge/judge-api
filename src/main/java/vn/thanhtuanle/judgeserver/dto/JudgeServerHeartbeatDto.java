package vn.thanhtuanle.judgeserver.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Heartbeat payload posted by a judge_server. Field names match QingdaoU's sender exactly.
 * Unknown keys (e.g. {@code action}) are ignored so protocol additions don't break parsing.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JudgeServerHeartbeatDto {

    private String hostname;

    @JsonProperty("judger_version")
    private String judgerVersion;

    @JsonProperty("cpu_core")
    private Integer cpuCore;

    /** CPU usage percent. */
    private Double cpu;

    /** Memory usage percent. */
    private Double memory;

    @JsonProperty("service_url")
    private String serviceUrl;
}
