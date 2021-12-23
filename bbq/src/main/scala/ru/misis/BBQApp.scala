package ru.misis

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import ru.misis.registry.{ItemRegistry, MenuRegistry, OrderRegistry, UserRegistry}
import ru.misis.routes.{ItemRoutes, MenuRoutes, OrderRoutes, UserRoutes}
import akka.http.scaladsl.server.Directives._
import ru.misis.services.{InitDB, ItemServiceImpl, MenuServiceImpl, OrderServiceImpl, UserServiceImpl}
import slick.jdbc.PostgresProfile.api._

import scala.util.Failure
import scala.util.Success

//#main-class
object BBQApp {
    //#start-http-server
    private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
        // Akka HTTP still needs a classic ActorSystem to start
        import system.executionContext

        val futureBinding = Http().newServerAt("localhost", 8080).bind(routes)
        futureBinding.onComplete {
            case Success(binding) =>
                val address = binding.localAddress
                system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
            case Failure(ex) =>
                system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
                system.terminate()
        }
    }

    lazy val db = Database.forConfig("database.postgres")

    //#start-http-server
    def main(args: Array[String]): Unit = {
        //#server-bootstrapping
        val rootBehavior = Behaviors.setup[Nothing] { context =>

            implicit val contextImpl = context.system
            implicit val executionContext = contextImpl.executionContext

            trait init {
                def db = BBQApp.db
                implicit val executionContext = contextImpl.executionContext
            }

            new InitDB(db).prepareRepository()

            val itemRegistry = new ItemRegistry() with ItemServiceImpl with init
            val itemRegistryActor = context.spawn(itemRegistry(), "ItemRegistryActor")
            context.watch(itemRegistryActor)

            val menuRegistry = new MenuRegistry() with MenuServiceImpl with init
            val menuRegistryActor = context.spawn(menuRegistry(), "MenuRegistryActor")
            context.watch(menuRegistryActor)

            val orderRegistry = new OrderRegistry() with OrderServiceImpl with init
            val orderRegistryActor = context.spawn(orderRegistry(), "OrderRegistryActor")
            context.watch(orderRegistryActor)

            val userRegistry = new UserRegistry() with UserServiceImpl with init
            val userRegistryActor = context.spawn(userRegistry(), "UserRegistryActor")
            context.watch(userRegistryActor)

            val userRoutes = new UserRoutes(userRegistryActor)
            val menuRoutes = new MenuRoutes(menuRegistryActor)
            val itemRoutes = new ItemRoutes(itemRegistryActor)
            val orderRoutes = new OrderRoutes(orderRegistryActor)
            startHttpServer(userRoutes.routes ~ itemRoutes.routes ~ menuRoutes.routes ~ orderRoutes.routes)(context.system)

            Behaviors.empty
        }
        val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
        //#server-bootstrapping
    }
}

//#main-class
