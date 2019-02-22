/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.ii.xtraplatform.feature.provider.sql

import java.util.function.{Function => JFunction, BiFunction => JBiFunction}
import java.util.{List => JList}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.alpakka.slick.javadsl.{SlickSession}
import akka.stream.alpakka.slick.scaladsl.{Slick => ScalaSlick}
import akka.stream.javadsl._
import slick.dbio.{DBIOAction, Effect, Streaming}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc._
import slick.sql.SqlStreamingAction

import scala.collection.immutable.Vector
import scala.concurrent.ExecutionContext

object SlickSql {


  def source[T >: Null,U](
                 session: SlickSession,
                 queryFunctions: JList[JFunction[U,String]],
                 mapper: JBiFunction[SlickRow, T, T],
                 insertContext: U,
                 actorSystem: ActorSystem
               ): Source[T, NotUsed] = {


    implicit val ec: ExecutionContext = actorSystem.dispatcher


    val insert1: SqlStreamingAction[Vector[T], T, Effect] = toDbio(queryFunctions.get(0).apply(insertContext), mapper, null)

    var streamingAction: DBIOAction[Vector[T], Streaming[T], Effect with Effect] = insert1.flatMap(results => toDbio(queryFunctions.get(1).apply(insertContext), mapper, results))

    for (i <- 2 until queryFunctions.size()) {
      streamingAction = streamingAction.flatMap(results => toDbio(queryFunctions.get(i).apply(insertContext), mapper, results))
    }

    //val transactionalStreamingAction = PostgresProfile.api.jdbcActionExtensionMethods(streamingAction).transactionally
    val transactionalStreamingAction = streamingAction.transactionally

    ScalaSlick
      .source[T](transactionalStreamingAction)(session)
      .asJava
  }

  private def toDbio[T >: Null](query: String, mapper: JBiFunction[SlickRow, T, T], results: Vector[T]): SqlStreamingAction[Vector[T], T, Effect] =
    if (results != null) sql"#$query".as[T](toSlick(mapper,  results.head)) else sql"#$query".as[T](toSlick(mapper))


  private def toSlick[T >: Null](mapper: JBiFunction[SlickRow, T, T], results: T): GetResult[T] =
    GetResult(pr => mapper(new SlickRow(pr), if (results.isInstanceOf[java.lang.Integer]) null else results))

  private def toSlick[T >: Null](mapper: JBiFunction[SlickRow, T, T]): GetResult[T] =
    GetResult(pr => mapper(new SlickRow(pr), null))

  final class SlickRow (delegate: PositionedResult) {
    final def nextBoolean(): java.lang.Boolean = delegate.nextBoolean()
    final def nextBigDecimal(): java.math.BigDecimal = delegate.nextBigDecimal().bigDecimal
    final def nextBlob(): java.sql.Blob = delegate.nextBlob()
    final def nextByte(): java.lang.Byte = delegate.nextByte()
    final def nextBytes(): Array[java.lang.Byte] = delegate.nextBytes().map(Byte.box(_))
    final def nextClob(): java.sql.Clob = delegate.nextClob()
    final def nextDate(): java.sql.Date = delegate.nextDate()
    final def nextDouble(): java.lang.Double = delegate.nextDouble()
    final def nextFloat(): java.lang.Float = delegate.nextFloat()
    final def nextInt(): java.lang.Integer = delegate.nextInt()
    final def nextLong(): java.lang.Long = delegate.nextLong()
    final def nextObject(): java.lang.Object = delegate.nextObject()
    final def nextShort(): java.lang.Short = delegate.nextShort()
    final def nextString(): java.lang.String = delegate.nextString()
    final def nextTime(): java.sql.Time = delegate.nextTime()
    final def nextTimestamp(): java.sql.Timestamp = delegate.nextTimestamp()
  }
}