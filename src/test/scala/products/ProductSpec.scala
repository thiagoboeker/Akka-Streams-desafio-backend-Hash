package products

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.testkit.{TestActor, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import ProductDomain.{Product, _}
import akka.stream.ActorMaterializer
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit
import akka.util.Timeout

import scala.concurrent.duration._

class ProductSpec extends TestKit(ActorSystem("ProductSpec")) with WordSpecLike with BeforeAndAfterAll{
   
   override def afterAll(): Unit = TestKit.shutdownActorSystem(system)
   
   "GetProduct flow" should {
      "Dispatch products" in {
         
         implicit val mat = ActorMaterializer()
         
         // IDs of products
         val productList = List("2001", "2002", "2003")
         val simpleSource = Source(productList)
         val probe = TestProbe()
         
         val probeSink = TestSink.probe[Seq[Product]]
         
         // Responds with Products instances
         probe.setAutoPilot((sender: ActorRef, msg: Any) => {
            msg match {
               case GetProductByID(id) => sender ! Product(id, "A product", "Product to be sended", 1000);TestActor.KeepRunning
            }
            TestActor.KeepRunning
         })
         
         // Runs the graph, buffers in the groupedWithin
         val matValues = simpleSource.via(GetProduct(probe.ref)).groupedWithin(3, 1.seconds).runWith(probeSink)
         matValues
           .request(3)
           .expectNext(Seq(
                  Product("2001", "A product", "Product to be sended", 1000),
                  Product("2002", "A product", "Product to be sended", 1000),
                  Product("2003", "A product", "Product to be sended", 1000)
               )
           )
           .expectComplete()
      }
   }
   
}
