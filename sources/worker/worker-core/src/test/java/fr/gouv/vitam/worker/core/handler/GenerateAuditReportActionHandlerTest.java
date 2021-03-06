package fr.gouv.vitam.worker.core.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessionRegisterSummaryModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.worker.core.plugin.CheckExistenceObjectPlugin;
import fr.gouv.vitam.worker.core.plugin.CheckIntegrityObjectPlugin;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;


public class GenerateAuditReportActionHandlerTest {

    private GenerateAuditReportActionHandler handler;
    private LogbookLifeCyclesClient logbookLifeCyclesClient;
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    private LogbookOperationsClient logbookOperationsClient;
    private LogbookOperationsClientFactory logbookOperationsClientFactory;
    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private StorageClient storageClient;
    private StorageClientFactory storageClientFactory;
    private AdminManagementClient adminManagementClient;
    private AdminManagementClientFactory adminManagementClientFactory;

    private HandlerIOImpl action;
    private GUID guid = GUIDFactory.newGUID();

    private static final String JOP_RESULTS = "GenerateAuditReportArctionHandler/jopResults.json";
    private static final String LFC_RESULTS = "GenerateAuditReportArctionHandler/lfcResults.json";


    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final WorkerParameters params =
        WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(org.assertj.core.util.Lists.newArrayList("objectName.json"))
            .setObjectName("archiveUnit.json").setCurrentStep("currentStep")
            .setContainerName(guid.getId()).setLogbookTypeProcess(LogbookTypeProcess.UPDATE)
            .putParameterValue(WorkerParameterName.auditType, "tenant")
            .putParameterValue(WorkerParameterName.objectId, "0")
            .putParameterValue(WorkerParameterName.auditActions,
                CheckExistenceObjectPlugin.getId() + ", " + CheckIntegrityObjectPlugin.getId());

    @Before
    public void setUp() throws Exception {
        File tempFolder = folder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());
        SystemPropertyUtil.refresh();

        logbookLifeCyclesClient = mock(LogbookLifeCyclesClient.class);
        logbookLifeCyclesClientFactory = mock(LogbookLifeCyclesClientFactory.class);

        logbookOperationsClient = mock(LogbookOperationsClient.class);
        logbookOperationsClientFactory = mock(LogbookOperationsClientFactory.class);

        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);

        storageClient = mock(StorageClient.class);
        storageClientFactory = mock(StorageClientFactory.class);

        adminManagementClient = mock(AdminManagementClient.class);
        adminManagementClientFactory = mock(AdminManagementClientFactory.class);

        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);

        handler = new GenerateAuditReportActionHandler(storageClientFactory, adminManagementClientFactory,
            logbookOperationsClientFactory);
        action = new HandlerIOImpl(workspaceClientFactory, logbookLifeCyclesClientFactory, guid.getId(), "workerId",
            Lists.newArrayList());
    }

    @RunWithCustomExecutor
    @Test
    public void executeOK() throws Exception {
        final JsonNode jopResults =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(JOP_RESULTS));
        final JsonNode lfcResults =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(LFC_RESULTS));
        reset(workspaceClient);
        reset(storageClient);
        reset(logbookLifeCyclesClient);
        reset(logbookOperationsClient);
        reset(adminManagementClient);
        Mockito.doNothing().when(workspaceClient).createContainer(any());
        Mockito.doNothing().when(workspaceClient).putObject(any(), any(), any());
        Mockito.doNothing().when(workspaceClient).deleteObject(any(), any());

        when(storageClient.storeFileFromWorkspace(any(), any(), any(), any())).thenReturn(null);

        when(logbookOperationsClient.selectOperation(any())).thenReturn(jopResults);
        when(logbookOperationsClient.selectOperationById(any())).thenReturn(jopResults);
        when(logbookLifeCyclesClient.selectObjectGroupLifeCycle(any())).thenReturn(lfcResults);

        final RequestResponseOK<AccessionRegisterSummaryModel> requestResponseOK = new RequestResponseOK();
        requestResponseOK.addAllResults(Lists.newArrayList());
        when(adminManagementClient.getAccessionRegister(any())).thenReturn(requestResponseOK);

        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @RunWithCustomExecutor
    @Test
    public void executeReferentialExceptionThenFatal() throws Exception {
        final JsonNode jopResults =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(JOP_RESULTS));
        final JsonNode lfcResults =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(LFC_RESULTS));
        reset(workspaceClient);
        reset(storageClient);
        reset(logbookLifeCyclesClient);
        reset(logbookOperationsClient);
        reset(adminManagementClient);
        Mockito.doNothing().when(workspaceClient).createContainer(any());
        Mockito.doNothing().when(workspaceClient).putObject(any(), any(), any());
        Mockito.doNothing().when(workspaceClient).deleteObject(any(), any());

        when(storageClient.storeFileFromWorkspace(any(), any(), any(), any())).thenReturn(null);

        when(logbookOperationsClient.selectOperation(any())).thenReturn(jopResults);
        when(logbookOperationsClient.selectOperationById(any())).thenReturn(jopResults);
        when(logbookLifeCyclesClient.selectObjectGroupLifeCycle(any())).thenReturn(lfcResults);

        final RequestResponseOK<AccessionRegisterSummaryModel> requestResponseOK = new RequestResponseOK();
        requestResponseOK.addAllResults(Lists.newArrayList());
        when(adminManagementClient.getAccessionRegister(any()))
            .thenThrow(new ReferentialException("ReferentialException"));

        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

}
