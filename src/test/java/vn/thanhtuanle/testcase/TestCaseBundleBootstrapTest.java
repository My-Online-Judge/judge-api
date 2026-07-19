package vn.thanhtuanle.testcase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.problem.ProblemRepository;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestCaseBundleBootstrapTest {

    @Mock ProblemRepository problemRepository;
    @Mock TestCaseBundleStore store;

    private Problem problem(String slug) {
        Problem p = new Problem();
        p.setProblemSlug(slug);
        return p;
    }

    @Test
    void run_publishesOnlyProblemsMissingABundle() {
        when(problemRepository.findAll()).thenReturn(List.of(problem("has"), problem("missing")));
        when(store.hasBundle("has")).thenReturn(true);
        when(store.hasBundle("missing")).thenReturn(false);

        new TestCaseBundleBootstrap(problemRepository, store).run(null);

        verify(store).ensureBucket();
        verify(store).publish("missing");
        verify(store, never()).publish("has");
    }

    @Test
    void run_continuesWhenOneProblemFailsToPublish() {
        when(problemRepository.findAll()).thenReturn(List.of(problem("bad"), problem("good")));
        when(store.hasBundle("bad")).thenReturn(false);
        when(store.hasBundle("good")).thenReturn(false);
        when(store.publish("bad")).thenThrow(new TestCaseBundleException("boom"));

        new TestCaseBundleBootstrap(problemRepository, store).run(null);

        verify(store).publish("good"); // not aborted by "bad" failing
    }
}
