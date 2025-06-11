package routerserver

import actor.GenericClient
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.flatspec.AnyFlatSpec
import akka.actor.typed.ActorRef
import message.{CameraMap, Config, Message, Pong}
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import util.ForwardConfigData
import io.vertx.core.{Future, Vertx}
import router.VertxRouter
import utils.ActorTypes.CameraManager
import utils.{ChildStatuses, Info}

import scala.collection.immutable.Queue
import scala.concurrent.{Await, Promise}
import scala.concurrent.duration.FiniteDuration

class RouterServerTest extends AnyFlatSpec:

  "A router object" should "send a ForwardConfigData message to the GUIBackEnd" in testRouterSentMessage()

  val testKit: ActorTestKit = ActorTestKit()

  def testRouterSentMessage(): Unit =
    val probe = testKit.createTestProbe[Message]()

    // Create a TestServer for testing the receive of messages
    object TestServer:
      def apply(): TestServer = new TestServer()

    class TestServer extends GenericClient:
      private val vertxRouter = VertxRouter()

      vertxRouter.initRoutes()

      override def onMessage(msg: Message, clientInfo: Info): Unit =
        msg match {
          case CameraMap(replyTo, map) => // receive message to test
            vertxRouter.setServerRef(probe.ref)
            vertxRouter.updateCameraMap(map, Info())
            replyTo ! Pong(Info()) // add reply only for test purposes

          case ForwardConfigData(cameraRef, data) => // receive message to test
            val command: String = "\"/workspace/domain/build/release/domain/bin/domain\" -v=\"/workspace/domain/video/video.avi\" -id=3"
            val windowData: String = "--x=" + data.get("startX") +
              " --y=" + data.get("startY") +
              " --width=" + data.get("width") +
              " --height=" + data.get("height")
            cameraRef ! Config(clientInfo.self, Queue(command, windowData))
        }

    val serverRef = testKit.spawn(TestServer().create())

    // Wait for test server startup
    Thread.sleep(1000)

    // initialize the server ref on vertx router via CameraMap message
    serverRef ! CameraMap(probe.ref, Map(Info(probe.ref, Set.empty, CameraManager) -> ChildStatuses.Idle))
    probe.expectMessageType[Pong](FiniteDuration(10, "seconds")) // test if the server received the CameraMap message

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
              // it means that it sent the ForwardConfigData to the server
              probe.expectMessageType[ForwardConfigData](FiniteDuration(15, "seconds"))
              Future.succeededFuture(())
            } else {
              // otherwise it didn't sent any message to the server
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

    Await.result(promise.future, FiniteDuration(20, "seconds"))
    testKit.stop(serverRef, FiniteDuration(7, "seconds"))

