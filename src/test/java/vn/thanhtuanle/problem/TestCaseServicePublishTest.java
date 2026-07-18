package vn.thanhtuanle.problem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.entity.TestCase;
import vn.thanhtuanle.testcase.TestCaseBundleStore;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestCaseServicePublishTest {

    @Mock ProblemRepository problemRepository;
    @Mock TestCaseRepository testCaseRepository;
    @Mock vn.thanhtuanle.common.util.GenerateTestCaseInfoUtil infoGenerator;
    @Mock TestCaseBundleStore bundleStore;
    @InjectMocks TestCaseService testCaseService;

    @Test
    void deleteTestCase_republishesBundleToMinio() {
        UUID id = UUID.randomUUID();
        Problem problem = new Problem();
        problem.setProblemSlug("simple-a-plus-b");
        TestCase tc = TestCase.builder().input("simple-a-plus-b/1.in")
                .output("simple-a-plus-b/1.out").problem(problem).build();
        when(testCaseRepository.findById(id)).thenReturn(Optional.of(tc));

        testCaseService.deleteTestCase("simple-a-plus-b", id);

        verify(infoGenerator).generateInfo("simple-a-plus-b");
        verify(bundleStore).publish(eq("simple-a-plus-b"));
    }
}
