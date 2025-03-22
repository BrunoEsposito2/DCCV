package actor

import akka.actor.typed.ActorRef
import message.{CameraMap, Config, ConfigServiceSuccess, InputServiceSuccess, Message, SubscribeServiceFailure, SubscribeServiceSuccess}
import router.VertxRouter
import utils.Info

import util.ForwardConfigData

import scala.collection.immutable.Queue

object GUIBackEnd:
  def apply(): GUIBackEnd = new GUIBackEnd()

private class GUIBackEnd extends GenericClient:
  private val vertxRouter = VertxRouter()

  // Inizializziamo il router HTTP appena viene creato il Server
  vertxRouter.initRoutes()

  override def onMessage(msg: Message, clientInfo: Info): Unit =
    msg match {
      case CameraMap(replyTo, map) =>
        vertxRouter.setServerRef(clientInfo.self)
        vertxRouter.updateCameraMap(map, clientInfo)

      case SubscribeServiceSuccess(cameraInfo) =>
        vertxRouter.updateServiceStatus("subscribe", "success")

      case SubscribeServiceFailure(info, cause) =>
        vertxRouter.updateServiceStatus("subscribe", "failure")

      case InputServiceSuccess(cameraInfo) =>
        vertxRouter.updateServiceStatus("input", "success")

      case ConfigServiceSuccess(author) =>
        vertxRouter.updateServiceStatus("config", "success")

      case ForwardConfigData(cameraRef, data) =>
        val command: String = "\"/workspace/domain/build/release/domain/bin/domain\" -v=\"/workspace/domain/video/video.avi\" -id=3"
        val windowData: String = "--x=" + data.get("startX") +
          " --y=" + data.get("startY") +
          " --width=" + data.get("width") +
          " --height=" + data.get("height")
        cameraRef ! Config(clientInfo.self, Queue(command, windowData))
    }