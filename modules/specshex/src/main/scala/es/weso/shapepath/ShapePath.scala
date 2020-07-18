package es.weso.shapepath

import cats._
import cats.data.Writer
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.writer._
import es.weso.rdf.nodes.{BNode, IRI}
import es.weso.shex.{EachOf, IRILabel, NodeConstraint, OneOf, Schema, Shape, ShapeAnd, ShapeExpr, ShapeLabel, ShapeNot, ShapeOr, TripleConstraint, TripleExpr}

case class ShapePath(startsWithRoot: Boolean, steps: List[Step])

sealed abstract class Item
case class SchemaItem(s: Schema) extends Item
case class ShapeExprItem(se: ShapeExpr) extends Item
case class TripleExprItem(te: TripleExpr) extends Item

case class Value(items: List[Item])

sealed abstract class ProcessingError
case class UnmatchItemContextLabel(item: Item, step: Step, ContextLabel: Context) extends ProcessingError

object ShapePath {


  type Comp[A] = Writer[List[ProcessingError],A]

  def checkContext(ctx: Context)(item: Item): Boolean = ctx match {
    case ShapeAndCtx => item match {
      case ShapeExprItem(se) => se match {
        case _: ShapeAnd => true
        case _ => false
      }
      case _ => false
    }
    case ShapeOrCtx => item match {
      case ShapeExprItem(se) => se match {
        case _: ShapeOr => true
        case _ => false
      }
      case _ => false
    }
    case ShapeNotCtx => item match {
      case ShapeExprItem(se) => se match {
        case _: ShapeNot => true
        case _ => false
      }
      case _ => false
    }
    case NodeConstraintCtx => item match {
      case ShapeExprItem(se) => se match {
        case _: NodeConstraint => true
        case _ => false
      }
      case _ => false
    }
    case ShapeCtx => item match {
      case ShapeExprItem(se) => se match {
        case _: Shape => true
        case _ => false
      }
      case _ => false
    }
    case EachOfCtx => item match {
      case TripleExprItem(te) => te match {
        case _:EachOf => true
        case _ => false
      }
      case _ => false
    }
    case OneOfCtx => item match {
      case TripleExprItem(te) => te match {
        case _:OneOf => true
        case _ => false
      }
      case _ => false
    }
    case TripleConstraintCtx => item match {
      case TripleExprItem(te) => te match {
        case _:TripleConstraint => true
        case _ => false
      }
      case _ => false
    }
  }

  def matchShapeExprId(lbl: ShapeLabel)(se: ShapeExpr): Boolean = se.id match {
    case None => false
    case Some(idLbl) => idLbl == lbl
  }

  def evaluateIndex(items: List[Item], index: ExprIndex): Comp[Value] = {
    val zero: Comp[Value] = Value(List()).pure[Comp]
    def cmb(item: Item, current: Comp[Value]): Comp[Value] = item match {
      case SchemaItem(s) => index match {
        case IntShapeIndex(idx) => Value(s.localShapes.slice(idx - 1,1).map(ShapeExprItem(_))).pure[Comp]
        case ShapeLabelIndex(lbl) => Value(s.localShapes.filter(matchShapeExprId(lbl)).map(ShapeExprItem(_))).pure[Comp]
        case _ => Value(List()).pure[Comp]
      }
      case ShapeExprItem(se) => index match {
        case IntShapeIndex(idx) => Value(List()).pure[Comp]
        case ShapeLabelIndex(lbl) => Value(List()).pure[Comp]
        case _ => Value(List()).pure[Comp]
      }
      case TripleExprItem(te) => Value(List()).pure[Comp]
    }
    items.foldRight(zero)(cmb)
  }

  def evaluateStep(s: Schema)(step: Step, current: Comp[Value]): Comp[Value] = step match {
    case es: ExprStep => {
      es.maybeContext match {
        case None => for {
          currentValue <- current
          value <- evaluateIndex(currentValue.items,es.exprIndex)
        } yield value
        case Some(ctx) => for {
          currentValue <- current
          (matched,unmatched) = currentValue.items.partition(checkContext(ctx))
          newValue <- evaluateIndex(matched,es.exprIndex)
          errors: List[ProcessingError] = unmatched.map(UnmatchItemContextLabel(_,step,ctx))
          _ <- errors.tell
        } yield newValue
      }
    }
  }
  def evaluateShapePath(p: ShapePath, s: Schema, v: Value): Comp[Value] = {
    val zero: Comp[Value] = if (p.startsWithRoot) {
      Value(s.localShapes.map(ShapeExprItem(_))).pure[Comp]
    } else v.pure[Comp]
    p.steps.foldRight(zero)(evaluateStep(s))
  }
}

sealed abstract class Step
case class ExprStep(maybeContext: Option[Context], exprIndex: ExprIndex) extends Step

sealed abstract class Context
case object ShapeAndCtx extends Context
case object ShapeOrCtx extends Context
case object ShapeNotCtx extends Context
case object NodeConstraintCtx extends Context
case object ShapeCtx extends Context
case object EachOfCtx extends Context
case object OneOfCtx extends Context
case object TripleConstraintCtx extends Context

sealed abstract class ExprIndex
sealed abstract class ShapeExprIndex extends ExprIndex
case class IntShapeIndex(v: Int) extends ShapeExprIndex
case class ShapeLabelIndex(lbl: ShapeLabel) extends ShapeExprIndex

sealed abstract class TripleExprIndex extends ExprIndex
case class IntTripleExprIndex(v: Int) extends TripleExprIndex
case class LabelTripleExprIndex(lbl: ShapeLabel, n: Option[Int]) extends TripleExprIndex

