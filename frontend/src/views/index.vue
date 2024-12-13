<script setup>
import { watch } from 'vue'
import { useRouter } from 'vue-router'
import pcv from './components-pc/index.vue'
import mobilev from './components-mobile/index.vue'

import { pc } from '@/router/pc'
import { mobile } from '@/router/mobile'
import { deviceStore } from '@/store/deviceStore'
const devices = deviceStore()
const router = useRouter()
watch(
    () => devices.device,
    () => {
      if (devices.device === 'pc' && router.hasRoute('mobile')) {
        // 移除移动端的路由
        if (router.hasRoute('mobile')) {
          router.removeRoute('mobile')
        }
        // 新增PC端的路由
        router.addRoute(pc[0])
      } else if (devices.device === 'mobile') {
        // 移除PC端的路由
        if (router.hasRoute('pc')) {
          router.removeRoute('pc')
        }
        // 新增移动端的路由
        router.addRoute(mobile[0])
      }
      // 刷新页面，更新当前的页面
      router.replace(router.currentRoute.value.href)
    },
    { immediate: true, deep: true }
)
</script>

<template>
  <div>
    <pcv v-if="devices.device === 'pc'" />
    <mobilev v-if="devices.device === 'mobile'" />
  </div>
</template>
<style lang="scss" scoped>
</style>