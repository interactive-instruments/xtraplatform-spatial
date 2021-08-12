/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql

import akka.NotUsed
import akka.stream.alpakka.slick.javadsl.SlickSession
import akka.stream.javadsl._
import slick.dbio.{DBIOAction, Effect, Streaming}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc._
import slick.sql.SqlStreamingAction

import java.util
import java.util.concurrent.CompletionStage
import java.util.function.{BiFunction => JBiFunction, Function => JFunction}
import java.util.{List => JList}
import scala.collection.JavaConverters._
import scala.collection.immutable.Vector
import scala.compat.java8.FutureConverters._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object SlickSql {

  def run[T](session: SlickSession,
             query: String,
             mapper: JFunction[PositionedResult, T]): CompletionStage[util.Collection[T]] = {
    implicit val ec: ExecutionContextExecutor = ExecutionContext.global
    session.db.run(SQLActionBuilder(query, SetParameter.SetUnit).as[T](toSlick(mapper))).map(v => v.asJavaCollection).toJava
  }

  def source[T](
                 session: SlickSession,
                 query: String,
                 mapper: JFunction[PositionedResult, T]
               ): Source[T, NotUsed] = {
    val streamingAction = SQLActionBuilder(query, SetParameter.SetUnit).as[T](toSlick(mapper))

    Source.fromPublisher(session.db.stream(streamingAction))
  }


  def source[T, R >: Null](
                            session: SlickSession,
                            executionContext: ExecutionContext,
                            toStatements: JList[JFunction[T, String]],
                            resultMapper: JBiFunction[SlickRow, R, R],
                            feature: T
                          ): Source[R, NotUsed] = {

    Source.fromPublisher(session.db.stream(toTransaction(feature, executionContext, toStatements, resultMapper)))
  }

  def toTransaction[T, R >: Null](feature: T,
                                  executionContext: ExecutionContext,
                                  toStatements: JList[JFunction[T, String]],
                                  mapper: JBiFunction[SlickRow, R, R]
                                 ): DBIOAction[Vector[R], Streaming[R], Effect with Effect.Transactional] = {
    implicit val ec: ExecutionContext = executionContext

    var streamingAction: DBIOAction[Vector[R], Streaming[R], Effect with Effect] = toDbio(toStatements.get(0).apply(feature), mapper, null)

    for (i <- 1 until toStatements.size()) {
      streamingAction = streamingAction.flatMap(results => {
        val statement = toStatements.get(i).apply(feature)
        var nextAction: DBIOAction[Vector[R], Streaming[R], Effect] = toDbio("SELECT null", mapper, results)

        if (statement != null) {
          nextAction = toDbio(statement, mapper, results)
        }
        nextAction
      })
    }

    streamingAction.transactionally
  }

  private def toDbio[T >: Null](query: String, mapper: JBiFunction[SlickRow, T, T], results: Vector[T]): SqlStreamingAction[Vector[T], T, Effect] =
    if (results != null && results.nonEmpty) sql"#$query".as[T](toSlick(mapper, results.head)) else sql"#$query".as[T](toSlick(mapper))


  private def toSlick[T >: Null](mapper: JBiFunction[SlickRow, T, T], results: T): GetResult[T] =
    GetResult(pr => mapper(new SlickRow(pr), if (results.isInstanceOf[java.lang.Integer]) null else results))

  private def toSlick[T >: Null](mapper: JBiFunction[SlickRow, T, T]): GetResult[T] =
    GetResult(pr => mapper(new SlickRow(pr), null))

  private def toSlick[T](mapper: JFunction[PositionedResult, T]): GetResult[T] =
    GetResult(pr => mapper(pr))

  //TODO: remove
  final class SlickRow(delegate: PositionedResult) {
    def nextBoolean(): java.lang.Boolean = delegate.nextBoolean()

    def nextBigDecimal(): java.math.BigDecimal = delegate.nextBigDecimal().bigDecimal

    def nextBlob(): java.sql.Blob = delegate.nextBlob()

    def nextByte(): java.lang.Byte = delegate.nextByte()

    //def nextBytes(): Array[java.lang.Byte] = delegate.nextBytes()

    def nextBytes(): Array[Byte] = delegate.nextBytes()

    def nextClob(): java.sql.Clob = delegate.nextClob()

    def nextDate(): java.sql.Date = delegate.nextDate()

    def nextDouble(): java.lang.Double = delegate.nextDouble()

    def nextFloat(): java.lang.Float = delegate.nextFloat()

    def nextInt(): java.lang.Integer = delegate.nextInt()

    def nextLong(): java.lang.Long = delegate.nextLong()

    def nextObject(): java.lang.Object = delegate.nextObject()

    def nextShort(): java.lang.Short = delegate.nextShort()

    def nextString(): java.lang.String = delegate.nextString()

    def nextTime(): java.sql.Time = delegate.nextTime()

    def nextTimestamp(): java.sql.Timestamp = delegate.nextTimestamp()
  }

}