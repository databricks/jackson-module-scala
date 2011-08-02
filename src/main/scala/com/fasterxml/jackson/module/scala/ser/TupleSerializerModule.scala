package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.module.scala.JacksonModule

import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.JsonGenerator
import org.codehaus.jackson.map.{BeanProperty, SerializerProvider, TypeSerializer, BeanDescription, SerializationConfig, Serializers}
import org.codehaus.jackson.map.ser.ContainerSerializerBase
import org.codehaus.jackson.schema.SchemaAware

import java.lang.reflect.Type

private class TupleSerializer(javaType: JavaType, property: BeanProperty)
  extends ContainerSerializerBase[Product](classOf[Product]) {

  def serialize(value: Product, jgen: JsonGenerator, provider: SerializerProvider)
  {
    jgen.writeStartArray()
    value.productIterator.foreach { item =>
      val itemRef = item.asInstanceOf[AnyRef]
      val itemType = itemRef.getClass
      val ser = provider.findValueSerializer(itemType, property)
      ser.serialize(itemRef, jgen, provider)
    }
    jgen.writeEndArray()
  }

  def getSchema(provider: SerializerProvider, typeHint: Type) = {
    val o = createSchemaNode("array")
    val a = o.putArray("items")

    (for (i <- 0 to javaType.containedTypeCount()) yield {
        val eltType = javaType.containedType(i)
        provider.findValueSerializer(eltType, property) match {
          case ser: SchemaAware => ser.getSchema(provider, eltType.getRawClass)
          case _ => { createSchemaNode("any") }
        }
    }).foreach(a.add(_))

    o
  }

  def _withValueTypeSerializer(newVts: TypeSerializer) = this
}

private object TupleSerializerResolver extends Serializers.None {

  private val PRODUCT = classOf[Product]

  override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription, beanProperty: BeanProperty) = {
    if (!PRODUCT.isAssignableFrom(javaType.getRawClass)) null else
    new TupleSerializer(javaType, beanProperty)
  }

}

trait TupleSerializerModule {
  self: JacksonModule =>

  this += TupleSerializerResolver
}