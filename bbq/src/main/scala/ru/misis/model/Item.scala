package ru.misis.model

import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._


case class Item(id: Int, name: String = "Null", price: Double = -1.0)

trait ItemRepo {

    class ItemTable(tag: Tag) extends Table[Item](tag, "Item") {
        val id = column[Int]("id", O.PrimaryKey)
        val name = column[String]("name")
        val price = column[Double]("price")
        def * = (
            id,
            name,
            price
        ) <> ((Item.apply _).tupled, Item.unapply)
    }

    val itemTable = TableQuery[ItemTable]
}