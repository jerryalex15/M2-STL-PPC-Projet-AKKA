package upmc.akka.leader

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, PoisonPill, Props}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import scala.util.Random

// === Messages échangés
case class PlayConductor ()
case class Exit()
case class Elected ()
case class Election ()
case class IsNeedElection ()
case class LearnCurrentConductorId(id:Int)

class MusicianActor (node : ActorRef, val id : Int) extends Actor {
  import DataBaseActor._

  // === Initialisation du Musicien ===

  // Variables pour les informations globales
  val database = context.actorOf(Props[DataBaseActor], "databaseActor")
  val provider = context.actorOf(Props(new ProviderActor(self)), "providerActor") // Le Provider fourni les mesures
  val player = context.actorOf(Props[PlayerActor], name = "playerActor")  // Le Player de ce Musicien
  var musicians = new Array[ActorSelection](4)    // Le tableau des adresses de tous les Musiciens distants
  var alives = new Array[Boolean](4)   // Le tableau de si les Musiciens de chaque index sont vivants
  var currentConductorId = -1     // Le chef d'orchestre actuel
  var nbOthersAlive = 0; // Nombre de noeuds en vie (hormis soi-même)

  var needElection = false
  var isScheduling = false // Mutex pour le scheduler

  val scheduler = context.system.scheduler
  val TIME_BASE = 1800 milliseconds
  val WAITING_TIME_BEFORE_EXIT = 30000 milliseconds

  var isAlone = true
  alives(id) = true

  for (i <- 0 to 3 if i != id) {
    try {
      musicians(i) = context.actorSelection("akka.tcp://LeaderSystem" + i + "@127.0.0.1:600" + i + "/user/node" + i + "/musicianActor" + i)
      musicians(i) ! Alive(id)
    } catch {
      case e: Throwable => println(s"[Error] Musician - Cannot contact musician $i: ${e.getMessage}")
    }
  }

  def receive: Receive = {
    case IsNeedElection =>
      for (i <- 0 to 3) {
        if (alives(i) && currentConductorId == -1) {
          needElection = true
        }
      }
      if (needElection) self ! Election

    case PlayConductor =>
      if (currentConductorId == id) {
        val diceRoll = Random.nextInt(6) + Random.nextInt(6) + 2
        provider ! GetMeasure (diceRoll)
      }

    case Measure (chordlist) =>
      if(currentConductorId == id) {
        if (nbOthersAlive > 0) {
          val aliveMusicians = (0 until 4).filter(i => i != id && alives(i)).toList
          val chosenId = aliveMusicians(Random.nextInt(aliveMusicians.length))
          println(s"Musician - Sending measure to musician $chosenId")

          // Mutex : Planifie PlayConductor seulement si rien n'est en cours
          if (!isScheduling) {
            isScheduling = true
            musicians(chosenId) ! Measure(chordlist)
            scheduler.scheduleOnce(TIME_BASE) {
              self ! PlayConductor
              isScheduling = false // Libère le verrou après l'exécution
            }
          }
        } else {
          println("Musician - No musicians available. Waiting...")
          isAlone = true
          scheduler.scheduleOnce(WAITING_TIME_BEFORE_EXIT, self, Exit)
        }
      } else {    // Si on est un simple Musicien, on a reçu cette Measure du chef d'orchestre
        println("Musician => Measure received, playing...")
        player ! Measure (chordlist)
      }

    case Alive(n) =>
      isAlone = false
      println(s"alives$n = " + alives(n))
      if (!alives(n)) {
        alives(n) = true
        nbOthersAlive += 1
        println(s"Musician - Musician $n is alive. Total: $nbOthersAlive")
        if (nbOthersAlive > 0 && currentConductorId == id){
          self ! PlayConductor
        }
      }
      if(currentConductorId != -1) musicians(n) ! LearnCurrentConductorId (currentConductorId)

    case Dead(n) =>
      if (alives(n)) {
        alives(n) = false
        nbOthersAlive -= 1
        println(s"Musician - Musician $n is dead. Total: $nbOthersAlive")
      }
      if (currentConductorId == n || currentConductorId == -1) self ! Election

    case Exit =>
      if (isAlone) {
        println(s"Musician - Musician $id is exiting (alone for too long).")
        context.system.terminate()
        System.exit(0)
      }

    case LearnCurrentConductorId(n) =>
      if (n != id && alives(n)) {
        currentConductorId = n
        println(s"Musician$id - Learning current conductor: $n")
      }

    case Election =>
      val aliveMusicians = (0 until 4).filter(alives).toList
      if (aliveMusicians.contains(id)) {
        self ! Elected
      } else if (aliveMusicians.nonEmpty) {
        currentConductorId = aliveMusicians.head
      }

    case Elected =>
      if (currentConductorId != id) {
        currentConductorId = id
        println(s"Musician - Musician $id has been elected as the new conductor.")
        self ! PlayConductor
      }
  }
}