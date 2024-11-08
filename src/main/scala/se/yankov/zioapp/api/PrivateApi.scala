package se.yankov.zioapp
package api

import zio.*
import zio.http.*
import zio.http.Middleware.customAuthZIO

import java.util.UUID

import se.yankov.zioapp.domain.ValidationStatus
import se.yankov.zioapp.domain.item.{ CreateItemInput, ItemId, UpdateItemInput }
import se.yankov.zioapp.implementation.auth.AuthService
import se.yankov.zioapp.implementation.json.ItemCodecs.given
import se.yankov.zioapp.implementation.json.given

object PrivateApi:

  val api: Routes[AuthService & PrivateApiHandler, Nothing] =
    Routes(
      Method.POST / "items"                             ->
        handler { (req: Request) =>
          req
            .parseRequest[CreateItemInput[ValidationStatus.NonValidated.type]]
            .flatMap(input => ZIO.serviceWithZIO[PrivateApiHandler](_.createItem(input)))
            .toJsonResponse
            .handleErrors
        },
      Method.GET / "items" / pathCodec[UUID, ItemId]    ->
        handler { (id: ItemId, _: Request) =>
          ZIO.serviceWithZIO[PrivateApiHandler](_.getItem(id)).toJsonResponse.handleErrors
        },
      Method.PUT / "items" / pathCodec[UUID, ItemId]    ->
        handler { (id: ItemId, req: Request) =>
          req
            .parseRequest[UpdateItemInput[ValidationStatus.NonValidated.type]]
            .flatMap(input => ZIO.serviceWithZIO[PrivateApiHandler](_.updateItem(id, input)))
            .toJsonResponse
            .handleErrors
        },
      Method.DELETE / "items" / pathCodec[UUID, ItemId] ->
        handler { (id: ItemId, _: Request) =>
          ZIO.serviceWithZIO[PrivateApiHandler](_.deleteItem(id)).toJsonResponse.handleErrors
        },
    ) @@ requireContentType @@ customAuthZIO[AuthService](res =>
      AuthService.validateJwtV2(res.authHeader.getOrElse(""))
    )

  extension (req: Request)
    def authHeader: Option[String] = req.headers(Header.Authorization).headOption.map(_.renderedValue)
