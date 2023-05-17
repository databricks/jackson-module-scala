package com.fasterxml.jackson.module.scala.util

import collection.mutable.{ArrayBuffer, ListBuffer}
import collection.GenTraversable
import scala.collection.Factory
import scala.reflect.ClassTag

import scala.language.higherKinds

/**
 * The CompanionSorter performs a topological sort on class hierarchy to ensure that the most specific builders
 * get registered first.
 */
class CompanionSorter[CC[X] <: GenTraversable[X]] {

  private[this] val companions = new ArrayBuffer[(Class[_], Factory[_, CC[_]])]()

  def add[T[_] <: CC[_]](companion: Factory[_, CC[_]])
                        (implicit ct: ClassTag[T[_]]): CompanionSorter[CC] = {
    companions += ct.runtimeClass -> companion
    this
  }

  def toList: List[(Class[_], Factory[_, CC[_]])] = {
    val cs = companions.toArray
    val output = new ListBuffer[(Class[_], Factory[_, CC[_]])]()

    val remaining = cs.map(_ => 1)
    val adjMatrix = Array.ofDim[Int](cs.length, cs.length)

    // Build the adjacency matrix. Only mark the in-edges.
    for (i <- 0 until cs.length; j <- 0 until cs.length) {
      val (ic, _) = cs(i)
      val (jc, _) = cs(j)

      if (i != j && ic.isAssignableFrom(jc)) {
        adjMatrix(i)(j) = 1
      }
    }

    // While we haven't removed every node, remove all nodes with 0 degree in-edges.
    while (output.length < cs.length) {
      val startLength = output.length

      for (i <- 0 until cs.length) {
        if (remaining(i) == 1 && dotProduct(adjMatrix(i), remaining) == 0) {
          output += companions(i)
          remaining(i) = 0
        }
      }

      // If we couldn't remove any nodes, it means we've found a cycle. Realistically this should never happen.
      if (output.length == startLength) {
        throw new IllegalStateException("Companions contain a cycle.")
      }
    }

    output.toList
  }

  private[this] def dotProduct(a: Array[Int], b: Array[Int]): Int = {
    if (a.length != b.length) throw new IllegalArgumentException()

    (0 until a.length).map(i => a(i) * b(i)).sum
  }
}
