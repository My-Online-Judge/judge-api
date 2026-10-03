package vn.thanhtuanle.submission.problem;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import vn.thanhtuanle.entity.Problem;
import vn.thanhtuanle.entity.TestCase;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * The submission side reaches problems only through {@link ProblemCatalog}. In sub-project 2b the problem
 * code leaves this service and {@code LocalProblemCatalog} is replaced by a gRPC client; this rule is
 * what makes that a swap instead of a rewrite.
 */
@AnalyzeClasses(packages = "vn.thanhtuanle", importOptions = ImportOption.DoNotIncludeTests.class)
class SubmissionSeamTest {

    @ArchTest
    static final ArchRule submissionSideUsesNoProblemCode = noClasses()
            .that().resideInAnyPackage("vn.thanhtuanle.submission..", "vn.thanhtuanle.judge..",
                    "vn.thanhtuanle.messaging..")
            .should().dependOnClassesThat().resideInAnyPackage("vn.thanhtuanle.problem..", "vn.thanhtuanle.testcase..")
            .orShould().dependOnClassesThat().belongToAnyOf(Problem.class, TestCase.class);
}
