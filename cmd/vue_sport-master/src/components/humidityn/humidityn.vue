<template>
<div>
    <div style=" margin:0 auto ;display:flex; flex-direction: column;justify-content:center; align-items:center">
     <div id="main" style="width:100%;height:400px;"></div>
     <h1>{{valuea}}</h1>
     <button @click="clickevent" style="width:100px" >点击显示图表</button>
     <button @click="clickevent1" style="width:100px" >点击获取最新湿度</button>

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
    //   this.userlist = res.data; // 将返回数据赋值
      this.total = res.time; // 总个数
      this.valuea= res.items.Humidity.value
   //  alert(JSON.stringify(res.items.LightLux.value));
      },
      

    clickevent(){
    var myChart = echarts.init(document.getElementById('main'));

        // 指定图表的配置项和数据
    var option = {
  series: [
    {
      type: 'gauge',
      center: ['50%', '60%'],
      startAngle: 200,
      endAngle: -20,
      min: 0,
      max: 60,
      splitNumber: 12,
      itemStyle: {
        color: '#FFAB91'
      },
      progress: {
        show: true,
        width: 30
      },
      pointer: {
        show: false
      },
      axisLine: {
        lineStyle: {
          width: 30
        }
      },
      axisTick: {
        distance: -45,
        splitNumber: 5,
        lineStyle: {
          width: 2,
          color: '#999'
        }
      },
      splitLine: {
        distance: -52,
        length: 14,
        lineStyle: {
          width: 3,
          color: '#999'
        }
      },
      axisLabel: {
        distance: -20,
        color: '#999',
        fontSize: 20
      },
      anchor: {
        show: false
      },
      title: {
        show: false
      },
      detail: {
        valueAnimation: true,
        width: '60%',
        lineHeight: 40,
        borderRadius: 8,
        offsetCenter: [0, '-15%'],
        fontSize: 60,
        fontWeight: 'bolder',
        formatter: '{value} °C',
        color: 'auto'
      },
      data: [
        {
          value: this.valuea
        }
      ]
    },
    {
      type: 'gauge',
      center: ['50%', '60%'],
      startAngle: 200,
      endAngle: -20,
      min: 0,
      max: 60,
      itemStyle: {
        color: '#FD7347'
      },
      progress: {
        show: true,
        width: 8
      },
      pointer: {
        show: false
      },
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      },
      splitLine: {
        show: false
      },
      axisLabel: {
        show: false
      },
      detail: {
        show: false
      },
      data: [
        {
          value: this.valuea
        }
      ]
    }
  ]
};
setInterval(function () {
  const random = this.valuea;
  myChart.setOption({
    series: [
      {
        data: [
          {
            value: random
          }
        ]
      },
      {
        data: [
          {
            value: random
          }
        ]
      }
    ]
  });
}, 2000);
        // 使用刚指定的配置项和数据显示图表。
        myChart.setOption(option);
        }}
}
</script>
