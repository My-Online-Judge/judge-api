package vn.thanhtuanle.judge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.thanhtuanle.entity.Language;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.messaging.event.SubmissionRequestedEvent;
import vn.thanhtuanle.testcase.TestCaseBundleStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JudgeServiceTestCaseIdTest {

    @Mock TestCaseBundleStore bundleStore;

    @Test
    void buildRequestedEvent_setsSlugDoubleUnderscoreHashAsTestCaseId() {
        when(bundleStore.currentVersion("simple-a-plus-b")).thenReturn("abc123def456");
        JudgeService service = new JudgeService(bundleStore);

        Problem problem = new Problem();
        problem.setProblemSlug("simple-a-plus-b");
        problem.setTimeLimit(1000);
        problem.setMemoryLimit(64L);
        Language language = languageStub();

        SubmissionRequestedEvent event =
                service.buildRequestedEvent("sub-1", "int main(){}", problem, language);

        assertThat(event.getTestCaseId()).isEqualTo("simple-a-plus-b__abc123def456");
    }

    private Language languageStub() {
        Language l = new Language();
        l.setIdentifier("c");
        l.setSrcName("main.c");
        l.setExeName("main");
        l.setCompileMaxMemory(268435456L);
        l.setCompileCommand("/usr/bin/gcc -o main main.c");
        l.setRunCommand("main");
        l.setSeccompRule("c_cpp");
        return l;
    }
}
