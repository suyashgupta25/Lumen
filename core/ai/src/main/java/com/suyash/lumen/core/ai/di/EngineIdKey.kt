package com.suyash.lumen.core.ai.di

import com.suyash.lumen.core.ai.EngineId
import dagger.MapKey

@MapKey
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class EngineIdKey(val value: EngineId)
