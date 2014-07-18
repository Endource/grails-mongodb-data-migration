package grails.plugins.mongodb.datamigration

import grails.test.spock.IntegrationSpec
import grails.util.Environment

class RunnerIntegrationSpec extends IntegrationSpec {

    def db
    def mongo //injected by mongodb plugin
    def grailsApplication

    def setup() {
        def databaseName = grailsApplication.config.grails.mongo.databaseName as String
        db = mongo.getDB(databaseName)

        db.migrations.drop()
        db.cities.drop()
    }

    void "read a test run list"() {

        given:
            Runner runner = new Runner()
            runner.setRunlist("classpath:/resources/a/datamigration.json")

        when: "we read the runlist at integration/resources/a/"
            def changelogs = runner.getChangelogs()

        then:
            changelogs.size() == 3

        and:
            changelogs.findAll { it.context == "test" }.size() == 1
            changelogs.findAll { it.context == "default" }.size() == 2
    }

    void "execute a run list for grails.env=test"() {

        given:
            Runner runner = new Runner()
            runner.setRunlist("classpath:/resources/a/datamigration.json")
            runner.environment = Environment.TEST

        when:
            runner.execute()

        then:
            db.migrations.count() == 3

        and:
            db.cities.findOne(name: "Aberdeen")
            db.cities.findOne(name: "Birmingham")
            db.cities.findOne(name: "Cardiff")
    }

    void "execute a run list for grails.env=production"() {

        given:
            Runner runner = new Runner()
            runner.setRunlist("classpath:/resources/a/datamigration.json")
            runner.environment = Environment.PRODUCTION

        when:
            runner.execute()

        then:
            db.migrations.count() == 2

        and:
            db.cities.findOne(name: "Aberdeen")
            db.cities.findOne(name: "Birmingham")

    }

    void "fail properly if run list can't be found"() {

        given:
            Runner runner = new Runner()
            runner.setRunlist("classpath:/resources/a/idontexist.json")

        expect:
            runner.getChangelogs() == []

        and: "runner executes but doesn't blow up"
            runner.execute()
    }

    void "can parse json and insert as a document"() {

        given:
            Runner runner = new Runner()
            runner.setRunlist("classpath:/resources/a/jsonmigration.json")

        expect:
            runner.execute()
    }

    void "Application will wait until lock is released"() {

        given: "two runners, one with the lock"
            Runner runnerA = new Runner()
            Runner runnerB = new Runner()

        and: "An existing lock on the database"
            runnerA.insertLock()

        when: "start the clock"
            def start = new Date().time
            Thread.start {
                sleep(5000)
                runnerA.releaseLock()
            }

        and: "start the runner not holding the lock"
            runnerB.execute()

        then: "at least 3s has elapsed"
            def end = new Date().time
            (end - start) > 3000
    }

    void "Migration timesout"() {

        given:
            Runner runner = new Runner()
            runner.timeout = 1

        when:
            runner.insertLock()
        and:
            runner.execute()

        then:
            thrown Exception

    }

}
