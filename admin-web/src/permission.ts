import router from './router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { Message } from 'element-ui'
import { Route } from 'vue-router'
import { UserModule } from '@/store/modules/user'
import Cookies from 'js-cookie'
import i18n from '@/i18n'
import Vue from 'vue'

NProgress.configure({ 'showSpinner': false })

router.beforeEach(async (to: Route, _: Route, next: any) => {
  NProgress.start()
  if (Cookies.get('token')) {
    next()
  } else {
    if (!to.meta.notNeedAuth) {
      next('/login')
    } else {
      next()
    }
  }
})

router.afterEach((to: Route) => {
  NProgress.done()
  document.title = String(i18n.t(to.meta.titleKey || 'routes.brand'))
})

new Vue({ i18n }).$watch(() => i18n.locale, () => {
  document.title = String(i18n.t(router.currentRoute.meta.titleKey || 'routes.brand'))
})
