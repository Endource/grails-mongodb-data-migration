package grails.plugins.mongodb.datamigration

import grails.util.Environment
import grails.util.Holders
import groovy.json.JsonSlurper
import org.codehaus.groovy.grails.commons.spring.GrailsApplicationContext
import org.codehaus.groovy.grails.core.io.ResourceLocator
import org.codehaus.groovy.grails.io.support.PathMatchingResourcePatternResolver
import org.codehaus.groovy.grails.io.support.Resource
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.reflect.Field

class Runner {

    def migrationsPath
    def runlist

    def environment
    def grailsApplication
    def applicationContext
    ResourceLocator grailsResourceLocator
    def db

    protected Logger log = LoggerFactory.getLogger(getClass())

    public Runner() {

        environment = Environment.current
        grailsApplication = Holders.getGrailsApplication()
        grailsResourceLocator = grailsApplication.getMainContext().getBean("grailsResourceLocator")

        runlist = "classpath:/migrations/datamigration.json"
        migrationsPath = runlist.substring(0, runlist.lastIndexOf("/"))

    }

    protected void setRunlist(String runlist) {
        this.runlist = runlist
        migrationsPath = runlist.substring(0, runlist.lastIndexOf("/"))
    }

    public void execute() {

        def mongo = grailsApplication.getMainContext().getBean("mongo")
        def databaseName = grailsApplication.config.grails.mongo.databaseName as String
        db = mongo.getDB(databaseName)

        ChangeLog[] changelogs = getChangelogs()

        changelogs.each { ChangeLog changelog ->
            invokeChangeLog(changelog)
        }
    }

    protected void invokeChangeLog(ChangeLog changelog) {

        Binding binding = new Binding();
        binding.setVariable("db", db)
        GroovyShell shell = new GroovyShell(binding)

        if (db.migrations.findOne(id: changelog.filename)) {
            log.info("Changelog exists: ${changelog.filename}")
        } else {
            if (changelog.context == 'default' || environment.name.toLowerCase() == changelog.context) {
                log.info("Changelog matches context '${changelog.context}' for current environment ${environment.name.toLowerCase()}")
                try {
                    shell.evaluate(grailsResourceLocator.findResourceForURI("${migrationsPath}/${changelog.filename}").inputStream.text)

                    db.migrations.insert(id: changelog.filename, author: changelog.author, context: changelog.context, description: changelog.description)
                    log.info("Changelog inserted: ${changelog.filename}")

                } catch(Exception e) {
                    log.error("Changelog (${migrationsPath}/${changelog.filename}) failed: ${e.message}")
                    e.printStackTrace()
                }
            }
        }

    }

    protected ChangeLog[] getChangelogs() {

        def changelogs = []

        def slurper = new JsonSlurper()

        def datamigrationJson = grailsResourceLocator.findResourceForURI(runlist).inputStream.text

        def changelogsJson = slurper.parseText(datamigrationJson).changelogs

        println changelogsJson

        changelogsJson.each {

            if (it.filename) {
                changelogs << new ChangeLog(filename: it.filename, author: it.author ?: "anonymous", description: it.description ?: "no description", context: it.context ?: "default")
            } else {
                log.error("Filename missing for entry: ${it}")
            }
        }

        return changelogs
    }

}

class ChangeLog {

    String author
    String description
    String context
    String filename

}