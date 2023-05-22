package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.modifiers.SeqTypeModifierModule

import com.fasterxml.jackson.core.JsonParser

import com.fasterxml.jackson.databind.{BeanDescription, BeanProperty, JsonDeserializer, JavaType, DeserializationContext, DeserializationConfig}
import com.fasterxml.jackson.databind.deser.std.{ContainerDeserializerBase, CollectionDeserializer, StdValueInstantiator}
import com.fasterxml.jackson.databind.deser.{Deserializers, ValueInstantiator, ContextualDeserializer}
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.`type`.CollectionLikeType

import collection.Factory
import collection.immutable.Queue

import java.util.AbstractCollection
import scala.collection.mutable
import com.fasterxml.jackson.module.scala.util.CompanionSorter

private class BuilderWrapper[E](val builder: mutable.Builder[E, _ <: Iterable[E]]) extends AbstractCollection[E] {

  override def add(e: E) = { builder += e; true }

  // Required by AbstractCollection, but the deserializer doesn't care about them.
  def iterator() = null
  def size() = 0
}

private object SeqDeserializer {
  val COMPANIONS = new CompanionSorter[collection.Seq]()
    .add[collection.Seq](collection.Seq)
    .add[Seq](Seq)
    .add[IndexedSeq](IndexedSeq)
    .add[mutable.ArraySeq](mutable.ArraySeq)
    .add[mutable.Buffer](mutable.Buffer)
    .add[mutable.IndexedSeq](mutable.IndexedSeq)
//    .add(mutable.LinearSeq) Removed in Scala 2.13
    .add[mutable.ListBuffer](mutable.ListBuffer)
//    .add(mutable.MutableList) Removed in Scala 2.13
    .add[mutable.Queue](mutable.Queue)
//    .add(mutable.ResizableArray) Removed in Scala 2.13
    .add[Queue](Queue)
    .add[Stream](Stream)
    .add[LazyList](LazyList) // Added in Scala 2.13
    .toList

  def companionFor(cls: Class[_]): Factory[_, collection.Iterable[_]] = {
    val f: Option[Factory[_, collection.Seq[_]]] = COMPANIONS
      .find { _._1.isAssignableFrom(cls) }
      .map { _._2 }
    val i: Factory[_, Iterable[_]] = Iterable
    f.getOrElse(i)
  }

  def builderFor[A](cls: Class[_]): mutable.Builder[A,Iterable[A]] =
    companionFor(cls).newBuilder.asInstanceOf[mutable.Builder[A,Iterable[A]]]
}

private class SeqInstantiator(config: DeserializationConfig, valueType: Class[_])
  extends StdValueInstantiator(config, valueType) {

  override def canCreateUsingDefault = true

  override def createUsingDefault(ctxt: DeserializationContext) =
    new BuilderWrapper[AnyRef](SeqDeserializer.builderFor[AnyRef](valueType))  
}

private class SeqDeserializer(collectionType: JavaType, containerDeserializer: CollectionDeserializer)
  extends ContainerDeserializerBase[Iterable[_]](collectionType)
  with ContextualDeserializer {

  def this(collectionType: JavaType, valueDeser: JsonDeserializer[Object], valueTypeDeser: TypeDeserializer, valueInstantiator: ValueInstantiator) =
    this(collectionType, new CollectionDeserializer(collectionType, valueDeser, valueTypeDeser, valueInstantiator))

  def createContextual(ctxt: DeserializationContext, property: BeanProperty) = {
    val newDelegate = containerDeserializer.createContextual(ctxt, property)
    new SeqDeserializer(collectionType, newDelegate)
  }

  override def getContentType = containerDeserializer.getContentType

  override def getContentDeserializer = containerDeserializer.getContentDeserializer

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): Iterable[_] =
    containerDeserializer.deserialize(jp, ctxt) match {
      case wrapper: BuilderWrapper[_] => wrapper.builder.result()
    }
}

private object SeqDeserializerResolver extends Deserializers.Base {

  lazy final val SEQ = classOf[Iterable[_]]

  override def findCollectionLikeDeserializer(collectionType: CollectionLikeType,
                     config: DeserializationConfig,
                     beanDesc: BeanDescription,
                     elementTypeDeserializer: TypeDeserializer,
                     elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    val rawClass = collectionType.getRawClass

    if (!SEQ.isAssignableFrom(rawClass)) null
    else {
      val deser = elementDeserializer.asInstanceOf[JsonDeserializer[AnyRef]]
      val instantiator = new SeqInstantiator(config, rawClass)
      new SeqDeserializer(collectionType, deser, elementTypeDeserializer, instantiator)
    }
  }

}

trait SeqDeserializerModule extends SeqTypeModifierModule {
  this += (_ addDeserializers SeqDeserializerResolver)
}