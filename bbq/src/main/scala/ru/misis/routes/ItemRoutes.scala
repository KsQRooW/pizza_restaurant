package ru.misis.routes

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ru.misis.model.Item
import ru.misis.registry.ItemRegistry
import ru.misis.registry.ItemRegistry._

class ItemRoutes(itemRegistry: ActorRef[ItemRegistry.Command])(implicit val system: ActorSystem[_]){
    import ru.misis.JsonFormats._
    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

    // If ask takes more time than this to complete the request is failed
    private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

    val routes: Route =
        path("items") {
            get {
                complete(itemRegistry.ask(GetItems))
            }
        } ~
        path("items") {
            (post & entity(as[Item])) { item =>
                onSuccess(itemRegistry.ask(CreateItem(item, _))) { performed =>
                    complete((StatusCodes.Created, performed))
                }
            }
        } ~
        path("item" / Segment) { item =>
              get {
                  rejectEmptyResponse {
                      onSuccess(itemRegistry.ask(GetItem(item, _))) { response =>
                          complete(response.maybeItem)
                      }
                  }
              }
        } ~
        path("item" / Segment) { itemId =>
              (put & entity(as[Item])) { item =>
                  onSuccess(itemRegistry.ask(UpdateItem(itemId.toInt, item, _))) { performed =>
                      complete((StatusCodes.OK, performed))
                  }
              }
        } ~
        path("item" / Segment) { item =>
              delete {
                  onSuccess(itemRegistry.ask(DeleteItem(item, _))) { performed =>
                      complete((StatusCodes.OK, performed))
                  }
              }
        }

}
