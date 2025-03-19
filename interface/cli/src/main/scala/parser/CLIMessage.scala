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

package parser

enum CLIMessage(val message: String):
  case CommandNotFound extends CLIMessage("command not found: type -help to get the valid commands list.")
  case TooManyArguments extends CLIMessage("too many arguments: type -help to get the valid commands list.")
  case CameraNotFound extends CLIMessage("camera not found: try -showCameras to get an updated list of active cameras.")
  case NoCommandTyped extends CLIMessage("no command typed: use -help to get the valid commands list")
  case CantSubscribeToIdleCamera extends CLIMessage("error: can't subscribe to idle camera.")
  case InputError extends CLIMessage("before sending input use command -subscribe <cameraName> to subscribe to a camera.")
  case InsertCommand extends CLIMessage("insert command: ")
  case OutputPrefix extends CLIMessage("output received: ")

object CLIMessage:
  def getCameraSubscribingMessage(cameraName: String) = "subscribed to camera " + cameraName + "."
  def getCameraConfigMessage(cameraName: String) = cameraName + " succesfully configured."
  def getHelpMessage: String = "Available commands:\n" + CLICommand.values.map(cmd => cmd.helpText).mkString("\n")