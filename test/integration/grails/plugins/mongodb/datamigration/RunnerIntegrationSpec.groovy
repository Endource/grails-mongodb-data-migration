package grails.plugins.mongodb.datamigration

import grails.test.spock.IntegrationSpec
import grails.util.Environment

class RunnerIntegrationSpec extends IntegrationSpec {

    def db
    def grailsApplication

    def setup() {

        def mongo = grailsApplication.getMainContext().getBean("mongo")
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


}
