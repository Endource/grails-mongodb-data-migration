import grails.plugin.mongodbcreatedrop.MongoCreateDropMixin
import grails.test.mixin.TestMixin
import grails.util.Environment
import grails.util.Mixin



@Mixin(MongoCreateDropMixin)
@TestMixin(MongoCreateDropMixin)
class MyBootStrap {

    def grailsApplication

    def init = { servletContext ->
        println "Current environment is ${Environment.current.name}"

//        createDropMongo(grailsApplication)
    }

}
