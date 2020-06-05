package de.rub.nds.tlstest.suite.tests.both.tls12.rfc5246;

import de.rub.nds.modifiablevariable.util.Modifiable;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.AlertDescription;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.protocol.message.AlertMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ApplicationMessage;
import de.rub.nds.tlsattacker.core.record.Record;
import de.rub.nds.tlsattacker.core.record.RecordCryptoComputations;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlstest.framework.Validator;
import de.rub.nds.tlstest.framework.annotations.*;
import de.rub.nds.tlstest.framework.constants.KeyExchangeType;
import de.rub.nds.tlstest.framework.constants.SeverityLevel;
import de.rub.nds.tlstest.framework.execution.WorkflowRunner;
import de.rub.nds.tlstest.framework.testClasses.Tls12Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;


@RFC(number = 5264, section = "6.2.3.2 CBC Block Cipher")
@ServerTest
public class CBCBlockCipher extends Tls12Test {

    private ConditionEvaluationResult supportsCBCCipherSuites() {
        List<CipherSuite> suites = new ArrayList<>(context.getConfig().getSiteReport().getCipherSuites());
        suites.removeIf(i -> !i.isCBC());
        if (suites.size() > 0) {
            return ConditionEvaluationResult.enabled("");
        }
        return ConditionEvaluationResult.disabled("No CBC Suites are supported");
    }

    @TlsTest(description = "Each uint8 in the padding data " +
            "vector MUST be filled with the padding length value. The receiver " +
            "MUST check this padding and MUST use the bad_record_mac alert to " +
            "indicate padding errors.", securitySeverity = SeverityLevel.CRITICAL)
    @KeyExchange(supported = KeyExchangeType.ALL12)
    @MethodCondition(method = "supportsCBCCipherSuites")
    public void invalidCBCPadding(WorkflowRunner runner) {
        Config c = context.getConfig().createConfig();
        runner.replaceSupportedCiphersuites = true;
        runner.respectConfigSupportedCiphersuites = true;

        List<CipherSuite> suites = CipherSuite.getImplemented();
        suites.removeIf(i -> !i.isCBC());
        c.setDefaultClientSupportedCiphersuites(suites);

        Record record = new Record();
        record.setComputations(new RecordCryptoComputations());
        record.getComputations().setPadding(Modifiable.xor(new byte[]{0x01}, 0));

        ApplicationMessage appData = new ApplicationMessage();
        appData.setData(Modifiable.explicit("test".getBytes()));

        SendAction sendAction = new SendAction(appData);
        sendAction.setRecords(record);

        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HANDSHAKE);
        workflowTrace.addTlsActions(
                sendAction,
                new ReceiveAction(new AlertMessage())
        );

        runner.execute(workflowTrace, c).validateFinal(i -> {
            WorkflowTrace trace = i.getWorkflowTrace();
            assertTrue(trace.executedAsPlanned());

            AlertMessage msg = trace.getFirstReceivedMessage(AlertMessage.class);
            Validator.testAlertDescription(i, AlertDescription.BAD_RECORD_MAC, msg);
        });

    }

    @TlsTest(description = "The padding length MUST be such that the total size of the " +
            "GenericBlockCipher structure is a multiple of the cipher’s block " +
            "length. Legal values range from zero to 255, inclusive. This " +
            "length specifies the length of the padding field exclusive of the " +
            "padding_length field itself.", securitySeverity = SeverityLevel.MEDIUM)
    @KeyExchange(supported = KeyExchangeType.ALL12)
    @MethodCondition(clazz = CBCBlockCipher.class, method = "supportsCBCCipherSuites")
    public void invalidPaddingLength(WorkflowRunner runner) {
        Config c = context.getConfig().createConfig();
        runner.replaceSupportedCiphersuites = true;
        runner.respectConfigSupportedCiphersuites = true;
        c.setHighestProtocolVersion(ProtocolVersion.TLS12);

        List<CipherSuite> suites = CipherSuite.getImplemented();
        suites.removeIf(i -> !i.isCBC());
        c.setDefaultClientSupportedCiphersuites(suites);

        ApplicationMessage applicationMessage = new ApplicationMessage();
        applicationMessage.setData("test".getBytes());

        Record record = new Record();
        record.setComputations(new RecordCryptoComputations());
        //record.getComputations().setAdditionalPaddingLength(Modifiable.add(21));

        SendAction sendAction = new SendAction(applicationMessage);
        sendAction.setRecords(record);

        WorkflowTrace workflowTrace = runner.generateWorkflowTrace(WorkflowTraceType.HANDSHAKE);
        workflowTrace.addTlsActions(
                sendAction,
                new ReceiveAction(new AlertMessage())
        );

        runner.execute(workflowTrace, c).validateFinal(i -> {
            WorkflowTrace trace = i.getWorkflowTrace();
            assertTrue(trace.executedAsPlanned());

            AlertMessage msg = trace.getFirstReceivedMessage(AlertMessage.class);
            Validator.testAlertDescription(i, AlertDescription.BAD_RECORD_MAC, msg);
        });
    }


}
