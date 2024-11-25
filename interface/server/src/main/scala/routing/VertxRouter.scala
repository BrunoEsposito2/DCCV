package routing

import io.vertx.core.{Future, Vertx}
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.{BodyHandler, CorsHandler}
import io.vertx.core.http.{HttpMethod, HttpServer}

import java.util
import java.util.HashSet

object VertxRouter:
  def apply(): VertxRouter = new VertxRouter()

private class VertxRouter:
  private val vertx = Vertx.vertx()
  private val httpServer = vertx.createHttpServer()
  private var currentCameraId: String = "camera1" // Default camera ID
  private var subscribeStatus: String = "pending"
  private var inputStatus: String = "pending"
  private var configStatus: String = "pending"

  def initRoutes(): Future[HttpServer] =
    val router = Router.router(vertx)

    // Configurazione CORS
    val allowedHeaders = new util.HashSet[String]()
    allowedHeaders.add("Access-Control-Allow-Headers")
    allowedHeaders.add("Access-Control-Allow-Origin")
    allowedHeaders.add("Origin")
    allowedHeaders.add("X-Requested-With")
    allowedHeaders.add("Content-Type")
    allowedHeaders.add("Accept")

    val allowedMethods = new util.HashSet[HttpMethod]()
    allowedMethods.add(HttpMethod.GET)
    allowedMethods.add(HttpMethod.POST)
    allowedMethods.add(HttpMethod.OPTIONS)

    router.route().handler(
      CorsHandler.create()
        .addOrigin("*")
        .allowedHeaders(allowedHeaders)
        .allowedMethods(allowedMethods)
    )

    router.route().handler(BodyHandler.create())

    // Pre-flight OPTIONS request
    router.options().handler(ctx => {
      ctx.response()
        .putHeader("Access-Control-Allow-Origin", "*")
        .putHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS")
        .putHeader("Access-Control-Allow-Headers", "Content-Type")
        .end()
    })

    // Endpoint per il cambio camera
    router.post("/camera/switch").handler(ctx => {
      try {
        val body = ctx.body().asJsonObject()
        currentCameraId = body.getString("cameraId")

        ctx.response()
          .putHeader("Content-Type", "application/json")
          .putHeader("Access-Control-Allow-Origin", "*")
          .end(new JsonObject()
            .put("cameraId", currentCameraId)
            .encode())
      } catch {
        case e: Exception =>
          ctx.response()
            .putHeader("Content-Type", "application/json")
            .putHeader("Access-Control-Allow-Origin", "*")
            .setStatusCode(500)
            .end(new JsonObject()
              .put("error", e.getMessage)
              .encode())
      }
    })

    // Endpoint per le notifiche di stato
    router.get("/status").handler(ctx => {
      try {
        ctx.response()
          .putHeader("Content-Type", "application/json")
          .putHeader("Access-Control-Allow-Origin", "*")
          .end(new JsonObject()
            .put("subscribe", subscribeStatus)
            .put("input", inputStatus)
            .put("config", configStatus)
            .encode())
      } catch {
        case e: Exception =>
          ctx.response()
            .putHeader("Content-Type", "application/json")
            .putHeader("Access-Control-Allow-Origin", "*")
            .setStatusCode(500)
            .end(new JsonObject()
              .put("error", e.getMessage)
              .encode())
      }
    })

    // Avvia il server
    httpServer.requestHandler(router).listen(4000).onComplete(result => {
      if (result.succeeded()) {
        println(s"HTTP server running on port 4000")
      } else {
        println(s"Failed to start server: ${result.cause().getMessage}")
      }
    })

  def getCurrentCameraId: String = currentCameraId

  def updateServiceStatus(service: String, status: String): Unit =
    service match
      case "subscribe" => subscribeStatus = status
      case "input" => inputStatus = status
      case "config" => configStatus = status