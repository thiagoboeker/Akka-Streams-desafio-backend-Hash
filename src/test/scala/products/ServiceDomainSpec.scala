package products

import java.time.LocalDate

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{RunnableGraph, Sink, Source}
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.{TestActor, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import products.ProductDomain.{GetProductByID, Product}
import products.UserDomain.{GetUserByID, User}
import products.ServiceDomain._

import scala.util.Random
class ServiceDomainSpec extends TestKit(ActorSystem("ServiceDomain")) with WordSpecLike with BeforeAndAfterAll{
   
   
   override def afterAll(): Unit = TestKit.shutdownActorSystem(system)
   
   "A Service" should {
      "Serve products with discounts" in {
         
         // Starts all
         implicit val mat = ActorMaterializer()
         val productList = List("2001", "2002", "2003")
         val myID = "ThiagoBoeker"
         val ProductProbe = TestProbe()
         val date = LocalDate.now()
         
         /*
          *    Config the auto-pilot actors
          */
         ProductProbe.setAutoPilot((sender: ActorRef, msg: Any) => {
            val rand = new Random()
            msg match {
               case GetProductByID(id) => sender ! Product(id, "A product", "Product to be sended", rand.nextInt(10) * 1000);TestActor.KeepRunning
            }
            TestActor.KeepRunning
         })
   
         val UserProbe = TestProbe()
         UserProbe.setAutoPilot((sender: ActorRef, msg: Any) => {
            msg match {
               case GetUserByID(id) => sender ! User(id, "Jhon", "Doe", date)
            }
            TestActor.KeepRunning
         })
         
         
         /*
          *  Runs the graph who have the whole business logic in it.
          */
         ServiceGraphV1(myID, productList, UserProbe.ref, ProductProbe.ref).runWith(Sink.foreach(println))
         
      }
   }
}
