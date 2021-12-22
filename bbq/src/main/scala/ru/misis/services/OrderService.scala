package ru.misis.services

import ru.misis.model.{Item, ItemRepo, Order, OrderRepo, OrderStatus, User, UserRepo}

import scala.concurrent.{ExecutionContext, Future}
import ru.misis.registry.OrderRegistry.{OrderDto, OrdersDto}
import slick.jdbc.PostgresProfile.api._

trait OrderService {
  def getOrders(): Future[OrdersDto]
  def createOrder(order: Order): Future[Unit]
  def getOrder(orderId: Int): Future[Option[OrderDto]]

  def getOrderStatus(orderId: Int): Future[Option[OrderStatus]]
  def updateOrderStatus(orderId: Int, status: Boolean): Future[Int]

  def deleteOrder(orderId: Int): Future[Int]
}

trait OrderServiceImpl extends OrderService with OrderRepo with UserRepo with ItemRepo{
  def db: Database
  implicit def executionContext: ExecutionContext

  override def getOrders(): Future[OrdersDto] = {
    db.run{
      orderTable.result.map(orders => OrdersDto(orders.map(_.orderId).toSet))
    }
  }

  override def createOrder(order: Order): Future[Unit] = {
    db.run {
      DBIO.seq(
        orderTable += order,
        orderStatusTable += OrderStatus(order.orderId),
        userTable += User(order.userId),
        itemTable += Item(order.itemId)
      ).transactionally
    }
  }

  override def getOrder(orderId: Int): Future[Option[OrderDto]] = {
    db.run {
      orderTable
        .join(userTable).on{ case (order, user) => order.userId === user.id }
        .join(orderStatusTable).on{ case ((order, user), status) => order.orderId ===status.orderid}
        .join(itemTable).on{ case (((order, user), status), item) => order.itemId === item.id}
        .filter{ case (((order, _), _), _) => order.orderId === orderId}
        .map{ case (((order, user), status), item) => (order, user, status, item) }
        .result
    }.map(_
      .groupBy(_._1.orderId)
      .map{case (order, seq) => OrderDto(order, seq.map(_._2.name).head, seq.map(_._3.status).head, seq.map(_._4))}
      .headOption
    )
  }

  override def getOrderStatus(orderId: Int): Future[Option[OrderStatus]] = {
    db.run(orderStatusTable.filter(_.orderid === orderId)
      .result
      .headOption)
  }

  override def updateOrderStatus(orderId: Int, status: Boolean): Future[Int] = {
    db.run(orderStatusTable.filter(_.orderid === orderId)
      .update(???))
  }


  override def deleteOrder(orderId: Int): Future[Int] = {
    db.run{
      orderTable.filter(_.orderId === orderId).delete
    }
  }
}
