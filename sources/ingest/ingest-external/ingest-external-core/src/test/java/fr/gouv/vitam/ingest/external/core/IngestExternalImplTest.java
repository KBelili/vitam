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
package fr.gouv.vitam.ingest.external.core;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.format.identification.FormatIdentifier;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.format.identification.exception.FileFormatNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierBadRequestException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierFactoryException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierTechnicalException;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.format.identification.siegfried.FormatIdentifierSiegfried;
import fr.gouv.vitam.ingest.external.common.config.IngestExternalConfiguration;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientMock;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({FormatIdentifierFactory.class})
public class IngestExternalImplTest {
    private static final String PATH = "/tmp";
    private static final String SCRIPT_SCAN_CLAMAV = "scan-clamav.sh";
    IngestExternalImpl ingestExternalImpl;
    private InputStream stream;

    private static final long timeoutScanDelay = 60000;

    @Before
    public void setUp() {
        final IngestExternalConfiguration config = new IngestExternalConfiguration();
        config.setPath(PATH);
        config.setAntiVirusScriptName(SCRIPT_SCAN_CLAMAV);
        config.setTimeoutScanDelay(timeoutScanDelay);
        ingestExternalImpl = new IngestExternalImpl(config);
        PowerMockito.mockStatic(FormatIdentifierFactory.class);
    }

    @Test
    public void getFormatIdentifierFactoryThenThrowFormatIdentifierNotFoundException() throws Exception {
        FormatIdentifierFactory identifierFactory = PowerMockito.mock(FormatIdentifierFactory.class);
        when(FormatIdentifierFactory.getInstance()).thenReturn(identifierFactory);
        when(identifierFactory.getFormatIdentifierFor(anyObject()))
            .thenThrow(new FormatIdentifierNotFoundException(""));
        stream = PropertiesUtils.getResourceAsStream("unfixed-virus.txt");
        ingestExternalImpl.upload(stream);
    }

    @Test
    public void getFormatIdentifierFactoryError() throws Exception {
        FormatIdentifierFactory identifierFactory = PowerMockito.mock(FormatIdentifierFactory.class);
        when(FormatIdentifierFactory.getInstance()).thenReturn(identifierFactory);
        when(identifierFactory.getFormatIdentifierFor(anyObject())).thenThrow(new FormatIdentifierFactoryException(""));
        stream = PropertiesUtils.getResourceAsStream("unfixed-virus.txt");
        ingestExternalImpl.upload(stream);
    }

    @Test
    public void getFormatIdentifierFactoryThenThrowFormatIdentifierTechnicalException() throws Exception {
        FormatIdentifierFactory identifierFactory = PowerMockito.mock(FormatIdentifierFactory.class);
        when(FormatIdentifierFactory.getInstance()).thenReturn(identifierFactory);
        when(identifierFactory.getFormatIdentifierFor(anyObject()))
            .thenThrow(new FormatIdentifierTechnicalException(""));
        stream = PropertiesUtils.getResourceAsStream("unfixed-virus.txt");
        ingestExternalImpl.upload(stream);
    }

    @Test
    public void getFormatIdentifierFactoryThenThrowFileFormatNotFoundException() throws Exception {
        FormatIdentifierFactory identifierFactory = PowerMockito.mock(FormatIdentifierFactory.class);
        when(FormatIdentifierFactory.getInstance()).thenReturn(identifierFactory);

        FormatIdentifier formatIdentifierMock = PowerMockito.mock(FormatIdentifier.class);
        when(identifierFactory.getFormatIdentifierFor(anyObject())).thenReturn(formatIdentifierMock);
        when(formatIdentifierMock.analysePath(anyObject())).thenThrow(new FileFormatNotFoundException(""));

        stream = PropertiesUtils.getResourceAsStream("unfixed-virus.txt");
        ingestExternalImpl.upload(stream);
    }

    @Test
    public void getFormatIdentifierFactoryThenThrowFormatIdentifierBadRequestException() throws Exception {
        FormatIdentifierFactory identifierFactory = PowerMockito.mock(FormatIdentifierFactory.class);
        when(FormatIdentifierFactory.getInstance()).thenReturn(identifierFactory);

        FormatIdentifier formatIdentifierMock = PowerMockito.mock(FormatIdentifier.class);
        when(identifierFactory.getFormatIdentifierFor(anyObject())).thenReturn(formatIdentifierMock);
        when(formatIdentifierMock.analysePath(anyObject())).thenThrow(new FormatIdentifierBadRequestException(""));

        stream = PropertiesUtils.getResourceAsStream("unfixed-virus.txt");
        ingestExternalImpl.upload(stream);
    }


    @Test
    public void formatNotSupportedInInternalReferential() throws Exception {
        FormatIdentifierSiegfried siegfried =
            getMockedFormatIdentifierSiegfried();
        when(siegfried.analysePath(anyObject())).thenReturn(getNotSupprtedFormatIdentifierResponseList());
        stream = PropertiesUtils.getResourceAsStream("unfixed-virus.txt");
        ingestExternalImpl.upload(stream);
    }


    @Test
    public void formatSupportedInInternalReferential() throws Exception {
        FormatIdentifierSiegfried siegfried =
            getMockedFormatIdentifierSiegfried();
        when(siegfried.analysePath(anyObject())).thenReturn(getFormatIdentifierZipResponse());
        stream = PropertiesUtils.getResourceAsStream("unfixed-virus.txt");
        ingestExternalImpl.upload(stream);
    }

    @Test
    public void givenFixedVirusFile()
        throws Exception {
        FormatIdentifierSiegfried siegfried =
            getMockedFormatIdentifierSiegfried();
        when(siegfried.analysePath(anyObject())).thenReturn(getFormatIdentifierTarResponse());
        stream = PropertiesUtils.getResourceAsStream("fixed-virus.txt");
        ingestExternalImpl.upload(stream);
    }

    @Test
    public void givenUnFixedVirusFileAndSupportedMediaType()
        throws Exception {
        FormatIdentifierSiegfried siegfried =
            getMockedFormatIdentifierSiegfried();
        when(siegfried.analysePath(anyObject())).thenReturn(getFormatIdentifierTarResponse());
        stream = PropertiesUtils.getResourceAsStream("unfixed-virus.txt");
        ingestExternalImpl.upload(stream);
    }

    private List<FormatIdentifierResponse> getFormatIdentifierZipResponse() {
        List<FormatIdentifierResponse> list = new ArrayList<>();
        list.add(new FormatIdentifierResponse("ZIP Format", "application/zip" +
            ".zip",
            "x-fmt/263", "pronom"));
        return list;
    }

    @Test
    public void givenNoVirusFile() throws Exception {
        FormatIdentifierSiegfried siegfried =
            getMockedFormatIdentifierSiegfried();
        when(siegfried.analysePath(anyObject())).thenReturn(getFormatIdentifierZipResponse());
        stream = PropertiesUtils.getResourceAsStream("no-virus.txt");
        final Response xmlResponse = ingestExternalImpl.upload(stream);

        final InputStream inputstreamMockATR =
            IOUtils.toInputStream(IngestInternalClientMock.MOCK_INGEST_INTERNAL_RESPONSE_STREAM);

        try {
            assertTrue(IOUtils.contentEquals(inputstreamMockATR, xmlResponse.readEntity(InputStream.class)));
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    private List<FormatIdentifierResponse> getFormatIdentifierTarResponse() {
        List<FormatIdentifierResponse> list = new ArrayList<>();
        list.add(new FormatIdentifierResponse("ZIP Format", "application/x-tar",
            "x-fmt/263", "pronom"));
        return list;
    }


    private List<FormatIdentifierResponse> getNotSupprtedFormatIdentifierResponseList() {
        List<FormatIdentifierResponse> list = new ArrayList<>();
        list.add(new FormatIdentifierResponse("xsd Format", "application/xsd" +
            ".xsd",
            "x-fmt/263", "pronom"));
        return list;
    }


    private FormatIdentifierSiegfried getMockedFormatIdentifierSiegfried()
        throws FormatIdentifierNotFoundException, FormatIdentifierFactoryException, FormatIdentifierTechnicalException {
        FormatIdentifierSiegfried siegfried = mock(FormatIdentifierSiegfried.class);
        FormatIdentifierFactory identifierFactory = PowerMockito.mock(FormatIdentifierFactory.class);
        when(FormatIdentifierFactory.getInstance()).thenReturn(identifierFactory);
        when(identifierFactory.getFormatIdentifierFor(anyObject())).thenReturn(siegfried);
        return siegfried;
    }
}
