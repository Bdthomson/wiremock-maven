package wiremock.admin;

import wiremock.admin.model.ListStubMappingsResult;
import wiremock.admin.model.PathParams;
import wiremock.common.Json;
import wiremock.core.Admin;
import wiremock.http.Request;
import wiremock.http.ResponseDefinition;
import wiremock.matching.StringValuePattern;

public class FindStubMappingsByMetadataTask implements AdminTask {

    @Override
    public ResponseDefinition execute(Admin admin, Request request, PathParams pathParams) {
        StringValuePattern pattern = Json.read(request.getBodyAsString(), StringValuePattern.class);
        ListStubMappingsResult stubMappings = admin.findAllStubsByMetadata(pattern);
        return ResponseDefinition.okForJson(stubMappings);
    }
}
