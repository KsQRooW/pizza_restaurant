package ru.misis

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.concurrent.ScalaFutures
import ru.misis.model.Item
import ru.misis.registry.ItemRegistry
import ru.misis.routes.ItemRoutes
import ru.misis.services.{InitDB, ItemServiceImpl}
import slick.jdbc.PostgresProfile.api._

//#set-up
class ItemRoutesSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {
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

  val itemRegistry = new ItemRegistry() with ItemServiceImpl with init
  val itemRegistryActor = testKit.spawn(itemRegistry(), "ItemRegistryActor")
  val itemRoutes = new ItemRoutes(itemRegistryActor).routes


  "ItemRoutes" should {
    "return no users if no present (GET /items)" in {
      // note that there's no need for the host part in the uri:
      val request = HttpRequest(uri = "/items")

      request ~> itemRoutes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"items":[]}""")
      }
    }


    "be able to add items (POST /items)" in {
      val item = Item(1, "eggs", 100)

      // using the RequestBuilding DSL:
      val entity = Marshal(item).to[MessageEntity].futureValue
      val request = Post("/items").withEntity(entity)

      request ~> itemRoutes ~> check {
        status should ===(StatusCodes.Created)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and we know what message we're expecting back:
        entityAs[String] should ===("""{"description":"Item eggs created."}""")
      }
    }

    "возвращает список items в количестве 1 шт после добавления (GET /items)" in {
      val request = HttpRequest(uri = "/items")

      request ~> itemRoutes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"items":[{"id":1,"name":"eggs","price":100.0}]}""")
      }
    }

    "возвращает добавленный item (GET /item)" in {
      val request = HttpRequest(uri = "/item/eggs")

      request ~> itemRoutes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"id":1,"name":"eggs","price":100.0}""")
      }
    }
  }
}