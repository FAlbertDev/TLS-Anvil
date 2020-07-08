package de.rub.nds.tlstest.suite.tests.both;

import de.rub.nds.modifiablevariable.ModifiableVariableProperty;
import de.rub.nds.modifiablevariable.integer.ModifiableInteger;
import de.rub.nds.modifiablevariable.singlebyte.ModifiableByte;
import de.rub.nds.modifiablevariable.util.Modifiable;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.constants.RunningModeType;
import de.rub.nds.tlsattacker.core.protocol.ModifiableVariableHolder;
import de.rub.nds.tlsattacker.core.protocol.message.ProtocolMessage;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowConfigurationFactory;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlstest.framework.annotations.ClientTest;
import de.rub.nds.tlstest.framework.annotations.KeyExchange;
import de.rub.nds.tlstest.framework.annotations.ServerTest;
import de.rub.nds.tlstest.framework.annotations.TlsTest;
import de.rub.nds.tlstest.framework.annotations.TlsVersion;
import de.rub.nds.tlstest.framework.constants.KeyExchangeType;
import de.rub.nds.tlstest.framework.execution.AnnotatedState;
import de.rub.nds.tlstest.framework.execution.AnnotatedStateContainer;
import de.rub.nds.tlstest.framework.execution.WorkflowRunner;
import de.rub.nds.tlstest.framework.testClasses.TlsGenericTest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;


/**
 * Describes tests that execute complete handshake workflows each one modifying a different length field.
 * Note: Some length fields are only relevant in specific conditions (e.g. TLS protocol versions).
 *   thus, some TLS-Handshaked sent by this test will succeed. To evaluate these test cases,
 *   it is necessary to execute the test against different versions of a single TLS implemenation or
 *   different TLS implementations and compare the number failed tests. The direct comparision of the
 *   failed tests count only works, if the tested TLS implementations support the same set of CipherSuites.
 */
public class LengthFieldTest extends TlsGenericTest {

    /**
     * @param c The Config used to create a workflow using WorkflowConfigurationFactory
     * @param runningMode The mode used to create a workflow using WorkflowConfigurationFactory
     * @return An AnnotatedStateContainer, where each state contains a complete handshake workflow modifying a single length field.
     */
    private AnnotatedStateContainer generateWorkflows(Config c, RunningModeType runningMode) {
        WorkflowTrace origTrace = new WorkflowConfigurationFactory(c).createWorkflowTrace(WorkflowTraceType.HANDSHAKE, runningMode);
        List<ModifiableFields> origFields = ModifiableFields.forWorkflowTrace(origTrace);
        AnnotatedStateContainer container = new AnnotatedStateContainer();

        for (int i = 0; i < origFields.size(); i++) {
            for (int j = 0; j < origFields.get(i).getFields().size(); j++) {
                WorkflowTrace trace = WorkflowTrace.copy(origTrace);
                List<ModifiableFields> modifiableFields = ModifiableFields.forWorkflowTrace(trace);

                ModifiableFields modifiableField = modifiableFields.get(i);
                ModifiableVariableHolder object = modifiableField.getHolder();
                Field field = modifiableField.getFields().get(j);
                String transformation = "";

                field.setAccessible(true);
                try {
                    if (field.getType() == ModifiableInteger.class) {
                        field.set(object, Modifiable.add(10));
                    } else if (field.getType() == ModifiableByte.class) {
                        field.set(object, Modifiable.add((byte)10));
                    }
                    else {
                        throw new RuntimeException("Unknown modifiable variable type " + field.getType().toString());
                    }

                    transformation = object.getClass().getName() + "." + field.getName();
                } catch (Exception e) {
                    LOGGER.error("Exception occurred", e);
                }

                Config copy = c.createCopy();
                AnnotatedState state = new AnnotatedState(new State(copy, trace));
                state.setInspectedCipherSuite(c.getDefaultSelectedCipherSuite());
                state.addAdditionalTestInfo(transformation);
                container.add(state);
            }
        }

        return container;
    }

    private AnnotatedStateContainer getContainer(Config c, List<CipherSuite> cipherSuites, RunningModeType runningMode) {
        AnnotatedStateContainer container = new AnnotatedStateContainer();
        for (CipherSuite i : cipherSuites) {
            c.setDefaultClientSupportedCiphersuites(i);
            c.setDefaultServerSupportedCiphersuites(i);
            c.setDefaultSelectedCipherSuite(i);

            c.getDefaultServerConnection().setTimeout(1000);
            c.getDefaultServerConnection().setFirstTimeout(5000);
            c.getDefaultClientConnection().setTimeout(1000);
            c.getDefaultClientConnection().setFirstTimeout(5000);

            container.addAll(generateWorkflows(c, runningMode));
        }
        return container;
    }


    @ServerTest
    @TlsVersion(supported = ProtocolVersion.TLS13)
    @TlsTest(description = "Manipulating length fields")
    @KeyExchange(supported = KeyExchangeType.ALL13)
    public void serverTestTls13(WorkflowRunner runner) {
        runner.useRecordFragmentationDerivation = false;
        runner.useTCPFragmentationDerivation = false;

        List<CipherSuite> tls13SupportedCipherSuites = new ArrayList<>(context.getConfig().getSiteReport().getSupportedTls13CipherSuites());
        Config c = context.getConfig().createTls13Config();
        AnnotatedStateContainer container = getContainer(c, tls13SupportedCipherSuites, RunningModeType.CLIENT);

        runner.execute(container).validateFinal(i -> {
            assertFalse("Workflow could be executed as planned", i.getWorkflowTrace().executedAsPlanned());
        });
    }

    @ServerTest
    @TlsVersion(supported = ProtocolVersion.TLS12)
    @TlsTest(description = "Manipulating length fields")
    @KeyExchange(supported = KeyExchangeType.ALL12)
    public void serverTestTls12(WorkflowRunner runner) {
        runner.useRecordFragmentationDerivation = false;
        runner.useTCPFragmentationDerivation = false;

        List<CipherSuite> cipherSuites = new ArrayList<>(context.getConfig().getSiteReport().getCipherSuites());
        Config c = context.getConfig().createConfig();
        AnnotatedStateContainer container = getContainer(c, cipherSuites, RunningModeType.CLIENT);

        runner.execute(container).validateFinal(i -> {
            assertFalse("Workflow could be executed as planned", i.getWorkflowTrace().executedAsPlanned());
        });
    }

    @ClientTest
    @TlsVersion(supported = ProtocolVersion.TLS13)
    @TlsTest(description = "Manipulating length fields")
    @KeyExchange(supported = KeyExchangeType.ALL13)
    public void clientTestTls13(WorkflowRunner runner) {
        runner.useRecordFragmentationDerivation = false;
        runner.useTCPFragmentationDerivation = false;

        List<CipherSuite> tls13SupportedCipherSuites = new ArrayList<>(context.getConfig().getSiteReport().getSupportedTls13CipherSuites());
        Config c = context.getConfig().createTls13Config();
        AnnotatedStateContainer container = getContainer(c, tls13SupportedCipherSuites, RunningModeType.SERVER);

        runner.execute(container).validateFinal(i -> {
            assertFalse("Workflow could be executed as planned", i.getWorkflowTrace().executedAsPlanned());
        });
    }

    @ClientTest
    @TlsVersion(supported = ProtocolVersion.TLS12)
    @TlsTest(description = "Manipulating length fields")
    @KeyExchange(supported = KeyExchangeType.ALL12)
    public void clientTestTls12(WorkflowRunner runner) {
        runner.useRecordFragmentationDerivation = false;
        runner.useTCPFragmentationDerivation = false;

        List<CipherSuite> cipherSuites = new ArrayList<>(context.getConfig().getSiteReport().getCipherSuites());
        Config c = context.getConfig().createConfig();
        AnnotatedStateContainer container = getContainer(c, cipherSuites, RunningModeType.SERVER);

        runner.execute(container).validateFinal(i -> {
            assertFalse("Workflow could be executed as planned", i.getWorkflowTrace().executedAsPlanned());
        });
    }
}


/**
 * Helper class to store an object (holder) with its containing ModifiableVariables of the LENGTH type.
 */
class ModifiableFields {
    private final ModifiableVariableHolder holder;
    private final List<Field> fields;

    public ModifiableFields(ModifiableVariableHolder holder) {
        this.holder = holder;
        this.fields = this.holder.getAllModifiableVariableFields()
                .stream()
                .filter(i -> {
                    if (i.isAnnotationPresent(ModifiableVariableProperty.class)) {
                        return i.getAnnotation(ModifiableVariableProperty.class).type() == ModifiableVariableProperty.Type.LENGTH;
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    public List<Field> getFields() {
        return fields;
    }

    public ModifiableVariableHolder getHolder() {
        return holder;
    }

    public static List<ModifiableFields> forWorkflowTrace(WorkflowTrace trace) {
        List<ProtocolMessage> sendingMessages = trace.getSendingActions().stream()
                .flatMap(i -> ((SendAction)i).getSendMessages().stream())
                .collect(Collectors.toList());

        AnnotatedStateContainer result = new AnnotatedStateContainer();
        List<ModifiableFields> modifiableFields = new ArrayList<>();
        for (ProtocolMessage msg : sendingMessages) {
            List<ModifiableFields> fields = msg.getAllModifiableVariableHolders().stream()
                    .map(ModifiableFields::new).collect(Collectors.toList());
            modifiableFields.addAll(fields);
        }

        return modifiableFields;
    }
}