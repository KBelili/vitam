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
package fr.gouv.vitam.worker.core.plugin.evidence;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.functional.administration.common.BackupService;
import fr.gouv.vitam.functional.administration.common.exception.BackupServiceException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static fr.gouv.vitam.common.json.JsonHandler.unprettyPrint;


/**
 * EvidenceAuditFinalize class
 */
public class DataCorrectionFinalize extends ActionHandler {

    private static final String CORRECTION_FINALIZE = "CORRECTION_FINALIZE";
    BackupService backupService = new BackupService();


    @VisibleForTesting DataCorrectionFinalize(BackupService backupService) {
        this.backupService = backupService;
    }

    public DataCorrectionFinalize() { /*nothing to do */ }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handlerIO)
        throws ProcessingException, ContentAddressableStorageServerException {
        ItemStatus itemStatus = new ItemStatus(CORRECTION_FINALIZE);

        try {


            File reportFile = handlerIO.getNewLocalFile("report.json");
            List<URI> uriListObjectsWorkspace =
                handlerIO.getUriList(handlerIO.getContainerName(), param.getObjectName());

            try (FileOutputStream fileOutputStream = new FileOutputStream(reportFile);
                BufferedOutputStream buffOut = new BufferedOutputStream(fileOutputStream);
            ) {

                for (URI uri : uriListObjectsWorkspace) {

                    File file = handlerIO.getFileFromWorkspace(param.getObjectName() + "/" + uri.getPath());

                    ArrayList reportLine = JsonHandler.getFromFile(file, ArrayList.class);

                    buffOut.write(unprettyPrint(reportLine).getBytes());
                    buffOut.write(System.lineSeparator().getBytes());
                    buffOut.flush();
                }

                backupService
                    .backup(new FileInputStream(reportFile), DataCategory.REPORT,
                        handlerIO.getContainerName() + ".json");
            }

        } catch (ContentAddressableStorageNotFoundException | IOException | InvalidParseOperationException | BackupServiceException e) {
            throw new ProcessingException(e);

        }

        itemStatus.increment(StatusCode.OK);
        return new ItemStatus(CORRECTION_FINALIZE).setItemsStatus(CORRECTION_FINALIZE, itemStatus);
    }



}
