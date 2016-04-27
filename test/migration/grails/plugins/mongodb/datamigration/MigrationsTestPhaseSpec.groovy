package grails.plugins.mongodb.datamigration

import com.mongodb.MongoClient
import grails.test.spock.IntegrationSpec

class MigrationsTestPhaseSpec extends IntegrationSpec {

    def database
    MongoClient mongo //injected by mongodb plugin
    def grailsApplication

    def setup() {
        def databaseName = grailsApplication.config.grails.mongo.databaseName as String
        database = mongo.getDatabase(databaseName)

        database.migrations.drop()
        database.migrations_lock.drop()
        database.cities.drop()
    }

    void "Migrations test phase"() {

        given:
            Runner runner = new Runner()
            runner.setRunlist("classpath:/resources/a/migrationTestPhase.json")

        when: "runner executes"
            runner.execute()

        then:
            database.cities.findOne(name: "Zagreb")
    }

}
