package ru.misis.model

import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._


case class Order(id: Int, orderId: Int, userId: Int, itemId: Int)
case class OrderStatus(orderId: Int, status: Boolean = false)

trait OrderRepo {

  class OrderTable(tag: Tag) extends Table[Order](tag, "Order") {
    val id = column[Int]("id", O.PrimaryKey)
    val orderId = column[Int]("orderId")
    val userId = column[Int]("userId")
    val itemId = column[Int]("itemId")
    def * = (
      id,
      orderId,
      userId,
      itemId
    ) <> ((Order.apply _).tupled, Order.unapply)
  }

  class OrderStatusTable(tag: Tag) extends Table[OrderStatus](tag, "OrderStatus") {
    val orderid = column[Int]("orderId")
    val status = column[Boolean]("status")
    def * = (
      orderid,
      status
    ) <> ((OrderStatus.apply _).tupled, OrderStatus.unapply)
  }

  val orderTable = TableQuery[OrderTable]
  val orderStatusTable = TableQuery[OrderStatusTable]
}