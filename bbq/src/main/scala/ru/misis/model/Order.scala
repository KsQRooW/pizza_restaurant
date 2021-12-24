package ru.misis.model

import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._

import java.util.UUID


case class OrderItem(id: String = UUID.randomUUID().toString, orderId: Int, userId: Int, itemId: Int, menuId: Int, count: Int)
case class Order(orderId: Int, status: String = "Accepted")

trait OrderRepo {

  class OrderItemTable(tag: Tag) extends Table[OrderItem](tag, "OrderItem") {
    val id = column[String]("id", O.PrimaryKey)
    val orderId = column[Int]("orderId")
    val userId = column[Int]("userId")
    val itemId = column[Int]("itemId")
    val menuId = column[Int]("menuId")
    val count = column[Int]("count")
    def * = (
      id,
      orderId,
      userId,
      itemId,
      menuId,
      count
    ) <> ((OrderItem.apply _).tupled, OrderItem.unapply)
  }

  class OrderTable(tag: Tag) extends Table[Order](tag, "Order") {
    val orderId = column[Int]("orderId", O.PrimaryKey)
    val status = column[String]("status")
    def * = (
      orderId,
      status
    ) <> ((Order.apply _).tupled, Order.unapply)
  }

  val orderItemTable = TableQuery[OrderItemTable]
  val orderTable = TableQuery[OrderTable]
}