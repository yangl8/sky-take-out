package com.sky.service;

import com.sky.entity.AddressBook;
import java.util.List;

public interface AddressBookService {

    /**
     * Lists addresses matching the supplied criteria
     *
     * @param addressBook Address filters, including the current user's scope
     * @return Matching addresses
     */
    List<AddressBook> list(AddressBook addressBook);

    /**
     * Creates an address for the current user
     *
     * @param addressBook Address details to save
     */
    void save(AddressBook addressBook);

    /**
     * Retrieves an address only when it belongs to the current user
     *
     * @param id Address ID
     * @return The owned address, or {@code null} when it is not found
     */
    AddressBook getById(Long id);

    /**
     * Updates an address owned by the current user
     *
     * @param addressBook Updated address details
     */
    void update(AddressBook addressBook);

    /**
     * Sets one address as the user's default and clears the user's previous default in one transaction
     *
     * @param addressBook Address to set as the user's default
     */
    void setDefault(AddressBook addressBook);

    /**
     * Deletes an address owned by the current user
     *
     * @param id Address ID
     */
    void deleteById(Long id);

}
