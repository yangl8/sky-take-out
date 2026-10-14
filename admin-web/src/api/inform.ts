import request from '@/utils/request'
  export const getInformData = (params: any) => {
    return request({
      url: '/messages/page',
      method: 'get',
      params,
    },)
  }
  export const getCountUnread = () => {
    return request({
      url: '/messages/countUnread',
      method: 'get'
    },)
  }
  export const batchMsg = (data: any) => {
    return request({
      url: '/messages/batch',
      method: 'put',
      data
    })
  }
    export const setStatus = (params: any) => {
      return request({
        url: `/messages/${params}`,
        method: 'PUT'
      })
    }