package ru.misis.services

import ru.misis.model.{User, UserRepo}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

trait UserService {
  def getUsers(): Future[Seq[User]]
  def createUser(user: User): Future[Int] //registry(users + user)
  def getUser(name: String): Future[Option[User]] //users.find(_.name == name)
  def deleteUser(name: String): Future[Int] //users.filterNot(_.name == name)
  def updateUser(name: String, user: User): Future[Int]
}


trait UserServiceImpl extends UserService with UserRepo{
  def db: Database
  implicit def executionContext: ExecutionContext

  override def getUsers(): Future[Seq[User]] = {
    db.run{ userTable.result }
  }

  override def createUser(user: User): Future[Int] = {
    db.run(userTable += user)
  }

  override def getUser(name: String): Future[Option[User]] = {
    db.run(
      userTable.filter(_.name === name).result.headOption
    )
  }

  override def deleteUser(name: String): Future[Int] = {
    db.run{
      userTable.filter(_.name === name).delete
    }
  }

  override def updateUser(name: String, user: User): Future[Int] = {
    db.run{
      userTable.filter(_.name === name)
        .map(user => (user.name, user.age, user.countryOfResidence))
        .update((user.name, user.age, user.countryOfResidence))
    }
  }

}