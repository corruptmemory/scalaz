package scalaz
package syntax
package effect

import scalaz.effect.MonadControlIO

/** Wraps a value `self` and provides methods related to `MonadControlIO` */
trait MonadControlIOV[F[_],A] extends SyntaxV[F[A]] {
  implicit def F: MonadControlIO[F]
  ////

  ////
}

trait ToMonadControlIOV0 {
  implicit def ToMonadControlIOVUnapply[FA](v: FA)(implicit F0: Unapply[MonadControlIO, FA]) =
    new MonadControlIOV[F0.M,F0.A] { def self = F0(v); implicit def F: MonadControlIO[F0.M] = F0.TC }

}

trait ToMonadControlIOV extends ToMonadControlIOV0 with ToLiftControlIOV with ToMonadV {
  implicit def ToMonadControlIOV[F[_],A](v: F[A])(implicit F0: MonadControlIO[F]) =
    new MonadControlIOV[F,A] { def self = v; implicit def F: MonadControlIO[F] = F0 }

  ////

  ////
}

trait MonadControlIOSyntax[F[_]] extends LiftControlIOSyntax[F] with MonadSyntax[F] {
  implicit def ToMonadControlIOV[A](v: F[A])(implicit F0: MonadControlIO[F]): MonadControlIOV[F, A] = new MonadControlIOV[F,A] { def self = v; implicit def F: MonadControlIO[F] = F0 }

  ////

  ////
}
