package se.yankov.zioapp

import zio.*
import zio.config.ConfigErrorOps
import zio.config.magnolia.deriveConfig
import zio.config.typesafe.TypesafeConfigProvider

import implementation.kafka.KafkaConfig
import implementation.postgres.DbConfig

type ConfigEnv = DbConfig & KafkaConfig & HttpConfig

final case class AppConfig(db: DbConfig, kafka: KafkaConfig, http: HttpConfig)

object AppConfig:
  val port: URIO[HttpConfig, Int] = ZIO.serviceWithZIO[HttpConfig](conf => ZIO.succeed(conf.port))

  val layer: TaskLayer[ConfigEnv] =
    val configLayer = ZLayer(TypesafeConfigProvider.fromResourcePath().kebabCase.load(deriveConfig[AppConfig]))
      .mapError(e => new RuntimeException(e.prettyPrint()))
    configLayer.project(_.kafka) ++ configLayer.project(_.db) ++ configLayer.project(_.http)
