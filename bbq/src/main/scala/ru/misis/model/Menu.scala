package ru.misis.model

import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class MenuItem(id: String = UUID.randomUUID().toString, menuId: Int, itemId: Int, price: Double = -1.0)
case class Menu(id: Int, name: String)


trait MenuRepo {
    class MenuItemTable(tag: Tag)  extends Table[MenuItem](tag, "MenuItem") {
        val id = column[String]("id", O.PrimaryKey)
        val menuId = column[Int]("menuId")
        val itemId = column[Int]("itemId")
        val price = column[Double]("price")

        def * = (
            id,
            menuId,
            itemId,
            price
        ) <> ((MenuItem.apply _).tupled, MenuItem.unapply)
    }

    class MenuTable(tag: Tag)  extends Table[Menu](tag, "Menu") {
        val id = column[Int]("id", O.PrimaryKey)
        val name = column[String]("name")

        def * = (
            id,
            name,
        ) <> ((Menu.apply _).tupled, Menu.unapply)
    }

    val menuItemTable = TableQuery[MenuItemTable]
    val menuTable = TableQuery[MenuTable]
}