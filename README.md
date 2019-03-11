# Akka-Streams-desafio-backend-Hash

This is a rework of a project of mine using Akka Streams. Very fun to do it so far, and Akka is some really good tech. Akka is assyncronous by default and free of locks and so on which by definition are slower in orders of magnitude.

# How it works

I have 4 objects that in the Akka context we call Domains, as follows:

```Scala
  object DiscountDomain {
    // 
  }
  object ProductDomain {
    //
  }
  object ServiceDomain {
    //
  }
  object UserDomain {
    //
  }
```

These domains have all the logic and components isolated and the Domain the wraps all the service is the ServiceDomain as this:

```Scala
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

```

In this ServiceDomain we define our main Graph using the GraphDSL akka component. So we first define a custom shape in the ServiceShape class that will have two input ports and one output port. Then we define a ServiceComponent class that will serve as a open shape from a Source. The flow of the data in the graph is assigned by the *~>* operator from the GraphDSL.
And finally a runnable graph as a Source in the ServiceGraphV1 function which takes some assets, transform them in Sources as roll out the data in the ServiceComponent.

# Wrap

This is great tech and I'm looking foward to mastered it more and build some really powerfull stuf with it. 
