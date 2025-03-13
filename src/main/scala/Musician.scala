package upmc.akka.leader

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, PoisonPill, Props}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import scala.util.Random

case class JoueChefOrchestre ()
case class SelfTerminate()
case class ChefOrchestreElu ()
case class CreeElection ()
case class CreationElection ()
case class GetNouveauChefOrchestre(id:Int)

class MusicianActor (node : ActorRef, val id : Int) extends Actor {
  import DataBaseActor._
  
  val database = context.actorOf(Props[DataBaseActor], "databaseActor")
  val provider = context.actorOf(Props(new ProviderActor(self)), "providerActor")
  val player = context.actorOf(Props[PlayerActor], name = "playerActor")
  
  var autresMusiciens = new Array[ActorSelection](4)
  var musiciensVivant = new Array[Boolean](4)
  var currentChefOrchestre = -1
  var nbAutresMusiciensVivant = 0;

  var besoinElection = false
  var estPrevu = false // Mutex pour le scheduler

  val scheduler = context.system.scheduler
  val tempsAttenteEntreNote = 1800 milliseconds
  val tempsAttenteAvantExit = 30000 milliseconds

  var estSeul = true
  musiciensVivant(id) = true

  for (i <- 0 to 3 if i != id) {
    try {
      autresMusiciens(i) = context.actorSelection("akka.tcp://LeaderSystem" + i + "@127.0.0.1:600" + i + "/user/node" + i + "/musicianActor" + i)
      autresMusiciens(i) ! Alive(id)
    } catch {
      case e: Throwable => println(s"[Error] {Musicien} Pas de communication avec le musicien $i: ${e.getMessage}")
    }
  }

  def receive: Receive = {
    case CreationElection =>
      for (i <- 0 to 3) {
        if (musiciensVivant(i) && currentChefOrchestre == -1) {
          besoinElection = true
        }
      }
      if (besoinElection) self ! CreeElection

    case JoueChefOrchestre =>
      if (currentChefOrchestre == id) {
        val diceRoll = Random.nextInt(6) + Random.nextInt(6) + 2
        provider ! GetMeasure (diceRoll)
      }

    case Measure (chordlist) =>
      if(currentChefOrchestre == id) {
        if (nbAutresMusiciensVivant > 0) {
          val aliveMusicians = (0 until 4).filter(i => i != id && musiciensVivant(i)).toList
          val chosenId = aliveMusicians(Random.nextInt(aliveMusicians.length))
          println(s"{Musicien} Envoi la mesure au musicien $chosenId")

          // JoueChefOrchestre seulement si rien n'est en cours
          if (!estPrevu) {
            estPrevu = true
            autresMusiciens(chosenId) ! Measure(chordlist)
            scheduler.scheduleOnce(tempsAttenteEntreNote) {
              self ! JoueChefOrchestre
              estPrevu = false
            }
          }
        } else {
          println("{Musicien} Pas de musicien disponible. En attente...")
          estSeul = true
          scheduler.scheduleOnce(tempsAttenteAvantExit, self, SelfTerminate)
        }
      } else {    // Si on est un simple Musicien, on a reçu cette Measure du chef d'orchestre
        println("{Musicien} Mesure reçu, joue...")
        player ! Measure (chordlist)
      }

    case Alive(n) =>
      estSeul = false
      println(s"alives$n = " + musiciensVivant(n))
      if (!musiciensVivant(n)) {
        musiciensVivant(n) = true
        nbAutresMusiciensVivant += 1
        println(s"{Musicien} - Musicien $n est vivant. Total: $nbAutresMusiciensVivant")
        if (nbAutresMusiciensVivant > 0 && currentChefOrchestre == id){
          self ! JoueChefOrchestre
        }
      }
      if(currentChefOrchestre != -1) autresMusiciens(n) ! GetNouveauChefOrchestre (currentChefOrchestre)

    case Dead(n) =>
      if (musiciensVivant(n)) {
        musiciensVivant(n) = false
        nbAutresMusiciensVivant -= 1
        println(s"{Musicien} Musicien $n est mort. Total: $nbAutresMusiciensVivant")
      }
      if (currentChefOrchestre == n || currentChefOrchestre == -1) self ! CreeElection

    case SelfTerminate =>
      if (estSeul) {
        println(s"{Musicien} Musicien $id a quitté (Seul).")
        context.system.terminate()
        System.exit(0)
      }

    case GetNouveauChefOrchestre(n) =>
      if (n != id && musiciensVivant(n)) {
        currentChefOrchestre = n
        println(s"{Musicien} Musicien $id - Nouveau chef d'orchestre: $n")
      }

    case CreeElection =>
      val aliveMusicians = (0 until 4).filter(musiciensVivant).toList
      if (aliveMusicians.contains(id)) {
        self ! ChefOrchestreElu
      } else if (aliveMusicians.nonEmpty) {
        currentChefOrchestre = aliveMusicians.head
      }

    case ChefOrchestreElu =>
      if (currentChefOrchestre != id) {
        currentChefOrchestre = id
        println(s"{Musicien} Musicien $id est élu en temps que nouveau chef d'orchestre.")
        self ! JoueChefOrchestre
      }
  }
}