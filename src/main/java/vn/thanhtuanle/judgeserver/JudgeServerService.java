package vn.thanhtuanle.judgeserver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.thanhtuanle.common.util.HashUtil;
import vn.thanhtuanle.entity.JudgeServer;
import vn.thanhtuanle.judgeserver.dto.JudgeServerHeartbeatDto;
import vn.thanhtuanle.judgeserver.dto.JudgeServerResponseDto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JudgeServerService {

    private final JudgeServerRepository judgeServerRepository;

    @Value("${judge.server.token}")
    private String judgeServerToken;

    /**
     * Verify the shared token, then upsert the judge_server keyed by hostname.
     *
     * @return false if the token is invalid (caller should answer 401); true on success.
     */
    @Transactional
    public boolean handleHeartbeat(JudgeServerHeartbeatDto dto, String clientToken, String ip) {
        if (!isTokenValid(clientToken)) {
            log.warn("Rejected judge_server heartbeat from {}: invalid token", ip);
            return false;
        }

        JudgeServer server = judgeServerRepository.findByHostname(dto.getHostname())
                .orElseGet(() -> JudgeServer.builder().hostname(dto.getHostname()).build());

        server.setIp(ip);
        server.setJudgerVersion(dto.getJudgerVersion());
        server.setCpuCore(dto.getCpuCore());
        server.setCpuUsage(dto.getCpu());
        server.setMemoryUsage(dto.getMemory());
        server.setServiceUrl(dto.getServiceUrl());
        server.setLastHeartbeat(LocalDateTime.now());

        judgeServerRepository.save(server);
        log.debug("Heartbeat from judge_server '{}' ({})", dto.getHostname(), dto.getServiceUrl());
        return true;
    }

    /** A server is considered alive if it sent a heartbeat within this window. */
    private static final Duration HEARTBEAT_TIMEOUT = Duration.ofSeconds(30);

    @Transactional(readOnly = true)
    public List<JudgeServerResponseDto> listServers() {
        LocalDateTime cutoff = LocalDateTime.now().minus(HEARTBEAT_TIMEOUT);
        return judgeServerRepository.findAll().stream()
                .map(s -> JudgeServerResponseDto.builder()
                        .hostname(s.getHostname())
                        .ip(s.getIp())
                        .judgerVersion(s.getJudgerVersion())
                        .cpuCore(s.getCpuCore())
                        .cpuUsage(s.getCpuUsage())
                        .memoryUsage(s.getMemoryUsage())
                        .serviceUrl(s.getServiceUrl())
                        .lastHeartbeat(s.getLastHeartbeat())
                        .taskNumber(s.getTaskNumber())
                        .disabled(s.isDisabled())
                        .alive(s.getLastHeartbeat() != null && s.getLastHeartbeat().isAfter(cutoff))
                        .build())
                .toList();
    }

    private boolean isTokenValid(String clientToken) {
        if (clientToken == null) {
            return false;
        }
        String expected = HashUtil.sha256Hex(judgeServerToken);
        // Constant-time comparison to avoid leaking the token via timing.
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                clientToken.getBytes(StandardCharsets.UTF_8));
    }
}
