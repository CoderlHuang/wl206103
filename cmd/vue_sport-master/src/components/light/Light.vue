<template>
<div>
    <div style=" margin:0 auto ;display:flex; flex-direction: column;justify-content:center; align-items:center">
     <div id="main" style="width:100%;height:400px;"></div>
     <h1>{{valuea}}</h1>
     <button @click="clickevent" style="width:100px" >点击显示图表</button>
     <button @click="clickevent1" style="width:100px" >点击最新光照值</button>
    </div>
  
    
</div>
</template>
 <script>

export default{
    data(){
      return{
        valuea:''
      }
    },
 
    methods:{
      async clickevent1(){
      const { data: res } = await this.$http.get("light", {
        params: this.queryInfo
      });
      // this.userlist = res.data; // 将返回数据赋值
      this.total = res.time; // 总个数
       this.valuea= res.items.LightLux.value


   //  alert(JSON.stringify(res.items.LightLux.value));
      },
      

      clickevent(){
    var myChart = echarts.init(document.getElementById('main'));
 
        // 指定图表的配置项和数据
    var option = {
  tooltip: {
    formatter: '{a} <br/>{b} : {c}%'
  },
  series: [
    {
      name: 'Pressure',
      type: 'gauge',
      progress: {
        show: true
      },
      detail: {
        valueAnimation: true,
        formatter: '{value}'
      },
      data: [
        {
          value: this.valuea,
          name: 'sunshine'
        }
      ]
    },
    
  ]
};
        // 使用刚指定的配置项和数据显示图表。
        myChart.setOption(option);
        }}
}
</script>
