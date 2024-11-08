package se.yankov.zioapp
package api

import zio.*

import api.item.*
import domain.*
import domain.events.EventError
import domain.item.*

final case class PrivateApiHandler(itemService: ItemService):

  def createItem(input: CreateItemInput[ValidationStatus.NonValidated.type])
      : IO[RepositoryError.DbEx | RepositoryError.Conflict | RepositoryError.ConversionError | EventError | NonEmptyChunk[ItemValidationError], ItemResult] =
    for {
      validatedInput <- ZIO.fromEither(ItemValidator.validate(input))
      item           <- itemService.addItem(validatedInput)
    } yield ItemResult.fromDomain(item)

  def updateItem(id: ItemId, input: UpdateItemInput[ValidationStatus.NonValidated.type])
      : IO[
        RequestError | RepositoryError.DbEx | RepositoryError.MissingEntity | RepositoryError.ConversionError | NonEmptyChunk[ItemValidationError],
        ItemResult,
      ] =
    for {
      validatedInput <- ZIO.fromEither(ItemValidator.validate(input))
      item           <- itemService.updateItem(id, validatedInput)
    } yield ItemResult.fromDomain(item)

  def deleteItem(id: ItemId)
      : IO[RequestError | RepositoryError.DbEx | RepositoryError.MissingEntity, Unit] =
    itemService.deleteItem(id)

  def getItem(id: ItemId)
      : IO[RepositoryError.DbEx | RepositoryError.MissingEntity | RepositoryError.ConversionError | RequestError, ItemResult] =
    itemService.getItemById(id).map(ItemResult.fromDomain)

object PrivateApiHandler:
  val layer: RLayer[ItemService, PrivateApiHandler] = ZLayer.derive[PrivateApiHandler]
