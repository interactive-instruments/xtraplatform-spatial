/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.codelists.app;

import de.ii.xtraplatform.codelists.domain.CodelistData;
import de.ii.xtraplatform.codelists.domain.CodelistImporter;
import de.ii.xtraplatform.web.domain.Endpoint;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataStore;
import io.dropwizard.jersey.caching.CacheControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author zahnen
 */
//TODO: move to xtraplatform, adjust to auto mode
@Path("/admin/codelists/")
@Produces(MediaType.APPLICATION_JSON)
public class CodelistEndpoint implements Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodelistEndpoint.class);

    private final EntityDataStore<CodelistData> codelistRepository;
    private final CodelistImporter codelistImporter;

    CodelistEndpoint(EntityDataStore<EntityData> entityRepository,
                     CodelistImporter codelistImporter) {
        this.codelistRepository = entityRepository.forType(CodelistData.class);
        this.codelistImporter = codelistImporter;
    }

    @GET
    @CacheControl(noCache = true)
    public List<String> getCodelists(/*@Auth(minRole = Role.PUBLISHER) AuthenticatedUser user*/) {
        return codelistRepository.ids();
    }

    @GET
    @Path("/{id}")
    @CacheControl(noCache = true)
    public CodelistData getCodelist(/*@Auth(minRole = Role.PUBLISHER) AuthenticatedUser user,*/
            @PathParam("id") String id) {
        return codelistRepository.get(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @CacheControl(noCache = true)
    public CodelistData addCodelist(/*@Auth(minRole = Role.PUBLISHER) AuthenticatedUser user,*/
            Map<String, String> request) {

        CodelistData codelistData;
        try {
            codelistData = null;//codelistImporter.generate(request);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("The codelist has an invalid source type.");
        }


        if (codelistRepository.has(codelistData.getId())) {
            throw new BadRequestException("A codelist with id '" + codelistData.getId() + "' already exists.");
        }

        try {
            CodelistData added = codelistRepository.put(codelistData.getId(), codelistData)
                                                   .get();

            return added;
        } catch (InterruptedException | ExecutionException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    @DELETE
    @Path("/{id}")
    @CacheControl(noCache = true)
    public void deleteCodelist(/*@Auth(minRole = Role.PUBLISHER) AuthenticatedUser user,*/
            @PathParam("id") String id) throws IOException {
        codelistRepository.delete(id);
    }

}