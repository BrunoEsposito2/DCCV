package actor

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import message.{CameraMap, ConfigServiceSuccess, InputServiceSuccess, Message, Ping, Subscribe, SubscribeServiceFailure, SubscribeServiceSuccess, SwitchToCamera, Unsubscribe}
import routing.VertxRouter

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}

object Server:
  def apply(): Server = new Server()

private class Server extends AbstractService:
  private val vertxRouter = VertxRouter()
  private var replyToRef: Option[ActorRef[Message]] = None

  // Inizializziamo il router HTTP appena viene creato il Server
  vertxRouter.initRoutes()

  override def onMessage(msg: Message): Unit =
    msg match {
      case CameraMap(replyTo, map) =>
        replyToRef = Some(map.head._1.self)

      case SwitchToCamera(cameraRef) =>
        replyToRef.get ! SwitchToCamera(cameraRef)

      case SubscribeServiceSuccess(cameraInfo) =>
        vertxRouter.updateServiceStatus("subscribe", "success")

      case SubscribeServiceFailure(info, cause) =>
        vertxRouter.updateServiceStatus("subscribe", "failure")

      case InputServiceSuccess(cameraInfo) =>
        vertxRouter.updateServiceStatus("input", "success")

      case ConfigServiceSuccess(author) =>
        vertxRouter.updateServiceStatus("config", "success")
    }