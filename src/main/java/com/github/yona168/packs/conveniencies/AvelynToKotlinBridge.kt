package com.github.yona168.packs.conveniencies

import com.gitlab.avelyn.architecture.base.Component
import com.gitlab.avelyn.architecture.base.Parent
import com.gitlab.avelyn.architecture.base.Toggleable
import com.gitlab.avelyn.core.base.Events
import com.gitlab.avelyn.core.components.ComponentPlugin
import org.bukkit.event.Event


fun ComponentPlugin.onEnable(function: () -> Unit): ComponentPlugin =this.onEnable(Runnable{function()})
fun Component.onEnable(function: () -> Unit): Component = this.onEnable(Runnable { function() })
fun Component.onDisable(function: ()->Unit):Component=this.onDisable(Runnable{function()})
infix fun Parent<Toggleable>.ktAddChild(o: Toggleable): Toggleable = this.addChild(o)
inline fun <reified Type : Event> myListen(crossinline cons: (Type) -> Unit): Toggleable = Events.listen(Type::class.java) { event: Type -> cons(event) }