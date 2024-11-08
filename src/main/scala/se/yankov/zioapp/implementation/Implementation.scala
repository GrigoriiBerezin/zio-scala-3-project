package se.yankov.zioapp
package implementation

import se.yankov.zioapp.domain.events.EventPublisher
import se.yankov.zioapp.domain.item.ItemRepository
import se.yankov.zioapp.implementation.auth.AuthService
import se.yankov.zioapp.implementation.kafka.EventPublisherImplementation
import se.yankov.zioapp.implementation.postgres.*
import zio.RLayer

import javax.sql.DataSource

type ImplementationEnv = AuthService & ItemRepository & EventPublisher

val layer: RLayer[ConfigEnv, ImplementationEnv] =
  PostgresDataSource.layer >>> ItemRepositoryImplementation.layer ++ AuthService.layer ++ EventPublisherImplementation.layer
