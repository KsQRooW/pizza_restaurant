package ru.misis.services

import ru.misis.model.{Item, ItemRepo}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

trait ItemService {
    def getItems(): Future[Seq[Item]]
    def createItem(item: Item): Future[Int] //registry(items + item)
    def getItem(name: String): Future[Option[Item]] //items.find(_.name == name)
    def deleteItem(name: String): Future[Int] //items.filterNot(_.name == name)
}


trait ItemServiceImpl extends ItemService with ItemRepo{
    def db: Database
    implicit def executionContext: ExecutionContext

    override def getItems(): Future[Seq[Item]] = {
        db.run{ itemTable.result }
    }

    override def createItem(item: Item): Future[Int] = {
        db.run(itemTable += item)
    }

    override def getItem(name: String): Future[Option[Item]] = {
        db.run(
            itemTable.filter(_.name === name).result.headOption
        )
    }

    override def deleteItem(name: String): Future[Int] = {
        db.run{
            itemTable.filter(_.name === name).delete
        }
    }

}