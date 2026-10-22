package com.vaadin.tutorial.addressbook;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.tutorial.addressbook.backend.Contact;
import com.vaadin.tutorial.addressbook.backend.ContactService;

/* User Interface written in Java.
 *
 * Define the user interface shown on the Vaadin generated web page as a route view.
 * By default, a new view instance is automatically created when the page is loaded. To reuse
 * the same instance, add @PreserveOnRefresh.
 */
@Route("")
// Any other path shows the same screen, as the old servlet mapped to /*
@RouteAlias(":path*")
@PageTitle("Addressbook")
public class AddressbookUI extends HorizontalLayout {

    /*
     * Hundreds of widgets. Vaadin's user interface components are just Java
     * objects that encapsulate and handle cross-browser support and
     * client-server communication. The default Vaadin components are in the
     * com.vaadin.flow.component packages and there are over 500 more in
     * vaadin.com/directory.
     */
    TextField filter = new TextField();
    Grid<Contact> contactList = new Grid<>(Contact.class);
    Button newContact = new Button("New contact");

    // ContactForm is an example of a custom component class
    ContactForm contactForm = new ContactForm();

    // ContactService is a in-memory mock DAO that mimics
    // a real-world datasource. Typically implemented for
    // example as EJB or Spring Data based service.
    ContactService service = ContactService.createDemoService();

    /*
     * The "Main method".
     *
     * This is the entry point executed to initialize and configure the
     * visible user interface. Executed on every browser reload because a new
     * instance is created for each web page loaded.
     */
    public AddressbookUI() {
        configureComponents();
        buildLayout();
    }

    private void configureComponents() {
        /*
         * Synchronous event handling.
         *
         * Receive user interaction events on the server-side. This allows you
         * to synchronously handle those events. Vaadin automatically sends only
         * the needed changes to the web page without loading a new page.
         */
        newContact.addClickListener(e -> contactForm.edit(new Contact()));

        filter.setPlaceholder("Filter contacts...");
        filter.setValueChangeMode(ValueChangeMode.LAZY);
        filter.addValueChangeListener(e -> refreshContacts(e.getValue()));

        contactList.setColumns("firstName", "lastName", "email");
        contactList.setSelectionMode(Grid.SelectionMode.SINGLE);
        contactList.addSelectionListener(
                e -> contactForm.edit(contactList.asSingleSelect().getValue()));
        refreshContacts();
    }

    /*
     * Robust layouts.
     *
     * Layouts are components that contain other components. HorizontalLayout
     * contains TextField and Button. It is wrapped with a Grid into
     * VerticalLayout for the left side of the screen. Allow user to resize the
     * components with a SplitPanel.
     *
     * In addition to programmatically building layout in Java, you may also
     * choose to setup layout declaratively with Vaadin Designer, CSS and HTML.
     */
    private void buildLayout() {
        HorizontalLayout actions = new HorizontalLayout(filter, newContact);
        actions.setWidth("100%");
        filter.setWidth("100%");
        actions.setFlexGrow(1, filter);

        VerticalLayout left = new VerticalLayout(actions, contactList);
        left.setSizeFull();
        contactList.setSizeFull();
        left.setFlexGrow(1, contactList);

        // This view is the main layout
        setSizeFull();
        setFlexGrow(1, left);

        // Split and allow resizing
        add(left, contactForm);
    }

    /*
     * Choose the design patterns you like.
     *
     * It is good practice to have separate data access methods that handle the
     * back-end access and/or the user interface updates. You can further split
     * your code into classes to easier maintenance. With Vaadin you can follow
     * MVC, MVP or any other design pattern you choose.
     */
    void refreshContacts() {
        refreshContacts(filter.getValue());
    }

    private void refreshContacts(String stringFilter) {
        contactList.setItems(service.findAll(stringFilter));
        contactForm.setVisible(false);
    }

}
