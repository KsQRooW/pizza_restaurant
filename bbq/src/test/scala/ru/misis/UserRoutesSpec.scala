package ru.misis

//#user-routes-spec
//#test-top
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.misis.model.User
import ru.misis.registry.UserRegistry
import ru.misis.routes.UserRoutes
import ru.misis.services.{InitDB, UserServiceImpl}

import slick.jdbc.PostgresProfile.api._

//#set-up
class UserRoutesSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {
  self =>

  val server: EmbeddedPostgres = EmbeddedPostgres
    .builder()
    .setPort(5334)
    .start()


  lazy val testKit = ActorTestKit()

  implicit def typedSystem = testKit.system

  val db: Database = Database.forURL(url = server.getJdbcUrl("postgres", "postgres"),
    driver = "org.postgresql.Driver")

  trait init {
    def db = self.db

    implicit val executionContext = self.typedSystem.executionContext
  }


  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._

  val repo = new InitDB(db)

  repo.cleanRepository().flatMap(_ => repo.prepareRepository())

  val userRegistry = new UserRegistry() with UserServiceImpl with init
  val userRegistryActor = testKit.spawn(userRegistry(), "UserRegistryActor")
  val userRoutes = new UserRoutes(userRegistryActor).routes


  "UserRoutes" should {
    "return no users if no present (GET /users)" in {
      // note that there's no need for the host part in the uri:
      val request = HttpRequest(uri = "/users")

      request ~> userRoutes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"users":[]}""")
      }
    }


    "be able to add users (POST /users)" in {
      val user = User(1, "Kapi", 42, "jp")

      // using the RequestBuilding DSL:
      val entity = Marshal(user).to[MessageEntity].futureValue
      val request = Post("/users").withEntity(entity)

      request ~> userRoutes ~> check {
        status should ===(StatusCodes.Created)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and we know what message we're expecting back:
        entityAs[String] should ===("""{"description":"User Kapi created."}""")
      }
    }

    "возвращает список пользователей в количестве 1 шт после добавления (GET /users)" in {
      val request = HttpRequest(uri = "/users")

      request ~> userRoutes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"users":[{"age":42,"countryOfResidence":"jp","id":1,"name":"Kapi"}]}""")
      }
    }

    "возвращает добавленное меню (GET /user)" in {
      val request = HttpRequest(uri = "/user/Kapi")

      request ~> userRoutes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"age":42,"countryOfResidence":"jp","id":1,"name":"Kapi"}""")
      }
    }
/*
    "удаляет user (DELETE /user)" in {

      val request = Delete(uri = "/user/Kapi")

      request ~> userRoutes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"description":"User Kapi deleted."}""")
      }
    } */
  }
}