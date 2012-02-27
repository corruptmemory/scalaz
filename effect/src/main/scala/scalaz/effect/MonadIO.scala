package scalaz
package effect

////
/**
 *
 */
////
trait MonadIO[F[_]] extends LiftIO[F] with Monad[F] { self =>
  ////

  // derived functions

  ////
  val monadIOSyntax = new scalaz.syntax.effect.MonadIOSyntax[F] {}
}

trait MonadIOInstances {
  implicit def ioPartialOrder[M[_]: MonadIO]: MonadPartialOrder[M, IO] = 
    new MonadPartialOrder[M, IO] {
      val MG = MonadIO[M]
      val MF = Monad[IO]
      def promote[A](m: IO[A]) = MG.liftIO(m)
    }
}

object MonadIO extends MonadIOInstances {
  @inline def apply[F[_]](implicit F: MonadIO[F]): MonadIO[F] = F

  ////

  // TODO for some reason, putting this in RegionTInstances causes scalac to blow the stack
  implicit def regionTMonadIO[S, M[_]](implicit M0: MonadIO[M]) =
    new MonadIO[({type λ[α] = RegionT[S, M, α]})#λ] with RegionTLiftIO[S, M] with RegionTMonad[S, M] {
      implicit def M = M0
      implicit def L = M0
    }
  ////
}

