/**
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License.  You may obtain a copy of the License at
  *
  *    http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package kafka.etcd

import com.coreos.jetcd.data.ByteSequence
import com.coreos.jetcd.kv.TxnResponse
import org.apache.zookeeper.KeeperException
import org.apache.zookeeper.data.Stat


import scala.collection.JavaConverters._

import scala.util.Try

private[etcd] class ZkGetDataResponse(tryResponse: Try[TxnResponse]) extends ZkResult(tryResponse) {

  override def resultCode: KeeperException.Code = tryResponse.map { resp =>
    if (resp.isSucceeded) {
      KeeperException.Code.OK
    } else KeeperException.Code.NONODE
  }.recover(onError).get

  def data: Option[Array[Byte]] =
    tryResponse.map { resp =>
      if (resp.isSucceeded) {
        val data: Option[ByteSequence] = for {
          resp <- resp.getGetResponses.asScala.headOption
          kv <- resp.getKvs.asScala.headOption
        } yield kv.getValue

        val result = data.map(_.getBytes).get.map(_.toChar).mkString("")
        if (result == "[null]")
          None
        else data.map(_.getBytes)
      }
      else None
    }.getOrElse(None)

  def stat: Option[Stat] = {
    tryResponse.map { resp =>
      if (resp.isSucceeded) {
        val data: Option[Long] = for {
          resp <- resp.getGetResponses.asScala.headOption
          kv <- resp.getKvs.asScala.headOption
        } yield kv.getVersion
        val newStat = new Stat()
        newStat.setVersion(data.get.toInt)
        Option(newStat)
      }
      else None
    }.getOrElse(None)
  }
}
