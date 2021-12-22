package ru.misis.model

import scala.collection.immutable

import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._

case class User(id: Int, name: String = "Null", age: Int = -1, countryOfResidence: String = "Null")


trait UserRepo {

  class UserTable(tag: Tag) extends Table[User](tag, "User") {
    val id = column[Int]("id", O.PrimaryKey)
    val name = column[String]("name")
    val age = column[Int]("age")
    val countryOfResidence = column[String]("countryOfResidence")
    def * = (
      id,
      name,
      age,
      countryOfResidence
    ) <> ((User.apply _).tupled, User.unapply)
  }

  val userTable = TableQuery[UserTable]
}