package com.dpgil.pathlinker.path_linker.internal;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;

import com.dpgil.pathlinker.path_linker.internal.model.PathLinkerModel;

@Api
@Path("/pathlinker")
public interface PathLinkerCyRest {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public PathLinkerModel testModel();
}
