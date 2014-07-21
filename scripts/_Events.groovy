import org.codehaus.groovy.grails.test.runner.phase.IntegrationTestPhaseConfigurer
import org.codehaus.groovy.grails.test.spock.GrailsSpecTestType

eventTestPhasesStart = { phasesToRun ->

    if (grailsSettings.forkSettings.test) {
        grailsConsole.error("Migration tests don't support forked mode execution, please disabled forked mode for 'test' in BuildConfig.groovy")
        System.exit(1)
    }

    /**
     * Creates a new test phase and type called migration so migration tests can be called as:
     * "grails test-app migration:migration" or just "grails test-app migration:"
     *
     * Don't ask me how this stuff works, this was cobbled together with examples on stackoverflow and plugin source code.
     */

    phasesToRun << "migration"

    def testTypeName = "migration"
    def testDirectory = "migration"

    def migrationPhaseConfigurer = new IntegrationTestPhaseConfigurer(projectTestRunner.projectTestCompiler, projectLoader)

    projectTestRunner.testFeatureDiscovery.configurers.migration = migrationPhaseConfigurer

    def migrationTestType = new GrailsSpecTestType(testTypeName, testDirectory)

    projectTestRunner.testFeatureDiscovery.testExecutionContext.migrationTests = [migrationTestType]

}

migrationTestPhasePreparation = {
    // called at the start of the phase
    println "*** Starting migration tests"
    integrationTestPhasePreparation()
}