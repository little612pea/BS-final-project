import ProductVue from '@/views/components-pc/Product.vue'
import CardVue from '@/views/components-pc/Compare.vue'
import BorrowVue from '@/views/components-pc/Borrow.vue'
import SpaceVue from "@/views/components-pc/Space.vue";
import LoginVue from '@/views/components-pc/Login.vue'
import HomeVue from "@/views/components-pc/Home.vue";
import RegisterVue from "@/views/components-pc/Register.vue";

// 导出路由对象数组
export const pc = [
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
                        path: '/home/borrow', // 第三个tab
                        component: BorrowVue,
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
