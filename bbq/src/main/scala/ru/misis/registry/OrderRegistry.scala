package ru.misis.registry

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import ru.misis.model.{Item, OrderItem, Order}
import ru.misis.services.OrderService

import scala.concurrent.ExecutionContext

abstract class OrderRegistry(implicit val system: ActorSystem[_], executionContext: ExecutionContext) extends OrderService{
    import OrderRegistry._
    //private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

    def apply(): Behavior[Command] = Behaviors.receiveMessage {
        case GetOrders(replyTo) =>
            getOrders().map(replyTo ! _)
            Behaviors.same
        case CreateOrder(order, replyTo) =>
            createOrder(order).map(_ => replyTo ! ActionPerformed(s"Order ${order.orderId} created."))
            Behaviors.same
        case GetOrder(orderId, replyTo) =>
            getOrder(orderId).map(replyTo ! GetOrderResponse(_))
            Behaviors.same
        case GetOrdersWithStatus(status, replyTo) =>
            getOrdersWithStatus(status).map(replyTo ! _)
            Behaviors.same
        case UpdateOrder(orderId, order, replyTo) =>
            updateOrder(orderId, order).map(_ => replyTo ! ActionPerformed(s"Order ${orderId} updated."))
            Behaviors.same
        case DeleteOrder(orderId, replyTo) =>
            deleteOrder(orderId).map(_ => replyTo ! ActionPerformed(s"Order $orderId deleted."))
            Behaviors.same

        case UpdateOrderStatus(orderId, status, replyTo) =>
            updateOrderStatus(orderId, status).map(_ => replyTo ! ActionPerformed(s"Order ${orderId} status changed to ${status}."))
            Behaviors.same
        case GetOrderStatus(orderId, replyTo) =>
            getOrderStatus(orderId).map(order => replyTo ! ActionPerformed(s"Order number ${order.map(_.orderId).getOrElse("None")} status is ${order.map(_.status).getOrElse("None")}."))
            Behaviors.same
    }
}

object OrderRegistry {
    sealed trait Command
    case class GetOrders(replyTo: ActorRef[OrdersDto]) extends Command
    case class CreateOrder(order: OrderDto, replyTo: ActorRef[ActionPerformed]) extends Command
    case class GetOrder(orderId: Int, replyTo: ActorRef[GetOrderResponse]) extends Command
    case class GetOrdersWithStatus(status: String, replyTo: ActorRef[OrdersDto]) extends Command
    case class UpdateOrder(orderId: Int, order: OrderItem, replyTo: ActorRef[ActionPerformed]) extends Command
    case class DeleteOrder(orderId: Int, replyTo: ActorRef[ActionPerformed]) extends Command

    case class UpdateOrderStatus(orderId: Int, status: String, replyTo: ActorRef[ActionPerformed]) extends Command
    case class GetOrderStatus(orderId: Int, replyTo: ActorRef[ActionPerformed]) extends Command


    case class OrdersDto(orders: Set[Int])
    case class OrderDto(orderId: Int, userId: Int, menuId: Int, itemAndCount: Seq[(Int, Int)])
    case class OrderOut(orderId: Int, userId: Int, status: String, items: Seq[ItemOrder])
    case class ItemOrder(item: Item, count: Int)

    final case class GetOrderResponse(maybeOrder: Option[OrderOut])
    //final case class GetOrdersResponse(maybeOrders: Option[OrdersDto])
    final case class ActionPerformed(description: String)
}