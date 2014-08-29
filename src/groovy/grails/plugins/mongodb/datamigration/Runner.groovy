package grails.plugins.mongodb.datamigration

import grails.util.Environment
import grails.util.Holders
import groovy.json.JsonSlurper
import org.codehaus.groovy.grails.core.io.ResourceLocator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Runner {

    def migrationsPath
    def runlist
    def timeout

    def environment
    def grailsApplication
    ResourceLocator grailsResourceLocator
    def db
    def mongo

    protected Logger log = LoggerFactory.getLogger(getClass())

    public Runner() {

        environment = Environment.current
        grailsApplication = Holders.getGrailsApplication()
        grailsResourceLocator = grailsApplication.getMainContext().getBean("grailsResourceLocator")

        def mongo = grailsApplication.getMainContext().getBean("mongo")
        def databaseName = grailsApplication.config.grails.mongo.databaseName as String
        db = mongo.getDB(databaseName)

        runlist = "classpath:/migration/datamigration.json"
        migrationsPath = runlist.substring(0, runlist.lastIndexOf("/"))
        timeout = grailsApplication.config.grails.plugin.mongodb.datamigration.timeout ?: 120

        if (grailsApplication.config.grails.plugin.mongodb.datamigration.updateOnStart) {
            log.info("MongoDB data migration enabled - running migrations....")
            execute()
        } else {
            log.info("MongoDB data migration disabled - skipping migrations....")
        }

    }

    protected void setRunlist(String runlist) {
        this.runlist = runlist
        migrationsPath = runlist.substring(0, runlist.lastIndexOf("/"))
    }

    public void execute() {

        lockOrWait()

        try {
            ChangeLog[] changelogs = getChangelogs()

            changelogs.each { ChangeLog changelog ->
                invokeChangeLog(changelog)
            }
        } finally {
            releaseLock()
        }
    }

    protected void lockOrWait() {

        def time = 0

        while (lockExists() && time <= timeout) {

            log.info("Migrations lock detected. Sleeping for 5s. Total time ${time}s")

            sleep(5000)

            time += 5
        }

        if (time > timeout) {
            log.error("Timeout waiting for data migration lock")
            throw new Exception("Timeout waiting for data migration lock")
        } else {
            insertLock()
        }
    }

    protected boolean lockExists() {
        return db.migrations_lock.findOne(name: 'lock')
    }

    protected void insertLock() {
        db.migrations_lock.insert(name: 'lock', lockedAt: new Date())
        log.info("Inserting migrations lock")
    }

    protected void releaseLock() {
        db.migrations_lock.remove(name: 'lock')
        assert !db.migrations_lock.findOne(name: 'lock')
        log.info("Removed migrations lock")
    }

    protected void invokeChangeLog(ChangeLog changelog) {

        Binding binding = new Binding();
        binding.setVariable("db", db)

        binding.setVariable("JSON", com.mongodb.util.JSON)
        GroovyShell shell = new GroovyShell(this.class.classLoader, binding)

        if (db.migrations.findOne(name: changelog.filename)) {
            log.info("Changelog exists: ${changelog.filename}")
        } else {
            if (changelog.context == 'default' || environment.name.toLowerCase() == changelog.context) {
                log.info("Changelog matches context '${changelog.context}' for current environment ${environment.name.toLowerCase()}")
                try {
                    shell.evaluate(grailsResourceLocator.findResourceForURI("${migrationsPath}/${changelog.filename}").inputStream.text)

                    db.migrations.insert(name: changelog.filename, author: changelog.author, context: changelog.context, description: changelog.description)
                    log.info("Changelog inserted: ${changelog.filename}")

                } catch(Exception e) {
                    log.error("Changelog (${migrationsPath}/${changelog.filename}) failed: ${e.message}")
                    throw e
                }
            }
        }

    }

    protected ChangeLog[] getChangelogs() {

        def changelogs = []

        def slurper = new JsonSlurper()

        log.info("Searching run list from ${runlist}")
        def datamigrationJson = grailsResourceLocator.findResourceForURI(runlist)?.inputStream?.text

        if (!datamigrationJson) {
            log.error("Couldn't read runlist from: ${runlist}. No migrations to run.")
            return []
        }

        def changelogsJson = slurper.parseText(datamigrationJson).changelogs

        changelogsJson.each {

            if (it.filename) {
                log.debug("New changelog: ${it.filename}")
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