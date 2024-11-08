package se.yankov.zioapp

import zio.*
import zio.http.*
import zio.http.netty.NettyConfig
import zio.logging.backend.SLF4J

import api.*
import implementation.postgres.Migration

object ZioApp extends ZIOAppDefault:

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  private def publicApiProgram(port: Int): RIO[PublicApiHandler, Nothing] =
    (Server.install(PublicApi.api) *>
      ZIO.logDebug(s"Public API server started on port $port") *>
      ZIO.never)
      .provideSomeLayer(
        ZLayer.succeed(Server.Config.default.port(port)) ++
          ZLayer.succeed(NettyConfig.default.leakDetection(NettyConfig.LeakDetectionLevel.PARANOID)) >>>
          Server.customized
      )

  private def privateApiProgram(port: Int): RIO[PrivateApiHandler, Nothing] =
    (Server.install(PrivateApi.api) *>
      ZIO.logDebug(s"Private API server started on port $port") *>
      ZIO.never)
      .provideSomeLayer(
        ZLayer.succeed(Server.Config.default.port(port)) ++
          ZLayer.succeed(NettyConfig.default.leakDetection(NettyConfig.LeakDetectionLevel.PARANOID)) >>>
          Server.customized
      )

  private def internalApiProgram(port: Int): RIO[InternalApiHandler, Nothing] =
    (Server.install(InternalApi.api) *>
      ZIO.logDebug(s"Internal API server started on port $port") *>
      ZIO.never)
      .provideSomeLayer(
        ZLayer.succeed(Server.Config.default.port(port)) ++
          ZLayer.succeed(NettyConfig.default.leakDetection(NettyConfig.LeakDetectionLevel.PARANOID)) >>>
          Server.customized
      )

  override val run: UIO[ExitCode] =
    (Migration.run *> ZIO.collectAllPar(
      publicApiProgram(1337) :: privateApiProgram(1338) :: internalApiProgram(1339) :: Nil
    ))
      .provide(
        AppConfig.layer >+>
          (implementation.layer >+> domain.layer >>> (PublicApiHandler.layer ++ PrivateApiHandler.layer ++ InternalApiHandler.layer))
      )
      .foldCauseZIO(
        error => ZIO.logError(s"Program failed: ${error.squash.getMessage}").exitCode,
        _ => ZIO.succeed(ExitCode.success),
      )
