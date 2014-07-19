grails-mongodb-data-migration
=============================

- cluster considerations, multiple nodes trying to update.
  - write the changelog entry and then run the migration?

- does the plugin block start up of grails until migrations are complete?

- Config.groovy needs grails.plugin.mongodb.datamigration.updateOnStart=true

- info 'grails.plugins.mongodb.datamigration.Runner'

- possible to use jongo (is in the binding) but we don't test or use it much