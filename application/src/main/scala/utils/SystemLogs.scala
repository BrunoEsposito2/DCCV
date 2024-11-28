/*
 * Distributed Cluster for Computer Vision
 * Copyright (C) 2024 Andrea Ingargiola, Bruno Esposito
 * andrea.ingargiola@studio.unibo.it
 * bruno.esposito@studio.unibo.it
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package utils

enum InputServiceErrors(val log: String):
  case MissingChild extends InputServiceErrors("Child process undefined: operation aborted for potential unwanted side effects.")
  case MissingStdin extends InputServiceErrors("Child process stdin still undefined, please retry in a few moments.")
  case MissingConnection extends InputServiceErrors("The launched process was unable to connect to the socket")
  
enum StandardChildProcessCommands(val command: String):
  case Kill extends StandardChildProcessCommands("k")
  
enum ChildStatuses(val status: String):
  case Running extends ChildStatuses("Running")
  case Idle extends ChildStatuses("Idle")
  
enum ActorTypes(val actorType:String):
  case CameraManager extends ActorTypes("CameraManager")
  case Client extends ActorTypes("Client")
  case Utility extends ActorTypes("Utility")
  case Undefined extends ActorTypes("Undefined")
  
  
