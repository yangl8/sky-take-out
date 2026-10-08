package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.exception.AddressBookBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Slf4j
public class AddressBookServiceImpl implements AddressBookService {
    @Autowired
    private AddressBookMapper addressBookMapper;

    /**
     * 条件查询
     *
     * @param addressBook
     * @return
     */
    public List<AddressBook> list(AddressBook addressBook) {
        return addressBookMapper.list(addressBook);
    }

    /**
     * 新增地址
     *
     * @param addressBook
     */
    public void save(AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBook.setIsDefault(0);
        addressBookMapper.insert(addressBook);
    }

    /**
     * 根据id查询
     *
     * @param id
     * @return
     */
    public AddressBook getById(Long id) {
        return addressBookMapper.getByIdAndUserId(id, BaseContext.getCurrentId());
    }

    /**
     * 根据id修改地址
     *
     * @param addressBook
     */
    public void update(AddressBook addressBook) {
        Long userId = BaseContext.getCurrentId();
        requireOwnedAddress(addressBook.getId(), userId);
        addressBook.setUserId(userId);
        int updated = addressBookMapper.update(addressBook);
        if (updated == 0) {
            // MySQL may report zero when an owned row already has these values
            requireOwnedAddress(addressBook.getId(), userId);
        }
    }

    /**
     * 设置默认地址
     *
     * @param addressBook
     */
    @Transactional
    public void setDefault(AddressBook addressBook) {
        Long userId = BaseContext.getCurrentId();
        requireOwnedAddress(addressBook.getId(), userId);

        //1、将当前用户的所有地址修改为非默认地址 update address_book set is_default = ? where user_id = ?
        addressBook.setIsDefault(0);
        addressBook.setUserId(userId);
        addressBookMapper.updateIsDefaultByUserId(addressBook);

        //2、将当前地址改为默认地址 update address_book set is_default = ? where id = ?
        addressBook.setIsDefault(1);
        int updated = addressBookMapper.update(addressBook);
        if (updated == 0) {
            AddressBook currentAddress = addressBookMapper.getByIdAndUserId(addressBook.getId(), userId);
            if (currentAddress == null || !Integer.valueOf(1).equals(currentAddress.getIsDefault())) {
                throw new AddressBookBusinessException("设置默认地址失败");
            }
        }
    }

    /**
     * 根据id删除地址
     *
     * @param id
     */
    public void deleteById(Long id) {
        Long userId = BaseContext.getCurrentId();
        if (addressBookMapper.deleteById(id, userId) == 0) {
            throw new AddressBookBusinessException("地址不存在");
        }
    }

    private void requireOwnedAddress(Long id, Long userId) {
        if (addressBookMapper.getByIdAndUserId(id, userId) == null) {
            throw new AddressBookBusinessException("地址不存在");
        }
    }

}
