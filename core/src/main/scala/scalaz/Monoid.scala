package scalaz

import collection.generic.CanBuildFrom
import collection.mutable.ArraySeq

trait Monoid[A] {
  val zero: Zero[A]
  val semigroup: Semigroup[A]

  import Monoid._

  def z: A =
    zero.zero

  def append(a1: A, a2: => A): A =
    semigroup.append(a1, a2)

  def deriving[B](implicit n: ^*^[B, A]): Monoid[B] =
    new Monoid[B] {
      implicit val zero = Monoid.this.zero.deriving
      implicit val semigroup = Monoid.this.semigroup.deriving
      monoid[B]
    }

}

object Monoid extends Monoids

trait Monoids {
  def monoid[A](implicit zz: Zero[A], s: Semigroup[A]): Monoid[A] = new Monoid[A] {
    val zero = zz
    val semigroup = s
  }

  implicit val UnitMonoid: Monoid[Unit] =
    monoid

  implicit val BooleanMonoid: Monoid[Boolean] =
    monoid

  implicit val IntMonoid: Monoid[Int] =
    monoid

  implicit val StringMonoid: Monoid[String] =
    monoid

  implicit def StreamMonoid[A]: Monoid[Stream[A]] =
    monoid

  implicit def ListMonoid[A]: Monoid[List[A]] =
    monoid

  implicit def OptionMonoid[A: Semigroup]: Monoid[Option[A]] =
    monoid

  implicit def ArraySeqMonoid[A]: Monoid[ArraySeq[A]] =
    monoid[ArraySeq[A]]

  implicit def Tuple2Monoid[A, B](implicit ma: Monoid[A], mb: Monoid[B]): Monoid[(A, B)] = {
    implicit val sa = implicitly[Monoid[(A, B)]].semigroup
    implicit val za = implicitly[Monoid[(A, B)]].zero
    monoid[(A, B)]
  }

  implicit def Tuple3Monoid[A, B, C](implicit ma: Monoid[A], mb: Monoid[B], mc: Monoid[C]): Monoid[(A, B, C)] = {
    implicit val sa = implicitly[Monoid[(A, B, C)]].semigroup
    implicit val za = implicitly[Monoid[(A, B, C)]].zero
    monoid[(A, B, C)]
  }

  implicit def Function1Monoid[A, B](implicit mb: Monoid[B]): Monoid[A => B] = {
    implicit val sb = implicitly[Monoid[A => B]].semigroup
    implicit val zb = implicitly[Monoid[A => B]].zero
    monoid[A => B]
  }

  /**A monoid for sequencing Applicative effects. */
  def liftMonoid[F[_], M](implicit m: Monoid[M], a: Applicative[F]): Monoid[F[M]] = new Monoid[F[M]] {
    val zero = new Zero[F[M]] {
      val zero =
        a.point(m.z)
    }
    val semigroup: Semigroup[F[M]] = new Semigroup[F[M]] {
      def append(x: F[M], y: => F[M]) =
        a.liftA2[M, M, M](m1 => m2 => m.append(m1, m2))(x)(y)
    }
  }
}
