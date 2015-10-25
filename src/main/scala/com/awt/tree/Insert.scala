package com.awt.tree

import scalaz._
import scalaz.syntax.order._
import scalaz.Ordering._
import scalaz.syntax.either._

/**
  * User: Oleg
  * Date: 25-Oct-15
  * Time: 14:53
  */
sealed trait Insert[A, B] {
  type Higher

  def apply(elem: A, tree: B): B \/ Higher
}


object Insert {
  def apply[A, B, H](insert: (A, B) => B \/ H) = new Insert[A, B] {
    type Higher = H

    def apply(elem: A, tree: B) = insert(elem, tree)
  }
}

trait InsertImpl {
  implicit def insertUnit[A] = Insert[A, Unit, A] { (elem, _unit) => elem.right }


  implicit def insertLeaf[A: Order] =
    Insert[A, A, Tree23[A, A]] { (elem, tree) => elem cmp tree match {
      case EQ => elem.left
      case GT => Tree2(tree, elem, elem).right
      case LT => Tree2(elem, tree, tree).right
    }
    }

  implicit def insertNode[A: Order, B](implicit lower: Insert[A, B] {type Higher = Tree2[A, B]}) =
    Insert[A, Tree23[A, B], Tree23[A, Tree23[A, B]]] { (elem, tree) => tree match {
      case Tree2(ltree, rtree, sep) => elem cmp sep match {
        case LT => lower(elem, ltree) match {
          case -\/(lnew) => Tree2(lnew, rtree, sep).left
          case \/-(Tree2(lnew, mnew, sepnew)) => Tree3(lnew, mnew, rtree, sepnew, sep).left
        }
        case EQ | GT => lower(elem, rtree) match {
          case -\/(rnew) => Tree2(ltree, rnew, sep).left
          case \/-(Tree2(mnew, rnew, sepnew)) => Tree3(ltree, mnew, rnew, sep, sepnew).left
        }
      }
      case Tree3(ltree, mtree, rtree, lsep, rsep) => elem cmp lsep match {
        case LT => lower(elem, ltree) match {
          case -\/(lnew) => Tree3(lnew, mtree, rtree, lsep, rsep).left
          case \/-(tnew) => Tree2(tnew, Tree2(mtree, rtree, rsep), lsep).right
        }
        case EQ | GT => elem cmp rsep match {
          case LT => lower(elem, mtree) match {
            case -\/(mnew) => Tree3(ltree, mnew, rtree, lsep, rsep).left
            case \/-(Tree2(lnew, rnew, sepnew)) => Tree2(Tree2(ltree, lnew, lsep), Tree2(rnew, rtree, rsep), sepnew).right
          }
          case EQ | GT => lower(elem, rtree) match {
            case -\/(rnew) => Tree3(ltree, mtree, rnew, lsep, rsep).left
            case \/-(tnew) => Tree2(Tree2(ltree, mtree, lsep), tnew, rsep).right
          }
        }
      }
    }
    }
}
