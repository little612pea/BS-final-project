import './assets/css/main.css'
import axios from 'axios'
import { createApp } from 'vue'
// import Vue from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import App from './App.vue'
import router from './router'
import store from './store'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-ui/lib/theme-chalk/index.css';


const app = createApp(App)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}
axios.defaults.baseURL = 'http://localhost:8000';
app.use(createPinia())
app.use(router)
app.use(store)
app.use(ElementPlus)
app.mount('#app')