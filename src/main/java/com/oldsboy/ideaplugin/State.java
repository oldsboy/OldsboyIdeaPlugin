package com.oldsboy.ideaplugin;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

@com.intellij.openapi.components.State(name = "oldsboy-config",storages = {@Storage(value = "oldsboy-config.xml")})
public class State implements PersistentStateComponent<Config> {
    private Config config;

    public static State getInstance() {
        return ServiceManager.getService(State.class);
    }

    @Override
    public @NotNull Config getState() {
        if (this.config == null) {
            this.config = new Config();
            this.config.setRegex_list(new ArrayList<>());
            this.config.setBlack_list(new ArrayList<>());
        }
        return this.config;
    }

    @Override
    public void loadState(@NotNull Config config) {
        this.config = config;
    }
}
