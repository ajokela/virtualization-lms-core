package scala.virtualization.lms
package common

import java.io.PrintWriter
import scala.virtualization.lms.util.OverloadHack
import scala.virtualization.lms.internal.{CGenBase, CLikeCodegen, CudaGenBase, ScalaGenBase}

trait PrimitiveOps extends Variables with OverloadHack {
  this: ImplicitOps =>

  /**
   * Useful chaining implicits
   */
  implicit def intToRepDouble(i: Int) = unit(i.toDouble)

  /**
   * Conversions
   */
  implicit def repIntToRepDouble(x: Rep[Int]) = implicit_convert[Int,Double](x)
  implicit def repIntToRepFloat(x: Rep[Int]) = implicit_convert[Int,Float](x)
  implicit def repFloatToRepDbl(x: Rep[Float]) = implicit_convert[Float,Double](x)
  //implicit def repDblToRepFloat(x: Rep[Double]) = implicit_convert[Double,Float](x)

  /**
   *  Double
   */
  implicit def doubleToDoubleOpsCls(n: Double) = new DoubleOpsCls(n)
  implicit def repDoubleToDoubleOpsCls(n: Rep[Double]) = new DoubleOpsCls(n)
  implicit def varDoubleToDoubleOpsCls(n: Var[Double]) = new DoubleOpsCls(readVar(n))
  
  object Double {
    def parseDouble(s: Rep[String]) = obj_double_parse_double(s)
    def PositiveInfinity = obj_double_positive_infinity
  }

  class DoubleOpsCls(lhs: Rep[Double]){
    def floatValue() = double_float_value(lhs)
  }

  def obj_double_parse_double(s: Rep[String]): Rep[Double]
  def obj_double_positive_infinity: Rep[Double]
  def double_float_value(lhs: Rep[Double]): Rep[Float]

  /**
   * Int
   */

  object Integer {
    def parseInt(s: Rep[String]) = obj_integer_parse_int(s)
  }

  implicit def intToIntOpsCls(n: Int) = new IntOpsCls(n)
  implicit def repIntToIntOpsCls(n: Rep[Int]) = new IntOpsCls(n)
  implicit def varIntToIntOpsCls(n: Var[Int]) = new IntOpsCls(readVar(n))
    
  class IntOpsCls(lhs: Rep[Int]){
    // TODO (tiark): either of these cause scalac to crash        
    //def /[A](rhs: Rep[A])(implicit mA: Manifest[A], f: Fractional[A], o: Overloaded1) = int_divide_frac(lhs, rhs)
    //def /(rhs: Rep[Int]) = int_divide(lhs, rhs)
    def doubleValue() = int_double_value(lhs)
  }

  def infix_/(lhs: Rep[Int], rhs: Rep[Int]) = int_divide(lhs, rhs)
  def infix_%(lhs: Rep[Int], rhs: Rep[Int]) = int_mod(lhs, rhs)

  def obj_integer_parse_int(s: Rep[String]): Rep[Int]
  def int_divide_frac[A:Manifest:Fractional](lhs: Rep[Int], rhs: Rep[A]): Rep[A]
  def int_divide(lhs: Rep[Int], rhs: Rep[Int]): Rep[Int]
  def int_mod(lhs: Rep[Int], rhs: Rep[Int]): Rep[Int]
  def int_double_value(lhs: Rep[Int]): Rep[Double]
}

trait PrimitiveOpsExp extends PrimitiveOps with BaseExp {
  this: ImplicitOps =>

  /**
   * Double
   */
  case class ObjDoubleParseDouble(s: Exp[String]) extends Def[Double]
  case class ObjDoublePositiveInfinity() extends Def[Double]
  case class DoubleFloatValue(lhs: Exp[Double]) extends Def[Float]

  def obj_double_parse_double(s: Exp[String]) = ObjDoubleParseDouble(s)
  def obj_double_positive_infinity = ObjDoublePositiveInfinity()
  def double_float_value(lhs: Exp[Double]) = DoubleFloatValue(lhs)

  /**
   * Int
   */
  case class ObjIntegerParseInt(s: Exp[String]) extends Def[Int]
  case class IntDivideFrac[A:Manifest:Fractional](lhs: Exp[Int], rhs: Exp[A]) extends Def[A]
  case class IntDivide(lhs: Exp[Int], rhs: Exp[Int]) extends Def[Int]
  case class IntMod(lhs: Exp[Int], rhs: Exp[Int]) extends Def[Int]
  case class IntDoubleValue(lhs: Exp[Int]) extends Def[Double]

  def obj_integer_parse_int(s: Rep[String]) = ObjIntegerParseInt(s)
  def int_divide_frac[A:Manifest:Fractional](lhs: Exp[Int], rhs: Exp[A]) : Exp[A] = IntDivideFrac(lhs, rhs)
  def int_divide(lhs: Exp[Int], rhs: Exp[Int]) : Exp[Int] = IntDivide(lhs, rhs)
  def int_mod(lhs: Exp[Int], rhs: Exp[Int]) = IntMod(lhs, rhs)
  def int_double_value(lhs: Exp[Int]) = IntDoubleValue(lhs)
}

trait ScalaGenPrimitiveOps extends ScalaGenBase {
  val IR: PrimitiveOpsExp
  import IR._
  
  override def emitNode(sym: Sym[_], rhs: Def[_])(implicit stream: PrintWriter) = rhs match {
    case ObjDoubleParseDouble(s) => emitValDef(sym, "java.lang.Double.parseDouble(" + quote(s) + ")")
    case ObjDoublePositiveInfinity() => emitValDef(sym, "scala.Double.PositiveInfinity")
    case DoubleFloatValue(lhs) => emitValDef(sym, quote(lhs) + ".floatValue()")
    case ObjIntegerParseInt(s) => emitValDef(sym, "java.lang.Integer.parseInt(" + quote(s) + ")")
    case IntDivideFrac(lhs,rhs) => emitValDef(sym, quote(lhs) + " / " + quote(rhs))
    case IntDivide(lhs,rhs) => emitValDef(sym, quote(lhs) + " / " + quote(rhs))
    case IntMod(lhs,rhs) => emitValDef(sym, quote(lhs) + " % " + quote(rhs))
    case IntDoubleValue(lhs) => emitValDef(sym, quote(lhs) + ".doubleValue()")
    case _ => super.emitNode(sym, rhs)    
  }
}

trait CLikeGenPrimitiveOps extends CLikeCodegen {
  val IR: PrimitiveOpsExp
  import IR._

  //TODO: stdlib.h needs to be included in the common header file
  override def emitNode(sym: Sym[_], rhs: Def[_])(implicit stream: PrintWriter) = {
      rhs match {
        case ObjDoubleParseDouble(s) =>
          emitValDef(sym, "atof(" + quote(s) + ")")
        case _ => super.emitNode(sym, rhs)
      }
    }
}

trait CudaGenPrimitiveOps extends CudaGenBase with CLikeGenPrimitiveOps
trait CGenPrimitiveOps extends CGenBase with CLikeGenPrimitiveOps
