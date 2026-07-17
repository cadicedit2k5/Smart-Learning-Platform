package com.smartlearning.common.mapper;

public interface CrudMapper<E, C, U, R> extends
        CreateMapper<E, C>,
        UpdateMapper<E, U>,
        ResponseMapper<E, R>
{
}
