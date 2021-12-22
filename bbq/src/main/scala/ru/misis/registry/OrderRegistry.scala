package ru.misis.registry

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import ru.misis.model.{Item, Order, OrderStatus}
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

        case UpdateOrderStatus(orderId, status, replyTo) =>
            updateOrderStatus(orderId, status).map(_ => replyTo ! ActionPerformed(s"Order ${orderId} status changed to ${status}."))
            Behaviors.same
        case GetOrderStatus(orderId, replyTo) =>
            getOrderStatus(orderId).map(order => replyTo ! ActionPerformed(s"Order number ${order.map(_.orderId).getOrElse("None")} status is ${order.map(_.status).getOrElse("None")}."))
            Behaviors.same

        case GetOrder(orderId, replyTo) =>
            getOrder(orderId).map(replyTo ! GetOrderResponse(_))
            Behaviors.same
        case DeleteOrder(orderId, replyTo) =>
            deleteOrder(orderId).map(_ => replyTo ! ActionPerformed(s"Order $orderId deleted."))
            Behaviors.same
    }
}

object OrderRegistry {
    sealed trait Command
    case class GetOrders(replyTo: ActorRef[OrdersDto]) extends Command
    case class CreateOrder(order: Order, replyTo: ActorRef[ActionPerformed]) extends Command

    case class UpdateOrderStatus(orderId: Int, status: Boolean, replyTo: ActorRef[ActionPerformed]) extends Command
    case class GetOrderStatus(orderId: Int, replyTo: ActorRef[ActionPerformed]) extends Command

    case class GetOrder(orderId: Int, replyTo: ActorRef[GetOrderResponse]) extends Command
    case class DeleteOrder(orderId: Int, replyTo: ActorRef[ActionPerformed]) extends Command


    case class OrdersDto(orders: Set[Int])
    case class OrderDto(orderId: Int, name: String, status: Boolean, items: Seq[Item])

    final case class GetOrderResponse(maybeOrder: Option[OrderDto])
    final case class ActionPerformed(description: String)

}