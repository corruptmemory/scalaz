package scalaz

sealed trait Endo[A] {
  def run: A => A

  final def apply(a: A): A = run(a)

  final def fix: A = apply(fix)

  final def compose(other: Endo[A]): Endo[A] = Endo.endo(run compose other.run)

  final def andThen(other: Endo[A]): Endo[A] = other compose this
}

object Endo extends EndoFunctions with EndoInstances {
  def apply[A](f: A => A): Endo[A] = new Endo[A] {
    val run = f
  }
}

trait EndoInstances {
  implicit def endoInstance[A]: Monoid[Endo[A]] = new Monoid[Endo[A]] {
    def append(f1: Endo[A], f2: => Endo[A]) = f1 compose f2
    def zero = Endo.idEndo
  }
  implicit def endoInstances: Zip[Endo] with Unzip[Endo] = new Zip[Endo] with Unzip[Endo] {
    def zip[A, B](a: => Endo[A], b: => Endo[B]) =
      Endo {
        case (x, y) => (a(x), b(y))
      }

    // CAUTION: cheats with null
    def unzip[A, B](a: Endo[(A, B)]) =
      (Endo(x => a((x, null.asInstanceOf[B]))._1), Endo(x => a((null.asInstanceOf[A], x))._2))
  }
}

trait EndoFunctions {
  final def endo[A](f: A => A): Endo[A] = new Endo[A] {
    val run = f
  }

  final def constantEndo[A](a: => A): Endo[A] = endo[A](_ => a)

  final def idEndo[A]: Endo[A] = endo[A](a => a)

  import Isomorphism.{IsoSet, IsoFunctorTemplate}

  implicit def IsoEndo[A] = new IsoSet[Endo[A], A => A] {
    def to: (Endo[A]) => (A) => A = _.run
    def from: ((A) => A) => Endo[A] = endo
  }

  implicit def IsoFunctorEndo = new IsoFunctorTemplate[Endo, ({type λ[α]=(α => α)})#λ] {
    def to[A](fa: Endo[A]): (A) => A = fa.run
    def from[A](ga: (A) => A): Endo[A] = endo(ga)
  }
}
