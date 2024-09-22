import { createRouter, createWebHistory } from 'vue-router'
import BookVue from '@/components/Book.vue'
import CardVue from '@/components/Card.vue'
import BorrowVue from '@/components/Borrow.vue'
import SpaceVue from "@/components/Space.vue";
import LoginVue from '@/components/Login.vue'
import HomeVue from "@/components/Home.vue";
import RegisterVue from "@/components/Register.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/home',
      component: HomeVue,
      children: [ // 子路由
        {
          path: '/home/book', // 首页默认加载的tab
          component: BookVue,
        },
        {
          path: '/home/book', // 第一个tab
          component: BookVue,
        },
        {
          path: '/home/card', // 第二个tab
          component: CardVue,
        },
        {
          path: '/home/borrow', // 第三个tab
          component: BorrowVue,
        },
        {
          path: '/home/space', // 第三个tab
          component: SpaceVue,
        }
      ]
    },
    {
      path: '/login',
      component: LoginVue
    },
    {
      path: '/register',
      component: RegisterVue
    }
  ]
})

export default router
