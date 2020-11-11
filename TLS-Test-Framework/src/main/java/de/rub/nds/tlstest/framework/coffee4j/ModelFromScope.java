/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.rub.nds.tlstest.framework.coffee4j;

import de.rub.nds.tlstest.framework.constants.KeyExchangeType;
import de.rub.nds.tlstest.framework.model.DerivationType;
import de.rub.nds.tlstest.framework.model.ModelType;
import de.rwth.swc.coffee4j.junit.provider.model.ModelSource;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExecutionCondition;

/**
 * 
 *  This is an extended copy of the ModelFromMethod of Coffee4j.
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ModelSource(ScopeBasedProvider.class)
public @interface ModelFromScope {
    String name() default "TlsTest";
    ModelType baseModel() default ModelType.GENERIC;
    DerivationType[] scopeLimitations() default {};
    DerivationType[] scopeExtensions() default {};
    
    KeyExchangeType[] requiredKeyEx() default {KeyExchangeType.ALL12, KeyExchangeType.ALL13};
    boolean mergeSupportedWithClassSupported() default false;
    boolean requiresServerKeyExchMsg() default false;
}
