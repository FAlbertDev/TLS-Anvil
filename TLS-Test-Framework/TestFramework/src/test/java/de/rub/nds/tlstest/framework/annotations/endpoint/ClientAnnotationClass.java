/**
 * TLS-Test-Framework - A framework for modeling TLS tests
 *
 * Copyright 2020 Ruhr University Bochum and
 * TÜV Informationstechnik GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.framework.annotations.endpoint;

import de.rub.nds.tlstest.framework.TestContext;
import de.rub.nds.tlstest.framework.annotations.ClientTest;
import de.rub.nds.tlstest.framework.annotations.ServerTest;
import de.rub.nds.tlstest.framework.annotations.TlsTest;
import de.rub.nds.tlstest.framework.junitExtensions.EndpointCondition;
import de.rub.nds.tlstest.framework.utils.ConditionTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;


@ClientTest
public class ClientAnnotationClass {

    @RegisterExtension
    static ConditionTest ext = new ConditionTest(EndpointCondition.class);

    @BeforeAll
    static void setup() {
        TestContext testContext = new TestContext();
        testContext.getConfig().parse(new String[]{ "client", "-port", "443" });
    }

    @ClientTest
    public void execute_supportedForConfig() { }

    @TlsTest
    public void execute_inheritedClassAnnotation() { }

    @ServerTest
    public void not_execute_unsupportedModeForConfig() { }

}

