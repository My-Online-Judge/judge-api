package vn.thanhtuanle.messaging.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.thanhtuanle.judge.dto.JudgeResultDto;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionJudgedEvent {
    private String submissionId;
    private Integer status;
    private Integer result;

    @JsonProperty("cpu_time")
    private Integer cpuTime;

    @JsonProperty("real_time")
    private Integer realTime;

    private Long memory;

    @JsonProperty("error_message")
    private String errorMessage;

    private List<JudgeResultDto> details;
}
