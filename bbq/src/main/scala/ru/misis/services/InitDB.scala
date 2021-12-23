package ru.misis.services

import ru.misis.model.{ItemRepo, MenuRepo, OrderRepo, UserRepo}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

class InitDB(db: Database) extends ItemRepo with MenuRepo with OrderRepo with UserRepo{

    def prepareRepository(): Future[Unit] = {
        db.run { DBIO.seq(
            itemTable.schema.createIfNotExists,
            menuItemTable.schema.createIfNotExists,
            menuTable.schema.createIfNotExists,
            orderTable.schema.createIfNotExists,
            userTable.schema.createIfNotExists,
            orderStatusTable.schema.createIfNotExists
        )}
    }

    def cleanRepository(): Future[Unit] = {
        db.run { DBIO.seq(
            itemTable.schema.dropIfExists,
            menuItemTable.schema.dropIfExists,
            menuTable.schema.dropIfExists,
            orderTable.schema.dropIfExists,
            userTable.schema.dropIfExists,
            orderStatusTable.schema.dropIfExists
        )}
    }

}
