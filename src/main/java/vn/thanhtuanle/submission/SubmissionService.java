package vn.thanhtuanle.submission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vn.thanhtuanle.common.constant.Permissions;
import vn.thanhtuanle.common.enums.SubmissionResult;
import vn.thanhtuanle.common.exception.ResourceNotFoundException;
import vn.thanhtuanle.common.payload.PageResponse;
import vn.thanhtuanle.entity.Language;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.entity.Submission;
import vn.thanhtuanle.entity.User;
import vn.thanhtuanle.judge.JudgeService;
import vn.thanhtuanle.messaging.event.SubmissionRequestedAppEvent;
import vn.thanhtuanle.messaging.event.SubmissionRequestedEvent;
import vn.thanhtuanle.submission.dto.SubmissionRequestDto;
import vn.thanhtuanle.submission.dto.SubmissionResponseDto;
import vn.thanhtuanle.submission.mapper.SubmissionMapper;
import vn.thanhtuanle.problem.ProblemRepository;
import vn.thanhtuanle.language.LanguageRepository;

import vn.thanhtuanle.user.UserService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final JudgeService judgeService;
    private final ProblemRepository problemRepository;
    private final LanguageRepository languageRepository;
    private final SubmissionMapper submissionMapper;
    private final UserService userService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final SubmissionSseRegistry sseRegistry;
    private final SubmissionDetailAssembler detailAssembler;

    @Transactional
    public SubmissionResponseDto submit(SubmissionRequestDto req) {
        log.info("Start submission for problem: {}", req.getProblemSlug());
        judgeService.validate(req.getSourceCode());

        Problem problem = problemRepository.findByProblemSlug(req.getProblemSlug())
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found"));
        Language language = languageRepository.findByIdentifier(req.getLanguageIdentifier())
                .orElseThrow(() -> new ResourceNotFoundException("Language not found"));
        User currentUser = userService.getCurrentUser();

        Submission submission = createPendingSubmission(req, problem, language, currentUser);
        submissionRepository.save(submission);
        MDC.put("submissionId", submission.getId().toString());
        try {
            SubmissionRequestedEvent event = judgeService.buildRequestedEvent(
                    submission.getId().toString(), submission.getSourceCode(), problem, language);
            applicationEventPublisher.publishEvent(new SubmissionRequestedAppEvent(event));
            log.info("Submission {} queued for judging", submission.getId());
            return submissionMapper.toDto(submission);
        } finally {
            MDC.remove("submissionId");
        }
    }

    private Submission createPendingSubmission(SubmissionRequestDto req, Problem problem, Language language,
            User user) {
        return Submission.builder()
                .sourceCode(req.getSourceCode())
                .problem(problem)
                .user(user)
                .language(language)
                .time(0)
                .memory(0L)
                .status(SubmissionResult.PENDING.getValue())
                .shareSubmission(req.getShareSubmission())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<SubmissionResponseDto> getSubmissionsByProblemSlug(String problemSlug, int page, int size) {
        log.info("Service to get submissions by problem slug: {}", problemSlug);

        if (!problemRepository.existsByProblemSlug(problemSlug)) {
            throw new IllegalArgumentException("Problem not found with slug: " + problemSlug);
        }

        Pageable pageable = PageRequest.of(page, size);
        return PageResponse.of(submissionRepository
                .findByProblemSlugOrderByCreatedAtDesc(problemSlug, pageable)
                .map(submissionMapper::toDto));
    }

    @Transactional(readOnly = true)
    public SubmissionResponseDto getSubmissionById(String id) {
        log.info("Service to get submission by id: {}", id);
        Submission submission = submissionRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + id));
        assertCanRead(submission, id);
        return submissionMapper.toDto(submission, detailAssembler.assemble(submission));
    }

    @Transactional(readOnly = true)
    public SseEmitter streamVerdict(String id) {
        // Load + authorize BEFORE subscribing: an unknown id must 404 identically to a denied
        // id (no existence oracle), and no emitter may ever be registered for a request that
        // fails either check (registering first would let a denied/unknown request evict a
        // legitimate owner's live emitter — an eviction DoS).
        Submission submission = submissionRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + id));
        assertCanRead(submission, id);

        // Now that the caller is authorized, register the emitter, then replay the verdict
        // immediately if the submission is already terminal so a late subscriber isn't lost.
        SseEmitter emitter = sseRegistry.subscribe(id);
        if (SubmissionResult.isTerminal(submission.getStatus())) {
            log.info("Submission {} already terminal (status={}) at subscribe, replaying verdict",
                    id, submission.getStatus());
            sseRegistry.complete(id, submissionMapper.toDto(submission, detailAssembler.assemble(submission)));
        }
        return emitter;
    }

    @Transactional(readOnly = true)
    public PageResponse<SubmissionResponseDto> getSubmissionsByUser(String userId, int page, int size) {
        log.info("Service to get submissions by user_id: {}", userId);

        UUID requested = UUID.fromString(userId);
        if (!requested.equals(userService.getCurrentUser().getId()) && !hasReadAnyAuthority()) {
            throw new ResourceNotFoundException("Submissions not found for user: " + userId);
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<Submission> submissionPage = submissionRepository.findByUserIdOrderByCreatedAtDesc(UUID.fromString(userId),
                pageable);

        log.info("Found {} submissions for user_id: {}", submissionPage.getTotalElements(), userId);
        return PageResponse.of(submissionPage.map(submissionMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<SubmissionResponseDto> getSubmissionsByUserAndProblem(String userId, String problemSlug,
            int page, int size) {
        log.info("Service to get submissions by user_id: {} and problem_slug: {}", userId, problemSlug);

        UUID requested = UUID.fromString(userId);
        if (!requested.equals(userService.getCurrentUser().getId()) && !hasReadAnyAuthority()) {
            throw new ResourceNotFoundException("Submissions not found for user: " + userId);
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<Submission> submissionPage = submissionRepository
                .findByUserIdAndProblemSlugOrderByCreatedAtDesc(UUID.fromString(userId), problemSlug, pageable);

        log.info("Found {} submissions for user_id: {} and problem_slug: {}", submissionPage.getTotalElements(), userId,
                problemSlug);
        return PageResponse.of(submissionPage.map(submissionMapper::toDto));
    }

    /**
     * Owner-or-admin gate. Throws {@link ResourceNotFoundException} — deliberately a 404 rather
     * than a 403, so the response does not confirm that an id exists.
     */
    private void assertCanRead(Submission submission, String id) {
        User current = userService.getCurrentUser();
        boolean isOwner = submission.getUser() != null
                && submission.getUser().getId().equals(current.getId());
        if (isOwner || hasReadAnyAuthority()) {
            return;
        }
        log.warn("User {} denied access to submission {}", current.getId(), id);
        throw new ResourceNotFoundException("Submission not found with id: " + id);
    }

    private boolean hasReadAnyAuthority() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> Permissions.SUBMISSION_READ_ANY.equals(a.getAuthority()));
    }
}
