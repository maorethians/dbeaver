/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.connection;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWNetworkProfile;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.runtime.IVariableResolver;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Connection configuration.
 */
public class DBPConnectionConfiguration implements DBPObject {
    // Variables
    public static final String VARIABLE_HOST = "host";
    public static final String VARIABLE_PORT = "port";
    public static final String VARIABLE_SERVER = "server";
    public static final String VARIABLE_DATABASE = "database";
    public static final String VARIABLE_USER = "user";
    public static final String VARIABLE_PASSWORD = "password";
    public static final String VARIABLE_URL = "url";

    public static final String VAR_PROJECT_PATH = "project.path";
    public static final String VAR_PROJECT_NAME = "project.name";

    private String hostName;
    private String hostPort;
    private String serverName;
    private String databaseName;
    private String userName;
    private String userPassword;
    private String url;
    private String clientHomeId;

    private String configProfileName;
    private String userProfileName;

    @NotNull
    private final Map<String, String> properties;
    @NotNull
    private final Map<String, String> providerProperties;
    @NotNull
    private final Map<DBPConnectionEventType, DBRShellCommand> events;
    @NotNull
    private final List<DBWHandlerConfiguration> handlers;
    private final DBPConnectionBootstrap bootstrap;
    private DBPConnectionType connectionType;
    private String connectionColor;
    private int keepAliveInterval;

    public DBPConnectionConfiguration() {
        this.connectionType = DBPConnectionType.DEFAULT_TYPE;
        this.properties = new HashMap<>();
        this.providerProperties = new HashMap<>();
        this.events = new HashMap<>();
        this.handlers = new ArrayList<>();
        this.bootstrap = new DBPConnectionBootstrap();
        this.keepAliveInterval = 0;
    }

    public DBPConnectionConfiguration(@NotNull DBPConnectionConfiguration info) {
        this.hostName = info.hostName;
        this.hostPort = info.hostPort;
        this.serverName = info.serverName;
        this.databaseName = info.databaseName;
        this.userName = info.userName;
        this.userPassword = info.userPassword;
        this.url = info.url;
        this.clientHomeId = info.clientHomeId;
        this.configProfileName = info.configProfileName;
        this.userProfileName = info.userProfileName;
        this.connectionType = info.connectionType;
        this.properties = new HashMap<>(info.properties);
        this.providerProperties = new HashMap<>(info.providerProperties);
        this.events = new HashMap<>(info.events.size());
        for (Map.Entry<DBPConnectionEventType, DBRShellCommand> entry : info.events.entrySet()) {
            this.events.put(entry.getKey(), new DBRShellCommand(entry.getValue()));
        }
        this.handlers = new ArrayList<>(info.handlers.size());
        for (DBWHandlerConfiguration handler : info.handlers) {
            this.handlers.add(new DBWHandlerConfiguration(handler));
        }
        this.bootstrap = new DBPConnectionBootstrap(info.bootstrap);
        this.keepAliveInterval = info.keepAliveInterval;
    }

    public String getClientHomeId() {
        return clientHomeId;
    }

    public void setClientHomeId(String clientHomeId) {
        this.clientHomeId = clientHomeId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getHostPort() {
        return hostPort;
    }

    public void setHostPort(String hostPort) {
        this.hostPort = hostPort;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(@Nullable String userPassword) {
        this.userPassword = userPassword;
    }

    ////////////////////////////////////////////////////
    // Properties (connection properties, usually used by driver)

    public String getProperty(String name) {
        return properties.get(name);
    }

    public void setProperty(String name, String value) {
        properties.put(name, value);
    }

    @NotNull
    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(@NotNull Map<String, String> properties) {
        this.properties.clear();
        this.properties.putAll(properties);
    }

    ////////////////////////////////////////////////////
    // Provider properties (extra configuration parameters)

    public String getProviderProperty(String name) {
        return providerProperties.get(name);
    }

    public void setProviderProperty(String name, String value) {
        providerProperties.put(name, value);
    }

    public void removeProviderProperty(String name) {
        providerProperties.remove(name);
    }

    @NotNull
    public Map<String, String> getProviderProperties() {
        return providerProperties;
    }

    public void setProviderProperties(@NotNull Map<String, String> properties) {
        this.providerProperties.clear();
        this.providerProperties.putAll(properties);
    }

    ////////////////////////////////////////////////////
    // Events

    public DBRShellCommand getEvent(DBPConnectionEventType eventType) {
        return events.get(eventType);
    }

    public void setEvent(DBPConnectionEventType eventType, DBRShellCommand command) {
        if (command == null) {
            events.remove(eventType);
        } else {
            events.put(eventType, command);
        }
    }

    public DBPConnectionEventType[] getDeclaredEvents() {
        Set<DBPConnectionEventType> eventTypes = events.keySet();
        return eventTypes.toArray(new DBPConnectionEventType[0]);
    }

    ////////////////////////////////////////////////////
    // Network handlers

    @NotNull
    public List<DBWHandlerConfiguration> getHandlers() {
        return handlers;
    }

    public void setHandlers(@NotNull List<DBWHandlerConfiguration> handlers) {
        this.handlers.clear();
        this.handlers.addAll(handlers);
    }

    public void updateHandler(DBWHandlerConfiguration handler) {
        for (int i = 0; i < handlers.size(); i++) {
            if (handlers.get(i).getId().equals(handler.getId())) {
                handlers.set(i, handler);
                return;
            }
        }
        this.handlers.add(handler);
    }

    @Nullable
    public DBWHandlerConfiguration getHandler(String id) {
        for (DBWHandlerConfiguration cfg : handlers) {
            if (cfg.getId().equals(id)) {
                return cfg;
            }
        }
        return null;
    }

    ////////////////////////////////////////////////////
    // Misc

    public DBPConnectionType getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(DBPConnectionType connectionType) {
        this.connectionType = connectionType;
    }

    /**
     * Color in RGB format
     *
     * @return RGB color or null
     */
    public String getConnectionColor() {
        return connectionColor;
    }

    public void setConnectionColor(String color) {
        this.connectionColor = color;
    }

    @NotNull
    public DBPConnectionBootstrap getBootstrap() {
        return bootstrap;
    }

    /**
     * Keep-Alive interval (seconds).
     * Zero or negative means no keep-alive.
     */
    public int getKeepAliveInterval() {
        return keepAliveInterval;
    }

    public void setKeepAliveInterval(int keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }

    public String getConfigProfileName() {
        return configProfileName;
    }

    public void setConfigProfileName(String configProfileName) {
        this.configProfileName = configProfileName;
    }

    public String getUserProfileName() {
        return userProfileName;
    }

    public void setUserProfileName(String userProfileName) {
        this.userProfileName = userProfileName;
    }

    @Override
    public String toString() {
        return "Connection: " + (url == null ? databaseName : url);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DBPConnectionConfiguration)) {
            return false;
        }
        DBPConnectionConfiguration source = (DBPConnectionConfiguration) obj;
        return
            CommonUtils.equalOrEmptyStrings(this.hostName, source.hostName) &&
                CommonUtils.equalOrEmptyStrings(this.hostPort, source.hostPort) &&
                CommonUtils.equalOrEmptyStrings(this.serverName, source.serverName) &&
                CommonUtils.equalOrEmptyStrings(this.databaseName, source.databaseName) &&
                CommonUtils.equalOrEmptyStrings(this.userName, source.userName) &&
                CommonUtils.equalOrEmptyStrings(this.userPassword, source.userPassword) &&
                CommonUtils.equalOrEmptyStrings(this.url, source.url) &&
                CommonUtils.equalObjects(this.clientHomeId, source.clientHomeId) &&
                CommonUtils.equalObjects(this.configProfileName, source.configProfileName) &&
                CommonUtils.equalObjects(this.userProfileName, source.userProfileName) &&
                CommonUtils.equalObjects(this.connectionType, source.connectionType) &&
                CommonUtils.equalObjects(this.properties, source.properties) &&
                CommonUtils.equalObjects(this.providerProperties, source.providerProperties) &&
                CommonUtils.equalObjects(this.events, source.events) &&
                CommonUtils.equalObjects(this.handlers, source.handlers) &&
                CommonUtils.equalObjects(this.bootstrap, source.bootstrap) &&
                this.keepAliveInterval == source.keepAliveInterval;
    }

    public void resolveDynamicVariables(IVariableResolver variableResolver) {
        hostName = GeneralUtils.replaceVariables(hostName, variableResolver);
        hostPort = GeneralUtils.replaceVariables(hostPort, variableResolver);
        serverName = GeneralUtils.replaceVariables(serverName, variableResolver);
        databaseName = GeneralUtils.replaceVariables(databaseName, variableResolver);
        userName = GeneralUtils.replaceVariables(userName, variableResolver);
        userPassword = GeneralUtils.replaceVariables(userPassword, variableResolver);
        url = GeneralUtils.replaceVariables(url, variableResolver);
        for (Map.Entry<String, String> prop : this.properties.entrySet()) {
            prop.setValue(GeneralUtils.replaceVariables(prop.getValue(), variableResolver));
        }
        for (Map.Entry<String, String> prop : this.providerProperties.entrySet()) {
            prop.setValue(GeneralUtils.replaceVariables(prop.getValue(), variableResolver));
        }
        for (DBWHandlerConfiguration handler : handlers) {
            if (handler.isEnabled()) {
                handler.resolveDynamicVariables(variableResolver);
            }
        }
        bootstrap.resolveDynamicVariables(variableResolver);
    }

    public void setConfigProfile(DBWNetworkProfile profile) {
        if (profile == null) {
            configProfileName = null;
        } else {
            configProfileName = profile.getProfileName();
            for (DBWHandlerConfiguration handlerConfig : profile.getConfigurations()) {
                if (handlerConfig.isEnabled()) {
                    updateHandler(new DBWHandlerConfiguration(handlerConfig));
                }
            }
        }
    }

}
