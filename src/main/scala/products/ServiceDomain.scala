package products

import akka.actor.ActorRef
import akka.stream
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, MergePreferred, RunnableGraph, Sink, Source, Zip, ZipWith}
import akka.stream._
import products.ProductDomain.{GetProduct, GetProductByID, Product, ProductID}
import products.UserDomain.{GetUser, GetUserByID, User, UserID}
import DiscountDomain._

import scala.collection.immutable

object ServiceDomain {
   
   // Define a custom shape for the service which will have a UserID and ProductID as Input
   case class ServiceShape(in0: Inlet[UserID], in1: Inlet[ProductID], out0: Outlet[Product]) extends Shape {
      
      override def inlets: immutable.Seq[Inlet[_]] = List(in0, in1)
      
      override def outlets: immutable.Seq[Outlet[_]] = List(out0)
      
      override def deepCopy(): Shape = ServiceShape(in0.carbonCopy(), in1.carbonCopy(), out0.carbonCopy())
      
   }
   
   // The Service Component with his shape
   def ServiceComponent(UserActor: ActorRef, ProductActor: ActorRef) = GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      
      val userPort = b.add(Broadcast[UserID](1))
      val productPort = b.add(Broadcast[ProductID](1))
      
      val getUser = b.add(GetUser(UserActor))
      val getProduct = b.add(GetProduct(ProductActor))
      val zip = b.add(Zip[User, Product])
      val getDiscount = b.add(GetDiscount)
      
      userPort ~> getUser ~> zip.in0
      productPort ~> getProduct ~> zip.in1
      zip.out ~> getDiscount
      
      ServiceShape(userPort.in, productPort.in, getDiscount.out)
   }
   
   // Implements the business logic as a Source to some other component who will work with the data provided
   def ServiceGraphV1(userID: UserID, productList: List[ProductID], userActor: ActorRef, productActor: ActorRef) =
      Source.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
         
         // Will repeated the userID util finish
      val userSource = b.add(Source.repeat(userID))
         
         // Source of products ID TODO * add some throttle to get some backpressure downstream
      val productSource = b.add(Source(productList))
         
         // The Service component
      val serviceComponent = b.add(ServiceComponent(userActor, productActor))
      
         // Feeds it
      userSource ~> serviceComponent.in0
      productSource ~> serviceComponent.in1
         
         // Returns the output for the upstream component
      SourceShape(serviceComponent.out0)
   })
}
