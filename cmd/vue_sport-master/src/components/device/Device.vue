<template>
<div>
   需要更改产品名称 <p><input type="text" v-model="inputdata" ></p>
    需要更改的key<p><input type="text" v-model="inputdatb" ></p>
    需要删除的key<p><input type="text" v-model="inputdatc" ></p>
    <!-- 需要增加产品的名字<p><input type="text" v-model="inputdatd" ></p> -->



    <button @click="add">更改产品名称</button>
    <button @click="select">查询产品</button>
        <button @click="deletea">删除产品</button>
        <!-- <button @click="create">增加产品</button> -->


    <ul v-for="(item,index) in message" :key="index" >
        <li>productName:{{item.productName}}</li>
     <span>  productKey:</span> <li @click="productKey">{{item.productKey}}</li>
        <li>authType:{{item.authType}}</li>
        <li>gmtCreate:{{item.gmtCreate}}</li>
        <li>deviceCount:{{item.deviceCount}}</li>
        <li>nodeType:{{item.nodeType}}</li>       
    </ul>
    <!-- <h2>获取的总数据：{{message}}</h2> -->
</div>
</template>
<script>
import axios from 'axios'
export default ({
    data() {
       return {
           inputdata:"",
           inputdatb:"",
           inputdatc:"",
           inputdatd:"",

           message:[]
       }
    },
    methods:{
        // async create(){
        //         const {data:res} = await this.$http.get("create?str="+this.inputdatd);
        //         alert(res)

        // },

       async deletea(){
        const {data:res} = await this.$http.get("delete?delestr="+this.inputdatc);
            alert(res)

        
        },
        productKey(event){
            this.inputdatb = event.target.innerText
            this.inputdatc = event.target.innerText
            console.log()
        },
        async add(){
            alert(this.inputdata),
            console.log(this.inputdatb)
        const {data:res} = await this.$http.get("update?str="+this.inputdata+"&oldstr="+this.inputdatb);
        alert(res)
        },
        async select(){
     console.log(1)
            axios.default.baseURL = 'http://localhost:8080/#/devices'
            axios.get('/select')  //返回的是一个Promise
    .then(res=>
    //this.message=JSON.stringify(res.data.data.list)
    this.message = res.data.data.list

    //alert(JSON.stringify(res.data.data.list))

     )
    .catch(err=>console.log(err));


        }
      

        
    }
})
</script>

