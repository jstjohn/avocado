/*
 * Licensed to Big Data Genomics (BDG) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The BDG licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bdgenomics.avocado.algorithms.mutect

import org.bdgenomics.avocado.models.AlleleObservation
import org.bdgenomics.adam.util.PhredUtils.phredToErrorProbability
import scala.math._

trait LikelihoodModel {
  def logLikelihood(ref: String,
                    alt: String,
                    obs: Iterable[AlleleObservation],
                    f: Option[Double]): Double
}

case class LogOdds(m1: LikelihoodModel, m2: LikelihoodModel) {

  def logOdds(ref: String, alt: String,
              obs: Iterable[AlleleObservation],
              f: Option[Double]): Double =
    m1.logLikelihood(ref, alt, obs, f) - m2.logLikelihood(ref, alt, obs, f)
}

object MutectLogOdds extends LogOdds(MfmModel, M0Model) {
}

/**
 * Use for the log odds that a normal is not a heterozygous site
 */
object MutectSomaticLogOdds extends LogOdds(M0Model, MHModel) {
}

object M0Model extends LikelihoodModel {

  def logLikelihood(ref: String,
                    alt: String,
                    obs: Iterable[AlleleObservation],
                    f: Option[Double]): Double =
    MfmModel.logLikelihood(ref, alt, obs, Some(0.0))
}

/**
 * M_{m, 0.5} -- probability of a heterozygous site
 */
object MHModel extends LikelihoodModel {

  def logLikelihood(ref: String,
                    alt: String,
                    obs: Iterable[AlleleObservation],
                    f: Option[Double]): Double =
    MfmModel.logLikelihood(ref, alt, obs, Some(0.5))
}

/**
 * M_{m, f}
 */
object MfmModel extends LikelihoodModel {

  def P_bi(obs: AlleleObservation, r: String, m: String, f: Double): Double = {
    val ei = phredToErrorProbability(obs.phred)

    if (obs.allele == r) {
      f * (ei / 3.0) + (1.0 - f) * (1.0 - ei)
    } else if (obs.allele == m) {
      f * (1.0 - ei) + (1.0 - f) * (ei / 3.0)
    } else {
      ei / 3.0
    }
  }

  def logLikelihood(ref: String, alt: String,
                    obs: Iterable[AlleleObservation],
                    f: Option[Double]): Double = {
    val fEstimate: Double = f.getOrElse(obs.count(_.allele == alt).toDouble / obs.size)
    obs.map { ob => log10(P_bi(ob, ref, alt, fEstimate)) }.sum
  }
}

