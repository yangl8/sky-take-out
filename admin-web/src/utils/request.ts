import axios, { AxiosAdapter } from 'axios'
import { Message, MessageBox } from 'element-ui'
import { UserModule } from '@/store/modules/user'
import {
  getRequestKey,
  pending,
  checkPending,
  removePending
} from './requestOptimize'
import router from '@/router'
import { localizeAdminMessage } from '@/i18n/adminMessages'
import i18n from '@/i18n'
const CancelToken = axios.CancelToken;

const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  'timeout': 600000
})

// Request interceptors
service.interceptors.request.use(
  (config: any) => {
    // console.log(config, 'config')
    // config.data = config.params
    // Add X-Access-Token header to every request, you can add other custom headers here
    if (UserModule.token) {
      config.headers['token'] = UserModule.token
    } else if (UserModule.token && config.url != '/login') {
      window.location.href = '/login'
      return false
    }

    // config.headers['Access-Control-Allow-Origin'] = '*'
    // config.headers['Content-Type'] = 'application/json;'
    if (config.method === 'get' && config.params) {
      let url = config.url + '?';
      for (const propName of Object.keys(config.params)) {
        const value = config.params[propName];
        var part = encodeURIComponent(propName) + '=';
        if (value !== null && typeof (value) !== 'undefined') {
          if (typeof value === 'object') {
            for (const key of Object.keys(value)) {
              let params = propName + '[' + key + ']';
              var subPart = encodeURIComponent(params) + '=';
              url += subPart + encodeURIComponent(value[key]) + '&';
            }
          } else {
            url += part + encodeURIComponent(value) + '&';
          }
        }
      }
      url = url.slice(0, -1);
      config.params = {};
      config.url = url;
    }
    const key = getRequestKey(config);
    // console.log(pending,key,checkPending(key),'checkPending(key)')
    if (checkPending(key)) {
      const source = CancelToken.source();
      config.cancelToken = source.token;
      source.cancel(String(i18n.t('apiErrors.duplicateRequest')));
    } else {
      pending[key] = true;
    }
    return config
  },
  (error: any) => {
    Promise.reject(error)
  }
)

// Response interceptors
service.interceptors.response.use(
  (response: any) => {
    // console.log(response, 'response')
    if (response.data.status === 401) {
      router.push('/login')
      // const res = response.data
      // return response
    }
    response.config.url = response.config.url.replace('/api', '')
    if (response.data && typeof response.data === 'object') {
      ;['msg', 'desc'].forEach((field) => {
        if (typeof response.data[field] === 'string') {
          response.data[field] = localizeAdminMessage(response.data[field])
        }
      })
    }
    const key = getRequestKey(response.config);
    removePending(key);
    // if (response.data.code === 0) {
    //   Message.error(response.data.msg)
    //   //   router.push('/login')
    //   // }
    //   // return window.location.href = '/login'
    //   // window.location.href = '/login'
    //   // return false
    // } else
    if (response.data.code === 1) {
      // const res = response.data
      return response
    }
    return response
  },
  (error: any) => {
    // console.log(error.config, pending, 'error')
    if (error && error.response) {
      const responseData = error.response.data
      const serverMessage = responseData && (responseData.msg || responseData.desc)
      if (serverMessage) {
        error.message = localizeAdminMessage(serverMessage)
      }
      switch (error.response.status) {
        case 401:
          router.push('/login')
          break;
        case 405:
          error.message = String(i18n.t('apiErrors.methodNotAllowed'))
      }
    } else if (error) {
      error.message = String(i18n.t('apiErrors.requestFailed'))
    }
    error.config.url = error.config.url.replace('/api', '')
    const key = getRequestKey(error.config);
    removePending(key);
    // console.log(error, pending, 'error11')
    // Message({
    //   'message': error.message,
    //   'type': 'error',
    //   'duration': 5 * 1000
    // })
    // router.push('/login')
    return Promise.reject(error)
  }
)

export default service
