package ru.misis.routes

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ru.misis.registry.MenuRegistry
import ru.misis.registry.MenuRegistry.{ActionPerformed, CreateMenu, DeleteMenu, GetMenu, GetMenuResponse, GetMenus, MenuDto, MenusDto}

import scala.concurrent.Future

class MenuRoutes(menuRegistry: ActorRef[MenuRegistry.Command])(implicit val system: ActorSystem[_]) {
    //#import-json-formats
    import ru.misis.JsonFormats._
    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

    // If ask takes more time than this to complete the request is failed
    private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))


    def getMenus(): Future[MenusDto] =
        menuRegistry.ask(GetMenus)
    def getMenu(name: String): Future[GetMenuResponse] =
        menuRegistry.ask(GetMenu(name, _))
    def createMenu(menu: MenuDto): Future[ActionPerformed] =
        menuRegistry.ask(CreateMenu(menu, _))
    def deleteMenu(name: String): Future[ActionPerformed] =
        menuRegistry.ask(DeleteMenu(name, _))

    //#all-routes
    //#menus-get-post
    //#menus-get-delete
    val routes: Route =
    path("menus") {
        get {
            complete(getMenus())
        }
    } ~
    path("menus") {
        (post & entity(as[MenuDto])) { menu =>
            onSuccess(createMenu(menu)) { performed =>
                complete((StatusCodes.Created, performed))
            }
        }
    } ~
    path("menu" / Segment) { name =>
        get {
            rejectEmptyResponse {
                onSuccess(getMenu(name)) { response =>
                    complete(response.maybeMenu)
                }
            }
        }
    } ~
    path("menu" / Segment) { name =>
        delete {
            onSuccess(deleteMenu(name)) { performed =>
                complete((StatusCodes.OK, performed))
            }
        }
    }
}
