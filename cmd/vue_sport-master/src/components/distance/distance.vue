<template>
<div>
    <div style=" margin:0 auto ;display:flex; flex-direction: column;justify-content:center; align-items:center">
     <div id="main" style="width:100%;height:400px;"></div>
     <h1>{{valuea}}</h1>
     <button @click="clickevent" style="width:100px" >点击显示图表</button>

    </div>
  
    
</div>
</template>
 <script>

export default{
    data(){
      return{
        valuea:""
      }
    },
 
    methods:{
      async clickevent1(){
      const { data: res } = await this.$http.get("light", {
        params: this.queryInfo
      });
    //   this.userlist = res.data; // 将返回数据赋值
      this.total = res.time; // 总个数
       this.valuea=(res.items.Distance.value)


   //  alert(JSON.stringify(res.items.LightLux.value));
      },
      

      clickevent(){
    var myChart = echarts.init(document.getElementById('main'));
 
        // 指定图表的配置项和数据
    var option = {
  xAxis: {
    type: 'category',
    data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
  },
  yAxis: {
    type: 'value'
  },
  series: [
    {
      data: this.value,
      type: 'line',
      smooth: true
    }
  ]
};

        // 使用刚指定的配置项和数据显示图表。
        myChart.setOption(option);
        }}
}
</script>
