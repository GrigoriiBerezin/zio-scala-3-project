package se.yankov.zioapp

import zio.*
import zio.http.*
import zio.http.codec.PathCodec.literal
import zio.http.netty.NettyConfig
import zio.logging.backend.SLF4J

import api.*
import implementation.postgres.Migration

import se.yankov.zioapp.implementation.auth.AuthService

object ZioApp extends ZIOAppDefault:

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  private val server
      : RIO[PublicApiHandler & PrivateApiHandler & InternalApiHandler & ConfigEnv & AuthService, Nothing] =
    (Routes
      .fromIterable(
        Chunk(
          literal("public") / PublicApi.api,
          literal("private") / PrivateApi.api,
          literal("internal") / InternalApi.api,
        ).map(_.routes).flatten
      )
      .serve <* AppConfig.port.flatMap(port => ZIO.logDebug(s"Server started on port $port")))
      .provideSomeLayer(
        ZLayer.fromZIO(AppConfig.port.map(Server.Config.default.port)) ++
          ZLayer.succeed(NettyConfig.default.leakDetection(NettyConfig.LeakDetectionLevel.PARANOID)) >>>
          Server.customized
      )

  override val run: UIO[ExitCode] =
    (Migration.run *> server)
      .provide(
        AuthService.layer ++ AppConfig.layer >+>
          (implementation.layer >+> domain.layer >>> (PublicApiHandler.layer ++ PrivateApiHandler.layer ++ InternalApiHandler.layer))
      )
      .foldCauseZIO(
        error => ZIO.logError(s"Program failed: ${error.squash.getMessage}").exitCode,
        _ => ZIO.succeed(ExitCode.success),
      )
