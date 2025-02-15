import { createRouter, createWebHistory } from 'vue-router'

import { pc } from './pc'
// 默认pc端路由展示
const routes = pc

const router = createRouter({
  history: createWebHistory(),
  // path和component对应关系的位置
  routes,
  // 路由滚动行为定制
  scrollBehavior() {
    return {
      top: 0
    }
  }
})

export default router