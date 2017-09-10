import akka.NotUsed
import akka.actor.{Actor, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._

object AkkaHttpSwitchRoutesActor extends App {
  implicit val system: ActorSystem = ActorSystem("test")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val timeout = Timeout(1 second)

  val route1 = path("first") {
    get {
      complete {
        "First"
      }
    }
  } ~ path("switch") {
    get {
      complete {
        "Switching to second"
      }
    }
  }

  val route2 = path("second") {
    get {
      complete {
        "Second"
      }
    }
  }

  val routeDecider = system.actorOf(RouteDecider.props(route1, route2, _.uri.path == Path(s"/switch")))

  def queryActor(request: HttpRequest): Future[(HttpRequest, Route)] = (routeDecider ? request).mapTo[(HttpRequest, Route)]

  val flow: Flow[HttpRequest, HttpResponse, NotUsed] =
    Flow[HttpRequest]
        .mapAsync[(HttpRequest, Route)](1)(queryActor)
        .mapAsync[HttpResponse](1) {
    case (request, route) =>
      val handle = Route.asyncHandler(route)
      handle(request)
  }

  Http().bindAndHandle(flow, interface = "0.0.0.0", port = 8089)

}

class RouteDecider(first: Route, second: Route, isSwitchRequest: HttpRequest => Boolean) extends Actor {

  private var switched = false

  override def receive: Receive = {
    case request: HttpRequest =>
      val previouslySwitched = switched
      if (isSwitchRequest(request)) switched = true
      sender ! (if (!switched || (!previouslySwitched && switched)) (request, first) else (request, second))
  }

}

object RouteDecider {
  def props(first: Route, second: Route, isSwitchRequest: HttpRequest => Boolean) =
    Props(new RouteDecider(first,second, isSwitchRequest))
}