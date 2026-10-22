package com.vaadin.tutorial.addressbook;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.converter.LocalDateToDateConverter;
import com.vaadin.tutorial.addressbook.backend.Contact;

import java.time.LocalDate;

/* Create custom UI Components.
 *
 * Create your own Vaadin components by inheritance and composition.
 * This is a form component inherited from VerticalLayout. Use
 * Use Binder to bind data fields from DTO to UI fields.
 * Similarly named field by naming convention or customized
 * with @PropertyId annotation.
 */
public class ContactForm extends FormLayout {

    Button save = new Button("Save", this::save);
    Button cancel = new Button("Cancel", this::cancel);
    TextField firstName = new TextField();
    TextField lastName = new TextField();
    TextField phone = new TextField();
    TextField email = new TextField();
    DatePicker birthDate = new DatePicker();

    Contact contact;

    // Easily bind forms to beans and manage validation and buffering
    Binder<Contact> formFieldBindings = new Binder<>(Contact.class);

    public ContactForm() {
        configureComponents();
        buildLayout();
    }

    private void configureComponents() {
        /*
         * Highlight primary actions.
         *
         * With Vaadin built-in styles you can highlight the primary save button
         * and give it a keyboard shortcut for a better UX.
         */
        save.addThemeVariants(ButtonVariant.PRIMARY);
        save.addClickShortcut(Key.ENTER);
        setVisible(false);

        // Birth date as the Vaadin 8 DateField showed and read it in en-US:
        // short year (6/11/59), typed two-digit years within 80 years before
        // and 20 after today, four-digit years still accepted, and the same
        // message for text that is not a date.
        birthDate.setI18n(new DatePicker.DatePickerI18n()
                .setDateFormats("M/d/yy", "M/d/yyyy")
                .setReferenceDate(LocalDate.now().minusYears(30))
                .setBadInputErrorMessage("Date format not recognized"));

        // Bind the properties of the contact POJO to fields in this form.
        // DatePicker holds a LocalDate; Contact keeps its java.util.Date.
        formFieldBindings.forField(birthDate)
                .withConverter(new LocalDateToDateConverter())
                .bind("birthDate");
        formFieldBindings.bindInstanceFields(this);
    }

    private void buildLayout() {
        setSizeUndefined();
        // As the Vaadin 8 FormLayout: one column at every width, labels beside
        // the fields, and a 37 px margin (FormLayout has no padding setting)
        setAutoResponsive(true);
        setMaxColumns(1);
        setLabelsAside(true);
        setLabelWidth("5em");
        getStyle().setPadding("37px");

        HorizontalLayout actions = new HorizontalLayout(save, cancel);
        actions.setSpacing(true);

        // Labels beside the fields need form items; Save and Cancel sit above
        // the field column, with no label
        addFormItem(actions, "");
        addFormItem(firstName, "First name");
        addFormItem(lastName, "Last name");
        addFormItem(phone, "Phone");
        addFormItem(email, "Email");
        addFormItem(birthDate, "Birth date");
    }

    /*
     * Use any JVM language.
     *
     * Vaadin supports all languages supported by Java Virtual Machine 1.6+.
     * This allows you to program user interface in Java 8, Scala, Groovy or any
     * other language you choose. The new languages give you very powerful tools
     * for organizing your code as you choose. For example, you can implement
     * the listener methods in your compositions or in separate controller
     * classes and receive to various Vaadin component events, like button
     * clicks. Or keep it simple and compact with Lambda expressions.
     */
    public void save(ClickEvent<Button> event) {
        try {
            // Commit the fields from UI to DAO
            formFieldBindings.writeBean(contact);

            // Save DAO to backend with direct synchronous service API
            getAddressbook().service.save(contact);

            String msg = String.format("Saved '%s %s'.", contact.getFirstName(),
                    contact.getLastName());
            Notification.show(msg, 3000, Notification.Position.BOTTOM_END);
            getAddressbook().refreshContacts();
        } catch (ValidationException e) {
            // Validation exceptions could be shown here
        }
    }

    public void cancel(ClickEvent<Button> event) {
        // Place to call business logic.
        Notification.show("Cancelled", 3000, Notification.Position.BOTTOM_END);
        getAddressbook().contactList.select(null);
    }

    void edit(Contact contact) {
        this.contact = contact;
        if (contact != null) {
            // Read the properties of the contact POJO to fields in this form
            formFieldBindings.readBean(contact);
            firstName.focus();
        }
        setVisible(contact != null);
    }

    AddressbookUI getAddressbook() {
        return findAncestor(AddressbookUI.class);
    }

}
