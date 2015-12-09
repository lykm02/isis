/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.isis.core.metamodel.services.swagger.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;

import org.apache.isis.applib.annotation.ActionSemantics;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.applib.filter.Filters;
import org.apache.isis.applib.services.swagger.SwaggerService;
import org.apache.isis.core.commons.factory.InstanceUtil;
import org.apache.isis.core.metamodel.facets.object.domainservice.DomainServiceFacet;
import org.apache.isis.core.metamodel.services.ServiceUtil;
import org.apache.isis.core.metamodel.spec.ActionType;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.SpecificationLoader;
import org.apache.isis.core.metamodel.spec.feature.Contributed;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.core.metamodel.spec.feature.ObjectActionParameter;

import io.swagger.models.Info;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.util.Json;
import io.swagger.util.Yaml;

public class SwaggerSpecGenerator {

    private final SpecificationLoader specificationLoader;

    public SwaggerSpecGenerator(final SpecificationLoader specificationLoader) {
        this.specificationLoader = specificationLoader;
    }

    public String generate(final SwaggerService.Visibility visibility, final SwaggerService.Format format) {
        final Swagger swagger = new Swagger();
        swagger.basePath("/restful");
        swagger.info(new Info()
                        .version("1.0.0")
                        .title("Restful Objects"))
                .path("/",
                        new Path()
                            .get(new Operation()
                                    .description(roSpec("5.1"))
                                    .produces("application/json")
                                    .produces("application/json;profile=urn:org.restfulobjects:repr-types/home-page")
                                    .response(200,
                                            newResponse(Caching.NON_EXPIRING)
                                            .description("OK")
                                            .schema(new RefProperty("#/definitions/home-page"))
                                    )))
                .path("/user",
                        new Path()
                            .get(new Operation()
                                    .description(roSpec("6.1"))
                                    .produces("application/json")
                                    .produces("application/json;profile=urn:org.restfulobjects:repr-types/user")
                                    .response(200,
                                            newResponse(Caching.USER_INFO)
                                            .description("OK")
                                            .schema(new RefProperty("#/definitions/user"))
                                    )))
                .path("/services",
                        new Path()
                                .get(new Operation()
                                        .description(roSpec("7.1"))
                                        .produces("application/json")
                                        .produces("application/json;profile=urn:org.restfulobjects:repr-types/services")
                                        .response(200,
                                                newResponse(Caching.USER_INFO)
                                                        .description("OK")
                                                        .schema(new RefProperty("#/definitions/services"))
                                        )))

                .path("/version",
                        new Path()
                            .get(new Operation()
                                    .description(roSpec("8.1"))
                                    .produces("application/json")
                                    .produces("application/json;profile=urn:org.restfulobjects:repr-types/version")
                                    .response(200,
                                            newResponse(Caching.NON_EXPIRING)
                                            .description("OK")
                                            .schema(new RefProperty("#/definitions/version"))
                                    )));

        swagger.addDefinition("home-page",
                newModel(roSpec("5.2")));

        swagger.addDefinition("user",
                newModel(roSpec("6.2"))
                    .property("userName", stringProperty())
                    .property("roles", arrayOfStrings())
                    .property("links", arrayOfLinksGetOnly())
                    .required("userName")
                    .required("roles")
                );

        swagger.addDefinition("services",
                newModel(roSpec("7.2"))
                    .property("value", arrayOfLinksGetOnly())
                    .required("userName")
                    .required("roles")
                );

        swagger.addDefinition("version",
                newModel(roSpec("8.2"))
                    .property("specVersion", stringProperty())
                    .property("implVersion", stringProperty())
                    .property("optionalCapabilities",
                            new ObjectProperty()
                                .property("blobsClobs", stringProperty())
                                .property("deleteObjects", stringProperty())
                                .property("domainModel", stringProperty())
                                .property("validateOnly", stringProperty())
                                .property("protoPersistentObjects", stringProperty())
                            )
                    .required("userName")
                    .required("roles")
                );

        swagger.addDefinition("link",
                new ModelImpl()
                    .type("object")
                    .property("rel", stringProperty())
                    .property("href", stringProperty())
                    .property("title", stringProperty())
                    .property("method", stringPropertyEnum("GET", "POST", "PUT", "DELETE"))
                    .property("type", stringProperty())
                    .property("arguments", new ObjectProperty())
                    .required("rel")
                    .required("href")
                    .required("method")
        );

        swagger.addDefinition("link-get-only",
                new ModelImpl()
                    .type("object")
                    .property("rel", stringProperty())
                    .property("href", stringProperty())
                    .property("title", stringProperty())
                    .property("method", stringPropertyEnum("GET"))
                    .property("type", stringProperty())
                    .required("rel")
                    .required("href")
                    .required("method")
        );


        final Collection<ObjectSpecification> allSpecs = specificationLoader.allSpecifications();
        for (ObjectSpecification serviceSpec : allSpecs) {
            final DomainServiceFacet domainServiceFacet = serviceSpec.getFacet(DomainServiceFacet.class);
            if (domainServiceFacet == null) {
                continue;
            }
            if (!isVisible(visibility, domainServiceFacet)) {
                continue;
            }

            List<ObjectAction> serviceActions = serviceSpec
                    .getObjectActions(actionTypesFor(visibility), Contributed.EXCLUDED, Filters.<ObjectAction>any());
            if(serviceActions.isEmpty()) {
                continue;
            }

            String serviceId = serviceIdFor(serviceSpec);
            final String serviceDefinitionId = "service-" + serviceId;

            final Path servicePath = new Path();
            final String servicePathStr = "/services/" + serviceId;
            swagger.path(servicePathStr, servicePath);

            servicePath.get(
                    new Operation()
                        .description(roSpec("15.1"))
                        .produces("application/json")
                        .produces("application/json;profile=urn:org.restfulobjects:repr-types/object")
                        .response(200,
                                newResponse(Caching.TRANSACTIONAL)
                                    .description("OK")
                                    .schema(new RefProperty("#/definitions/"
                                            + serviceDefinitionId)))
            );

            final ObjectProperty serviceMembers = new ObjectProperty();
            ModelImpl serviceRepr =
                    newModel(roSpec("15.1.2") + ": representation of " + serviceId)
                    .property("title", stringProperty())
                    .property("serviceId", stringProperty()._default(serviceId))
                    .property("members", serviceMembers);

            swagger.addDefinition(serviceDefinitionId, serviceRepr);

            for (final ObjectAction serviceAction : serviceActions) {
                String serviceActionId = serviceAction.getId();

                // within the services representation itself
                final String serviceActionPromptPathRelStr = "actions/" + serviceActionId;
                serviceMembers.property(serviceActionId,
                        new ObjectProperty()
                            .property("id", stringPropertyEnum(serviceActionId))
                            .property("memberType", stringPropertyEnum("action"))
                            .property("links",
                                    new ObjectProperty()
                                        .property("rel", stringPropertyEnum("urn:org.restfulobjects:rels/details;action=" + serviceActionId + ""))
                                        .property("href", stringPropertyEnum(serviceActionPromptPathRelStr)))
                            .property("method", stringPropertyEnum("GET"))
                            .property("type", stringPropertyEnum("application/json;profile=urn:org.restfulobjects:repr-types/object-action"))
                );

                // and for the service action's own resources (prompt and invoke)
                final ObjectProperty actionParametersRepr = new ObjectProperty();
                swagger.path(servicePathStr + "/" + serviceActionPromptPathRelStr,
                        new Path()
                                .get(
                                        new Operation()
                                                .description(roSpec("18.1") + ": (prompt) resource for " + serviceId + "#" + serviceActionId)
                                                .produces("application/json")
                                                .produces("application/json;profile=urn:org.restfulobjects:repr-types/object-action")
                                                .response(200,
                                                        new Response()
                                                                .description(roSpec("18.2") + ": (prompt) representation of " + serviceId + "#" + serviceActionId)
                                                                .schema(new ObjectProperty()
                                                                        .property("id",
                                                                                stringPropertyEnum(serviceActionId))
                                                                        .property("memberType",
                                                                                stringPropertyEnum("action"))
                                                                        .property("links", arrayOfLinksGetOnly())
                                                                        .property("parameters", actionParametersRepr)
                                                                ))));
                final List<ObjectActionParameter> parameters = serviceAction.getParameters();

                int i = 0;
                for (final ObjectActionParameter parameter : parameters) {
                    final String parameterId = parameter.getId();
                    actionParametersRepr.property(parameterId,
                            new ObjectProperty()
                                .property("num", new IntegerProperty()._default(i++))
                                .property("id", stringPropertyEnum(parameterId))
                                .property("name", stringPropertyEnum(parameter.getName()))
                                .property("description", stringPropertyEnum(parameter.getDescription()))
                    );
                }

                // invoke path
                final Path serviceActionInvokePath = new Path();
                swagger.path( servicePathStr + "/" + serviceActionPromptPathRelStr + "/invoke", serviceActionInvokePath);

                final Operation invokeOperation =
                        new Operation()
                            .description(roSpec("19.1") + ": (invoke) resource of " + serviceId + "#" + serviceActionId)
                            .produces("application/json")
                            .produces("application/json;profile=urn:org.restfulobjects:repr-types/action-result")
                            .produces("application/json;profile=urn:org.apache.isis/v1")
                            .produces("application/json;profile=urn:org.apache.isis/v1;suppress=true");

                final ActionSemantics.Of semantics = serviceAction.getSemantics();
                if(semantics.isSafeInNature()) {
                    serviceActionInvokePath.get(invokeOperation);

                    for (final ObjectActionParameter parameter : parameters) {
                        invokeOperation
                                .parameter(
                                        new QueryParameter()
                                                .name(parameter.getId())
                                                .description(roSpec("2.9.1") + (!Strings.isNullOrEmpty(parameter.getDescription())? (": " + parameter.getDescription()) : ""))
                                                .required(false)
                                                .type("string")
                                );
                    }
                    if(!parameters.isEmpty()) {
                        invokeOperation.parameter(new QueryParameter()
                                .name("x-isis-querystring")
                                .description(roSpec("2.10") + ": all (formal) arguments as base64 encoded string")
                                .required(false)
                                .type("string"));
                    }

                } else {
                    if (semantics.isIdempotentInNature()) {
                        serviceActionInvokePath.put(invokeOperation);
                    } else {
                        serviceActionInvokePath.post(invokeOperation);
                    }

                    final ModelImpl bodyParam =
                            new ModelImpl()
                                .type("object");
                    for (final ObjectActionParameter parameter : parameters) {

                        final Property valueProperty;
                        // TODO: need to switch on parameter's type and create appopriate impl of valueProperty
                        // if(parameter.getSpecification().isValue()) ...
                        valueProperty = stringProperty();

                        bodyParam
                                .property(parameter.getId(),
                                        new ObjectProperty()
                                                .property("value", valueProperty)
                                );
                    }

                    invokeOperation
                            .consumes("application/json")
                            .parameter(
                                    new BodyParameter()
                                            .name("body")
                                            .schema(bodyParam));

                }


                invokeOperation
                        .response(
                                200, new Response()
                                        .description(roSpecForResponseOf(serviceAction) + ": (invoke) representation of " + serviceId + "#" + serviceActionId)
                                    .schema(new ObjectProperty())
                        );
            }

        }

        switch (format) {
            case JSON:
                return Json.pretty(swagger);
            case YAML:
                try {
                    return Yaml.pretty().writeValueAsString(swagger);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            default:
                throw new IllegalArgumentException("Unrecognized format: " + format);
        }
    }

    private String roSpecForResponseOf(final ObjectAction action) {
        final ActionSemantics.Of semantics = action.getSemantics();
        switch (semantics) {
            case SAFE_AND_REQUEST_CACHEABLE:
            case SAFE:
                return "19.2";
            case IDEMPOTENT:
            case IDEMPOTENT_ARE_YOU_SURE:
                return "19.3";
            default:
                return "19.4";
        }
    }

    boolean isVisible(final SwaggerService.Visibility visibility, final DomainServiceFacet domainServiceFacet) {
        if (domainServiceFacet.getNatureOfService() == NatureOfService.VIEW_REST_ONLY) {
            return true;
        }
        if (visibility.isPublic()) {
            return false;
        }
        return domainServiceFacet.getNatureOfService() == NatureOfService.VIEW_MENU_ONLY ||
               domainServiceFacet.getNatureOfService() == NatureOfService.VIEW;
    }

    private ModelImpl newModel(final String description) {
        return new ModelImpl()
                .description(description)
                .type("object")
                .property("links", arrayOfLinksGetOnly())
                .property("extensions", new MapProperty())
                .required("links")
                .required("extensions");
    }

    // TODO: this is horrid, there ought to be a facet we can call instead...
    String serviceIdFor(final ObjectSpecification serviceSpec) {
        Object tempServiceInstance = InstanceUtil.createInstance(serviceSpec.getCorrespondingClass());
        return ServiceUtil.id(tempServiceInstance);
    }

    StringProperty stringProperty() {
        return new StringProperty();
    }

    StringProperty stringPropertyEnum(String... enumValues) {
        StringProperty stringProperty = stringProperty();
        stringProperty._enum(Arrays.asList(enumValues));
        if(enumValues.length >= 1) {
            stringProperty._default(enumValues[0]);
        }
        return stringProperty;
    }

    ArrayProperty arrayOfLinksGetOnly() {
        return new ArrayProperty()
                .items(new RefProperty("#/definitions/link-get-only"));
    }

    ArrayProperty arrayOfStrings() {
        return new ArrayProperty().items(stringProperty());
    }

    private Response newResponse(final Caching caching) {
        return withCachingHeaders(new Response(), caching);
    }

    enum Caching {
        TRANSACTIONAL {
            @Override public void withHeaders(final Response response) {

            }
        },
        USER_INFO {
            @Override public void withHeaders(final Response response) {
                response
                        .header("Cache-Control",
                                new IntegerProperty()
                                        ._default(3600));
            }
        },
        NON_EXPIRING {
            @Override public void withHeaders(final Response response) {
                response
                        .header("Cache-Control",
                                new IntegerProperty()
                                        ._default(86400).description(roSpec("2.13")));
            }
        };

        public abstract void withHeaders(final Response response);
    }

    private static String roSpec(final String section) {
        return "RO Spec v1.0, section " + section;
    }

    private Response withCachingHeaders(final Response response, final Caching caching) {
        caching.withHeaders(response);

        return response;
    }

    private List<ActionType> actionTypesFor(final SwaggerService.Visibility visibility) {
        switch (visibility) {
        case PUBLIC:
            return Arrays.asList(ActionType.USER);
        case PRIVATE:
            return Arrays.asList(ActionType.USER);
        case PRIVATE_WITH_PROTOTYPING:
            return Arrays.asList(ActionType.USER, ActionType.EXPLORATION, ActionType.PROTOTYPE, ActionType.DEBUG);
        }
        throw new IllegalArgumentException("Unrecognized type '" + visibility + "'");
    }


}