/**
 * Licensed to Big Data Genomics (BDG) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The BDG licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bdgenomics.avocado.algorithms.hmm

object AlignmentState extends Enumeration {
  type AlignmentState = Value
  val Padding, Insertion, Match, Mismatch, Deletion = Value

  def isIndel(state: AlignmentState): Boolean = {
    state match {
      case Insertion | Deletion => true
      case _                    => false
    }
  }
}

class Alignment(val likelihood: Double, val prior: Double, val alignedReference: String, val alignedSequence: String, val alignmentStateSequence: String, val hasVariants: Boolean = false) {

  lazy val alignment = generateCompressedStateSequence(alignmentStateSequence)

  /**
   * Performs a run-length encoding on the alignment state sequence
   */
  def generateCompressedStateSequence(unitAlignment: String): List[(Int, Char)] = {
    if (unitAlignment.size == 0) return List[(Int, Char)]()

    val (run, tail) = unitAlignment.span(_ == unitAlignment.head)
    (run.size, unitAlignment.head) :: generateCompressedStateSequence(tail)
  }

  override def toString(): String = {
    alignedReference + ", " + alignedSequence + ", " + alignmentStateSequence + ", " + likelihood + ", " + prior
  }
}
