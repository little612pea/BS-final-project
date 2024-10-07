// store.js
import { createStore } from 'vuex';

export default createStore({
    state() {
        return {
            // 在应用加载时，从 localStorage 获取用户名
            username: localStorage.getItem('username') || ''
        };
    },
    mutations: {
        setUserName(state, username) {
            state.username = username;
            // 将用户名保存到 localStorage
            localStorage.setItem('username', username);
        },
        logout(state) {
            state.username = '';
            // 从 localStorage 中移除用户名
            localStorage.removeItem('username');
        }
    },
    actions: {
        login({ commit }, username) {
            // 执行登录逻辑，并设置用户名
            commit('setUserName', username);
        },
        logout({ commit }) {
            // 执行登出逻辑
            commit('logout');
        }
    }
});
