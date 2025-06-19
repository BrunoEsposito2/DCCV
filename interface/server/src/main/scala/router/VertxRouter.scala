package router

import akka.actor.typed.ActorRef
import io.vertx.core.{Future, Vertx}
import io.vertx.core.json.JsonObject
import io.vertx.core.json.JsonArray
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.{BodyHandler, CorsHandler}
import io.vertx.core.http.{HttpMethod, HttpServer}
import message.{Message, SwitchToCamera}
import utils.{ActorTypes, ChildStatuses, Info}

import util.ForwardConfigData
import java.util

object VertxRouter:
  def apply(): VertxRouter = new VertxRouter()

private class VertxRouter:
  private val vertx = Vertx.vertx()
  private val httpServer = vertx.createHttpServer()
  private var currentCameraId: String = "camera1" // Default camera ID
  private var subscribeStatus: String = "pending"
  private var inputStatus: String = "pending"
  private var configStatus: String = "pending"
  private var cameraMap: Map[String, ActorRef[Message]] = Map.empty
  private var cameraNames: Map[String, String] = Map.empty
  private var serverRef: Option[ActorRef[Message]] = None
  private var windowData: Map[String, Double] = Map.empty

  private var detectedCount: Int = 0
  private var detectionMode: String = "Initializing..."
  private var frameRate: Double = 0.0

  def updateDetectionData(count: Int, mode: String, fps: Double): Unit = {
    detectedCount = count
    detectionMode = mode
    frameRate = fps
  }

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

        if (serverRef.nonEmpty)
          serverRef.get ! SwitchToCamera(cameraMap(currentCameraId))
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

    router.post("/window").handler(ctx => {
      val response = ctx.response()
        .putHeader("Content-Type", "application/json")
        .putHeader("Access-Control-Allow-Origin", "*")

      try {
        val body = ctx.body().asJsonObject()

        val requiredFields = List("x", "y", "width", "height")
        if (!requiredFields.forall(body.containsKey)) {
          windowData = Map.empty
        } else {
            val x = body.getInteger("x")
            val y = body.getInteger("y")
            val width = body.getInteger("width")
            val height = body.getInteger("height")

            if (x < 0 || y < 0 || width <= 0 || height <= 0) {
              throw new IllegalArgumentException("Invalid coordinates or dimensions")
            }

            windowData = Map(
              "startX" -> x.toDouble,
              "startY" -> y.toDouble,
              "width" -> width.toDouble,
              "height" -> height.toDouble
            )
        }
        
        response
          .end(new JsonObject()
            .put("status", "approved")
            .encode())
          .onComplete { handler =>
            handler.succeeded() match
              case _ =>
                if (serverRef.nonEmpty)
                  serverRef.get ! ForwardConfigData(cameraMap(currentCameraId), windowData, currentCameraId)
          }
      } catch {
        case e: Exception =>
          if (!response.ended()) {
            response.setStatusCode(500)
              .end(new JsonObject().put("error", e.getMessage).encode())
          }
      }
    })

    // Endpoint per le notifiche di stato
    router.get("/status").handler(ctx => {
      try {
        val camerasArray = new JsonArray()

        cameraMap.foreach { case (cameraId, _) =>
          val cameraObj = new JsonObject()
            .put("id", cameraId)
            .put("name", cameraNames.getOrElse(cameraId, getFriendlyNameForCamera(cameraId)))
            .put("location", getLocationForCamera(cameraId))

          camerasArray.add(cameraObj)
        }
        
        ctx.response()
          .putHeader("Content-Type", "application/json")
          .putHeader("Access-Control-Allow-Origin", "*")
          .end(new JsonObject()
            .put("subscribe", subscribeStatus)
            .put("input", inputStatus)
            .put("config", configStatus)
            .put("currentCamera", currentCameraId)
            .put("cameras", camerasArray)
            .put("peopleCount", detectedCount)  // campi aggiunti
            .put("mode", detectionMode)
            .put("fps", frameRate)
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
    httpServer.requestHandler(router).listen(4000, "0.0.0.0").onComplete(result => {
      if (result.succeeded()) {
        println(s"HTTP server running on port 4000")
      } else {
        println(s"Failed to start server: ${result.cause().getMessage}")
      }
    })

  private def getFriendlyNameForCamera(cameraId: String): String =
    cameraId match {
      case "camera1" => "Main Entrance"
      case "camera2" => "Parking Area"
      case "camera3" => "Living Room"
      case _ => s"Camera ${cameraId.substring(6)}"
    }

  private def getLocationForCamera(cameraId: String): String =
    cameraId match {
      case "camera1" => "Front"
      case "camera2" => "Exterior"
      case "camera3" => "Indoor"
      case _ => "General"
    }
  def setServerRef(ref: ActorRef[Message]): Unit = serverRef = Option(ref)
  def getCameraMap: Map[String, ActorRef[Message]] = cameraMap
  def getCurrentCameraId: String = currentCameraId

  def updateServiceStatus(service: String, status: String): Unit =
    service match
      case "subscribe" => subscribeStatus = status
      case "input" => inputStatus = status
      case "config" => configStatus = status

  // Aggiorna la Map di camere disponibili
  def updateCameraMap(cameras: Map[Info, ChildStatuses], info: Info): Unit =
    // Se la Map non è vuota allora il contenuto viene gestito opportunamento
    if (cameraMap.nonEmpty)
      enumerateMap(cameras)
      setCurrentCamera(info)
    else  // altrimenti, si aggiungono direttamente i nuovi elementi
      addNewCameras(cameras)
      setCurrentCamera(info)

  private def setCurrentCamera(info: Info): Unit =
    if (cameraMap.nonEmpty && info.linkedActors.nonEmpty)
      val camera = cameraMap.values.filter(_.equals(info.linkedActors.head))
      if (camera.nonEmpty)
        val currentCameraRef = camera.head
        currentCameraId = cameraMap.filter(_._2.equals(currentCameraRef)).head._1

  private def addNewCameras(cameras: Map[Info, ChildStatuses]): Unit =
    var n: Int = 1
    cameras.foreach { case (id, status) =>
      cameraMap += (("camera" + n) -> id.self)
      n += 1
    }

  // Gestisce l'inserimento delle camere disponibili a partire da una Map non vuota
  private def enumerateMap(cameras: Map[Info, ChildStatuses]): Unit =
    // Calcola il numero di camere da aggiungere
    if (cameras.size > cameraMap.size) {
      var diff: Int = cameras.size - cameraMap.size

      if (diff > 0)
        // Prendo tutte le camere nuove
        var camerasToInsert: List[ActorRef[Message]] = List.empty
        cameras.foreach { case (id, status) =>
          if (!cameraMap.values.exists(x => x.equals(id.self))) {
            camerasToInsert = id.self::camerasToInsert
          }
        }
        var count: Int = 1
        // Aggiungo le nuove camere alla Map
        while (diff > 0)
          // Se non esiste una camera con questo ID, la aggiungo alla Map
          if (!cameraMap.keys.exists(s => s.equals("camera"+count)))
            cameraMap += (("camera" + count) -> camerasToInsert.head)
            camerasToInsert = camerasToInsert.tail
            diff -= 1
          count += 1
    } else if (cameras.size < cameraMap.size) {
      // Se la Map è già piena, rimuove le camere non più presenti
      val updatedRefs = cameras.keySet.map(_.self)
      cameraMap = cameraMap.filter { case (_, actor) => updatedRefs.contains(actor) }
    }
