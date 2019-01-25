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
package fr.gouv.vitam.batch.report.rest.repository;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.batch.report.model.PreservationReportModel;
import fr.gouv.vitam.batch.report.model.PreservationStatsModel;
import fr.gouv.vitam.batch.report.model.PreservationStatus;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import org.bson.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.ANALYSE;
import static org.assertj.core.api.Assertions.assertThat;

public class PreservationReportRepositoryTest {

    private static final int TENANT_ID = 0;
    public static final String PRESERVATION_REPORT = "PreservationReport" + GUIDFactory.newGUID().getId();

    @Rule
    public MongoRule mongoRule =
        new MongoRule(getMongoClientOptions(), PRESERVATION_REPORT);

    private PreservationReportRepository repository;

    private MongoCollection<Document> preservationReportCollection;
    private PreservationReportModel preservationReportModel;
    private String processId;

    @Before
    public void setUp() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        repository = new PreservationReportRepository(mongoDbAccess, PRESERVATION_REPORT);
        preservationReportCollection = mongoRule.getMongoCollection(PRESERVATION_REPORT);
        processId = "aeeaaaaaacgw45nxaaopkalhchougsiaaaaq";
        preservationReportModel = new PreservationReportModel("aeaaaaaaaagw45nxabw2ualhc4jvawqaaaaq", processId,
            TENANT_ID, "2018-11-15T11:13:20.986",
            PreservationStatus.OK, "unitId", "objectGroupId", ANALYSE, "VALID_ALL",
            "aeaaaaaaaagh65wtab27ialg5fopxnaaaaaq", "");
    }

    @Test
    public void should_append_report() {
        // Given
        populateDatabase();

        // When
        Document report = preservationReportCollection.find(
            and(eq("_id", "aeaaaaaaaagw45nxabw2ualhc4jvawqaaaaq"), eq(PreservationReportModel.TENANT, 0))).first();

        // Then
        assertThat(report.get("_id")).isEqualTo(preservationReportModel.getId());
        assertThat(report.get("objectGroupId")).isEqualTo(preservationReportModel.getObjectGroupId());
        assertThat(PreservationStatus.valueOf(report.get("status").toString())).isEqualTo(preservationReportModel.getStatus());
        assertThat(report.get("analyseResult")).isEqualTo(preservationReportModel.getAnalyseResult());
    }

    @Test
    public void should_find_collection_by_processid_tenant() {
        // Given
        populateDatabase();
        // When
        MongoCursor<PreservationReportModel> iterator =
            repository.findCollectionByProcessIdTenant(processId, TENANT_ID);

        // Then
        List<PreservationReportModel> documents = new ArrayList<>();
        while (iterator.hasNext()) {
            PreservationReportModel reportModel = iterator.next();
            documents.add(reportModel);
        }
        assertThat(documents.size()).isEqualTo(1);
    }

    private void populateDatabase() {
        List<PreservationReportModel> reports = new ArrayList<>();
        reports.add(preservationReportModel);
        repository.bulkAppendReport(reports);
    }

    @Test
    public void should_generate_statistic() {
        // Given
        populateDatabase();

        // When
        PreservationStatsModel stats = repository.stats(processId, TENANT_ID);

        // Then
        assertThat(stats.getNbActionsAnaylse()).isEqualTo(1);
        assertThat(stats.getNbStatusKos()).isEqualTo(0);
        assertThat(stats.getAnalyseResults().get("VALID_ALL")).isEqualTo(1);
    }

    @Test
    public void should_delete_report_by_id_and_tenant() {
        // Given
        populateDatabase();
        // When
        repository.deleteReportByIdAndTenant(processId, TENANT_ID);
        // Then
        FindIterable<Document> iterable =
            preservationReportCollection.find(and(eq("processId", processId), eq("tenantId", TENANT_ID)));
        MongoCursor<Document> iterator = iterable.iterator();
        List<Document> documents = new ArrayList<>();
        while (iterator.hasNext()) {
            documents.add(iterator.next());
        }
        assertThat(documents).isEmpty();
        assertThat(documents.size()).isEqualTo(0);
    }

}
