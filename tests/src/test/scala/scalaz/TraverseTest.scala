package scalaz

import scalacheck.ScalazProperties
import org.scalacheck.Arbitrary
import org.specs2.matcher.Parameters

class TraverseTest extends Spec {

  import scalaz._
  import std.AllInstances._
  import std.AllFunctions._
  import syntax.traverse._
  import WriterT._

  "list" should {
    import std.list._

    // ghci> import Data.Traversable
    // ghci> import Control.Monad.Writer
    // ghci> let (|>) = flip ($)
    // ghci> traverse (\x -> writer (x, x)) ["1", "2", "3"] |> runWriter
    // (["1","2","3"],"123")
    "apply effects in order" in {
      val s: Writer[String, List[Int]] = List(1, 2, 3).traverseU(x => Writer(x.toString, x))
      s.run must be_===(("123", List(1, 2, 3)))
    }

    "traverse through option effect" in {
      val s: Option[List[Int]] = List(1, 2, 3).traverseU((x: Int) => if (x < 3) some(x) else none)
      s must be_===(none[List[Int]])
    }

    "not blow the stack" in {
      val s: Option[List[Int]] = List.range(0, 32 * 1024).traverseU(x => some(x))
      s.map(_.take(3)) must be_===(some(List(0, 1, 2)))
    }
  }

  "stream" should {
    import std.stream._

    "apply effects in order" in {
      val s: Writer[String, Stream[Int]] = Stream(1, 2, 3).traverseU(x => Writer(x.toString, x))
      s.run must be_===(("123", Stream(1, 2, 3)))
    }

    // ghci> import Data.Traversable
    // ghci> traverse (\x -> if x < 3 then Just x else Nothing) [1 ..]
    // Nothing
    "allow partial traversal" in {
      val stream = Stream.from(1)
      val s: Option[Stream[Int]] = stream.traverseU((x: Int) => if (x < 3) some(x) else none)
      s must be_===(none)
    }
  }

  "combos" should {
    "traverse large stream over trampolined StateT including IO" in {
      // Example usage from Eric Torreborre
      import scalaz._
      import scalaz.effect._
      import Free.Trampoline
      import scalaz.syntax.applicative._

      implicit def StateTMApplicative[M1[_] : Monad, S, M2[_] : Applicative] =
        StateT.stateTMonadState[S, M1].compose(Applicative[M2])

      def traverseStateTrampoline[S, M[_]: Applicative, A, B](as: Stream[A])(f: A => State[S, M[B]]): State[S, M[Stream[B]]] = {
        type StateTrampM[a] = StateT[Trampoline, S, M[a]]
        as.traverse[StateTrampM, B](f(_: A).lift[Trampoline]).unliftId[Trampoline]
      }

      val as = Stream.range(0, 100000)
      val state: State[Int, IO[Stream[Int]]] = traverseStateTrampoline(as)(a => State((s: Int) => (IO(a - s), a)))
      state.eval(0).unsafePerformIO.take(3) must be_===(Stream(0, 1, 1))
    }
  }

  checkTraverseLaws[List, Int]
  checkTraverseLaws[Stream, Int]
  checkTraverseLaws[Option, Int]
  checkTraverseLaws[Id, Int]
  // checkTraverseLaws[NonEmptyList, Int]
  // checkTraverseLaws[({type λ[α]=Validation[Int, α]})#λ, Int]
  // checkTraverseLaws[Zipper, Int]
  // checkTraverseLaws[LazyOption, Int]

  def checkTraverseLaws[M[_], A](implicit mm: Traverse[M],
                                 ea: Equal[A],
                                 ema: Equal[M[A]],
                                 arbma: Arbitrary[M[A]],
                                 arba: Arbitrary[A], man: Manifest[M[_]]) = {
    man.toString should {
      import ScalazProperties.traverse._
      import std.option._, std.list._

      implicit val defaultParameters = Parameters(defaultValues.updated(maxSize, 5))

      "identityTraverse" in check(identityTraverse[M, A, A])
      "purity (Option)" in check(purity[M, Option, A])
      "purity (List)" in check(purity[M, List, A])
      "sequentialFusion (List/Option)" in check(sequentialFusion[M, List, Option, A, A, A])
      "parallelFusion (List/Option)" in check(parallelFusion[M, List, Option, A, A])
    }
  }
}
