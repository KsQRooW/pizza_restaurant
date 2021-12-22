package ru.misis.services

import ru.misis.model.{ItemRepo, Menu, MenuItem, MenuRepo}
import ru.misis.registry.MenuRegistry.{MenuDto, MenusDto}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

trait MenuService {
    def getMenus(): Future[MenusDto]
    def createMenu(menuDto: MenuDto): Future[Unit]
    def getMenu(name: String): Future[Option[MenuDto]]
    def deleteMenu(name: String): Future[Unit]
}

trait MenuServiceImpl extends MenuService with MenuRepo with ItemRepo{
    def db: Database
    implicit def executionContext: ExecutionContext

    override def getMenus(): Future[MenusDto] = {
        db.run{
            menuTable.result.map(menus => MenusDto(menus.map(_.name)))
        }
    }

    override def createMenu(menu: MenuDto): Future[Unit] = {
        db.run {
            DBIO.seq (
                (itemTable ++= menu.items),
                (menuTable += Menu(menu.id, menu.name)),
                (menuItemTable ++= menu.items.map(item => MenuItem(
                    menuId = menu.id,
                    itemId = item.id,
                    price = item.price)))
            ).transactionally
        }
    }

    override def getMenu(name: String): Future[Option[MenuDto]] = {
        db.run {
            menuTable
                .join(menuItemTable).on{ case (menu, itemMenu) => menu.id === itemMenu.menuId }
                .join(itemTable).on{ case ((menu, itemMenu), item) => item.id === itemMenu.itemId}
                .filter{ case ((menu, _), _) => menu.name === name}
                .map{ case ((menu, itemMenu), item) => (menu, item) }
                .result
        }.map(_
            .groupBy(_._1)
            .map{case (menu, seq) => MenuDto(menu.id, menu.name, seq.map(_._2).toSeq)}
            .headOption)
    }

    override def deleteMenu(name: String): Future[Unit] = ???
}