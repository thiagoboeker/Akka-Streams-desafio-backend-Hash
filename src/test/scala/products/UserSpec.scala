package products

import java.time.LocalDate
import java.util.Date

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.{TestActor, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import products.UserDomain.{GetUser, GetUserByID, User}

import scala.concurrent.duration._

class UserSpec extends TestKit(ActorSystem("UserSpec")) with WordSpecLike with BeforeAndAfterAll{
   
   override def afterAll(): Unit = TestKit.shutdownActorSystem(system)
   
   "A User producer" should {
      "Dispatch users" in {
         implicit val mat = ActorMaterializer()
         
         // Some ids
         val userList = List("2001", "2002", "2003")
         
         // IDs as a Source
         val simpleSource = Source(userList)
         val date = LocalDate.now()
         val probeSink = TestSink.probe[Seq[User]]
         val probe = TestProbe()
         
         // The auto pilot who will respond with Users instances
         probe.setAutoPilot((sender: ActorRef, msg: Any) => {
            msg match {
               case GetUserByID(id) => sender ! User(id, "Jhon", "Doe", date)
            }
            TestActor.KeepRunning
         })
         
         // The runnable graph, will buffer in groupedWithin
         val materializedUser = simpleSource.via(GetUser(probe.ref)).groupedWithin(3, 1.seconds).alsoTo(Sink.foreach(println)).runWith(probeSink)
         
         materializedUser
           .request(3)
           .expectNext(Seq(
              User("2001", "Jhon", "Doe", date),
              User("2002", "Jhon", "Doe", date),
              User("2003", "Jhon", "Doe", date)
           ))
           .expectComplete()
      }
   }
}
