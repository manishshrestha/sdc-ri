package org.somda.sdc.glue.provider.plugin;

import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.common.event.ContextStateModificationMessage;
import org.somda.sdc.biceps.common.event.DescriptionModificationMessage;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.model.participant.factory.CodedValueFactory;
import org.somda.sdc.biceps.model.participant.factory.InstanceIdentifierFactory;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.dpws.device.Device;
import org.somda.sdc.dpws.device.DiscoveryAccess;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.common.ComplexDeviceComponentMapper;
import org.somda.sdc.glue.common.ContextIdentificationMapper;
import org.somda.sdc.glue.provider.SdcDeviceContext;
import test.org.somda.common.LoggingTestWatcher;
import test.org.somda.common.TestLogging;

import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(LoggingTestWatcher.class)
class SdcRequiredTypesAndScopesTest {
    private SdcRequiredTypesAndScopes sdcRequiredTypesAndScopes;
    private EventBus eventBus;
    private DiscoveryAccess discoveryAccessMock;

    @BeforeAll
    static void beforeAll() {
        TestLogging.configure();
    }

    @BeforeEach
    void beforeEach() {
        discoveryAccessMock = mock(DiscoveryAccess.class);
        var deviceMock = mock(Device.class);
        when(deviceMock.getDiscoveryAccess()).thenReturn(discoveryAccessMock);
        var sdcDeviceContextMock = mock(SdcDeviceContext.class);
        when(sdcDeviceContextMock.getDevice()).thenReturn(deviceMock);
        when(sdcDeviceContextMock.getLocalMdibAccess()).thenReturn(mock(LocalMdibAccess.class));
        sdcRequiredTypesAndScopes = new SdcRequiredTypesAndScopes();
        sdcRequiredTypesAndScopes.beforeStartUp(sdcDeviceContextMock);

        eventBus = new EventBus();
        eventBus.register(sdcRequiredTypesAndScopes);
    }

    @Test
    void appendScopesAndSendHello() {
        {
            // Append scopes from outside
            var scopes = new ArrayList<>(Arrays.asList(URI.create("urn:dummy:one"), URI.create("urn:dummy:two")));
            sdcRequiredTypesAndScopes.appendScopesAndSendHello(new HashSet<>(scopes));
            var scopesCaptor = ArgumentCaptor.forClass(Collection.class);
            verify(discoveryAccessMock, times(2)).setScopes(scopesCaptor.capture());
            assertEquals(2, scopesCaptor.getAllValues().size());
            assertEquals(scopes.size() + 1, scopesCaptor.getAllValues().get(1).size());
            scopes.add(GlueConstants.SCOPE_SDC_PROVIDER);
            verifyScopes(scopes, scopesCaptor.getAllValues().get(1));
        }

        {
            // Scopes do not change
            var scopes = new ArrayList<>(Arrays.asList(URI.create("urn:dummy:one"), URI.create("urn:dummy:two")));
            var scopesCaptor = ArgumentCaptor.forClass(Collection.class);
            sdcRequiredTypesAndScopes.appendScopesAndSendHello(new HashSet<>(scopes));
            // Expect no further interaction - times retains 2
            verify(discoveryAccessMock, times(2)).setScopes(scopesCaptor.capture());
            assertEquals(2, scopesCaptor.getAllValues().size());
        }

        {
            // Scopes change, add duplicates
            var scopes = new ArrayList<>(Arrays.asList(URI.create("urn:dummy:three"), URI.create("urn:dummy:two"),
                    GlueConstants.SCOPE_SDC_PROVIDER)); // put this as a duplicate as the updater will insert it again
            var scopesCaptor = ArgumentCaptor.forClass(Collection.class);
            sdcRequiredTypesAndScopes.appendScopesAndSendHello(new HashSet<>(scopes));
            // Expect no further interaction - times retains 2
            verify(discoveryAccessMock, times(3)).setScopes(scopesCaptor.capture());
            assertEquals(3, scopesCaptor.getAllValues().size());
            assertEquals(scopes.size(), scopesCaptor.getAllValues().get(2).size());
            verifyScopes(scopes, scopesCaptor.getAllValues().get(2));
        }
    }

    @Test
    void mdibVersionSeenBefore() {
        var outdatedVersion = MdibVersion.create();
        var newVersion = MdibVersion.increment(outdatedVersion);
        var differentSequenceId = MdibVersion.create();

        assertFalse(sdcRequiredTypesAndScopes.mdibVersionSeenBefore(outdatedVersion));
        assertTrue(sdcRequiredTypesAndScopes.mdibVersionSeenBefore(outdatedVersion));
        assertFalse(sdcRequiredTypesAndScopes.mdibVersionSeenBefore(newVersion));
        assertTrue(sdcRequiredTypesAndScopes.mdibVersionSeenBefore(newVersion));
        assertFalse(sdcRequiredTypesAndScopes.mdibVersionSeenBefore(differentSequenceId));
    }

    @Test
    void updateContextsShallNotInvokeSetScopesAndHello() {
        // Verify setScopes and sendHello is not invoked by just updating contexts
        var mdibAccessMock = mock(MdibAccess.class);
        when(mdibAccessMock.getMdibVersion()).thenReturn(MdibVersion.create());
        var message = new ContextStateModificationMessage(mdibAccessMock, Collections.emptyList());
        sdcRequiredTypesAndScopes.updateContexts(message);
        verify(discoveryAccessMock, times(1)).setScopes(any());
        verify(discoveryAccessMock, never()).sendHello();
    }

    @Test
    void updateContextsShallGenerateCorrectScopeUris() {
        // Verify that a context state change is reflected in the scopes
        var expectedInstanceIdentifier = InstanceIdentifierFactory.createInstanceIdentifier("urn:dummy:test");
        var expectedScopeUri = ContextIdentificationMapper.fromInstanceIdentifier(expectedInstanceIdentifier,
                ContextIdentificationMapper.ContextSource.Location);
        var expectedContextState = createLocationContextState(expectedInstanceIdentifier);

        var mdibAccessMock = mock(MdibAccess.class);
        var mdibVersion = MdibVersion.create();
        when(mdibAccessMock.getMdibVersion()).thenReturn(mdibVersion);
        when(mdibAccessMock.findContextStatesByType(LocationContextState.class))
                .thenReturn(Collections.singletonList(expectedContextState));

        var message = new ContextStateModificationMessage(mdibAccessMock, Collections.singletonList(expectedContextState));
        sdcRequiredTypesAndScopes.updateContexts(message);
        sdcRequiredTypesAndScopes.appendScopesAndSendHello(Collections.emptySet());
        var scopesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(discoveryAccessMock, times(2)).setScopes(scopesCaptor.capture());
        assertEquals(2, scopesCaptor.getAllValues().size());
        assertEquals(2, scopesCaptor.getAllValues().get(1).size());
        verifyScopes(Arrays.asList(expectedScopeUri, GlueConstants.SCOPE_SDC_PROVIDER), scopesCaptor.getAllValues().get(1));
        verify(discoveryAccessMock, times(1)).sendHello();

        // Verify that an already seen MDIB version is filtered out
        sdcRequiredTypesAndScopes.updateContexts(message);
        sdcRequiredTypesAndScopes.appendScopesAndSendHello(Collections.emptySet());
        verify(discoveryAccessMock, times(2)).setScopes(scopesCaptor.capture());
        verify(discoveryAccessMock, times(1)).sendHello();
    }

    @Test
    void eventBusShallTriggerContextUpdatesWithSetScopesAndHello() {
        // Verify that a context state change is reflected in the scopes
        var expectedInstanceIdentifier = InstanceIdentifierFactory.createInstanceIdentifier("urn:dummy:test");
        var expectedScopeUri = ContextIdentificationMapper.fromInstanceIdentifier(expectedInstanceIdentifier,
                ContextIdentificationMapper.ContextSource.Location);
        var expectedContextState = createLocationContextState(expectedInstanceIdentifier);

        var mdibAccessMock = mock(MdibAccess.class);
        var mdibVersion = MdibVersion.create();
        when(mdibAccessMock.getMdibVersion()).thenReturn(mdibVersion);
        when(mdibAccessMock.findContextStatesByType(LocationContextState.class))
                .thenReturn(Collections.singletonList(expectedContextState));

        var message = new ContextStateModificationMessage(mdibAccessMock, Collections.singletonList(expectedContextState));
        verify(discoveryAccessMock, times(1)).setScopes(any());

        eventBus.post(message);

        var scopesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(discoveryAccessMock, times(2)).setScopes(scopesCaptor.capture());
        assertEquals(2, scopesCaptor.getAllValues().size());
        assertEquals(2, scopesCaptor.getAllValues().get(1).size());
        verifyScopes(Arrays.asList(expectedScopeUri, GlueConstants.SCOPE_SDC_PROVIDER), scopesCaptor.getAllValues().get(1));
        verify(discoveryAccessMock, times(1)).sendHello();

        // Verify that an already seen MDIB version is filtered out
        eventBus.post(message);
        verify(discoveryAccessMock, times(2)).setScopes(scopesCaptor.capture());
        verify(discoveryAccessMock, times(1)).sendHello();
    }

    @Test
    void updateDescriptionShallNotInvokeSetScopesAndHello() {
        // Verify setScopes and sendHello is not invoked by just updating a description
        var mdibAccessMock = mock(MdibAccess.class);
        when(mdibAccessMock.getMdibVersion()).thenReturn(MdibVersion.create());
        var message = new DescriptionModificationMessage(mdibAccessMock,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        sdcRequiredTypesAndScopes.updateDescription(message);
        verify(discoveryAccessMock, times(1)).setScopes(any());
        verify(discoveryAccessMock, never()).sendHello();
    }

    @Test
    void updateDescriptionShallGenerateCorrectScopeUris() {
        // Verify that a description change is reflected in the scopes
        var expectedInstanceIdentifier = InstanceIdentifierFactory.createInstanceIdentifier("urn:dummy:test");
        var expectedContextScopeUri = ContextIdentificationMapper.fromInstanceIdentifier(expectedInstanceIdentifier,
                ContextIdentificationMapper.ContextSource.Location);
        var expectedContextState = createLocationContextState(expectedInstanceIdentifier);

        var expectedCodedValue = CodedValueFactory.createIeeeCodedValue("70001");
        var expectedMdsScopeUri = ComplexDeviceComponentMapper.fromCodedValue(expectedCodedValue);
        var expectedMds = createMdsDescriptor(expectedCodedValue);

        var mdibAccessMock = mock(MdibAccess.class);
        when(mdibAccessMock.findContextStatesByType(LocationContextState.class))
                .thenReturn(Collections.singletonList(expectedContextState));
        var mdibEntity = mock(MdibEntity.class);
        when(mdibEntity.getDescriptor(MdsDescriptor.class))
                .thenReturn(Optional.of(expectedMds))
                .thenReturn(Optional.of(expectedMds));
        when(mdibAccessMock.findEntitiesByType(MdsDescriptor.class))
                .thenReturn(Collections.singletonList(mdibEntity));
        var message = new DescriptionModificationMessage(mdibAccessMock,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        sdcRequiredTypesAndScopes.updateDescription(message);
        sdcRequiredTypesAndScopes.appendScopesAndSendHello(Collections.emptySet());

        var scopesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(discoveryAccessMock, times(2)).setScopes(scopesCaptor.capture());
        assertEquals(2, scopesCaptor.getAllValues().size());
        assertEquals(3, scopesCaptor.getAllValues().get(1).size());
        verifyScopes(Arrays.asList(expectedContextScopeUri, expectedMdsScopeUri, GlueConstants.SCOPE_SDC_PROVIDER),
                scopesCaptor.getAllValues().get(1));
        verify(discoveryAccessMock, times(1)).sendHello();
    }

    @Test
    void eventBusShallTriggerDescriptionUpdatesWithSetScopesAndHello() {
        // Verify that a description change is reflected in the scopes
        var expectedInstanceIdentifier = InstanceIdentifierFactory.createInstanceIdentifier("urn:dummy:test");
        var expectedContextScopeUri = ContextIdentificationMapper.fromInstanceIdentifier(expectedInstanceIdentifier,
                ContextIdentificationMapper.ContextSource.Location);
        var expectedContextState = createLocationContextState(expectedInstanceIdentifier);

        var expectedCodedValue = CodedValueFactory.createIeeeCodedValue("70001");
        var expectedMdsScopeUri = ComplexDeviceComponentMapper.fromCodedValue(expectedCodedValue);
        var expectedMds = createMdsDescriptor(expectedCodedValue);

        var mdibAccessMock = mock(MdibAccess.class);
        when(mdibAccessMock.findContextStatesByType(LocationContextState.class))
                .thenReturn(Collections.singletonList(expectedContextState));
        var mdibEntity = mock(MdibEntity.class);
        when(mdibEntity.getDescriptor(MdsDescriptor.class))
                .thenReturn(Optional.of(expectedMds))
                .thenReturn(Optional.of(expectedMds));
        when(mdibAccessMock.findEntitiesByType(MdsDescriptor.class))
                .thenReturn(Collections.singletonList(mdibEntity));
        var message = new DescriptionModificationMessage(mdibAccessMock,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        verify(discoveryAccessMock, times(1)).setScopes(any());

        eventBus.post(message);

        var scopesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(discoveryAccessMock, times(2)).setScopes(scopesCaptor.capture());
        assertEquals(2, scopesCaptor.getAllValues().size());
        assertEquals(3, scopesCaptor.getAllValues().get(1).size());
        verifyScopes(Arrays.asList(expectedContextScopeUri, expectedMdsScopeUri, GlueConstants.SCOPE_SDC_PROVIDER),
                scopesCaptor.getAllValues().get(1));
        verify(discoveryAccessMock, times(1)).sendHello();
    }

    private void verifyScopes(Collection<URI> expectedScopes, Collection<URI> actualScopes) {
        assertEquals(expectedScopes.size(), actualScopes.size());
        int matchCount = 0;
        for (URI expectedScope : expectedScopes) {
            for (URI actualScope : actualScopes) {
                matchCount += expectedScope.equals(actualScope) ? 1 : 0;
            }
        }
        assertEquals(expectedScopes.size(), matchCount);
    }

    private LocationContextState createLocationContextState(InstanceIdentifier instanceIdentifier) {
        var locationContextState = new LocationContextState();
        locationContextState.setHandle("handle");
        locationContextState.setContextAssociation(ContextAssociation.ASSOC);
        locationContextState.getIdentification().add(instanceIdentifier);
        return locationContextState;
    }

    private MdsDescriptor createMdsDescriptor(CodedValue type) {
        var mdsDescriptor = new MdsDescriptor();
        mdsDescriptor.setHandle("handle");
        mdsDescriptor.setType(type);
        return mdsDescriptor;
    }
}
