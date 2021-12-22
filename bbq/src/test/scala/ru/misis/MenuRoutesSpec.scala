package ru.misis

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.misis.model.Item
import ru.misis.registry.MenuRegistry
import ru.misis.registry.MenuRegistry.MenuDto
import ru.misis.routes.MenuRoutes
import ru.misis.services.{InitDB, MenuServiceImpl}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.duration.DurationInt

class MenuRoutesSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest { self =>

    val server: EmbeddedPostgres = EmbeddedPostgres
        .builder()
        .setPort(5334)
        .start()


    lazy val testKit = ActorTestKit()

    //implicit def default = RouteTestTimeout(30.seconds)

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

    val menuRegistry = new MenuRegistry() with MenuServiceImpl with init
    val menuRegistryActor = testKit.spawn(menuRegistry(), "MenuRegistryActor")
    val menuRoutes = new MenuRoutes(menuRegistryActor).routes

    "MenuRoutes" should {
        "возвращает пустой список меню (GET /menus)" in {
            val request = HttpRequest(uri = "/menus")

            request ~> menuRoutes ~> check {
                status should === (StatusCodes.OK)

                // we expect the response to be json:
                contentType should ===(ContentTypes.`application/json`)

                // and no entries should be in the list:
                entityAs[String] should ===("""{"names":[]}""")
            }
        }

        "добавляет новое меню (POST /menus)" in {
            val menu = MenuDto(1, "daily", Seq(
                Item(1, "eggs", 100),
                Item(2, "steak", 1000)
            ))

            val entity = Marshal(menu).to[MessageEntity].futureValue
            val request = Post("/menus").withEntity(entity)

            request ~> menuRoutes ~> check {
                status should === (StatusCodes.Created)

                // we expect the response to be json:
                contentType should ===(ContentTypes.`application/json`)

                // and no entries should be in the list:
                entityAs[String] should ===("""{"description":"Menu daily created."}""")
            }
        }

        "возвращает список меню из одного элемента после добавления (GET /menus)" in {
            val request = HttpRequest(uri = "/menus")

            request ~> menuRoutes ~> check {
                status should ===(StatusCodes.OK)

                // we expect the response to be json:
                contentType should ===(ContentTypes.`application/json`)

                // and no entries should be in the list:
                entityAs[String] should ===("""{"names":["daily"]}""")
            }
        }

        "возвращает добавленное меню (GET /menu)" in {
            val request = HttpRequest(uri = "/menu/daily")

            request ~> menuRoutes ~> check {
                status should === (StatusCodes.OK)

                // we expect the response to be json:
                contentType should ===(ContentTypes.`application/json`)

                // and no entries should be in the list:
                entityAs[String] should ===("""{"id":1,"items":[{"id":1,"name":"eggs","price":100.0},{"id":2,"name":"steak","price":1000.0}],"name":"daily"}""")
            }
        }

    }


}
