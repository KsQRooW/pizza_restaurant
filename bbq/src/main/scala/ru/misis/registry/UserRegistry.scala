package ru.misis.registry

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import ru.misis.model.User
import ru.misis.services.UserService

import scala.concurrent.ExecutionContext

abstract class UserRegistry(implicit val system: ActorSystem[_], executionContext: ExecutionContext) extends UserService{
  import UserRegistry._

  def apply(): Behavior[Command] = Behaviors.receiveMessage {
      case GetUsers(replyTo) =>
        getUsers().map(replyTo ! Users(_))
        Behaviors.same
      case CreateUser(user, replyTo) =>
        createUser(user).map(_ => replyTo ! ActionPerformed(s"User ${user.name} created."))
        Behaviors.same
      case GetUser(name, replyTo) =>
        getUser(name).map(replyTo ! GetUserResponse(_))
        Behaviors.same
      case DeleteUser(name, replyTo) =>
        deleteUser(name).map(_ => replyTo ! ActionPerformed(s"User $name deleted."))
        Behaviors.same
      case UpdateUser(name, user, replyTo) =>
        updateUser(name, user).map(_ => replyTo ! ActionPerformed(s"User $name updated."))
        Behaviors.same
    }
}

object UserRegistry {
  // actor protocol
  sealed trait Command
  final case class GetUsers(replyTo: ActorRef[Users]) extends Command
  final case class CreateUser(user: User, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetUser(name: String, replyTo: ActorRef[GetUserResponse]) extends Command
  final case class DeleteUser(name: String, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class UpdateUser(name: String, user: User, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class Users(users: Seq[User])
  final case class GetUserResponse(maybeUser: Option[User])
  final case class ActionPerformed(description: String)
}