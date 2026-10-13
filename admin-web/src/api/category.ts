import request from '@/utils/request';






export const getCategoryPage = (params: any) => {
  return request({
    url: '/category/page',
    method: 'get',
    params
  });
};

export const deleCategory = (ids: string) => {
  return request({
    url: '/category',
    method: 'delete',
    params: { id:ids }
  });
};

export const editCategory = (params: any) => {
  return request({
    url: '/category',
    method: 'put',
    data: { ...params }
  });
};

export const addCategory = (params: any) => {
  return request({
    url: '/category',
    method: 'post',
    data: { ...params }
  });
};

export const enableOrDisableEmployee = (params: any) => {
  return request({
    url: `/category/status/${params.status}`,
    method: 'post',
    params: { id:params.id }
  })
}
