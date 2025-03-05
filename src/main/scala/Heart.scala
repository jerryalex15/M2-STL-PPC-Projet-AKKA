package upmc.akka.leader

import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

case class Beating(id: Int)
case object Check

class Heart(id: Int) extends Actor {
  // Send heartbeat every 1 second
  context.system.scheduler.schedule(0.seconds, 1.second, self, Check)

  def receive = {
    case Check => {
      // Send heartbeat to parent (Checker)
      context.parent ! Beating(id)
    }
  }
}

