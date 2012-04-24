package scalaz
package syntax
package std

import scalaz.syntax.Ops
import scalaz.{Monad, Tree, Zipper}
import scalaz.std.stream


trait StreamOps[A] extends Ops[Stream[A]] {
  final def merge(other: Stream[A]): Stream[A] = stream.merge(self, other)
  final def toZipper: Option[Zipper[A]] = stream.toZipper(self)
  final def zipperEnd: Option[Zipper[A]] = stream.zipperEnd(self)
  final def heads: Stream[Stream[A]] = stream.heads(self)
  final def tails: Stream[Stream[A]] = stream.tails(self)
  final def zapp[B, C](f: Stream[A => B => C]): Stream[(B) => C] = stream.zapp(self)(f)
  final def unfoldForest[B](f: A => (B, () => Stream[A])): Stream[Tree[B]] = stream.unfoldForest(self)(f)
  final def unfoldForestM[B, M[_] : Monad](f: A => M[(B, Stream[A])]): M[Stream[Tree[B]]] = stream.unfoldForestM(self)(f)
}

trait ToStreamOps {
  implicit def ToStreamOpsFromStream[A](a: Stream[A]): StreamOps[A] = new StreamOps[A] {
    val self = a
  }
}
