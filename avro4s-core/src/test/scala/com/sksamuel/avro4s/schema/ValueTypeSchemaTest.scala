package com.sksamuel.avro4s.schema

import com.sksamuel.avro4s.AvroSchema
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ValueTypeSchemaTest extends AnyWordSpec with Matchers {

  "SchemaEncoder" should {
    "support value class at the top level" in {
      val schema = AvroSchema[ValueClass]
      val expected = new org.apache.avro.Schema.Parser().parse(getClass.getResourceAsStream("/value_class_top_level.json"))
      schema.toString(true) shouldBe expected.toString(true)
    }
    "support value class as a nested field" in {
      case class Wibble(value: ValueClass)
      val schema = AvroSchema[Wibble]
      val expected = new org.apache.avro.Schema.Parser().parse(getClass.getResourceAsStream("/value_class_nested.json"))
      schema.toString(true) shouldBe expected.toString(true)
    }
  }
}

case class ValueClass(str: String) extends AnyVal
