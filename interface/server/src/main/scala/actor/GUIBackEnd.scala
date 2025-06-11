package actor

import akka.actor.typed.ActorRef
import akka.util.ByteString
import message.{CameraMap, Config, ConfigServiceSuccess, InputServiceFailure, InputServiceSuccess, Message, SubscribeServiceFailure, SubscribeServiceSuccess}
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

  override def startingSinkFunction(): ByteString => Unit =
    bs => {
      val data = bs.utf8String.strip()
      println("SINK DATA: " + data)
      
      // Processa i dati nel formato "count:mode:fps"
      val parts = data.split(":")
      if (parts.length == 3) {
        try {
          val count = parts(0).toInt
          val mode = parts(1)
          val fps = parts(2).toDouble
          
          // Aggiorna il VertxRouter con questi dati
          vertxRouter.updateDetectionData(count, mode, fps)

        } catch {
          case e: Exception =>
            println(s"Errore nel processare i dati: $data - ${e.getMessage}")
        }
      } else {
        // Se non è nel formato atteso, passa il dato originale
        println(s"Dato ricevuto: $data")
      }
    }

  override def onMessage(msg: Message, clientInfo: Info): Unit =
    msg match {
      case CameraMap(replyTo, map) =>
        println("CameraMap message received")
        vertxRouter.setServerRef(clientInfo.self)
        vertxRouter.updateCameraMap(map, clientInfo)

      case SubscribeServiceSuccess(cameraInfo) =>
        vertxRouter.updateServiceStatus("subscribe", "success")

      case SubscribeServiceFailure(info, cause) =>
        vertxRouter.updateServiceStatus("subscribe", "failure")

      case InputServiceSuccess(cameraInfo) =>
        vertxRouter.updateServiceStatus("input", "success")
      
      case InputServiceFailure(cause) =>
        println("Error catched: " + cause)
        vertxRouter.updateServiceStatus("input", "failure")

      case ConfigServiceSuccess(author) =>
        vertxRouter.updateServiceStatus("config", "success")

      case ForwardConfigData(cameraRef, data, cameraId) =>
        val pattern = """camera(\d+)""".r

        // Sostituzione nella stringa
        val id = pattern.findFirstMatchIn(cameraId) match {
          case Some(m) => 
            val number = m.group(1) // Estrae il gruppo catturato (i numeri)
            cameraId.replace(s"camera$number", s"$number")
          case None => "0" // Se non trova corrispondenze, restituisce la stringa originale
        }

        val command: String = "/DCCV/domain/build/release/domain/bin/domain -v=/DCCV/domain/video/video.avi -id=" + id
  
        val fullCommand = if (data.isEmpty) {
          command
        } else {
          command + " --x=" + data.get("startX").get +
            " --y=" + data.get("startY").get +
            " --width=" + data.get("width").get +
            " --height=" + data.get("height").get
        }
        cameraRef ! Config(clientInfo.self, Queue(fullCommand))

      case _ => println("Unknown message received")
    }