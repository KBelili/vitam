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
package fr.gouv.vitam.common.database.builder.facet;

import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FACET;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FACETARGS;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * Terms facet
 */
public class TermsFacet extends Facet {

    /**
     * Terms Facet constructor
     * 
     * @param name name of the facet
     * @param field field of the facet data
     * @throws InvalidCreateOperationException when not valid
     */
    public TermsFacet(String name, String field) throws InvalidCreateOperationException {
        super(name);
        setName(name);
        currentTokenFACET = FACET.TERMS;
        ObjectNode facetNode = JsonHandler.createObjectNode();
        facetNode.put(FACETARGS.FIELD.exactToken(), field);
        currentFacet = facetNode;
    }

    /**
     * Terms Facet constructor
     * 
     * @param name name of the facet
     * @param field field of the facet data
     * @param size of the facet
     * @throws InvalidCreateOperationException when not valid
     */
    public TermsFacet(String name, String field, Integer size) throws InvalidCreateOperationException {
        this(name, field);
        if (size == null || size <= 0) {
            throw new InvalidCreateOperationException("Size must be > 0 in Terms Facet");
        }
        currentFacet.put(FACETARGS.SIZE.exactToken(), size);
    }

}
