import request from '@/utils/request'






export const getSetmealPage = (params: any) => {
  return request({
    url: '/setmeal/page',
    method: 'get',
    params,
  },)
}

export const deleteSetmeal = (ids: string) => {
  return request({
    url: '/setmeal',
    method: 'delete',
    params: { ids }
  })
}

export const editSetmeal = (params: any) => {
  return request({
    url: '/setmeal',
    method: 'put',
    data: { ...params }
  })
}

export const addSetmeal = (params: any) => {
  return request({
    url: '/setmeal',
    method: 'post',
    data: { ...params }
  })
}

export const querySetmealById = (id: string | (string | null)[]) => {
  return request({
    url: `/setmeal/${id}`,
    method: 'get'
  })
}

export const setmealStatusByStatus = (params: any) => {
  return request({
    url: `/setmeal/status/${params.status}`,
    method: 'post',
    params: { id: params.ids }
  })
}

export const dishCategoryList = (params: any) => {
  return request({
    url: `/category/list`,
    method: 'get',
    params: { ...params }
  })
}
