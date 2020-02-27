package org.somda.sdc.glue.provider.plugin;

import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.event.ContextStateModificationMessage;
import org.somda.sdc.biceps.common.event.DescriptionModificationMessage;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.model.participant.factory.CodedValueFactory;
import org.somda.sdc.biceps.model.participant.factory.InstanceIdentifierFactory;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.dpws.device.Device;
import org.somda.sdc.dpws.device.DiscoveryAccess;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.common.uri.ComplexDeviceComponentMapper;
import org.somda.sdc.glue.common.uri.ContextIdentificationMapper;
import org.somda.sdc.glue.common.uri.UriMapperGenerationArgumentException;
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
    private LocalMdibAccess mdibAccessMock;


    @BeforeAll
    static void beforeAll() {
        TestLogging.configure();
    }

    @BeforeEach
    void beforeEach() {
        mdibAccessMock = mock(LocalMdibAccess.class);
        discoveryAccessMock = mock(DiscoveryAccess.class);
        var deviceMock = mock(Device.class);
        when(deviceMock.getDiscoveryAccess()).thenReturn(discoveryAccessMock);
        var sdcDeviceContextMock = mock(SdcDeviceContext.class);
        when(sdcDeviceContextMock.getDevice()).thenReturn(deviceMock);
        when(sdcDeviceContextMock.getLocalMdibAccess()).thenReturn(mdibAccessMock);
        sdcRequiredTypesAndScopes = new SdcRequiredTypesAndScopes();
        sdcRequiredTypesAndScopes.beforeStartUp(sdcDeviceContextMock);

        eventBus = new EventBus();
        eventBus.register(sdcRequiredTypesAndScopes);
    }

    @Test
    void appendScopesAndSendHello() {
        final int setScopesInteractionCount = 3;

        {
            // Append scopes from outside
            var scopes = new ArrayList<>(Arrays.asList(URI.create("urn:dummy:one"), URI.create("urn:dummy:two")));
            sdcRequiredTypesAndScopes.appendScopesAndSendHello(new HashSet<>(scopes));
            var scopesCaptor = ArgumentCaptor.forClass(Collection.class);
            verify(discoveryAccessMock, times(setScopesInteractionCount)).setScopes(scopesCaptor.capture());
            assertEquals(setScopesInteractionCount, scopesCaptor.getAllValues().size());
            assertEquals(scopes.size() + 1, scopesCaptor.getAllValues().get(2).size());
            scopes.add(GlueConstants.SCOPE_SDC_PROVIDER);
            verifyScopes(scopes, scopesCaptor.getAllValues().get(setScopesInteractionCount - 1));
        }

        {
            // Scopes do not change
            var scopes = new ArrayList<>(Arrays.asList(URI.create("urn:dummy:one"), URI.create("urn:dummy:two")));
            var scopesCaptor = ArgumentCaptor.forClass(Collection.class);
            sdcRequiredTypesAndScopes.appendScopesAndSendHello(new HashSet<>(scopes));
            // Expect no further interaction - times retains 2
            verify(discoveryAccessMock, times(setScopesInteractionCount)).setScopes(scopesCaptor.capture());
            assertEquals(setScopesInteractionCount, scopesCaptor.getAllValues().size());
        }

        {
            // Scopes change, add duplicates
            var scopes = new ArrayList<>(Arrays.asList(URI.create("urn:dummy:three"), URI.create("urn:dummy:two"),
                    GlueConstants.SCOPE_SDC_PROVIDER)); // put this as a duplicate as the updater will insert it again
            var scopesCaptor = ArgumentCaptor.forClass(Collection.class);
            sdcRequiredTypesAndScopes.appendScopesAndSendHello(new HashSet<>(scopes));
            // Expect no further interaction - times retains 2
            verify(discoveryAccessMock, times(setScopesInteractionCount + 1)).setScopes(scopesCaptor.capture());
            assertEquals(4, scopesCaptor.getAllValues().size());
            assertEquals(scopes.size(), scopesCaptor.getAllValues().get(setScopesInteractionCount).size());
            verifyScopes(scopes, scopesCaptor.getAllValues().get(setScopesInteractionCount));
        }
    }

    @Test
    void updateContextsShallGenerateCorrectScopeUris() {
        int setScopesInteractionCount = 3;
        int sendHelloInteractionCount = 1;
        int scopesCount = 2;

        // Verify that a context state change is reflected in the scopes
        var expectedInstanceIdentifier = InstanceIdentifierFactory.createInstanceIdentifier("urn:dummy:test");
        var expectedScopeUri = ContextIdentificationMapper.fromInstanceIdentifier(expectedInstanceIdentifier,
                ContextIdentificationMapper.ContextSource.Location);
        var expectedContextState = createLocationContextState(expectedInstanceIdentifier);

        var mdibVersion = MdibVersion.create();
        when(mdibAccessMock.getMdibVersion()).thenReturn(mdibVersion);
        when(mdibAccessMock.findContextStatesByType(LocationContextState.class))
                .thenReturn(Collections.singletonList(expectedContextState));

        sdcRequiredTypesAndScopes.appendScopesAndSendHello(Collections.emptySet());
        var scopesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(discoveryAccessMock, times(setScopesInteractionCount)).setScopes(scopesCaptor.capture());
        assertEquals(setScopesInteractionCount, scopesCaptor.getAllValues().size());
        assertEquals(scopesCount, scopesCaptor.getAllValues().get(setScopesInteractionCount - 1).size());
        verifyScopes(Arrays.asList(expectedScopeUri, GlueConstants.SCOPE_SDC_PROVIDER), scopesCaptor.getAllValues().get(setScopesInteractionCount - 1));
        verify(discoveryAccessMock, times(sendHelloInteractionCount)).sendHello();

        // Verify that an already seen MDIB version is filtered out
        sdcRequiredTypesAndScopes.appendScopesAndSendHello(Collections.emptySet());
        verify(discoveryAccessMock, times(setScopesInteractionCount)).setScopes(scopesCaptor.capture());
        verify(discoveryAccessMock, times(sendHelloInteractionCount)).sendHello();
    }

    @Test
    void eventBusShallTriggerContextUpdatesWithSetScopesAndHello() {
        int setScopesInteractionCount = 2;
        int sendHelloInteractionCount = 1;

        // Verify that a context state change is reflected in the scopes
        var expectedInstanceIdentifier = InstanceIdentifierFactory.createInstanceIdentifier("urn:dummy:test");
        var expectedScopeUri = ContextIdentificationMapper.fromInstanceIdentifier(expectedInstanceIdentifier,
                ContextIdentificationMapper.ContextSource.Location);
        var expectedContextState = createLocationContextState(expectedInstanceIdentifier);

        var mdibVersion = MdibVersion.create();
        when(mdibAccessMock.getMdibVersion()).thenReturn(mdibVersion);
        when(mdibAccessMock.findContextStatesByType(LocationContextState.class))
                .thenReturn(Collections.singletonList(expectedContextState));

        var message = new ContextStateModificationMessage(mdibAccessMock, Collections.singletonList(expectedContextState));
        verify(discoveryAccessMock, times(setScopesInteractionCount)).setScopes(any());

        eventBus.post(message);

        var scopesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(discoveryAccessMock, times(++setScopesInteractionCount)).setScopes(scopesCaptor.capture());
        assertEquals(setScopesInteractionCount, scopesCaptor.getAllValues().size());
        assertEquals(2, scopesCaptor.getAllValues().get(setScopesInteractionCount - 1).size());
        verifyScopes(Arrays.asList(expectedScopeUri, GlueConstants.SCOPE_SDC_PROVIDER), scopesCaptor.getAllValues().get(setScopesInteractionCount - 1));
        verify(discoveryAccessMock, times(sendHelloInteractionCount)).sendHello();

        // Verify that an already seen MDIB version is filtered out
        eventBus.post(message);
        verify(discoveryAccessMock, times(setScopesInteractionCount)).setScopes(scopesCaptor.capture());
        verify(discoveryAccessMock, times(sendHelloInteractionCount)).sendHello();
    }

    @Test
    void updateDescriptionShallGenerateCorrectScopeUris() throws UriMapperGenerationArgumentException {
        int setScopesInteractionCount = 3;
        int sendHelloInteractionCount = 1;
        int scopesCount = 3;

        // Verify that a description change is reflected in the scopes
        var expectedInstanceIdentifier = InstanceIdentifierFactory.createInstanceIdentifier("urn:dummy:test");
        var expectedContextScopeUri = ContextIdentificationMapper.fromInstanceIdentifier(expectedInstanceIdentifier,
                ContextIdentificationMapper.ContextSource.Location);
        var expectedContextState = createLocationContextState(expectedInstanceIdentifier);

        var expectedCodedValue = CodedValueFactory.createIeeeCodedValue("70001");
        var expectedMdsScopeUri = ComplexDeviceComponentMapper.fromCodedValue(expectedCodedValue);
        var expectedMds = createMdsDescriptor(expectedCodedValue);

        when(mdibAccessMock.findContextStatesByType(LocationContextState.class))
                .thenReturn(Collections.singletonList(expectedContextState));
        var mdibEntity = mock(MdibEntity.class);
        when(mdibEntity.getDescriptor(MdsDescriptor.class))
                .thenReturn(Optional.of(expectedMds))
                .thenReturn(Optional.of(expectedMds));
        when(mdibAccessMock.findEntitiesByType(MdsDescriptor.class))
                .thenReturn(Collections.singletonList(mdibEntity));

        sdcRequiredTypesAndScopes.appendScopesAndSendHello(Collections.emptySet());

        var scopesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(discoveryAccessMock, times(setScopesInteractionCount)).setScopes(scopesCaptor.capture());
        assertEquals(setScopesInteractionCount, scopesCaptor.getAllValues().size());
        assertEquals(scopesCount, scopesCaptor.getAllValues().get(setScopesInteractionCount - 1).size());
        verifyScopes(Arrays.asList(expectedContextScopeUri, expectedMdsScopeUri, GlueConstants.SCOPE_SDC_PROVIDER),
                scopesCaptor.getAllValues().get(setScopesInteractionCount - 1));
        verify(discoveryAccessMock, times(sendHelloInteractionCount)).sendHello();
    }

    @Test
    void eventBusShallTriggerDescriptionUpdatesWithSetScopesAndHello() throws UriMapperGenerationArgumentException{
        int setScopesInteractionCount = 2;
        int sendHelloInteractionCount = 1;
        int scopesCount = 3;

        // Verify that a description change is reflected in the scopes
        var expectedInstanceIdentifier = InstanceIdentifierFactory.createInstanceIdentifier("urn:dummy:test");
        var expectedContextScopeUri = ContextIdentificationMapper.fromInstanceIdentifier(expectedInstanceIdentifier,
                ContextIdentificationMapper.ContextSource.Location);
        var expectedContextState = createLocationContextState(expectedInstanceIdentifier);

        var expectedCodedValue = CodedValueFactory.createIeeeCodedValue("70001");
        var expectedMdsScopeUri = ComplexDeviceComponentMapper.fromCodedValue(expectedCodedValue);
        var expectedMds = createMdsDescriptor(expectedCodedValue);

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
        verify(discoveryAccessMock, times(setScopesInteractionCount)).setScopes(any());

        eventBus.post(message);

        var scopesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(discoveryAccessMock, times(++setScopesInteractionCount)).setScopes(scopesCaptor.capture());
        assertEquals(setScopesInteractionCount, scopesCaptor.getAllValues().size());
        assertEquals(scopesCount, scopesCaptor.getAllValues().get(setScopesInteractionCount-1).size());
        verifyScopes(Arrays.asList(expectedContextScopeUri, expectedMdsScopeUri, GlueConstants.SCOPE_SDC_PROVIDER),
                scopesCaptor.getAllValues().get(setScopesInteractionCount-1));
        verify(discoveryAccessMock, times(sendHelloInteractionCount)).sendHello();
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
