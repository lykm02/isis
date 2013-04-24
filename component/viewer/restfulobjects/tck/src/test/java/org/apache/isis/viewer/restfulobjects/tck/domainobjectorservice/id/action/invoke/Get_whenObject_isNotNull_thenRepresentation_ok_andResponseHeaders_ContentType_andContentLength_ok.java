package org.apache.isis.viewer.restfulobjects.tck.domainobjectorservice.id.action.invoke;

import static org.apache.isis.viewer.restfulobjects.tck.RestfulMatchers.hasProfile;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.isis.viewer.restfulobjects.applib.JsonRepresentation;
import org.apache.isis.viewer.restfulobjects.applib.LinkRepresentation;
import org.apache.isis.viewer.restfulobjects.applib.RestfulMediaType;
import org.apache.isis.viewer.restfulobjects.applib.client.RestfulClient;
import org.apache.isis.viewer.restfulobjects.applib.client.RestfulResponse;
import org.apache.isis.viewer.restfulobjects.applib.client.RestfulResponse.Header;
import org.apache.isis.viewer.restfulobjects.applib.domainobjects.ActionResultRepresentation;
import org.apache.isis.viewer.restfulobjects.applib.domainobjects.ActionResultRepresentation.ResultType;
import org.apache.isis.viewer.restfulobjects.applib.domainobjects.DomainObjectRepresentation;
import org.apache.isis.viewer.restfulobjects.applib.domainobjects.DomainServiceResource;
import org.apache.isis.viewer.restfulobjects.applib.domainobjects.ObjectActionRepresentation;
import org.apache.isis.viewer.restfulobjects.applib.util.UrlEncodingUtils;
import org.apache.isis.viewer.restfulobjects.tck.IsisWebServerRule;
import org.apache.isis.viewer.restfulobjects.tck.Util;

public class Get_whenObject_isNotNull_thenRepresentation_ok_andResponseHeaders_ContentType_andContentLength_ok {

    @Rule
    public IsisWebServerRule webServerRule = new IsisWebServerRule();

    private RestfulClient client;

    private DomainServiceResource serviceResource;

    @Before
    public void setUp() throws Exception {
        client = webServerRule.getClient();

        serviceResource = client.getDomainServiceResource();
    }

    @Test
    public void usingClientFollow() throws Exception {

        // given
        final JsonRepresentation givenAction = Util.givenAction(client, "ActionsEntities", "findById");
        final ObjectActionRepresentation actionRepr = givenAction.as(ObjectActionRepresentation.class);

        final LinkRepresentation invokeLink = actionRepr.getInvoke();
        final JsonRepresentation args =invokeLink.getArguments();
        
        // when
        args.mapPut("id.value", 1);

        // when
        final RestfulResponse<ActionResultRepresentation> restfulResponse = client.followT(invokeLink, args);
        
        // then
        then(restfulResponse);
    }

    
    @Test
    public void usingResourceProxy() throws Exception {

        // given, when
        
        JsonRepresentation args = JsonRepresentation.newMap();
        args.mapPut("id.value", 1);

        Response response = serviceResource.invokeActionQueryOnly("ActionsEntities", "findById", UrlEncodingUtils.urlEncode(args));
        RestfulResponse<ActionResultRepresentation> restfulResponse = RestfulResponse.ofT(response);
        
        then(restfulResponse);
    }

    private static void then(final RestfulResponse<ActionResultRepresentation> restfulResponse) throws JsonParseException, JsonMappingException, IOException {
        
        assertThat(restfulResponse.getHeader(Header.CONTENT_TYPE), hasProfile(RestfulMediaType.APPLICATION_JSON_ACTION_RESULT));
        assertThat(restfulResponse.getHeader(Header.CONTENT_LENGTH), is(3335));
        
        final ActionResultRepresentation actionResultRepr = restfulResponse.getEntity();

        assertThat(actionResultRepr.getResultType(), is(ResultType.DOMAIN_OBJECT));
        final DomainObjectRepresentation objRepr = actionResultRepr.getResult().as(DomainObjectRepresentation.class);
        
        assertThat(objRepr.getMembers(), is(not(nullValue())));
        assertThat(objRepr.getMembers().size(), is(greaterThan(0)));
        
        assertThat(objRepr.getTitle(), is(not(nullValue())));
        
        assertThat(objRepr.getLinks(), is(not(nullValue())));
        assertThat(objRepr.getMembers().size(), is(greaterThan(0)));
        
        assertThat(objRepr.getExtensions(), is(not(nullValue())));
    }

    private static Matcher<Integer> greaterThan(final int i) {
        return new TypeSafeMatcher<Integer>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("greater than " + i);
            }

            @Override
            protected boolean matchesSafely(Integer item) {
                return item > i;
            }
        };
    }

    
}
