package com.sky.service;

import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.result.PageResult;

public interface EmployeeService {

    /**
     * Authenticates an employee using the supplied credentials
     *
     * @param employeeLoginDTO Login credentials
     * @return The authenticated employee
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * Creates an employee account
     *
     * @param employeeDTO Employee data to save
     */
    void save(EmployeeDTO employeeDTO);

    /**
     * Returns a page of employees matching the supplied filters
     *
     * @param employeePageQueryDTO Pagination and employee filters
     * @return The matching page
     */
    PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    /**
     * Changes whether an employee account is enabled
     *
     * @param status New account status
     * @param id Employee ID
     */
    void startOrstop(Integer status, Long id);

    /**
     * Retrieves an employee by ID with the password masked
     *
     * @param id Employee ID
     * @return Employee details
     */
    public Employee getById(Long id);

    /**
     * Updates employee account details
     *
     * @param employeeDTO Updated employee data
     */
    void update(EmployeeDTO employeeDTO);
}
