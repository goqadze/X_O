<template>
  <div>
      <span :class="[ { 'bold' : userCanMove}, user.type == 1? 'green' : 'orange' ]">
        <strong> {{ user.name + ' \'' + userMoveType(user.type) + '\''  | uppercase }} </strong>
      </span>
      -
      <span :class="[ { 'bold' : !userCanMove}, user.type !== 1? 'green' : 'orange' ]">
        <strong> {{ '\'' + userMoveType(opponent.type) + '\' ' + opponent.name | uppercase }} </strong>
      </span>
  </div>
</template>

<script>
  export default {
    name: 'accept-offer',
    props: ['user', 'opponent', 'userCanMove'],
    data () {
      return {}
    },
    methods: {
      userMoveType(type){
        if (type === 1)
          return 'X';
        if (type === 2)
          return 'O';
        return '';
      }
    },
    filters: {
      uppercase: function(v) {
        return v.toUpperCase();
      }
    }
  }
</script>

<style scoped>
  .bold {
    font-weight: bold;
    font-size: 20px;
  }

  .green {
    color: #21a600;
  }

  .orange {
    color: #FFAF0B;
  }

</style>
