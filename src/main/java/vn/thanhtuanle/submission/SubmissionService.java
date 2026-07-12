package vn.thanhtuanle.submission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

        SubmissionRequestedEvent event = judgeService.buildRequestedEvent(
                submission.getId().toString(), submission.getSourceCode(), problem, language);
        applicationEventPublisher.publishEvent(new SubmissionRequestedAppEvent(event));

        log.info("Submission {} queued for judging", submission.getId());
        return submissionMapper.toDto(submission);
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
        return submissionMapper.toDto(submission);
    }

    @Transactional(readOnly = true)
    public PageResponse<SubmissionResponseDto> getSubmissionsByUser(String userId, int page, int size) {
        log.info("Service to get submissions by user_id: {}", userId);

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

        Pageable pageable = PageRequest.of(page, size);

        Page<Submission> submissionPage = submissionRepository
                .findByUserIdAndProblemSlugOrderByCreatedAtDesc(UUID.fromString(userId), problemSlug, pageable);

        log.info("Found {} submissions for user_id: {} and problem_slug: {}", submissionPage.getTotalElements(), userId,
                problemSlug);
        return PageResponse.of(submissionPage.map(submissionMapper::toDto));
    }
}
