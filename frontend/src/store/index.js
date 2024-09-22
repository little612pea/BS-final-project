// store.js
import { createStore } from 'vuex';

export default createStore({
    state() {
        return {
            username: ''
        };
    },
    mutations: {
        setUserName(state, username) {
            state.username = username;
        }
    },
    actions: {
        setUserName({ commit }, username) {
            commit('setUserName', username);
        }
    }
});