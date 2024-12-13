// 管理设备数据
import { defineStore } from 'pinia'
import { ref } from 'vue'
export const deviceStore = defineStore('devices', () => {

    const device = ref('pc') // 默认是PC端，====》 PC端：pc、移动端：mobile

    /**
     * 切换设备类型
     * @param {*} type
     */
    const handleToChangeDevice = (type) => {
        device.value = type
    }

    return {
        device,
        handleToChangeDevice
    }
})