import org.codehaus.groovy.grails.test.runner.phase.IntegrationTestPhaseConfigurer
import org.codehaus.groovy.grails.test.spock.GrailsSpecTestType

eventTestPhasesStart = { phasesToRun ->

    if (grailsSettings.forkSettings.test) {
        grailsConsole.error("Migration tests don't support forked mode execution, please disabled forked mode for 'test' in BuildConfig.groovy")
        System.exit(1)
    }

    /**
     * Creates a new test phase and type called migrations so migration tests can be called as:
     * "grails test-app migrations:migrations" or just "grails test-app migrations:"
     *
     * Don't ask me how this stuff works, this was cobbled together with examples on stackoverflow and plugin source code.
     */

    phasesToRun << "migrations"

    def testTypeName = "migrations"
    def testDirectory = "migrations"

    def migrationsPhaseConfigurer = new IntegrationTestPhaseConfigurer(projectTestRunner.projectTestCompiler, projectLoader)

    projectTestRunner.testFeatureDiscovery.configurers.migrations = migrationsPhaseConfigurer

    def migrationsTestType = new GrailsSpecTestType(testTypeName, testDirectory)

    projectTestRunner.testFeatureDiscovery.testExecutionContext.migrationsTests = [migrationsTestType]

}

migrationsTestPhasePreparation = {
    // called at the start of the phase
    println "*** Starting migrations tests"
    integrationTestPhasePreparation()
}