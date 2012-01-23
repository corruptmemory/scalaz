package scalaz

sealed trait WriterT[F[_], W, A] { self =>
  val run: F[(W, A)]

  import WriterT._

  def mapValue[X, B](f: ((W, A)) => (X, B))(implicit F: Functor[F]): WriterT[F, X, B] =
    writerT(F.map(run)(f))

  def mapWritten[X](f: W => X)(implicit ftr: Functor[F]): WriterT[F, X, A] =
    mapValue(wa => (f(wa._1), wa._2))

  def written(implicit F: Functor[F]): F[W] =
    F.map(run)(_._1)

  def over(implicit F: Functor[F]): F[A] =
    F.map(run)(_._2)

  def swap(implicit F: Functor[F]): WriterT[F, A, W] =
    mapValue(wa => (wa._2, wa._1))

  def :++>(w: => W)(implicit F: Functor[F], W: Semigroup[W]): WriterT[F, W, A] =
    mapWritten(W.append(_, w))

  def :++>>(f: A => W)(implicit F: Functor[F], W: Semigroup[W]): WriterT[F, W, A] =
    mapValue(wa => (W.append(wa._1, f(wa._2)), wa._2))

  def <++:(w: => W)(implicit F: Functor[F], W: Semigroup[W]): WriterT[F, W, A] =
    mapWritten(W.append(w, _))

  def <<++:(f: A => W)(implicit F: Functor[F], s: Semigroup[W]): WriterT[F, W, A] =
    mapValue(wa => (s.append(f(wa._2), wa._1), wa._2))

  def reset(implicit Z: Monoid[W], F: Functor[F]): WriterT[F, W, A] =
    mapWritten(_ => Z.zero)

  def map[B](f: A => B)(implicit F: Functor[F]): WriterT[F, W, B] =
    writerT(F.map(run)(wa => (wa._1, f(wa._2))))

  def foreach[B](f: A => Unit)(implicit E: Each[F]): Unit =
    E.each(run)(wa => f(wa._2))

  def ap[B](f: => WriterT[F, W, (A) => B])(implicit F: Apply[F], W: Semigroup[W]): WriterT[F, W, B] = writerT {
    F.map2(f.run, run) {
      case ((w1, fab), (w2, a)) => (W.append(w1, w2), fab(a))
    }
  }

  def flatMap[B](f: A => WriterT[F, W, B])(implicit F: Monad[F], s: Semigroup[W]): WriterT[F, W, B] =
    writerT(F.bind(run){wa =>
      val z = f(wa._2).run
      F.map(z)(wb => (s.append(wa._1, wb._1), wb._2))
    })

  def traverse[G[_] , B](f: A => G[B])(implicit G: Applicative[G], F: Traverse[F]): G[WriterT[F, W, B]] = {
    G.map(F.traverse(run){
      case (w, a) => G.map(f(a))(b => (w, b))
    })(WriterT(_))
  }

  def foldRight[B](z: => B)(f: (A, => B) => B)(implicit F: Foldable[F]) =
    F.foldR(run, z) { a => b =>
      f(a._2, b)
    }

  def bimap[C, D](f: (W) => C, g: (A) => D)(implicit F: Functor[F]) =
    writerT[F, C, D](F.map(run)({
      case (a, b) => (f(a), g(b))
    }))

  def bitraverse[G[_], C, D](f: (W) => G[C], g: (A) => G[D])(implicit G: Applicative[G], F: Traverse[F]) =
    G.map(F.traverse[G, (W, A), (C, D)](run) {
      case (a, b) => G.map2(f(a), g(b))((_, _))
    })(writerT(_))

  def rwst[R, S](implicit F: Functor[F]): ReaderWriterStateT[F, R, W, S, A] = ReaderWriterStateT(
    (r, s) => F.map(self.run) {
      case (w, a) => (w, a, s)
    }
  )
}

object WriterT extends WriterTFunctions with WriterTInstances {
  def apply[F[_], W, A](v: F[(W, A)]): WriterT[F, W, A] =
    writerT(v)
}

trait WriterTInstances4 {
  implicit def writerTFunctor[F[_], W](implicit F0: Functor[F]) = new WriterTFunctor[F, W] {
    implicit def F = F0
  }
}

trait WriterTInstances3 extends WriterTInstances4 {
  implicit def writerTPointed[F[_], W](implicit W0: Monoid[W], F0: Pointed[F]) = new WriterTPointed[F, W] {
    implicit def F = F0
    implicit def W = W0
  }
}

trait WriterTInstances2 extends WriterTInstances3 {
  implicit def writerTApply[F[_], W](implicit W0: Semigroup[W], F0: Apply[F]) = new WriterTApply[F, W] {
    implicit def F = F0
    implicit def W = W0
  }
}

trait WriterTInstances1 extends WriterTInstances2 {
  implicit def writerTApplicative[F[_], W](implicit W0: Monoid[W], F0: Applicative[F]) = new WriterTApplicative[F, W] {
    implicit def F = F0
    implicit def W = W0
  }
}

trait WriterTInstances0 extends WriterTInstances1 {
  implicit def writerTBiFunctor[F[_]](implicit F0: Functor[F]) = new WriterTBiFunctor[F] {
    implicit def F = F0
  }
  implicit def writerTMonad[F[_], W](implicit W0: Monoid[W], F0: Monad[F]) = new WriterTMonad[F, W] {
    implicit def F = F0
    implicit def W = W0
  }
  implicit def writerTCoPointed[F[_], W](implicit F0: CoPointed[F]) = new WriterTCoPointed[F, W] {
    implicit def F = F0
  }
  implicit def writerTFoldable[F[_], W](implicit F0: Foldable[F]) = new WriterTFoldable[F, W] {
    implicit def F = F0
  }
  implicit def writerTEqual[F[_], W, A](implicit E: Equal[F[(W, A)]]) = E.contramap((_: WriterT[F, W, A]).run)
}

trait WriterTInstances extends WriterTInstances0 {
  implicit def writerTBiTraverse[F[_]](implicit F0: Traverse[F]) = new WriterTBiTraverse[F] {
    implicit def F = F0
  }
  implicit def writerCoMonad[W] = new WriterCoMonad[W] {
    implicit def F = implicitly
  }
  implicit def writerTTraverse[F[_], W](implicit F0: Traverse[F]) = new WriterTTraverse[F, W] {
    implicit def F = F0
  }
  implicit def writerTIndex[W] = new WriterTIndex[W] {
  }
  implicit def writerTEach[F[_], W](implicit F0: Each[F]) = new WriterTEach[F, W] {
    implicit def F = F0
  }
  implicit def writerEqual[W, A](implicit W: Equal[W], A: Equal[A]) = {
    import std.tuple._
    Equal[(W, A)].contramap((_: Writer[W, A]).run)
  }
}

trait WriterTFunctions {
  def writerT[F[_], W, A](v: F[(W, A)]): WriterT[F, W, A] = new WriterT[F, W, A] {
    val run = v
  }

  def writer[W, A](v: (W, A)): Writer[W, A] =
    writerT[Id, W, A](v)

  def tell[W](w: W): Writer[W, Unit] = writer(w -> ())

  def put[F[_], W, A](value: F[A])(w: W)(implicit F: Functor[F]): WriterT[F, W, A] =
    WriterT(F.map(value)(a => (w, a)))

  /** Puts the written value that is produced by applying the given function into a writer transformer and associates with `value` */
  def putWith[F[_], W, A](value: F[A])(w: A => W)(implicit F: Functor[F]): WriterT[F, W, A] =
    WriterT(F.map(value)(a => (w(a), a)))
}

//
// Type class implementation traits
//
import WriterT.writerT

trait WriterTFunctor[F[_], W] extends Functor[({type λ[α]=WriterT[F, W, α]})#λ] {
  implicit def F: Functor[F]

  override def map[A, B](fa: WriterT[F, W, A])(f: (A) => B) = fa map f
}

trait WriterTPointed[F[_], W] extends Pointed[({type λ[α]=WriterT[F, W, α]})#λ] with WriterTFunctor[F, W] {
  implicit def F: Pointed[F]
  implicit def W: Monoid[W]

  def point[A](a: => A) = writerT(F.point((W.zero, a)))
}

trait WriterTApply[F[_], W] extends Apply[({type λ[α]=WriterT[F, W, α]})#λ] with WriterTFunctor[F, W] {
  implicit def F: Apply[F]
  implicit def W: Semigroup[W]

  override def ap[A, B](fa: => WriterT[F, W, A])(f: => WriterT[F, W, (A) => B]) = fa ap f
}

trait WriterTApplicative[F[_], W] extends Applicative[({type λ[α]=WriterT[F, W, α]})#λ] with WriterTApply[F, W] with WriterTPointed[F, W] {
  implicit def F: Applicative[F]
  implicit def W: Monoid[W]
}

trait WriterTEach[F[_], W] extends Each[({type λ[α]=WriterT[F, W, α]})#λ] {
  implicit def F: Each[F]

  def each[A](fa: WriterT[F, W, A])(f: (A) => Unit) = fa foreach f
}

// TODO does Index it make sense for F other than Id?
trait WriterTIndex[W] extends Index[({type λ[α]=WriterT[Id, W, α]})#λ] {
  def index[A](fa: WriterT[Id, W, A], i: Int) = if(i == 0) Some(fa.over) else None
}

trait WriterTMonad[F[_], W] extends Monad[({type λ[α]=WriterT[F, W, α]})#λ] with WriterTApplicative[F, W] with WriterTPointed[F, W] {
  implicit def F: Monad[F]

  def bind[A, B](fa: WriterT[F, W, A])(f: (A) => WriterT[F, W, B]) = fa flatMap f
}

trait WriterTFoldable[F[_], W] extends Foldable.FromFoldr[({type λ[α]=WriterT[F, W, α]})#λ] {
  implicit def F: Foldable[F]

  override def foldRight[A, B](fa: WriterT[F, W, A], z: => B)(f: (A, => B) => B) = fa.foldRight(z)(f)
}

trait WriterTTraverse[F[_], W] extends Traverse[({type λ[α]=WriterT[F, W, α]})#λ] with WriterTFoldable[F, W] {
  implicit def F: Traverse[F]

  def traverseImpl[G[_]: Applicative, A, B](fa: WriterT[F, W, A])(f: (A) => G[B]) = fa traverse f
}

trait WriterTBiFunctor[F[_]] extends BiFunctor[({type λ[α, β]=WriterT[F, α, β]})#λ] {
  implicit def F: Functor[F]

  override def bimap[A, B, C, D](fab: WriterT[F, A, B])(f: (A) => C, g: (B) => D) =
    fab.bimap(f, g)
}

trait WriterTBiTraverse[F[_]] extends BiTraverse[({type λ[α, β]=WriterT[F, α, β]})#λ] with WriterTBiFunctor[F] {
  implicit def F: Traverse[F]

  def bitraverse[G[_]: Applicative, A, B, C, D](fab: WriterT[F, A, B])(f: (A) => G[C], g: (B) => G[D]) =
    fab.bitraverse(f, g)
}

trait WriterTCoPointed[F[_], W] extends CoPointed[({type λ[α] = WriterT[F, W, α]})#λ] with WriterTFunctor[F, W] {
  implicit def F: CoPointed[F]

  def copoint[A](p: WriterT[F, W, A]): A = F.copoint(p.over)
}

trait WriterCoMonad[W] extends CoMonad[({type λ[α] = Writer[W, α]})#λ] with WriterTCoPointed[Id, W] {

  override def cojoin[A](fa: Writer[W, A]): Writer[W, Writer[W, A]] =
    Writer(fa.written, fa)

  override def cobind[A, B](fa: Writer[W, A])(f: (Writer[W, A]) => B): Writer[W, B] =
    Writer(fa.written, f(fa))
}