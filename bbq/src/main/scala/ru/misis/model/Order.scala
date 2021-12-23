package ru.misis.model

import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._


case class Order(id: Int, orderId: Int, userId: Int, itemId: Int, menuId: Int, count: Int)
case class OrderStatus(orderId: Int, status: String = "Accepted")

trait OrderRepo {

  class OrderTable(tag: Tag) extends Table[Order](tag, "Order") {
    val id = column[Int]("id", O.PrimaryKey)
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
    ) <> ((Order.apply _).tupled, Order.unapply)
  }

  class OrderStatusTable(tag: Tag) extends Table[OrderStatus](tag, "OrderStatus") {
    val orderId = column[Int]("orderId", O.PrimaryKey)
    val status = column[String]("status")
    def * = (
      orderId,
      status
    ) <> ((OrderStatus.apply _).tupled, OrderStatus.unapply)
  }

  val orderTable = TableQuery[OrderTable]
  val orderStatusTable = TableQuery[OrderStatusTable]
}