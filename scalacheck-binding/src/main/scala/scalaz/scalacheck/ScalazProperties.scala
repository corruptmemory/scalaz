package scalaz
package scalacheck

import org.scalacheck.{Arbitrary, Prop, Properties}
import Prop.forAll
import Scalaz._

/**
 * Scalacheck properties that should hold for instances of type classes defined in Scalaz Core.
 */
object ScalazProperties {

  object equal {
    def commutativity[A](implicit A: Equal[A], arb: Arbitrary[A]) = forAll(A.equalLaw.commutative _)

    def reflexive[A](implicit A: Equal[A], arb: Arbitrary[A]) = forAll(A.equalLaw.reflexive _)

    def transitive[A](implicit A: Equal[A], arb: Arbitrary[A]) = forAll(A.equalLaw.transitive _)

    def laws[A](implicit A: Equal[A], arb: Arbitrary[A]) = new Properties("equal") {
      property("commutativity") = commutativity[A]
      property("reflexive") = reflexive[A]
      property("transitive") = transitive[A]
    }
  }

  object semigroup {
    def associative[A](implicit A: Semigroup[A], eqa: Equal[A], arb: Arbitrary[A]) = forAll(A.semigroupLaw.associative _)

    def laws[A](implicit A: Semigroup[A], eqa: Equal[A], arb: Arbitrary[A]) = new Properties("semigroup") {
      property("associative") = associative[A]
    }
  }

  object monoid {
    def leftIdentity[A](implicit A: Monoid[A], eqa: Equal[A], arb: Arbitrary[A]) = forAll(A.monoidLaw.leftIdentity _)

    def rightIdentity[A](implicit A: Monoid[A], eqa: Equal[A], arb: Arbitrary[A]) = forAll(A.monoidLaw.rightIdentity _)

    def laws[A](implicit A: Monoid[A], eqa: Equal[A], arb: Arbitrary[A]) = new Properties("monoid") {
      include(semigroup.laws[A])
      property("leftIdentity") = leftIdentity[A]
      property("rightIdentity") = rightIdentity[A]
    }
  }

  object group {
    def inverseExists[A](implicit A: Group[A], eqa: Equal[A], arb: Arbitrary[A]) = forAll(A.groupLaw.inverseExists _)

    def laws[A](implicit A: Group[A], eqa: Equal[A], arb: Arbitrary[A]) = new Properties("group") {
      include(monoid.laws[A])
      property("inverseExists") = inverseExists[A]
    }
  }

  object functor {
    def identity[F[_], X](implicit F: Functor[F], afx: Arbitrary[F[X]], ef: Equal[F[X]]) =
      forAll(F.functorLaw.identity[X] _)

    def associative[F[_], X, Y, Z](implicit F: Functor[F], af: Arbitrary[F[X]], axy: Arbitrary[(X => Y)],
                                   ayz: Arbitrary[(Y => Z)], ef: Equal[F[Z]]) =
      forAll(F.functorLaw.associative[X, Y, Z] _)

    def laws[F[_]](implicit F: Functor[F], af: Arbitrary[F[Int]], axy: Arbitrary[(Int => Int)],
                   ef: Equal[F[Int]]) = new Properties("functor") {
      property("identity") = identity[F, Int]
      property("associative") = associative[F, Int, Int, Int]
    }
  }

  object applicative {
    def identity[F[_], X](implicit f: Applicative[F], afx: Arbitrary[F[X]], ef: Equal[F[X]]) =
      forAll(f.applicativeLaw.identity[X] _)

    def composition[F[_], X, Y, Z](implicit ap: Applicative[F], afx: Arbitrary[F[X]], au: Arbitrary[F[Y => Z]],
                                   av: Arbitrary[F[X => Y]], e: Equal[F[Z]]) = forAll(ap.applicativeLaw.composition[X, Y, Z] _)

    def homomorphism[F[_], X, Y](implicit ap: Applicative[F], ax: Arbitrary[X], af: Arbitrary[X => Y], e: Equal[F[Y]]) =
      forAll(ap.applicativeLaw.homomorphism[X, Y] _)

    def interchange[F[_], X, Y](implicit ap: Applicative[F], ax: Arbitrary[X], afx: Arbitrary[F[X => Y]], e: Equal[F[Y]]) =
      forAll(ap.applicativeLaw.interchange[X, Y] _)

    def laws[F[_]](implicit F: Applicative[F], af: Arbitrary[F[Int]],
                   aff: Arbitrary[F[Int => Int]], e: Equal[F[Int]]) = new Properties("applicative") {
      include(functor.laws[F])
      property("identity") = applicative.identity[F, Int]
      property("composition") = applicative.composition[F, Int, Int, Int]
      property("homomorphism") = applicative.homomorphism[F, Int, Int]
      property("interchange") = applicative.interchange[F, Int, Int]
    }
  }

  object monad {
    def rightIdentity[M[_], X](implicit M: Monad[M], e: Equal[M[X]], a: Arbitrary[M[X]]) =
      forAll(M.monadLaw.rightIdentity[X] _)

    def leftIdentity[M[_], X, Y](implicit am: Monad[M], emy: Equal[M[Y]], ax: Arbitrary[X], af: Arbitrary[(X => M[Y])]) =
      forAll(am.monadLaw.leftIdentity[X, Y] _)

    def associativity[M[_], X, Y, Z](implicit M: Monad[M], amx: Arbitrary[M[X]], af: Arbitrary[(X => M[Y])],
                                     ag: Arbitrary[(Y => M[Z])], emz: Equal[M[Z]]) =
      forAll(M.monadLaw.associativeBind[X, Y, Z] _)

    def laws[M[_]](implicit a: Monad[M], am: Arbitrary[M[Int]],
                   af: Arbitrary[Int => M[Int]], ag: Arbitrary[M[Int => Int]], e: Equal[M[Int]]) = new Properties("monad") {
      include(applicative.laws[M])

      property("Right identity") = monad.rightIdentity[M, Int]
      property("Left identity") = monad.leftIdentity[M, Int, Int]
      property("Associativity") = monad.associativity[M, Int, Int, Int]

    }
  }

  object traverse {
    def identityTraverse[F[_], X, Y](implicit f: Traverse[F], afx: Arbitrary[F[X]], axy: Arbitrary[X => Y], ef: Equal[F[Y]]) =
      forAll(f.traverseLaw.identityTraverse[X, Y] _)

    def purity[F[_], G[_], X](implicit f: Traverse[F], afx: Arbitrary[F[X]], G: Applicative[G], ef: Equal[G[F[X]]]) =
      forAll(f.traverseLaw.purity[G, X] _)

    def sequentialFusion[F[_], N[_], M[_], A, B, C](implicit fa: Arbitrary[F[A]], amb: Arbitrary[A => M[B]], bnc: Arbitrary[B => N[C]],
                                                      F: Traverse[F], N: Applicative[N], M: Applicative[M], MN: Equal[M[N[F[C]]]]): Prop =
      forAll(F.traverseLaw.sequentialFusion[N, M, A, B, C] _)

    def naturality[F[_], N[_], M[_], A](nat: (M ~> N))
                                       (implicit fma: Arbitrary[F[M[A]]], F: Traverse[F], N: Applicative[N], M: Applicative[M], NFA: Equal[N[F[A]]]): Prop =
      forAll(F.traverseLaw.naturality[N, M, A](nat) _)

    def parallelFusion[F[_], N[_], M[_], A, B](implicit fa: Arbitrary[F[A]], amb: Arbitrary[A => M[B]], anb: Arbitrary[A => N[B]],
                                               F: Traverse[F], N: Applicative[N], M: Applicative[M], MN: Equal[(M[F[B]], N[F[B]])]): Prop =
      forAll(F.traverseLaw.parallelFusion[N, M, A, B] _)

    def laws[F[_]](implicit fa: Arbitrary[F[Int]], amb: Arbitrary[Int => Option[Int]], anb: Arbitrary[Int => Stream[Int]],
                   F: Traverse[F], EF: Equal[F[Int]], MN: Equal[(Option[F[Int]], Stream[F[Int]])]) =
      new Properties("traverse") {
        property("identityTraverse") = identityTraverse[F, Int, Int]

        import std.list._, std.option._, std.stream._, std.anyVal._

        property("purity.option") = purity[F, Option, Int]
        property("purity.stream") = purity[F, Stream, Int]

        property("sequentialFusion") = sequentialFusion[F, Option, List, Int, Int, Int]
      }
  }

  object plus {
    def associative[F[_], X](implicit f: Plus[F], afx: Arbitrary[F[X]], ef: Equal[F[X]]) =
      forAll(f.plusLaw.associative[X] _)

    def laws[F[_]](implicit f: Plus[F], afx: Arbitrary[F[Int]], ef: Equal[F[Int]]) = new Properties("plus") {
      property("associative") = associative[F, Int]
    }
  }

  object empty {
    def emptyMap[F[_], X](implicit f: Empty[F], afx: Arbitrary[X => X], ef: Equal[F[X]]) =
      forAll(f.emptyLaw.emptyMap[X] _)

    def leftPlusIdentity[F[_], X](implicit f: Empty[F], afx: Arbitrary[F[X]], ef: Equal[F[X]]) =
      forAll(f.emptyLaw.leftPlusIdentity[X] _)

    def rightPlusIdentity[F[_], X](implicit f: Empty[F], afx: Arbitrary[F[X]], ef: Equal[F[X]]) =
      forAll(f.emptyLaw.rightPlusIdentity[X] _)

    def laws[F[_]](implicit f: Empty[F], afx: Arbitrary[F[Int]], af: Arbitrary[Int => Int], ef: Equal[F[Int]]) = new Properties("empty") {
      include(plus.laws[F])
      property("emptyMap") = emptyMap[F, Int]
      property("leftPlusIdentity") = leftPlusIdentity[F, Int]
      property("rightPlusIdentity") = rightPlusIdentity[F, Int]
    }
  }

  object monadPlus {
    def leftBindIdentity[F[_], X](implicit F: MonadPlus[F], afx: Arbitrary[X => F[X]], ef: Equal[F[X]]) =
      forAll(F.monadPlusLaw.leftBindIdentity[X] _)

    def rightBindIdentity[F[_], X](implicit F: MonadPlus[F], afx: Arbitrary[F[X]], ef: Equal[F[X]]) =
      forAll(F.monadPlusLaw.rightBindIdentity[X] _)

    def laws[F[_]](implicit F: MonadPlus[F], afx: Arbitrary[F[Int]], afy: Arbitrary[F[Int => Int]], ef: Equal[F[Int]]) = new Properties("monad plus") {
      include(monad.laws[F])
      include(plus.laws[F])
      property("leftBindIdentity") = leftBindIdentity[F, Int]
      property("rightBindIdentity") = rightBindIdentity[F, Int]
    }
  }
}
