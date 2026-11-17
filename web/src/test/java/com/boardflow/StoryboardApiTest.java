package com.boardflow;

import com.boardflow.domain.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
        "spring.datasource.url=jdbc:h2:mem:boardflow-test;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop"})
class StoryboardApiTest {
    @LocalServerPort int port;
    final HttpClient client = HttpClient.newHttpClient();
    final JsonMapper json = JsonMapper.builder().build();
    HttpResponse<String> request(String method,String path,String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path))
                .header("Content-Type","application/json").method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    ObjectNode create(boolean demo) throws Exception {
        var response=request("POST","/api/projects","{\"title\":\"Test storyboard\",\"demo\":"+demo+"}");
        assertThat(response.statusCode()).isEqualTo(201); return (ObjectNode)json.readTree(response.body());
    }
    String path(JsonNode project) {return "/api/projects/"+project.get("id").asText();}
    @Test void createsEditsReordersAndDeletesPersistentProject() throws Exception {
        ObjectNode project=create(true); String path=path(project);
        assertThat(project.get("shots").size()).isEqualTo(6);
        assertThat(project.get("totalDuration").asInt()).isEqualTo(36);
        var shots=project.withArray("shots");
        String first=shots.get(0).get("id").asText();
        ObjectNode edited=(ObjectNode)shots.get(0); edited.put("title","A <new> scene & a decision");
        edited.put("dialogue","你好 — a new line\nA second line");edited.put("duration",11);edited.put("angle","Low angle");
        // A real one-pixel PNG verifies the image storage path as well as metadata.
        edited.put("image","data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+j8ioAAAAASUVORK5CYII=");
        shots.add(shots.remove(0));
        var saved=request("PUT",path,project.toString());assertThat(saved.statusCode()).isEqualTo(200);
        JsonNode result=json.readTree(request("GET",path,null).body());
        assertThat(result.get("shots").get(5).get("id").asText()).isEqualTo(first);
        assertThat(result.get("shots").get(5).get("dialogue").asText()).contains("你好").contains("\n");
        assertThat(result.get("shots").get(5).get("image").asText()).startsWith("data:image/png;base64,");
        assertThat(result.get("totalDuration").asInt()).isEqualTo(41);
        assertThat(result.get("version").asLong()).isGreaterThan(project.get("version").asLong());
        assertThat(request("GET","/api/projects",null).body()).contains("shotCount");
        assertThat(request("DELETE",path,null).statusCode()).isEqualTo(204);
        assertThat(request("GET",path,null).statusCode()).isEqualTo(404);
    }
    @Test void rejectsStaleUpdatesWithoutOverwritingNewerWork() throws Exception {
        ObjectNode project=create(false);String path=path(project);
        project.put("title","Newer title");assertThat(request("PUT",path,project.toString()).statusCode()).isEqualTo(200);
        project.put("title","Stale title");assertThat(request("PUT",path,project.toString()).statusCode()).isEqualTo(409);
        assertThat(json.readTree(request("GET",path,null).body()).get("title").asText()).isEqualTo("Newer title");
    }
    @Test void validatesDurationCameraSettingsAndUniqueShotIds() throws Exception {
        ObjectNode project=create(true);String path=path(project);ObjectNode first=(ObjectNode)project.withArray("shots").get(0);
        first.put("duration",0);assertThat(request("PUT",path,project.toString()).statusCode()).isEqualTo(400);
        first.put("duration",3601);assertThat(request("PUT",path,project.toString()).statusCode()).isEqualTo(400);
        first.put("duration",6);first.put("angle","Unknown angle");assertThat(request("PUT",path,project.toString()).statusCode()).isEqualTo(400);
        first.put("angle","Eye level");first.put("id",project.get("shots").get(1).get("id").asText());
        assertThat(request("PUT",path,project.toString()).statusCode()).isEqualTo(400);
        assertThat(json.readTree(request("GET",path,null).body()).get("totalDuration").asInt()).isEqualTo(36);
    }
    @Test void rejectsInvalidImagesAndMalformedProjects() throws Exception {
        ObjectNode project=create(true);String path=path(project);ObjectNode first=(ObjectNode)project.withArray("shots").get(0);
        first.put("image","javascript:alert(1)");assertThat(request("PUT",path,project.toString()).statusCode()).isEqualTo(400);
        first.put("image","data:image/png;base64,bm90LWEtcG5n");assertThat(request("PUT",path,project.toString()).statusCode()).isEqualTo(400);
        assertThat(request("POST","/api/projects","{\"title\":\"   \"}").statusCode()).isEqualTo(400);
        assertThat(request("POST","/api/projects","broken-json").statusCode()).isEqualTo(400);
        assertThat(request("GET","/api/projects/"+UUID.randomUUID(),null).statusCode()).isEqualTo(404);
    }
    @Test void servesBrowserApplicationAndStaticAssets() throws Exception {
        assertThat(request("GET","/",null).body()).contains("Storyboard studio").contains("app.js");
        assertThat(request("GET","/styles.css",null).statusCode()).isEqualTo(200);
        assertThat(request("GET","/app.js",null).statusCode()).isEqualTo(200);
    }
    @Test void shotSubclassesProvideDistinctDomainRules() {
        List<StoryboardShot> shots=List.of(new EstablishingShot(""),new DialogueCloseUp("one two three four five six seven eight nine ten"),new ActionTrackingShot(""));
        assertThat(shots.stream().map(StoryboardShot::computeRecommendedDuration)).containsExactly(6,5,8);
        assertThat(shots.stream().map(StoryboardShot::getCameraSetupChecklist)).allSatisfy(list->assertThat(list).hasSize(4));
        assertThat(new DialogueCloseUp("  ").computeRecommendedDuration()).isEqualTo(3);
    }
}
