package es.weso.specshex

import fs2._
import es.weso.rdf.nodes.{IRI, RDFNode}
import es.weso.shex.{NodeConstraint, NodeKind, NumericFacet, Schema, Shape, ShapeAnd, ShapeExpr, ShapeOr, StringFacet, ValueSetValue, XsFacet}
import es.weso.rdf.RDFReader
import es.weso.shapeMaps.ShapeMap
import cats.effect.IO
import es.weso.rdf.triples.RDFTriple


// Tools
class Graph[V,E] {
    def maximalStronglyConnectedComponents: Int = ???
}

sealed abstract class PosNeg 
case object Pos extends PosNeg
case object Neg extends PosNeg


case class Typing(tm: Map[RDFNode,Shape]) {
    def contains(n: RDFNode, s: Shape): IO[Boolean] = ???
}

object SpecShEx {

 // RDF Graph 
 type RDFGraph = RDFReader

 // Computation type
 type Comp[A] = IO[A]

 def choose[A](ls: List[A]): Comp[A] = ???
 def checkAll[A,B](ls:List[A], check: A => Comp[B]): Comp[B] = ???

 // Graph management tools
 def neigh(n: RDFNode, g: RDFGraph): Comp[LazyList[RDFTriple]] =
   g.triplesWithSubject(n).compile.to(LazyList)

 def matchesShape(n: RDFNode, s: Shape, g: RDFReader, sch: Schema, m: ShapeMap): IO[Boolean] = ???

 def dependencyGraph(sch: Schema): IO[Graph[Shape, PosNeg]] = ???

  // 5.2
 // http://shex.io/shex-semantics/index.html#validation
 def isValid(g: RDFGraph, sch: Schema, ism: ShapeMap): Comp[Boolean] = ???

 def completeTyping(g: RDFGraph, sch: Schema): IO[Typing] = for {
   depGraph <- dependencyGraph(sch)
   k = depGraph.maximalStronglyConnectedComponents
   sm <- completeTypingOn(k, g, sch)
 } yield sm
 
 def completeTypingOn(n: Int, g: RDFGraph, s: Schema): Comp[Typing] = ???

 // 5.3 Shape Expressions
 // 5.3.2 Shape expression semantics
 // http://shex.io/shex-semantics/index.html#shape-expression-semantics
 def satisfies(n: RDFNode, s: ShapeExpr, g: RDFGraph, sch: Schema, t: Typing): Comp[Boolean] = s match {
     case nc: NodeConstraint => satisfies2(n,nc)
     case s: Shape => t.contains(n,s)
     case so: ShapeOr => for {
       se2 <- choose(so.shapeExprs)
       v <- satisfies(n,se2,g,sch,t)
     } yield v
     case sa: ShapeAnd => checkAll(sa.shapeExprs, satisfies(n,_,g,sch,t))
 }

 // 5.4 Node constraints
 // http://shex.io/shex-semantics/index.html#node-constraint-semantics
 // 5.4.1 http://shex.io/shex-semantics/#node-constraint-semantics
 def satisfies2(n: RDFNode, nc: NodeConstraint): Comp[Boolean] =
   every(n, List(
     (nodeSatisfies_NodeKind _, nc.nodeKind),
  //   (nodeSatisfies_Datatype _, nc.datatype),
  //   (nodeSatisfies_Values _, nc.values)
   ))

 def every[A,B](x: A, ls: List[((A, B) => Comp[Boolean], Option[B])]): Comp[Boolean] = ???

 // 5.4.2 http://shex.io/shex-semantics/#nodeKind
 def nodeSatisfies_NodeKind(node: RDFNode, nc: NodeKind): Comp[Boolean] = ???

 // 5.4.3 http://shex.io/shex-semantics/#datatype
 def nodeSatisfies_Datatype(node: RDFNode, datatype: IRI): Comp[Boolean] = ???

 def nodeSatisfies_Facet(node: RDFNode, facet: XsFacet): Comp[Boolean] = ???

 // 5.4.4 http://shex.io/shex-semantics/#xs-string
 def nodeSatisfies_StringFacet(node: RDFNode, facet: StringFacet): Comp[Boolean] = ???

 // 5.4.5 http://shex.io/shex-semantics/#xs-numeric
 def nodeSatisfies_NumericFacet(node: RDFNode, facet: NumericFacet): Comp[Boolean] = ???

 // 5.4.6 http://shex.io/shex-semantics/#values
 def nodeSatisfies_Values(node: RDFNode, values: List[ValueSetValue]): Comp[Boolean] = ???
}
