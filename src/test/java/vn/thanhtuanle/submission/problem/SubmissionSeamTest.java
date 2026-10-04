package vn.thanhtuanle.submission.problem;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * The submission side reaches problems only through {@link ProblemCatalog}. Since sub-project 2b its one
 * implementation is a gRPC client of problem-service; only this package may touch the generated stubs, so
 * the rest of the service never learns the wire types and the transport stays replaceable.
 */
@AnalyzeClasses(packages = "vn.thanhtuanle", importOptions = ImportOption.DoNotIncludeTests.class)
class SubmissionSeamTest {

    @ArchTest
    static final ArchRule onlyTheCatalogSpeaksToProblemService = noClasses()
            .that().resideOutsideOfPackages("vn.thanhtuanle.submission.problem..", "vn.thanhtuanle.oj.common..")
            .should().dependOnClassesThat().resideInAPackage("vn.thanhtuanle.oj.common.grpc.problem..");
}
