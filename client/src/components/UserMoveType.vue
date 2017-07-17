<template>
  <div>
    Your move type :
    <strong v-if="user.type == 1" style="font-size: 18px; color:#21a600">X</strong>
    <strong v-else="user.type == 2" style="font-size: 18px; color:#FFAF0B">O</strong>
  </div>
</template>

<script>
  export default {
    name: 'user-move-type',
    props: ['user'],
    data () {
      return {}
    }
  }
</script>

<style scoped>
</style>
