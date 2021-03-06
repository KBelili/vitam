/*
 *  Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *  <p>
 *  contact.vitam@culture.gouv.fr
 *  <p>
 *  This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 *  high volumetry securely and efficiently.
 *  <p>
 *  This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 *  software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 *  circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *  <p>
 *  As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 *  users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 *  successive licensors have only limited liability.
 *  <p>
 *  In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 *  developing or reproducing the software by the user in light of its specific status of free software, that may mean
 *  that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 *  experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 *  software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 *  to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *  <p>
 *  The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 *  accept its terms.
 */
package fr.gouv.vitam.processing.distributor.v2;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.processing.Action;
import fr.gouv.vitam.common.model.processing.ActionDefinition;
import fr.gouv.vitam.common.model.processing.ProcessBehavior;
import fr.gouv.vitam.common.model.processing.Step;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.exception.WorkerFamilyNotFoundException;
import fr.gouv.vitam.processing.common.parameter.DefaultWorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.client.WorkerClient;
import fr.gouv.vitam.worker.client.WorkerClientFactory;
import fr.gouv.vitam.worker.common.DescriptionStep;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class WorkerManagerTest {

    private static final String WORKER_DESCRIPTION =
        "{ \"name\" : \"workername\", \"family\" : \"DefaultWorker1\", \"capacity\" : 2, \"storage\" : 100," +
            "\"status\" : \"Active\", \"configuration\" : {\"serverHost\" : \"localhost\", \"serverPort\" : \"12345\" } }";

    private static final String WORKER_DESCRIPTION_2 =
        "{ \"name\" : \"workername\", \"family\" : \"DefaultWorker2\", \"capacity\" : 2, \"storage\" : 100," +
            "\"status\" : \"Active\", \"configuration\" : {\"serverHost\" : \"localhost\", \"serverPort\" : \"12345\" } }";
    private static final String BIG_WORKER_DESCRIPTION =
        "{ \"name\" : \"workername2\", \"family\" : \"BigWorker\", \"capacity\" : 4, \"storage\" : 100," +
            "\"status\" : \"Active\", \"configuration\" : {\"serverHost\" : \"localhost\", \"serverPort\" : \"12345\" } }";

    private static String registeredWorkerFile = "worker.db";

    @Rule
    public TempFolderRule tempFolderRule = new TempFolderRule();

    private WorkerManager workerManager;
    private static final WorkerClientFactory workerClientFactory = mock(WorkerClientFactory.class);
    private static final WorkerClient workerClient = mock(WorkerClient.class);

    @Before
    public void setup() throws Exception {
        reset(workerClientFactory);
        reset(workerClient);

        when(workerClientFactory.getClient()).thenReturn(workerClient);

        doNothing().when(workerClient).checkStatus();
        workerManager = new WorkerManager(workerClientFactory);
        workerManager.initialize();
    }

    @After
    public void tearDownAfter() throws Exception {
        cleanWorkerDb();
    }

    private static void cleanWorkerDb() {
        File WORKKER_DB_FILE = PropertiesUtils.fileFromDataFolder(registeredWorkerFile);
        if (null != WORKKER_DB_FILE && WORKKER_DB_FILE.exists()) {
            WORKKER_DB_FILE.delete();
        }
    }

    @Test
    public void givenBigWorkerFamilyAndStepOfBigWorkflowRunningOn() throws Exception {
        final String familyId = "BigWorker";
        final String workerId = "NewWorkerId2";
        DefaultWorkerParameters params = WorkerParametersFactory.newWorkerParameters();
        params.setWorkerGUID(GUIDFactory.newGUID());
        final Step step = new Step().setStepName("TEST").setWorkerGroupId(familyId);
        final List<Action> actions = new ArrayList<>();
        final Action action = new Action();
        actions.add(action);

        action.setActionDefinition(
            new ActionDefinition().setActionKey("DummyHandler").setBehavior(ProcessBehavior.NOBLOCKING));
        step.setBehavior(ProcessBehavior.NOBLOCKING).setActions(actions);
        DescriptionStep descriptionStep =
            new DescriptionStep(step, params);

        final WorkerTask task =
            new WorkerTask(descriptionStep, 0, "requestId", "contractId", "contextId", "applicationId",
                workerClientFactory);

        workerManager.registerWorker(familyId, workerId, BIG_WORKER_DESCRIPTION);
        WorkerFamilyManager workerFamilyManager = workerManager.findWorkerBy(familyId);
        assertNotNull(workerFamilyManager);

        CompletableFuture.supplyAsync(task, workerFamilyManager).get();
    }

    @Test
    public void givenCorrectQueueAndCorrectAsyncWhenSubmitJobThenOK() throws Exception {
        DefaultWorkerParameters params = WorkerParametersFactory.newWorkerParameters();
        params.setWorkerGUID(GUIDFactory.newGUID());
        final Step step = new Step().setStepName("TEST");
        final List<Action> actions = new ArrayList<>();
        final Action action = new Action();
        final String familyId = "DefaultWorker1";
        final String workerId = "NewWorkerId";

        action.setActionDefinition(
            new ActionDefinition().setActionKey("DummyHandler").setBehavior(ProcessBehavior.NOBLOCKING));
        actions.add(action);
        step.setBehavior(ProcessBehavior.NOBLOCKING).setActions(actions);
        DescriptionStep descriptionStep =
            new DescriptionStep(step, params);
        final WorkerTask task =
            new WorkerTask(descriptionStep, 0, "requestId", "contractId", "contextId", "applicationId",
                workerClientFactory);
        workerManager.registerWorker(familyId, workerId, WORKER_DESCRIPTION);

        WorkerFamilyManager workerFamilyManager = workerManager.findWorkerBy(familyId);
        assertNotNull(workerFamilyManager);
        CompletableFuture.supplyAsync(task, workerFamilyManager).get();

    }

    @Test
    public void givenProcessDistributorWhenRegisterWorkerThenOK() throws Exception {
        final String familyId = "DefaultWorker1";
        final String workerId = "NewWorkerId" + GUIDFactory.newGUID().getId();
        workerManager.registerWorker(familyId, workerId, WORKER_DESCRIPTION);
        assertTrue(workerManager.findWorkerBy(familyId) != null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenProcessDistributorWhenRegisterWorkerThenAlreadyExists() throws Exception {
        final String familyId = "DefaultWorker1";
        final String workerId = "NewWorkerId" + GUIDFactory.newGUID().getId();
        workerManager.registerWorker(familyId, workerId, WORKER_DESCRIPTION);
        assertTrue(workerManager.findWorkerBy(familyId) != null);
        workerManager.initialize();
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenProcessDistributorWhenRegisterExistingWorkerThenProcessingException() throws Exception {
        final String familyId = "DefaultWorker1";
        final String workerId = "NewWorkerId1" + GUIDFactory.newGUID().getId();

        workerManager.registerWorker(familyId, workerId, WORKER_DESCRIPTION);
        workerManager.registerWorker(familyId, workerId, WORKER_DESCRIPTION);
    }


    @Test
    public void givenProcessDistributorWhenUnRegisterExistingWorkerThenOK() throws Exception {
        final String familyId = "DefaultWorker2";
        final String workerId = "NewWorkerId2" + GUIDFactory.newGUID().getId();
        workerManager.registerWorker(familyId, workerId, WORKER_DESCRIPTION_2);
        WorkerFamilyManager workerfamily = workerManager.findWorkerBy(familyId);
        assertNotNull(workerfamily);
        assertEquals(workerfamily.getWorkers().size(), 1);

        workerManager.unregisterWorker(familyId, workerId);
        workerfamily = workerManager.findWorkerBy(familyId);
        assertNotNull(workerfamily);
        assertEquals(workerfamily.getWorkers().size(), 0);
    }

    @Test(expected = WorkerFamilyNotFoundException.class)
    public void givenProcessDistributorWhenUnRegisterNonExistingFamilyThenProcessingException() throws Exception {
        final String familyId = "NewFamilyId" + GUIDFactory.newGUID().getId();
        final String workerId = "NewWorkerId1";
        workerManager.unregisterWorker(familyId, workerId);
    }

    @Test
    public void givenProcessDistributorWhenUnRegisterNonExistingWorkerThenProcessingException() throws Exception {
        final String familyId = "DefaultWorker1";
        final String workerId = "NewWorkerId3" + GUIDFactory.newGUID().getId();
        final String workerUnknownId = "UnknownWorkerId";
        workerManager.registerWorker(familyId, workerId, WORKER_DESCRIPTION);
        workerManager.unregisterWorker(familyId, workerUnknownId);
    }

    @Test(expected = ProcessingBadRequestException.class)
    public void givenProcessDistributorWhenRegisterIncorrectJsonNodeThenProcessingException() throws Exception {
        final String familyId = "NewFamilyId";
        final String workerId = "NewWorkerId4" + GUIDFactory.newGUID().getId();
        workerManager.registerWorker(familyId, workerId, "{\"fakeKey\" : \"fakeValue\"}");
    }
}
