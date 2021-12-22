package ru.misis.routes

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ru.misis.model.{Order, OrderStatus}
import ru.misis.registry.OrderRegistry
import ru.misis.registry.OrderRegistry._
//import ru.misis.registry.OrderRegistry.{ActionPerformed, CreateOrder, DeleteOrder, GetOrder, GetOrderResponse, GetOrders, OrderDto, OrdersDto}

import scala.concurrent.Future

class OrderRoutes(orderRegistry: ActorRef[OrderRegistry.Command])(implicit val system: ActorSystem[_]) {
  //#import-json-formats
  import ru.misis.JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))


  def getOrders(): Future[OrdersDto] =
    orderRegistry.ask(GetOrders)
  def getOrder(orderId: Int): Future[GetOrderResponse] =
    orderRegistry.ask(GetOrder(orderId, _))

  def getOrderStatus(orderId: Int): Future[ActionPerformed] =
    orderRegistry.ask(GetOrderStatus(orderId, _))
  def updateOrderStatus(orderId: Int, status: Boolean): Future[ActionPerformed] =
    orderRegistry.ask(UpdateOrderStatus(orderId, status, _))

  def createOrder(order: Order): Future[ActionPerformed] =
    orderRegistry.ask(CreateOrder(order, _))
  def deleteOrder(orderId: Int): Future[ActionPerformed] =
    orderRegistry.ask(DeleteOrder(orderId, _))

  val routes: Route =
  path("orders") {
    get {
        complete(getOrders())
    }
  } ~
  path("orders") {
      (post & entity(as[Order])) { order =>
        onSuccess(createOrder(order)) { performed =>
          complete((StatusCodes.Created, performed))
        }
      }
    } ~
  path("order" / Segment) { orderId =>
      get {
        rejectEmptyResponse {
          onSuccess(getOrder(orderId.toInt)) { response =>
            complete(response.maybeOrder)
          }
        }
      }
    } ~
  path("status" /  Segment) { orderStatus =>
      get {
        rejectEmptyResponse {
          onSuccess(getOrderStatus(orderStatus.toInt)) { response =>
            complete(response)
          }
        }
      }
  } ~
  path("orderStatus" / Segment) { id =>
      (put & entity(as[OrderStatus])) { status =>
        onSuccess(updateOrderStatus(id.toInt, status.status)) { performed =>
          complete((StatusCodes.OK, performed))
        }
      }
  } ~
  path("order" / Segment) { orderId =>
      delete {
        onSuccess(deleteOrder(orderId.toInt)) { performed =>
          complete((StatusCodes.OK, performed))
        }
      }
  }
}
