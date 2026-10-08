package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.exception.AddressBookBusinessException;
import com.sky.mapper.AddressBookMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressBookServiceImplTest {

    @Mock
    private AddressBookMapper addressBookMapper;

    @InjectMocks
    private AddressBookServiceImpl addressBookService;

    @AfterEach
    void clearCurrentUser() {
        BaseContext.removeCurrentId();
    }

    @Test
    void getByIdShouldQueryOnlyTheCurrentUsersAddress() {
        BaseContext.setCurrentId(1L);
        AddressBook ownAddress = AddressBook.builder().id(10L).userId(1L).build();
        when(addressBookMapper.getByIdAndUserId(10L, 1L)).thenReturn(ownAddress);

        addressBookService.getById(10L);

        verify(addressBookMapper).getByIdAndUserId(10L, 1L);
        verify(addressBookMapper, never()).getById(10L);
    }

    @Test
    void updateShouldRejectAnotherUsersAddressWithoutWriting() {
        BaseContext.setCurrentId(1L);
        when(addressBookMapper.getByIdAndUserId(20L, 1L)).thenReturn(null);
        AddressBook update = AddressBook.builder().id(20L).userId(2L).detail("changed").build();

        assertThrows(AddressBookBusinessException.class, () -> addressBookService.update(update));

        verify(addressBookMapper, never()).update(update);
    }

    @Test
    void updateShouldForceCurrentUserIdForOwnedAddress() {
        BaseContext.setCurrentId(1L);
        when(addressBookMapper.getByIdAndUserId(10L, 1L))
                .thenReturn(AddressBook.builder().id(10L).userId(1L).build());
        AddressBook update = AddressBook.builder().id(10L).userId(2L).detail("changed").build();
        when(addressBookMapper.update(update)).thenReturn(1);

        addressBookService.update(update);

        verify(addressBookMapper).update(update);
        org.junit.jupiter.api.Assertions.assertEquals(1L, update.getUserId());
    }

    @Test
    void unchangedOwnedAddressShouldNotBeRejectedWhenUpdateAffectsNoRows() {
        BaseContext.setCurrentId(1L);
        AddressBook ownedAddress = AddressBook.builder().id(10L).userId(1L).build();
        when(addressBookMapper.getByIdAndUserId(10L, 1L)).thenReturn(ownedAddress, ownedAddress);
        AddressBook update = AddressBook.builder().id(10L).detail("same value").build();
        when(addressBookMapper.update(update)).thenReturn(0);

        addressBookService.update(update);

        verify(addressBookMapper).update(update);
    }

    @Test
    void updateShouldRejectIfOwnershipDisappearsBeforeConditionalUpdate() {
        BaseContext.setCurrentId(1L);
        when(addressBookMapper.getByIdAndUserId(10L, 1L))
                .thenReturn(AddressBook.builder().id(10L).userId(1L).build(), null);
        AddressBook update = AddressBook.builder().id(10L).detail("changed").build();
        when(addressBookMapper.update(update)).thenReturn(0);

        assertThrows(AddressBookBusinessException.class, () -> addressBookService.update(update));
    }

    @Test
    void deleteShouldRejectAnotherUsersAddressWithoutDeleting() {
        BaseContext.setCurrentId(1L);
        when(addressBookMapper.deleteById(20L, 1L)).thenReturn(0);

        assertThrows(AddressBookBusinessException.class, () -> addressBookService.deleteById(20L));

        verify(addressBookMapper).deleteById(20L, 1L);
    }

    @Test
    void setDefaultShouldRejectAnotherUsersAddressBeforeClearingOwnDefaults() {
        BaseContext.setCurrentId(1L);
        when(addressBookMapper.getByIdAndUserId(20L, 1L)).thenReturn(null);
        AddressBook address = AddressBook.builder().id(20L).build();

        assertThrows(AddressBookBusinessException.class, () -> addressBookService.setDefault(address));

        verify(addressBookMapper, never()).updateIsDefaultByUserId(address);
        verify(addressBookMapper, never()).update(address);
    }

    @Test
    void setDefaultShouldOnlyUpdateOwnedAddress() {
        BaseContext.setCurrentId(1L);
        when(addressBookMapper.getByIdAndUserId(10L, 1L))
                .thenReturn(AddressBook.builder().id(10L).userId(1L).build());
        AddressBook address = AddressBook.builder().id(10L).build();
        when(addressBookMapper.update(address)).thenReturn(1);

        addressBookService.setDefault(address);

        verify(addressBookMapper).updateIsDefaultByUserId(address);
        verify(addressBookMapper).update(address);
        org.junit.jupiter.api.Assertions.assertEquals(1L, address.getUserId());
    }

    @Test
    void deleteShouldRejectWhenConditionalDeleteAffectsNoRows() {
        BaseContext.setCurrentId(1L);
        when(addressBookMapper.deleteById(10L, 1L)).thenReturn(0);

        assertThrows(AddressBookBusinessException.class, () -> addressBookService.deleteById(10L));

        verify(addressBookMapper).deleteById(10L, 1L);
    }
}
