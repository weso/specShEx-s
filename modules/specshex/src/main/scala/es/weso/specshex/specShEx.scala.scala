package es.weso.specshex

import fs2._
import es.weso.rdf.nodes.RDFNode
import es.weso.shex.Shape
import es.weso.rdf.RDFReader
import es.weso.shex.Schema
import es.weso.shapeMaps.ShapeMap
import cats.effect.IO
import es.weso.rdf.triples.RDFTriple
import es.weso.shex.ShapeExpr
import es.weso.shex.NodeConstraint


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

 // http://shex.io/shex-semantics/index.html#validation
 def isValid(g: RDFGraph, sch: Schema, ism: ShapeMap): IO[Boolean] = ???


 def completeTyping(g: RDFGraph, sch: Schema): IO[Typing] = for {
   depGraph <- dependencyGraph(sch)
   k = depGraph.maximalStronglyConnectedComponents
   sm <- completeTypingOn(k, g, sch)
 } yield sm
 
 def completeTypingOn(n: Int, g: RDFGraph, s: Schema): IO[Typing] = ???

 // http://shex.io/shex-semantics/index.html#shape-expression-semantics
 def satisfies(n: RDFNode, s: ShapeExpr, g: RDFGraph, sch: Schema, t: Typing): IO[Boolean] = s match {
     case nc: NodeConstraint => satisfies2(n,nc)
     case s: Shape => t.contains(n,s)

 }

 // http://shex.io/shex-semantics/index.html#node-constraint-semantics
 def satisfies2(n: RDFNode, nc: NodeConstraint): IO[Boolean] = for {
     checkNodeKind <- nodeSatisfies(n,nc.nodeKind)
 } yield ???

 def neigh(n: RDFNode, g: RDFGraph): IO[Set[RDFTriple]] = ???

 def matchesShape(n: RDFNode, s: Shape, g: RDFReader, sch: Schema, m: ShapeMap): IO[Boolean] = ???


 def dependencyGraph(sch: Schema): IO[Graph[Shape, PosNeg]] = ???
  
}
