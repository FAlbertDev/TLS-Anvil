package de.rub.nds.tlstest.suite.tests.server.tls12.rfc5246;

import de.rub.nds.modifiablevariable.util.Modifiable;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.AlertDescription;
import de.rub.nds.tlsattacker.core.constants.AlertLevel;
import de.rub.nds.tlsattacker.core.constants.ProtocolMessageType;
import de.rub.nds.tlsattacker.core.protocol.message.*;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveTillAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlstest.framework.Validator;
import de.rub.nds.tlstest.framework.annotations.KeyExchange;
import de.rub.nds.tlstest.framework.annotations.RFC;
import de.rub.nds.tlstest.framework.annotations.ServerTest;
import de.rub.nds.tlstest.framework.annotations.TlsTest;
import de.rub.nds.tlstest.framework.constants.KeyExchangeType;
import de.rub.nds.tlstest.framework.constants.SeverityLevel;
import de.rub.nds.tlstest.framework.constants.TestStatus;
import de.rub.nds.tlstest.framework.execution.AnnotatedStateContainer;
import de.rub.nds.tlstest.framework.execution.WorkflowRunner;
import de.rub.nds.tlstest.framework.testClasses.Tls12Test;

import static org.junit.Assert.assertTrue;

@ServerTest
public class AlertProtocol extends Tls12Test {

    @TlsTest(description = "Unless some other fatal alert has been transmitted, each party is " +
            "required to send a close_notify alert before closing the write side " +
            "of the connection. The other party MUST respond with a close_notify " +
            "alert of its own and close down the connection immediately, " +
            "discarding any pending writes.")
    @RFC(number = 5264, section = "7.2.1 Closure Alerts")
    @KeyExchange(supported = KeyExchangeType.ALL12)
    public void close_notify(WorkflowRunner runner) {
        Config c = context.getConfig().createConfig();
        runner.replaceSupportedCiphersuites = true;

        AlertMessage alert = new AlertMessage();
        alert.setLevel(Modifiable.explicit(AlertLevel.WARNING.getValue()));
        alert.setDescription(Modifiable.explicit(AlertDescription.CLOSE_NOTIFY.getValue()));

        WorkflowTrace workflowTrace = new WorkflowTrace();
        workflowTrace.addTlsActions(
                new SendAction(new ClientHelloMessage(c)),
                new ReceiveTillAction(new ServerHelloDoneMessage()),
                new SendAction(alert),
                new ReceiveAction(new AlertMessage())
        );

        runner.execute(workflowTrace, c).validateFinal(i -> {
            WorkflowTrace trace = i.getWorkflowTrace();
            assertTrue(trace.smartExecutedAsPlanned());

            AlertMessage message = trace.getLastReceivedMessage(AlertMessage.class);
            if (message == null) {
                i.addAdditionalResultInfo("No close_notify alert received.");
                i.setStatus(TestStatus.PARTIALLY_SUCCEEDED);
                return;
            }
            Validator.receivedWarningAlert(i);
            Validator.testAlertDescription(i, AlertDescription.CLOSE_NOTIFY, message);

        });
    }



    @TlsTest(description = "Thus, any connection terminated with a fatal alert MUST NOT be resumed.", securitySeverity = SeverityLevel.CRITICAL)
    @RFC(number = 5264, section = "7.2.2 Error Alerts")
    @KeyExchange(supported = KeyExchangeType.ALL12)
    public void abortAfterFatalAlert(WorkflowRunner runner) {
        Config c = context.getConfig().createConfig();
        runner.replaceSupportedCiphersuites = true;
        runner.generateWorkflowTraceUntilSendingMessage(WorkflowTraceType.HANDSHAKE, ProtocolMessageType.CHANGE_CIPHER_SPEC);

        AnnotatedStateContainer container = new AnnotatedStateContainer();
        for (AlertDescription i : AlertDescription.values()) {
            AlertMessage alert = new AlertMessage();
            alert.setLevel(Modifiable.explicit(AlertLevel.FATAL.getValue()));
            alert.setDescription(Modifiable.explicit(i.getValue()));

            WorkflowTrace workflowTrace = new WorkflowTrace();
            workflowTrace.addTlsActions(
                    new SendAction(alert),
                    new SendAction(new ChangeCipherSpecMessage()),
                    new SendAction(new FinishedMessage()),
                    new ReceiveAction(new AlertMessage())
            );

            container.addAll(runner.prepare(workflowTrace, c));
        }


        runner.execute(container).validateFinal(Validator::receivedFatalAlert);
    }
}
