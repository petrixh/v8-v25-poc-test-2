package com.vaadin.tutorial.addressbook;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.lumo.Lumo;

/*
 * Application-wide page settings.
 *
 * The old app used the Valo theme, which does not exist in Vaadin 25. Lumo is loaded explicitly:
 * of the two themes in vaadin-core it is the closer match to Valo's sizes and density
 * (16 px text, field and row heights near Valo's). Without a theme here Vaadin loads Aura.
 */
@StyleSheet(Lumo.STYLESHEET)
public class AppShell implements AppShellConfigurator {
}
