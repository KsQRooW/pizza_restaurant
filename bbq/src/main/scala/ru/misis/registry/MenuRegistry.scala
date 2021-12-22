package ru.misis.registry

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import ru.misis.model.{Item, Menu}
import ru.misis.registry.ItemRegistry.{CreateItem, GetItem}
import ru.misis.services.MenuService

import scala.concurrent.{ExecutionContext, Future}


abstract class MenuRegistry(implicit val system: ActorSystem[_], executionContext: ExecutionContext) extends MenuService{
    import MenuRegistry._
    private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

    def apply(): Behavior[Command] =  Behaviors.receiveMessage {
        case GetMenus(replyTo) =>
            getMenus().map(replyTo ! _)
            Behaviors.same
        case CreateMenu(menuDto, replyTo) =>
            createMenu(menuDto).map(_ => replyTo ! ActionPerformed(s"Menu ${menuDto.name} created."))
            Behaviors.same
        case GetMenu(name, replyTo) =>
            getMenu(name).map(replyTo ! GetMenuResponse(_))
            Behaviors.same
        case DeleteMenu(name, replyTo) =>
            deleteMenu(name).map(_ => replyTo ! ActionPerformed(s"Menu $name updated."))
            Behaviors.same
    }

}

object MenuRegistry {
    sealed trait Command
    case class GetMenus(replyTo: ActorRef[MenusDto]) extends Command
    case class CreateMenu(menu: MenuDto, replyTo: ActorRef[ActionPerformed]) extends Command
    case class GetMenu(name: String, replyTo: ActorRef[GetMenuResponse]) extends Command
    case class DeleteMenu(name: String, replyTo: ActorRef[ActionPerformed]) extends Command


    case class MenusDto(names: Seq[String])
    case class MenuDto(id: Int, name: String, items: Seq[Item])

    final case class GetMenuResponse(maybeMenu: Option[MenuDto])
    final case class ActionPerformed(description: String)

    case class Menus(menus: Seq[Menu])
}
