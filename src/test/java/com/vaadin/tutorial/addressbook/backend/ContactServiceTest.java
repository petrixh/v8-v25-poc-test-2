package com.vaadin.tutorial.addressbook.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The store holds its own copies: callers never get a reference into it, and later edits of a saved
 * object do not reach it (commons-beanutils {@code cloneBean}). Each test uses its own
 * {@link ContactService}, not the shared demo instance.
 */
class ContactServiceTest {

    private static Contact contact(String first, String last, String phone, String email, Date birthDate) {
        Contact c = new Contact();
        c.setFirstName(first);
        c.setLastName(last);
        c.setPhone(phone);
        c.setEmail(email);
        c.setBirthDate(birthDate);
        return c;
    }

    private static void assertContact(Long id, String first, String last, String phone, String email,
            Date birthDate, Contact actual) {
        assertEquals(id, actual.getId(), "id");
        assertEquals(first, actual.getFirstName(), "firstName");
        assertEquals(last, actual.getLastName(), "lastName");
        assertEquals(phone, actual.getPhone(), "phone");
        assertEquals(email, actual.getEmail(), "email");
        assertEquals(birthDate, actual.getBirthDate(), "birthDate");
    }

    @Test
    void editingAContactFromFindAllDoesNotChangeTheStore() {
        ContactService service = new ContactService();
        Date born = new Date(0);
        service.save(contact("Ann", "Lee", "+ 358 555 100", "ann@lee.com", born));

        Contact returned = service.findAll(null).get(0);
        returned.setId(42L);
        returned.setFirstName("Changed");
        returned.setLastName("Changed");
        returned.setPhone("changed");
        returned.setEmail("changed");
        returned.setBirthDate(new Date(1));

        List<Contact> again = service.findAll(null);
        assertEquals(1, again.size());
        assertContact(0L, "Ann", "Lee", "+ 358 555 100", "ann@lee.com", born, again.get(0));
    }

    @Test
    void editingAnObjectAfterSaveDoesNotChangeTheStore() {
        ContactService service = new ContactService();
        Date born = new Date(0);
        Contact saved = contact("Ann", "Lee", "+ 358 555 100", "ann@lee.com", born);
        service.save(saved);

        // save sets the new id on the caller's object (the form's bean gets its id)
        assertEquals(0L, saved.getId());

        saved.setFirstName("Changed");
        saved.setLastName("Changed");
        saved.setPhone("changed");
        saved.setEmail("changed");
        saved.setBirthDate(new Date(1));

        assertContact(0L, "Ann", "Lee", "+ 358 555 100", "ann@lee.com", born, service.findAll(null).get(0));
    }

    @Test
    void allSixPropertiesAreCopied() {
        ContactService service = new ContactService();
        Date born = new Date(-1_000_000_000_000L);
        Contact saved = contact("Ann", "Lee", "+ 358 555 100", "ann@lee.com", born);
        service.save(saved);

        Contact returned = service.findAll(null).get(0);
        assertNotSame(saved, returned);
        assertContact(0L, "Ann", "Lee", "+ 358 555 100", "ann@lee.com", born, returned);
    }

    @Test
    void aNullBirthDateStaysNull() {
        ContactService service = new ContactService();
        service.save(contact("Ann", "Lee", "+ 358 555 100", "ann@lee.com", null));

        Contact returned = service.findAll(null).get(0);
        assertContact(0L, "Ann", "Lee", "+ 358 555 100", "ann@lee.com", null, returned);
        assertNull(returned.getBirthDate());
    }

    @Test
    void aNewContactGetsTheNextId() {
        ContactService service = new ContactService();
        Contact first = contact("A", "A", "", "", null);
        Contact second = contact("B", "B", "", "", null);
        service.save(first);
        service.save(second);
        assertEquals(0L, first.getId());
        assertEquals(1L, second.getId());

        // saving an existing contact keeps its id and does not use up a new one
        second.setFirstName("B2");
        service.save(second);
        assertEquals(1L, second.getId());
        assertEquals(2, service.count());

        Contact third = contact("C", "C", "", "", null);
        service.save(third);
        assertEquals(2L, third.getId());
        assertEquals(3, service.count());
    }
}
