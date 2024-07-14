package routing

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router

object VertxRouter:
  def apply(): VertxRouter = new VertxRouter()

private class VertxRouter:
  private val vertx = Vertx.vertx()
  private val httpServer = vertx.createHttpServer()

  def initRoutes() =
    val router = Router.router(vertx)

    router.get("/").handler(_.response()
      .putHeader("Access-Control-Allow-Origin", "*")
      .putHeader("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE")
      .end(JsonObject().put("response", "OK").encodePrettily()))