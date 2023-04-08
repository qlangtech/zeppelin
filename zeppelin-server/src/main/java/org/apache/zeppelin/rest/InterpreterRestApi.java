/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zeppelin.rest;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.zeppelin.annotation.ZeppelinApi;
import org.apache.zeppelin.common.Message;
import org.apache.zeppelin.common.Message.OP;
import org.apache.zeppelin.dep.Repository;
import org.apache.zeppelin.interpreter.InterpreterException;
import org.apache.zeppelin.interpreter.InterpreterOption;
import org.apache.zeppelin.interpreter.InterpreterPropertyType;
import org.apache.zeppelin.interpreter.InterpreterSetting;
import org.apache.zeppelin.interpreter.InterpreterSettingManager;
import org.apache.zeppelin.notebook.AuthorizationService;
import org.apache.zeppelin.rest.message.InterpreterInstallationRequest;
import org.apache.zeppelin.rest.message.NewInterpreterSettingRequest;
import org.apache.zeppelin.rest.message.RestartInterpreterRequest;
import org.apache.zeppelin.rest.message.UpdateInterpreterSettingRequest;
import org.apache.zeppelin.server.JsonResponse;
import org.apache.zeppelin.service.AuthenticationService;
import org.apache.zeppelin.service.InterpreterService;
import org.apache.zeppelin.service.ServiceContext;
import org.apache.zeppelin.service.SimpleServiceCallback;
import org.apache.zeppelin.socket.NotebookServer;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interpreter Rest API.
 */
@Path("/interpreter")
@Produces("application/json")
@Singleton
public class InterpreterRestApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(InterpreterRestApi.class);

    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;
    private final InterpreterService interpreterService;
    private final InterpreterSettingManager interpreterSettingManager;
    private final NotebookServer notebookServer;

    @Inject
    public InterpreterRestApi(
            AuthenticationService authenticationService,
            AuthorizationService authorizationService,
            InterpreterService interpreterService,
            InterpreterSettingManager interpreterSettingManager,
            NotebookServer notebookWsServer) {
        this.authenticationService = authenticationService;
        this.authorizationService = authorizationService;
        this.interpreterService = interpreterService;
        this.interpreterSettingManager = interpreterSettingManager;
        this.notebookServer = notebookWsServer;
    }

    /**
     * List all interpreter settings.
     */
    @GET
    @Path("setting")
    @ZeppelinApi
    public Response listSettings() {
        return new JsonResponse<>(Status.OK, "", interpreterSettingManager.get()).build();
    }

    /**
     * Get a setting.
     */
    @GET
    @Path("setting/{settingId}")
    @ZeppelinApi
    public Response getSetting(@PathParam("settingId") String settingId) {
        try {
            InterpreterSetting setting = interpreterSettingManager.get(settingId);
            if (setting == null) {
                return new JsonResponse<>(Status.NOT_FOUND).build();
            } else {
                return new JsonResponse<>(Status.OK, "", setting).build();
            }
        } catch (NullPointerException e) {
            LOGGER.error("Exception in InterpreterRestApi while creating ", e);
            return new JsonResponse<>(Status.INTERNAL_SERVER_ERROR, e.getMessage(),
                    ExceptionUtils.getStackTrace(e)).build();
        }
    }

    /**
     * Add new interpreter setting.
     *
     * @param message NewInterpreterSettingRequest
     */
    @POST
    @Path("setting")
    @ZeppelinApi
    public Response newSettings(String message) {
        try {
            NewInterpreterSettingRequest request =
                    NewInterpreterSettingRequest.fromJson(message);
            if (request == null) {
                return new JsonResponse<>(Status.BAD_REQUEST).build();
            }


            InterpreterSetting interpreterSetting = interpreterSettingManager
                    .createNewSetting(request.getName(), request.getGroup(), request.getDependencies(),
                            (request.getOption() == null ? new InterpreterOption() : request.getOption()), request.getProperties());
            LOGGER.info("new setting created with {}", interpreterSetting.getId());
            return new JsonResponse<>(Status.OK, "", interpreterSetting).build();
        } catch (IOException e) {
            LOGGER.error("Exception in InterpreterRestApi while creating ", e);
            return new JsonResponse<>(Status.NOT_FOUND, e.getMessage(), ExceptionUtils.getStackTrace(e))
                    .build();
        }
    }

    @PUT
    @Path("setting/{settingId}")
    @ZeppelinApi
    public Response updateSetting(String message, @PathParam("settingId") String settingId) {
        LOGGER.info("Update interpreterSetting {}", settingId);

        try {
            UpdateInterpreterSettingRequest request =
                    UpdateInterpreterSettingRequest.fromJson(message);
            interpreterSettingManager
                    .setPropertyAndRestart(settingId, request.getOption(), request.getProperties(),
                            request.getDependencies());
        } catch (InterpreterException e) {
            LOGGER.error("Exception in InterpreterRestApi while updateSetting ", e);
            return new JsonResponse<>(Status.NOT_FOUND, e.getMessage(), ExceptionUtils.getStackTrace(e))
                    .build();
        } catch (IOException e) {
            LOGGER.error("Exception in InterpreterRestApi while updateSetting ", e);
            return new JsonResponse<>(Status.INTERNAL_SERVER_ERROR, e.getMessage(),
                    ExceptionUtils.getStackTrace(e)).build();
        }
        InterpreterSetting setting = interpreterSettingManager.get(settingId);
        if (setting == null) {
            return new JsonResponse<>(Status.NOT_FOUND, "", settingId).build();
        }
        return new JsonResponse<>(Status.OK, "", setting).build();
    }

    /**
     * Remove interpreter setting.
     */
    @DELETE
    @Path("setting/{settingId}")
    @ZeppelinApi
    public Response removeSetting(@PathParam("settingId") String settingId) throws IOException {
        LOGGER.info("Remove interpreterSetting {}", settingId);
        interpreterSettingManager.remove(settingId);
        return new JsonResponse<>(Status.OK).build();
    }

    /**
     * Restart interpreter setting.
     */
    @PUT
    @Path("setting/restart/{settingId}")
    @ZeppelinApi
    public Response restartSetting(String message, @PathParam("settingId") String settingId) {
        LOGGER.info("Restart interpreterSetting {}, msg={}", settingId, message);

        InterpreterSetting setting = interpreterSettingManager.get(settingId);
        try {
            RestartInterpreterRequest request = RestartInterpreterRequest.fromJson(message);

            String noteId = request == null ? null : request.getNoteId();
            if (null == noteId) {
                interpreterSettingManager.close(settingId);
            } else {
                Set<String> entities = new HashSet<>();
                entities.add(authenticationService.getPrincipal());
                entities.addAll(authenticationService.getAssociatedRoles());
                if (authorizationService.hasRunPermission(entities, noteId) ||
                        authorizationService.hasWritePermission(entities, noteId) ||
                        authorizationService.isOwner(entities, noteId)) {
                    interpreterSettingManager.restart(settingId, authenticationService.getPrincipal(), noteId);
                } else {
                    return new JsonResponse<>(Status.FORBIDDEN, "No privilege to restart interpreter")
                            .build();
                }
            }
        } catch (InterpreterException e) {
            LOGGER.error("Exception in InterpreterRestApi while restartSetting ", e);
            return new JsonResponse<>(Status.NOT_FOUND, e.getMessage(), ExceptionUtils.getStackTrace(e))
                    .build();
        }
        if (setting == null) {
            return new JsonResponse<>(Status.NOT_FOUND, "", settingId).build();
        }
        return new JsonResponse<>(Status.OK, "", setting).build();
    }

    /**
     * List all available interpreters by group.
     */
    @GET
    @ZeppelinApi
    public Response listInterpreter() {
        Map<String, InterpreterSetting> m = interpreterSettingManager.getInterpreterSettingTemplates();
        return new JsonResponse<>(Status.OK, "", m).build();
    }

    /**
     * List of dependency resolving repositories.
     */
    @GET
    @Path("repository")
    @ZeppelinApi
    public Response listRepositories() {
        List<RemoteRepository> interpreterRepositories = interpreterSettingManager.getRepositories();
        return new JsonResponse<>(Status.OK, "", interpreterRepositories).build();
    }

    /**
     * Add new repository.
     *
     * @param message Repository
     */
    @POST
    @Path("repository")
    @ZeppelinApi
    public Response addRepository(String message) {
        try {
            Repository request = Repository.fromJson(message);
            interpreterSettingManager.addRepository(request.getId(), request.getUrl(),
                    request.isSnapshot(), request.getAuthentication(), request.getProxy());
            LOGGER.info("New repository {} added", request.getId());
        } catch (Exception e) {
            LOGGER.error("Exception in InterpreterRestApi while adding repository ", e);
            return new JsonResponse<>(Status.INTERNAL_SERVER_ERROR, e.getMessage(),
                    ExceptionUtils.getStackTrace(e)).build();
        }
        return new JsonResponse<>(Status.OK).build();
    }

    /**
     * Delete repository.
     *
     * @param repoId ID of repository
     */
    @DELETE
    @Path("repository/{repoId}")
    @ZeppelinApi
    public Response removeRepository(@PathParam("repoId") String repoId) {
        LOGGER.info("Remove repository {}", repoId);
        try {
            interpreterSettingManager.removeRepository(repoId);
        } catch (Exception e) {
            LOGGER.error("Exception in InterpreterRestApi while removing repository ", e);
            return new JsonResponse<>(Status.INTERNAL_SERVER_ERROR, e.getMessage(),
                    ExceptionUtils.getStackTrace(e)).build();
        }
        return new JsonResponse<>(Status.OK).build();
    }

    /**
     * Get available types for property
     */
    @GET
    @Path("property/types")
    public Response listInterpreterPropertyTypes() {
        return new JsonResponse<>(Status.OK, InterpreterPropertyType.getTypes()).build();
    }

    /**
     * Install interpreter
     */
    @POST
    @Path("install")
    @ZeppelinApi
    public Response installInterpreter(@NotNull String message) {
        LOGGER.info("Install interpreter: {}", message);
        InterpreterInstallationRequest request = InterpreterInstallationRequest.fromJson(message);

        try {
            interpreterService.installInterpreter(
                    request,
                    new SimpleServiceCallback<String>() {
                        @Override
                        public void onStart(String message, ServiceContext context) {
                            Message m = new Message(OP.INTERPRETER_INSTALL_STARTED);
                            Map<String, Object> data = new HashMap<>();
                            data.put("result", "Starting");
                            data.put("message", message);
                            m.data = data;
                            notebookServer.broadcast(m);
                        }

                        @Override
                        public void onSuccess(String message, ServiceContext context) {
                            Message m = new Message(OP.INTERPRETER_INSTALL_RESULT);
                            Map<String, Object> data = new HashMap<>();
                            data.put("result", "Succeed");
                            data.put("message", message);
                            m.data = data;
                            notebookServer.broadcast(m);
                        }

                        @Override
                        public void onFailure(Exception ex, ServiceContext context) {
                            Message m = new Message(OP.INTERPRETER_INSTALL_RESULT);
                            Map<String, Object> data = new HashMap<>();
                            data.put("result", "Failed");
                            data.put("message", ex.getMessage());
                            m.data = data;
                            notebookServer.broadcast(m);
                        }
                    });
        } catch (Throwable t) {
            return new JsonResponse<>(Status.INTERNAL_SERVER_ERROR, t.getMessage()).build();
        }

        return new JsonResponse<>(Status.OK).build();
    }
}
