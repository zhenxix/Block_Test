<script>
import BlockDataForm from './components/BlockDataForm.vue'

export default {
  name: 'App',
  components: {
    BlockDataForm
  }
}
</script>

<template>
  <div id="app">
    <block-data-form />
  </div>
</template>

<style>
/* 重置全局样式 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  width: 100%;
  overflow: hidden;
  background: #f5f7fa;
}

#app {
  height: 100%;
  width: 100%;
}
</style>
