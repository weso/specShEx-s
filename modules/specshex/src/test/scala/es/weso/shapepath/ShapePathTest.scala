package es.weso.shapepath
import cats.effect.IO
import es.weso.rdf.nodes.IRI
import es.weso.shex.{IRILabel, Schema}
import org.scalatest._
import matchers.should._
import org.scalatest.funspec.AnyFunSpec

class ShapePathTest extends AnyFunSpec with Matchers {
  describe(s"ShapePath") {
    it(s"Evaluates a shapePath") {
      // /@<#IssueShape>/2
      val two: TripleExprIndex = IntTripleExprIndex(2)
      val sTwo: Step = ExprStep(None, two)
      val issueShape: Step = ExprStep(None, ShapeLabelIndex(IRILabel(IRI("#IssueShape"))))
      val path: ShapePath = ShapePath(true, List(issueShape))

      val schemaStr =
        s"""|prefix : <http://example.org/>
            |prefix foaf: <http://xmlns.com/foaf/0.1/>
            |prefix xsd: <http://www.w3.org/2001/XMLSchema#>
            |
            |<#IssueShape> {
            |  :name xsd:string MinLength 4;
            |  :category ["bug" "feature request"];
            |  :postedBy @<#UserShape>;
            |  :processing {
            |    :reproduced [true false];
            |    :priority xsd:integer
            |   }?
            | }
            |
            |<#UserShape> IRI /User\\?id=[0-9]+/ {
            | ( foaf:name xsd:string
            | | foaf:givenName . + ;
            |   foaf:familyName . ) ;
            | foaf:mbox IRI
            |}
            |""".stripMargin

      val s: IO[Schema] = for {
        schema <- Schema.fromString(schemaStr)
      } yield (schema)

      s.attempt.unsafeRunSync match {
        case Left(e) => fail(s"Error: $e")
        case Right(s) => {
          val (es,v) = ShapePath.evaluateShapePath(path,s,Value(List())).run
          es shouldBe empty
          info(s"Schema parsed:\n$s\nValue: $v")
        }
      }
    }
  }
}