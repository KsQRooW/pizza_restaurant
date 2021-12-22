package ru.misis

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.misis.model.Order
import ru.misis.registry.OrderRegistry
import ru.misis.routes.OrderRoutes
import ru.misis.services.{InitDB, OrderServiceImpl}
import slick.jdbc.PostgresProfile.api._

class OrderRoutesSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest { self =>

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

  val orderRegistry = new OrderRegistry() with OrderServiceImpl with init
  val orderRegistryActor = testKit.spawn(orderRegistry(), "OrderRegistryActor")
  val orderRoutes = new OrderRoutes(orderRegistryActor).routes

  "OrderRoutes" should {
    "возвращает пустой список orders (GET /orders)" in {
      val request = HttpRequest(uri = "/orders")

      request ~> orderRoutes ~> check {
        status should === (StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"orders":[]}""")
      }
    }

    "добавляет новый заказ (POST /orders)" in {
      val order = Order(1, 1, 1, 1)

      val entity = Marshal(order).to[MessageEntity].futureValue
      val request = Post("/orders").withEntity(entity)

      request ~> orderRoutes ~> check {
        status should === (StatusCodes.Created)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"description":"Order 1 created."}""")
      }
    }

    "возвращает список orders из одного элемента после добавления (GET /orders)" in {
      val request = HttpRequest(uri = "/orders")

      request ~> orderRoutes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"orders":[1]}""")
      }
    }

    "возвращает добавленный order (GET /order)" in {
      val request = HttpRequest(uri = "/order/1")

      request ~> orderRoutes ~> check {
        status should === (StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"items":[{"id":1,"name":"Null","price":-1.0}],"name":"Null","orderId":1,"status":false}""")
      }
    }
  }
}
