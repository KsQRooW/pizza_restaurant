package ru.misis.services

import ru.misis.model.{Item, ItemRepo}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

trait ItemService {
    def getItems(): Future[Seq[Item]]
    def createItem(item: Item): Future[Int] //registry(items + item)
    def getItem(name: String): Future[Option[Item]] //items.find(_.name == name)
    def updateItem(itemId: Int, item: Item): Future[Int]
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

    override def updateItem(id: Int, item:Item): Future[Int] = {
        db.run{
            itemTable.filter(_.id === id).map(item => (item.name, item.price)).update((item.name, item.price))
        }
    }

    override def deleteItem(name: String): Future[Int] = {
        db.run{
            itemTable.filter(_.name === name).delete
        }
    }

}