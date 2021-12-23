package ru.misis.services

import ru.misis.model.{MenuItem, ItemRepo, MenuRepo, Order, OrderRepo, OrderStatus, User, UserRepo}

import scala.concurrent.{ExecutionContext, Future}
import ru.misis.registry.OrderRegistry.{ItemOrder, OrderDto, OrdersDto}
import slick.jdbc.PostgresProfile.api._

trait OrderService {
  def getOrders(): Future[OrdersDto]
  def createOrder(order: Order): Future[Unit]
  def getOrder(orderId: Int): Future[Option[OrderDto]]
  def getOrdersWithStatus(status: String): Future[OrdersDto]
  def updateOrder(orderId: Int, order: Order): Future[Int]
  def deleteOrder(orderId: Int): Future[Unit]

  def getOrderStatus(orderId: Int): Future[Option[OrderStatus]]
  def updateOrderStatus(orderId: Int, status: String): Future[Int]

}

trait OrderServiceImpl extends OrderService with OrderRepo with UserRepo with ItemRepo with MenuRepo{
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

        //db.run( orderStatusTable.filter(_.orderId =!=order.orderId).result )
          //.flatMap(orderstatus => orderStatusTable += OrderStatus(orderstatus.map(_.orderId).head))

        //userTable += User(order.userId)

        orderStatusTable += OrderStatus(order.orderId)
      ).transactionally
    }
  }

  override def getOrder(orderId: Int): Future[Option[OrderDto]] = {
    db.run {
      orderTable
        .join(userTable).on{ case (order, user) => order.userId === user.id }
        .join(orderStatusTable).on{ case ((order, user), status) => order.orderId ===status.orderId}
        .join(menuItemTable).on{ case (((order, user), status), menuitem) => order.itemId === menuitem.itemId}
        .join(itemTable).on{ case ((((order, user), status), menuitem), item) => menuitem.itemId === item.id}
        .filter{ case ((((order, _), _), _), _) => order.orderId === orderId}
        .map{ case ((((order, user), status), menuitem), item) => (order, user, status, menuitem, item) }
        .result
    }.map(_
      .groupBy(_._1.orderId)
      .map{case (order, seq) => OrderDto(order, seq.map(_._2.id).head, seq.map(_._3.status).head,
        seq.map { case (order, user, status, menuitem, item) => ItemOrder(item, order.count)}.distinct)}
      .headOption
    )
  }

  override def getOrderStatus(orderId: Int): Future[Option[OrderStatus]] = {
    db.run(orderStatusTable.filter(_.orderId === orderId)
      .result
      .headOption)
  }

  override def getOrdersWithStatus(status: String): Future[OrdersDto] = {
    db.run{
      orderStatusTable.filter(_.status === status).result.map(orders => OrdersDto(orders.map(_.orderId).toSet))
    }
  }

  override def updateOrderStatus(orderId: Int, status: String): Future[Int] = {
    db.run(orderStatusTable.filter(_.orderId === orderId)
      .map(_.status)
      .update(status))
  }

  override def updateOrder(orderId: Int, order: Order): Future[Int] = {
    db.run(orderTable.filter(_.orderId === orderId)
      .map(ord => (ord.menuId, ord.itemId, ord.count))
      .update((order.menuId, order.itemId, order.count))
    )
  }

  override def deleteOrder(orderId: Int): Future[Unit] = {
    db.run{
      DBIO.seq(
        orderTable.filter(_.orderId === orderId).delete,
        orderStatusTable.filter(_.orderId === orderId).delete
      ).transactionally
    }
  }
}
