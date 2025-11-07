/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package model

import com.codahale.metrics.SharedMetricRegistries
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsResultException, Json }

import java.time.{ Instant, LocalDate }
import helpers.TestData.{ FIVE, TEST_ADDRESS_STRING, TEST_FORENAME, TEST_HONOURS, TEST_ID, TEST_LINE1, TEST_LINE2, TEST_LOCAL_DATE, TEST_SECOND_FORENAME, TEST_SUBJECT, TEST_SURNAME, TEST_TITLE, TWO }

class MessagesCountsSpec extends PlaySpec {

  "MessagesCounts.transformReadPreference" must {
    val counts = MessagesCounts(total = 3, unread = 1)

    "return a function to extract the total count when no read preference is provided" in {
      val none = MessagesCounts.transformReadPreference(None)(counts)
      val both = MessagesCounts.transformReadPreference(Some(ReadPreference.Both))(counts)
      none mustBe both
      none mustBe MessageCount(3)
    }

    "return a function to extract the count of unread messages when a `No` preference is provided" in {
      MessagesCounts.transformReadPreference(Some(ReadPreference.No))(counts) mustBe MessageCount(1)
    }

    "return a function to extract the count of read messages when a `Yes` preference is provided" in {
      MessagesCounts.transformReadPreference(Some(ReadPreference.Yes))(counts) mustBe MessageCount(2)
    }

    SharedMetricRegistries.clear()
  }

  "MessagesCounts.format" should {

    "read the json correctly" in new Setup {
      Json.parse(messagesCountsJsonString).as[MessagesCounts] mustBe messagesCounts
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(messagesCountsJsonStringInvalid).as[MessagesCounts]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(messagesCounts) mustBe Json.parse(messagesCountsJsonString)
    }
  }

  "MessagesWithCount.format" should {
    import MessagesWithCount.format

    "read the json correctly" in new Setup {
      val updatedItems: Seq[MessageListItem] = Seq(messageListItem.copy(readTime = None))

      Json
        .parse(messagesWithCountCustomReadTimeInstantJsonString)
        .as[MessagesWithCount] mustBe messagesWithCount.copy(items = updatedItems)
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(messagesWithCountJsonStringInvalid).as[MessagesWithCount]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(messagesWithCount) mustBe Json.parse(messagesWithCountJsonString)
    }
  }

  trait Setup {
    val readTime: Instant = Instant.parse("2025-11-01T23:30:00Z")

    val taxpayerName: TaxpayerName = TaxpayerName(
      title = Some(TEST_TITLE),
      forename = Some(TEST_FORENAME),
      secondForename = Some(TEST_SECOND_FORENAME),
      surname = Some(TEST_SURNAME),
      honours = Some(TEST_HONOURS),
      line1 = Some(TEST_LINE1),
      line2 = Some(TEST_LINE2)
    )

    val messageListItem: MessageListItem = MessageListItem(
      id = TEST_ID,
      subject = TEST_SUBJECT,
      validFrom = TEST_LOCAL_DATE,
      taxpayerName = Some(taxpayerName),
      readTime = Some(readTime),
      sentInError = false,
      replyTo = Some(TEST_ADDRESS_STRING),
      messageDesc = None,
      counter = Some(1),
      language = None
    )

    val messagesCounts: MessagesCounts = MessagesCounts(total = FIVE, unread = TWO)

    val messagesWithCount: MessagesWithCount =
      MessagesWithCount(items = Seq(messageListItem), count = messagesCounts)

    val messagesCountsJsonString: String = """{"total":5,"unread":2}""".stripMargin
    val messagesCountsJsonStringInvalid: String = """{"total":5}""".stripMargin

    val messagesWithCountJsonString: String =
      """{
        |"items":[
        |{"id":"test_id",
        |"subject":"test_subject",
        |"validFrom":"2025-11-01",
        |"taxpayerName":
        |{"title":"test_title",
        |"forename":"test_forename",
        |"secondForename":"test_second_fore_name",
        |"surname":"test_surname",
        |"honours":"test_honours",
        |"line1":"test_line1",
        |"line2":"test_line2"
        |},
        |"readTime":"2025-11-01T23:30:00Z",
        |"sentInError":false,
        |"replyTo":"test_address",
        |"counter":1
        |}],
        |"count":{"total":5,"unread":2}}""".stripMargin

    val messagesWithCountCustomReadTimeInstantJsonString: String =
      """{
        |"items":[
        |{"id":"test_id",
        |"subject":"test_subject",
        |"validFrom":"2025-11-01",
        |"taxpayerName":
        |{"title":"test_title",
        |"forename":"test_forename",
        |"secondForename":"test_second_fore_name",
        |"surname":"test_surname",
        |"honours":"test_honours",
        |"line1":"test_line1",
        |"line2":"test_line2"
        |},
        |"sentInError":false,
        |"replyTo":"test_address",
        |"counter":1
        |}],
        |"count":{"total":5,"unread":2}}""".stripMargin

    val messagesWithCountJsonStringInvalid: String =
      """{
        |"items":[
        |{"id":"test_id",
        |"validFrom":"2025-11-01",
        |"taxpayerName":
        |{"title":"test_title",
        |"forename":"test_forename",
        |"secondForename":"test_second_fore_name",
        |"surname":"test_surname",
        |"honours":"test_honours",
        |"line1":"test_line1",
        |"line2":"test_line2"
        |},
        |"readTime":"2025-11-01T23:30:00Z",
        |"sentInError":false,
        |"replyTo":"test_address",
        |"counter":1
        |}],
        |"count":{"total":5,"unread":2}}""".stripMargin
  }
}
