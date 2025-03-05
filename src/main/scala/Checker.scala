package upmc.akka.leader

import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.Map

case class Alive(id: Int)
case class Dead(id: Int)
case object CheckEverybody
case object IncrDead

class Checker(id: Int, musiciens: List[Terminal]) extends Actor {
  val heart = context.actorOf(Props(new Heart(id)), "heart")
  val aliveMusicians = Map[Int, Boolean]()
  val deadCount = Map[Int, Int]()

  // Initialize all musicians as alive
  musiciens.foreach(m => {
    aliveMusicians(m.id) = false
    deadCount(m.id) = 0
  })

  // Mark self as alive
  aliveMusicians(id) = true

  // Schedule regular checks
  context.system.scheduler.schedule(0.seconds, 2.seconds, self, CheckEverybody)
  context.system.scheduler.schedule(0.seconds, 3.seconds, self, IncrDead)

  def receive = {
    case Beating(musicianId) => {
      // Received heartbeat from a musician
      aliveMusicians(musicianId) = true
      deadCount(musicianId) = 0
    }

    case Alive(musicianId) => {
      // Mark musician as alive
      aliveMusicians(musicianId) = true
      deadCount(musicianId) = 0
      context.parent ! Alive(musicianId)
    }

    case CheckEverybody => {
      // Check all musicians and notify parent about alive ones
      musiciens.foreach(m => {
        if (aliveMusicians(m.id) && m.id != id) {
          // Send Alive message to the musician
          val musicianPath = s"akka.tcp://MozartSystem${m.id}@${m.ip}:${m.port}/user/Musicien${m.id}"
          try {
            val musicianRef = context.actorSelection(musicianPath)
            musicianRef ! Alive(id)
          } catch {
            case _: Exception => // Ignore if can't reach
          }
        }
      })
    }

    case IncrDead => {
      // Increment dead count for musicians that haven't sent heartbeats
      musiciens.foreach(m => {
        if (!aliveMusicians(m.id) && m.id != id) {
          deadCount(m.id) += 1
          if (deadCount(m.id) >= 3) {
            // If no heartbeat for 3 checks, consider dead
            context.parent ! Dead(m.id)
          }
        }
      })

      // Reset alive status for next round
      musiciens.foreach(m => {
        if (m.id != id) {
          aliveMusicians(m.id) = false
        }
      })
    }
  }
}

