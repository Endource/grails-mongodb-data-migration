package grails.plugins.mongodb.datamigration

import grails.test.spock.IntegrationSpec

class MigrationsTestPhaseSpec extends IntegrationSpec {

    def db
    def mongo //injected by mongodb plugin
    def grailsApplication

    def setup() {
        def databaseName = grailsApplication.config.grails.mongo.databaseName as String
        db = mongo.getDB(databaseName)

        db.migrations.drop()
        db.migrations_lock.drop()
        db.cities.drop()
    }

    void "Migrations test phase"() {

        given:
            Runner runner = new Runner()
            runner.setRunlist("classpath:/resources/a/migrationTestPhase.json")

        when: "runner executes"
            runner.execute()

        then:
            db.cities.findOne(name: "Zagreb")
    }

}
