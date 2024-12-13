import ProductVue from '@/views/components-mobile/Product.vue'
import CardVue from '@/views/components-mobile/Compare.vue'
import SpaceVue from "@/views/components-mobile/Space.vue";
import LoginVue from '@/views/components-mobile/Login.vue'
import HomeVue from "@/views/components-mobile/Home.vue";
import RegisterVue from "@/views/components-mobile/Register.vue";

// 导出路由对象数组
export const mobile = [
    {
        path: '/',
        name: 'pc',
        component: () => import('@/views/index.vue'),
        redirect: '/login',
        children: [
            {
                path: '/home',
                component: HomeVue,
                children: [ // 子路由
                    {
                        path: '/home/product', // 首页默认加载的tab
                        component: ProductVue,
                    },
                    {
                        path: '/home/product', // 第一个tab
                        component: ProductVue,
                    },
                    {
                        path: '/home/card', // 第二个tab
                        component: CardVue,
                    },
                    {
                        path: '/home/space',
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
    }
]
