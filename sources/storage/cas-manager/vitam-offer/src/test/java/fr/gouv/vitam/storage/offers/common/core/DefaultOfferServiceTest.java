/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 *******************************************************************************/

package fr.gouv.vitam.storage.offers.common.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorageAbstract;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.offers.common.database.OfferLogDatabaseService;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageDatabaseException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

/**
 * Default offer service test implementation
 */
public class DefaultOfferServiceTest {
    private static final String CONTAINER_PATH = "container";
    private static final DataCategory OBJECT_TYPE = DataCategory.OBJECT;
    private static final String OBJECT_ID = GUIDFactory.newObjectGUID(0).getId();
    private static final String OBJECT_ID_2 = GUIDFactory.newObjectGUID(0).getId();
    private static final String OBJECT_ID_DELETE = GUIDFactory.newObjectGUID(0).getId();

    private static final String DEFAULT_STORAGE_CONF = "default-storage.conf";
    private static final String ARCHIVE_FILE_TXT = "archivefile.txt";
    private static final String OBJECT_ID_2_CONTENT = "Vitam Test Content";
    private static final String FAKE_CONTAINER = "fakeContainer";
    private static final String OBJECT = "object_";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private OfferLogDatabaseService offerDatabaseService;

    @BeforeClass
    public static void beforeClass() {
        ContentAddressableStorageAbstract.disableContainerCaching();
    }

    @Before
    public void init() throws Exception {
        File confFile = PropertiesUtils.findFile(DEFAULT_STORAGE_CONF);
        final ObjectNode conf = PropertiesUtils.readYaml(confFile, ObjectNode.class);
        conf.put("storagePath", tempFolder.getRoot().getAbsolutePath());
        PropertiesUtils.writeYaml(confFile, conf);
    }

    @Test
    public void initOKTest() throws Exception {
        new DefaultOfferServiceImpl(offerDatabaseService);
    }

    @Test
    public void createObjectTestNoContainer() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        offerService.createObject(FAKE_CONTAINER, OBJECT_ID, new FakeInputStream(1024), OBJECT_TYPE, null,
            VitamConfiguration.getDefaultDigestType());
    }

    @Test
    public void createContainerTest() throws Exception {
        final DefaultOfferServiceImpl offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);

        offerService.ensureContainerExists(CONTAINER_PATH);

        // check
        final File container = new File(tempFolder.getRoot(), CONTAINER_PATH);
        assertTrue(container.exists());
        assertTrue(container.isDirectory());

        offerService.ensureContainerExists(CONTAINER_PATH);
    }

    @Test
    public void createObjectTest() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);

        String computedDigest;

        // object
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            assertNotNull(in);
            computedDigest = offerService.createObject(CONTAINER_PATH, OBJECT_ID, in, OBJECT_TYPE, null,
                VitamConfiguration.getDefaultDigestType());
        }
        // check
        final File testFile = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);
        final File offerFile = new File(tempFolder.getRoot(), CONTAINER_PATH + "/" + OBJECT_ID);
        assertTrue(com.google.common.io.Files.equal(testFile, offerFile));

        final Digest digest = Digest.digest(testFile, VitamConfiguration.getDefaultDigestType());
        assertEquals(computedDigest, digest.toString());
        assertEquals(offerService.getObjectDigest(CONTAINER_PATH, OBJECT_ID, VitamConfiguration.getDefaultDigestType()),
            digest.toString());

        assertTrue(offerService.isObjectExist(CONTAINER_PATH, OBJECT_ID));
    }

    @Test
    public void getObjectTest() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);

        final InputStream streamToStore = StreamUtils.toInputStream(OBJECT_ID_2_CONTENT);
        offerService.createObject(CONTAINER_PATH, OBJECT_ID_2, streamToStore, OBJECT_TYPE, null,
            VitamConfiguration.getDefaultDigestType());

        final ObjectContent response = offerService.getObject(CONTAINER_PATH, OBJECT_ID_2);
        assertNotNull(response);
        assertNotNull(response.getInputStream());
        response.getInputStream().close();
    }

    @Test
    public void getCapacityOk() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);

        final InputStream streamToStore = StreamUtils.toInputStream(OBJECT_ID_2_CONTENT);
        offerService.createObject(CONTAINER_PATH, OBJECT_ID_2, streamToStore, OBJECT_TYPE, null,
            VitamConfiguration.getDefaultDigestType());

        // check
        final File container = new File(tempFolder.getRoot(), CONTAINER_PATH);
        assertTrue(container.exists());
        assertTrue(container.isDirectory());

        final JsonNode jsonNode = offerService.getCapacity(CONTAINER_PATH);
        assertNotNull(jsonNode);
        assertNotNull(jsonNode.get("usableSpace"));
    }

    @Test
    public void getCapacityNoContainerOK() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);
        final JsonNode jsonNode = offerService.getCapacity(CONTAINER_PATH);
        assertNotNull(jsonNode);
        assertNotNull(jsonNode.get("usableSpace"));
    }

    @Test
    public void deleteObjectTest() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);

        // creation of an object
        final InputStream streamToStore = StreamUtils.toInputStream(OBJECT_ID_2_CONTENT);

        String digest =
            offerService.createObject(CONTAINER_PATH, OBJECT_ID_DELETE, streamToStore,
                DataCategory.UNIT, null, VitamConfiguration.getDefaultDigestType());

        // check if the object has been created
        final ObjectContent response = offerService.getObject(CONTAINER_PATH, OBJECT_ID_DELETE);
        assertNotNull(response);
        assertNotNull(response.getInputStream());
        response.getInputStream().close();



        // check that if we try to delete an object
        // algorithm, it succeeds
        offerService.deleteObject(CONTAINER_PATH, OBJECT_ID_DELETE, DataCategory.UNIT);

        try {
            // check that the object has been deleted
            offerService.getObject(CONTAINER_PATH, OBJECT_ID_DELETE);
            fail("Should raized an exception");
        } catch (ContentAddressableStorageNotFoundException exc) {

        }

    }

    @Test
    public void listCreateCursorNoContainerTest() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);
        offerService.createCursor(CONTAINER_PATH);
    }

    @Test
    public void listCreateCursorTest() throws Exception {
        final DefaultOfferServiceImpl offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);
        offerService.ensureContainerExists(CONTAINER_PATH);
        String cursorId = offerService.createCursor(CONTAINER_PATH);
        assertNotNull(cursorId);
        List<JsonNode> list = offerService.next(CONTAINER_PATH, cursorId);
        assertNotNull(list);
        assertTrue(list.isEmpty());
        list = offerService.next(CONTAINER_PATH, cursorId);
        // TODO manage with exception
        assertNull(list);
    }

    @Test
    public void finalizeCursorTest() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);
        String id = offerService.createCursor(CONTAINER_PATH);
        assertNotNull(id);
        offerService.finalizeCursor(CONTAINER_PATH, id);
    }

    @Test
    public void listCursorTest() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);
        for (int i = 0; i < 150; i++) {
            offerService.createObject(CONTAINER_PATH, OBJECT + i, new FakeInputStream(50), OBJECT_TYPE, null,
                VitamConfiguration.getDefaultDigestType());
        }
        String cursorId = offerService.createCursor(CONTAINER_PATH);
        assertNotNull(cursorId);
        boolean hasNext = offerService.hasNext(CONTAINER_PATH, cursorId);
        assertTrue(hasNext);

        List<JsonNode> list = offerService.next(CONTAINER_PATH, cursorId);
        assertNotNull(list);
        assertEquals(100, list.size());

        hasNext = offerService.hasNext(CONTAINER_PATH, cursorId);
        assertTrue(hasNext);

        list = offerService.next(CONTAINER_PATH, cursorId);
        assertNotNull(list);
        assertEquals(50, list.size());

        hasNext = offerService.hasNext(CONTAINER_PATH, cursorId);
        assertFalse(hasNext);

        list = offerService.next(CONTAINER_PATH, cursorId);
        // TODO manage with exception
        assertNull(list);
    }

    @Test
    public void getOfferLogs() throws Exception {
        when(offerDatabaseService.searchOfferLog(CONTAINER_PATH, 0L, 2, Order.DESC))
            .thenReturn(getOfferLogs(CONTAINER_PATH, 0, 2, Order.DESC));
        when(offerDatabaseService.searchOfferLog(CONTAINER_PATH, 2L, 3, Order.DESC))
            .thenThrow(new ContentAddressableStorageDatabaseException("database error"));
        when(offerDatabaseService.searchOfferLog(CONTAINER_PATH, 5L, 4, Order.DESC))
            .thenThrow(new ContentAddressableStorageServerException("parse error"));
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);

        assertThatCode(() -> {
            offerService.getOfferLogs(CONTAINER_PATH, 0L, 2, Order.DESC);
        }).doesNotThrowAnyException();
        assertThatCode(() -> {
            offerService.getOfferLogs(CONTAINER_PATH, 2L, 3, Order.DESC);
        }).isInstanceOf(ContentAddressableStorageDatabaseException.class);
        assertThatCode(() -> {
            offerService.getOfferLogs(CONTAINER_PATH, 5L, 4, Order.DESC);
        }).isInstanceOf(ContentAddressableStorageServerException.class);
    }

    private List<OfferLog> getOfferLogs(String containerName, long offset, int limit, Order order) {
        List<OfferLog> offerLogs = new ArrayList<>();
        LongStream.range(offset + 1, offset + 1 + limit).forEach(l -> {
            OfferLog offerLog = new OfferLog(containerName, OBJECT + l, OfferLogAction.WRITE);
            offerLog.setSequence(l);
            offerLog.setTime(LocalDateTime.now());
            offerLogs.add(offerLog);
        });
        return offerLogs;
    }
}
