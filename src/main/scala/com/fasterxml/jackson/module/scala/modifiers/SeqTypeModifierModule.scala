package com.fasterxml.jackson.module.scala.modifiers

import com.fasterxml.jackson.module.scala.JacksonModule

private object SeqTypeModifier extends CollectionLikeTypeModifier {
  val BASE = classOf[collection.Seq[Any]]
}

trait SeqTypeModifierModule extends JacksonModule {
  this += SeqTypeModifier
}