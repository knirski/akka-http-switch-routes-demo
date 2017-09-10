import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow

import scala.concurrent.Future

object AkkaHttpSwitchRoutesSynchronized extends App {
  implicit val system: ActorSystem = ActorSystem("test")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

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

  var switched = false

  def pickRoute(request: HttpRequest, first: Route, second: Route, isSwitchRequest: HttpRequest => Boolean): Future[HttpResponse] = {
    val route = switched.synchronized {
      val previouslySwitched = switched
      if (isSwitchRequest(request)) switched = true
      if (!switched || (!previouslySwitched && switched)) route1 else route2
    }
    val handle = Route.asyncHandler(route)
    handle(request)
  }

  val flow: Flow[HttpRequest, HttpResponse, NotUsed] =
    Flow[HttpRequest].mapAsync[HttpResponse](1)(pickRoute(_, route1, route2, _.uri.path == Path(s"/switch")))

  Http().bindAndHandle(flow, interface = "0.0.0.0", port = 8089)

}
