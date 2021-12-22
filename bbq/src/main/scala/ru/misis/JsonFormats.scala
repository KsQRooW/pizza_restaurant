package ru.misis

import ru.misis.model.{Item, Order, OrderStatus, User}
import ru.misis.registry.ItemRegistry.Items
import ru.misis.registry.MenuRegistry.{MenuDto, MenusDto}
import ru.misis.registry.OrderRegistry.{OrderDto, OrdersDto}
import ru.misis.registry.UserRegistry.Users
import ru.misis.registry.{ItemRegistry, MenuRegistry, OrderRegistry, UserRegistry}

//#json-formats
import spray.json.DefaultJsonProtocol

object JsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._


  //  implicit val menusJsonFormat = jsonFormat1(MenuDto)
  //  implicit val menusJsonFormat = jsonFormat1(MenusDto)

  implicit val userJsonFormat = jsonFormat4(User)
  implicit val usersJsonFormat = jsonFormat1(Users)
  implicit val menusJsonFormat = jsonFormat1(MenusDto)
  implicit val itemJsonFormat = jsonFormat3(Item)
  implicit val itemsJsonFormat = jsonFormat1(Items)
  implicit val menuJsonFormat = jsonFormat3(MenuDto)
  implicit val orderDtoJsonFormat = jsonFormat4(OrderDto)
  implicit val ordersDtoJsonFormat = jsonFormat1(OrdersDto)
  implicit val orderJsonFormat = jsonFormat4(Order)
  implicit val orderStatusJsonFormat = jsonFormat2(OrderStatus)

  implicit val actionPerformedJsonFormat = jsonFormat1(UserRegistry.ActionPerformed)
  implicit val actionPerformedJsonFormat2 = jsonFormat1(MenuRegistry.ActionPerformed)
  implicit val actionPerformedJsonFormat3 = jsonFormat1(ItemRegistry.ActionPerformed)
  implicit val actionPerformedJsonFormat4 = jsonFormat1(OrderRegistry.ActionPerformed)
}
//#json-formats
