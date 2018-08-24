package wiremock.admin;

import wiremock.admin.model.PathParams;
import wiremock.common.Json;
import wiremock.core.Admin;
import wiremock.http.Request;
import wiremock.http.ResponseDefinition;
import wiremock.matching.StringValuePattern;

public class RemoveStubMappingsByMetadataTask implements AdminTask {

  @Override
  public ResponseDefinition execute(Admin admin, Request request, PathParams pathParams) {
    StringValuePattern pattern = Json.read(request.getBodyAsString(), StringValuePattern.class);
    admin.removeStubsByMetadata(pattern);
    return ResponseDefinition.okEmptyJson();
  }
}
