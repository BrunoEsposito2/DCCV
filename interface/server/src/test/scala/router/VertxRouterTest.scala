package router

import actor.Server
import org.scalatest.flatspec.AnyFlatSpec
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.javadsl.Behaviors
import io.vertx.core.{Future, Vertx}
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import message.Message
import utils.ActorTypes.CameraManager
import utils.{ChildStatuses, Info}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Promise}

class VertxRouterTest extends AnyFlatSpec:

  "The updateCameraMap method" should "basically initialize the map" in testInitCameraMap()
  "The updateCameraMap method" should "reduce the map dimension" in testReduceDimensionCameraMap()
  "The updateCameraMap method" should "correctly update the map" in testUpdateCameraMap()

  "The setCurrentCamera method called by updateCameraMap" should "correctly update the camera id" in {
    testSetCurrentCameraNonEmpty()
    testSetCurrentCameraEmpty()
  }

  "A router object" should "manage client requests" in {
    testRouterWindowClientRequests()
    testRouterStatusClientRequests()
    testRouterCameraSwitchClientRequests()
  }

  private final val PRINT_DEBUG: Boolean = true

  val testKit: ActorTestKit = ActorTestKit()

  def testInitCameraMap(): Unit =
    val testKit: ActorTestKit = ActorTestKit()
    val ref1 = testKit.spawn(Behaviors.empty)
    val ref2 = testKit.spawn(Behaviors.empty)
    val ref3 = testKit.spawn(Behaviors.empty)

    var map: Map[Info, ChildStatuses] = Map.empty
    map += (Info(ref1, Set.empty, CameraManager) -> ChildStatuses.Idle)
    map += (Info(ref2, Set.empty, CameraManager) -> ChildStatuses.Idle)
    map += (Info(ref3, Set.empty, CameraManager) -> ChildStatuses.Idle)

    val router = VertxRouter()
    router.updateCameraMap(map, Info(linkedActors = Set(ref1)))

    map.foreach { case (info, _) =>
      assert(router.getCameraMap.values.exists(ref => ref.equals(info.self)))
    }

  def testReduceDimensionCameraMap(): Unit =
    val testKit: ActorTestKit = ActorTestKit()
    val ref1 = testKit.spawn(Behaviors.empty)
    val ref2 = testKit.spawn(Behaviors.empty)
    val ref3 = testKit.spawn(Behaviors.empty)

    var map: Map[Info, ChildStatuses] = Map.empty
    map += (Info(ref1, Set.empty, CameraManager) -> ChildStatuses.Idle)
    map += (Info(ref2, Set.empty, CameraManager) -> ChildStatuses.Idle)
    map += (Info(ref3, Set.empty, CameraManager) -> ChildStatuses.Idle)

    val router = VertxRouter()
    router.updateCameraMap(map, Info(linkedActors = Set(ref1)))

    // controllo se l'inserimento normale funziona
    map.foreach { case (info, _) =>
      assert(router.getCameraMap.values.exists(ref => ref.equals(info.self)))
    }

    map = map.tail
    router.updateCameraMap(map, Info(linkedActors = Set(ref1)))

    // controllo se la riduzione di dimensione funziona
    map.foreach { case (info, _) =>
      assert(router.getCameraMap.values.exists(ref => ref.equals(info.self)))
    }
    assert(router.getCameraMap.size == map.size)


  def testUpdateCameraMap(): Unit =
    val testKit: ActorTestKit = ActorTestKit()
    val ref1 = testKit.spawn(Behaviors.empty)
    val ref2 = testKit.spawn(Behaviors.empty)
    val ref3 = testKit.spawn(Behaviors.empty)

    var map: Map[Info, ChildStatuses] = Map.empty
    map += (Info(ref1, Set.empty, CameraManager) -> ChildStatuses.Idle)
    map += (Info(ref2, Set.empty, CameraManager) -> ChildStatuses.Idle)
    map += (Info(ref3, Set.empty, CameraManager) -> ChildStatuses.Idle)

    val router = VertxRouter()

    // inizializzo la map
    router.updateCameraMap(map, Info(linkedActors = Set(ref1)))
    if (PRINT_DEBUG)
      println("Maps printing after initialization")
      printMaps(map, router.getCameraMap)

    // tolgo un elemento
    map = map.tail
    router.updateCameraMap(map, Info(linkedActors = Set(ref1)))
    if (PRINT_DEBUG)
      println("Maps printing after remove")
      printMaps(map, router.getCameraMap)

    // aggiungo due nuovi elementi
    val ref4 = testKit.spawn(Behaviors.empty)
    val ref5 = testKit.spawn(Behaviors.empty)
    map += (Info(ref4, Set.empty, CameraManager) -> ChildStatuses.Idle)
    map += (Info(ref5, Set.empty, CameraManager) -> ChildStatuses.Idle)
    router.updateCameraMap(map, Info(linkedActors = Set(ref4)))

    if (PRINT_DEBUG)
      println("Maps printing after update")
      printMaps(map, router.getCameraMap)

    // controllo se l'aggiornamento funziona
    map.foreach { case (info, _) =>
      assert(router.getCameraMap.values.exists(ref => ref.equals(info.self)))
    }
    assert(router.getCameraMap.size == map.size)


  /**
   * Tests the functionality of setting the current camera in the VertxRouter.
   * This test case verifies that the current camera ID is correctly updated
   * when the camera map is updated with different linked actors.
   *
   * The test creates a map of camera actors and updates the VertxRouter's
   * camera map twice, each time with a different linked actor. It then
   * asserts that the current camera ID is updated correctly after each update.
   *
   */
  def testSetCurrentCameraNonEmpty(): Unit =
    val testKit: ActorTestKit = ActorTestKit()
    val ref1 = testKit.spawn(Behaviors.empty)
    val ref2 = testKit.spawn(Behaviors.empty)
    val ref3 = testKit.spawn(Behaviors.empty)

    var map: Map[Info, ChildStatuses] = Map.empty
    map += (Info(ref1, Set.empty, CameraManager) -> ChildStatuses.Idle)
    map += (Info(ref2, Set.empty, CameraManager) -> ChildStatuses.Idle)
    map += (Info(ref3, Set.empty, CameraManager) -> ChildStatuses.Idle)

    val router = VertxRouter()

    router.updateCameraMap(map, Info(linkedActors = Set(ref1)))
    if (PRINT_DEBUG)
      println("Camera: " + router.getCurrentCameraId)
    assert(router.getCurrentCameraId == "camera1")

    router.updateCameraMap(map, Info(linkedActors = Set(ref2)))
    if (PRINT_DEBUG)
      println("Camera: " + router.getCurrentCameraId)
    assert(router.getCurrentCameraId == "camera2")

  def testSetCurrentCameraEmpty(): Unit =
    val testKit: ActorTestKit = ActorTestKit()
    var map: Map[Info, ChildStatuses] = Map.empty
    val router = VertxRouter()

    assert(router.getCurrentCameraId == "camera1")

    router.updateCameraMap(map, Info())
    if (PRINT_DEBUG)
      println("Camera: " + router.getCurrentCameraId)
    assert(router.getCurrentCameraId == "camera1")

    var ref1 = testKit.spawn(Behaviors.empty)

    router.updateCameraMap(map, Info(linkedActors = Set(ref1)))
    if (PRINT_DEBUG)
      println("Camera: " + router.getCurrentCameraId)
    assert(router.getCurrentCameraId == "camera1")

    ref1 = testKit.spawn(Behaviors.empty)
    map += (Info(ref1, Set.empty, CameraManager) -> ChildStatuses.Idle)

    router.updateCameraMap(map, Info())
    if (PRINT_DEBUG)
      println("Camera: " + router.getCurrentCameraId)
    assert(router.getCurrentCameraId == "camera1")

  def testRouterWindowClientRequests(): Unit =
    // creating the vertx router inside the server actor
    val serverRef = testKit.spawn(Server().create())

    val vertx = Vertx.vertx()
    val client = vertx.createHttpClient()
    val promise = Promise[Unit]()

    // Creating a window request json body
    val jsonBody = new JsonObject()
      .put("x", 0).put("y", 0)
      .put("width", 100).put("height", 100)

    // Starting a client to send the window request to the server
    client.request(HttpMethod.POST, 4000, "localhost", "/window")
      .compose(req =>
        req.putHeader("content-type", "application/json")
          .send(jsonBody.encode())
          .compose(resp => {
            if (resp.statusCode() == 200) { // vertx router successfully managed the request
              Future.succeededFuture(())
            } else {
              Future.failedFuture(s"Bad status code: ${resp.statusCode()}")
            }
          })
      ).onComplete { result =>
        promise.complete(
          if (result.succeeded()) {
            scala.util.Success(())
          } else {
            scala.util.Failure(result.cause())
          }
        )
        client.close()
        vertx.close()
      }
    Await.result(promise.future, FiniteDuration(10, "seconds"))
    testKit.stop(serverRef, FiniteDuration(7, "seconds"))

  def testRouterStatusClientRequests(): Unit =
    val subscribeStatus: String = "pending"
    val inputStatus: String = "pending"
    val configStatus: String = "pending"

    // creating the vertx router inside the server actor
    val serverRef = testKit.spawn(Server().create())

    val vertx = Vertx.vertx()
    val client = vertx.createHttpClient()
    val promise = Promise[Unit]()

    // Creating a status request json body
    val jsonBody = new JsonObject()
      .put("subscribe", subscribeStatus)
      .put("input", inputStatus)
      .put("config", configStatus)

    // Starting a client to send the window request to the server
    client.request(HttpMethod.GET, 4000, "localhost", "/status")
      .compose(req =>
        req.putHeader("content-type", "application/json")
          .send(jsonBody.encode())
          .compose(resp => {
            if (resp.statusCode() == 200) { // vertx router successfully managed the request
              Future.succeededFuture(())
            } else {
              Future.failedFuture(s"Bad status code: ${resp.statusCode()}")
            }
          })
      ).onComplete { result =>
        promise.complete(
          if (result.succeeded()) {
            scala.util.Success(())
          } else {
            scala.util.Failure(result.cause())
          }
        )
        client.close()
        vertx.close()
      }
    Await.result(promise.future, FiniteDuration(10, "seconds"))
    testKit.stop(serverRef, FiniteDuration(7, "seconds"))

  def testRouterCameraSwitchClientRequests(): Unit =
    // creating the vertx router inside the server actor
    val serverRef = testKit.spawn(Server().create())

    val vertx = Vertx.vertx()
    val client = vertx.createHttpClient()
    val promise = Promise[Unit]()

    // Creating a status request json body
    val jsonBody = new JsonObject()
      .put("cameraId", "camera1")

    // Starting a client to send the camera/switch request to the server
    client.request(HttpMethod.POST, 4000, "localhost", "/camera/switch")
      .compose(req =>
        req.putHeader("content-type", "application/json")
          .send(jsonBody.encode())
          .compose(resp => {
            if (resp.statusCode() == 200) { // vertx router successfully managed the request
              Future.succeededFuture(())
            } else {
              Future.failedFuture(s"Bad status code: ${resp.statusCode()}")
            }
          })
      ).onComplete { result =>
        promise.complete(
          if (result.succeeded()) {
            scala.util.Success(())
          } else {
            scala.util.Failure(result.cause())
          }
        )
        client.close()
        vertx.close()
      }
    Await.result(promise.future, FiniteDuration(10, "seconds"))
    testKit.stop(serverRef, FiniteDuration(7, "seconds"))

  private def printMaps(map: Map[Info, ChildStatuses], cameraMap: Map[String, ActorRef[Message]]): Unit =
    println("*** print of starting map refs ***")
    map.keys.foreach { info => println("ref: " + info.self + "\t type: " + info.actorType) }
    println("*** print of router map refs ***")
    cameraMap.foreach { case(name, ref) => println("name: " + name + "\t ref: " + ref) }

