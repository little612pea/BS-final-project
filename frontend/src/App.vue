<template>
  <div id="app">
    <router-view></router-view>
  </div>
</template>

<style>
#app {
  display: flex;
  justify-content: center;
  align-items: center;
}

@media (max-width: 750px) {
  #app {
    max-width: 100%;
    padding: 0 0;
  }
}
</style>
<script setup>
import { onBeforeUnmount } from 'vue'
import { deviceStore } from '@/store/deviceStore'
import { throttle } from '@/store/dt'
const { handleToChangeDevice } = deviceStore()

function resizeChange() {
  if (document.documentElement.clientWidth > 750) {
    // 默认设置当屏幕宽度 > 750 时，为PC端
    handleToChangeDevice('pc')
    document.querySelector('#app').style.minWidth = '1180px'
  } else {
    // 默认设置当屏幕宽度 <= 750 时，为移动端
    handleToChangeDevice('mobile')
    document.querySelector('#app').style.minWidth = 'auto'
  }
}
// 节流处理
const throttleResize = throttle(resizeChange, 200)
throttleResize()
window.addEventListener('resize', throttleResize, false)
onBeforeUnmount(() => {
  window.removeEventListener('resize', throttleResize, false)
})
</script>