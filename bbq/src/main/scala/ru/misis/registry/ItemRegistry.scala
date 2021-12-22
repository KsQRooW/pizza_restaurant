package ru.misis.registry

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import ru.misis.model.Item
import ru.misis.services.ItemService

import scala.concurrent.ExecutionContext

abstract class ItemRegistry(implicit val system: ActorSystem[_], executionContext: ExecutionContext)
    extends ItemService{

    import ItemRegistry._

    def apply(): Behavior[Command] = Behaviors.receiveMessage {
        case GetItems(replyTo) =>
            getItems().map(replyTo ! Items(_))
            Behaviors.same
        case CreateItem(item, replyTo) =>
            createItem(item).map(_ => replyTo ! ActionPerformed(s"Item ${item.name} created."))
            Behaviors.same
        case GetItem(name, replyTo) =>
            getItem(name).map(replyTo ! GetItemResponse(_))
            Behaviors.same
        case DeleteItem(name, replyTo) =>
            deleteItem(name).map(_ => ActionPerformed(s"Item $name deleted."))
            Behaviors.same
    }
}

object ItemRegistry {
    sealed trait Command
    case class GetItems(replyTo: ActorRef[Items]) extends Command
    case class CreateItem(item: Item, replyTo: ActorRef[ActionPerformed]) extends Command
    case class GetItem(name: String, replyTo: ActorRef[GetItemResponse]) extends Command
    case class DeleteItem(name: String, replyTo: ActorRef[ActionPerformed]) extends Command

    case class Items(items: Seq[Item])
    final case class GetItemResponse(maybeItem: Option[Item])
    final case class ActionPerformed(description: String)
}