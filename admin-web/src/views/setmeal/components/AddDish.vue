<template>
  <div class="addDish">
    <div class="leftCont">
      <div v-show="seachKey.trim() == ''"
           class="tabBut">
        <span v-for="(item, index) in dishType"
              :key="index"
              :class="{ act: index == keyInd }"
              @click="checkTypeHandle(index, item.id)">{{ categoryLabel(item.name) }}</span>
      </div>
      <div class="tabList">
        <div class="table"
             :class="{ borderNone: !dishList.length }">
          <div v-if="dishList.length == 0"
               style="padding-left: 10px">
            <Empty />
          </div>
          <el-checkbox-group v-if="dishList.length > 0"
                             v-model="checkedList"
                             @change="checkedListHandle">
            <div v-for="(item, index) in dishList"
                 :key="item.name + item.id"
                 class="items">
              <el-checkbox :key="index"
                           :label="item.name">
                <div class="item">
                  <span style="flex: 3; text-align: left">{{
                    menuName(item.dishName || item.name)
                  }}</span>
                  <span>{{ item.status == 0 ? $t('catalog.offSale') : $t('catalog.onSale') }}</span>
                  <span>{{ (Number(item.price) ).toFixed(2)*100/100 }}</span>
                </div>
              </el-checkbox>
            </div>
          </el-checkbox-group>
        </div>
      </div>
    </div>
    <div class="ritCont">
      <div class="tit">
        {{ $t('catalog.selectedDishes') }} ({{ checkedListAll.length }})
      </div>
      <div class="items">
        <div v-for="(item, ind) in checkedListAll"
             :key="ind"
             class="item">
          <span>{{ menuName(item.dishName || item.name) }}</span>
          <span class="price">￥ {{ (Number(item.price) ).toFixed(2)*100/100 }} </span>
          <span class="del"
                @click="delCheck(item.name)">
            <img src="./../../../assets/icons/btn_clean@2x.png"
                 alt="">
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { menuName, menuDescription } from '@/i18n/menu'
import { categoryLabel } from '@/i18n/categories'
import { Component, Prop, Vue, Watch } from 'vue-property-decorator'
// import {getDishTypeList, getDishListType} from '@/api/dish';
import { getCategoryList, queryDishList } from '@/api/dish'
import Empty from '@/components/Empty/index.vue'

@Component({
  name: 'selectInput',
  components: {
    Empty
  }
})
export default class extends Vue {
  menuName = menuName
  menuDescription = menuDescription
  categoryLabel = categoryLabel
  @Prop({ default: '' }) private value!: number
  @Prop({ default: [] }) private checkList!: any[]
  @Prop({ default: '' }) private seachKey!: string
  private dishType: [] = []
  private dishList: [] = []
  private allDishList: any[] = []
  private dishListCache: any[] = []
  private keyInd = 0
  private searchValue: string = ''
  public checkedList: any[] = []
  private checkedListAll: any[] = []
  private ids: any = new Set()
  created() {
    this.init()
  }

  @Watch('seachKey')
  private seachKeyChange(value: any) {
    if (value.trim()) {
      this.getDishForName(this.seachKey)
    }
  }

  public init() {
    this.getDishType()
    this.checkedList = this.checkList.map((it: any) => it.name)
    this.checkedListAll = this.checkList.reverse()
  }
  public getDishType() {
    getCategoryList({ type: 1 }).then(res => {
      if (res && res.data && res.data.code === 1) {
        this.dishType = res.data.data
        this.getDishList(res.data.data[0].id)
      } else {
        this.$message.error(res.data.msg)
      }
      // if (res.data.code == 200) {
      //   const { data } = res.data
      //   this.   = data
      //   this.getDishList(data[0].category_id)
      // } else {
      //   this.$message.error(res.data.desc)
      // }
    })
  }

  private getDishList(id: number) {
    queryDishList({ categoryId: id }).then(res => {
      if (res && res.data && res.data.code === 1) {
        if (res.data.data.length == 0) {
          this.dishList = []
          return
        }
        let newArr = res.data.data
        newArr.forEach((n: any) => {
          n.dishId = n.id
          n.copies = 1
          // n.dishCopies = 1
          n.dishName = n.name
        })
        this.dishList = newArr
        if (!this.ids.has(id)) {
          this.allDishList = [...this.allDishList, ...newArr]
        }
        this.ids.add(id)
      } else {
        this.$message.error(res.data.msg)
      }
    })
  }

  private getDishForName(name: any) {
    queryDishList({ name }).then(res => {
      if (res && res.data && res.data.code === 1) {
        let newArr = res.data.data
        newArr.forEach((n: any) => {
          n.dishId = n.id
          n.dishName = n.name
        })
        this.dishList = newArr
      } else {
        this.$message.error(res.data.msg)
      }
    })
  }
  private checkTypeHandle(ind: number, id: any) {
    this.keyInd = ind
    this.getDishList(id)
  }
  private checkedListHandle(value: [string]) {
    this.checkedListAll.reverse()
    const list = this.allDishList.filter((item: any) => {
      let data
      value.forEach((it: any) => {
        if (item.name == it) {
          data = item
        }
      })
      return data
    })
    const dishListCat = [...this.checkedListAll, ...list]
    let arrData: any[] = []
    this.checkedListAll = dishListCat.filter((item: any) => {
      let allArrDate
      if (arrData.length == 0) {
        arrData.push(item.name)
        allArrDate = item
      } else {
        const st = arrData.some(it => item.name == it)
        if (!st) {
          arrData.push(item.name)
          allArrDate = item
        }
      }
      return allArrDate
    })
    if (value.length < arrData.length) {
      this.checkedListAll = this.checkedListAll.filter((item: any) => {
        if (value.some(it => it == item.name)) {
          return item
        }
      })
    }
    this.$emit('checkList', this.checkedListAll)
    this.checkedListAll.reverse()
  }

  open(done: any) {
    this.dishListCache = JSON.parse(JSON.stringify(this.checkList))
  }

  close(done: any) {
    this.checkList = this.dishListCache
  }

  private delCheck(name: any) {
    const index = this.checkedList.findIndex(it => it === name)
    const indexAll = this.checkedListAll.findIndex(
      (it: any) => it.name === name
    )

    this.checkedList.splice(index, 1)
    this.checkedListAll.splice(indexAll, 1)
    this.$emit('checkList', this.checkedListAll)
  }
}
</script>
<style lang="scss">
.addDish {
  .el-checkbox__label {
    width: 100%;
  }
  .empty-box {
    margin-top: 50px;
    margin-bottom: 0px;
  }
}
</style>
<style lang="scss" scoped>
.addDish {
  padding: 0 20px;
  display: flex;
  line-height: 40px;
  .empty-box {
    img {
      width: 190px;
      height: 147px;
    }
  }

  .borderNone {
    border: none !important;
  }
  span,
  .tit {
    color: #333;
  }
  .leftCont {
    display: flex;
    border-right: solid 1px #efefef;
    width: 60%;
    padding: 15px;
    .tabBut {
      width: 110px;
      font-weight: bold;
      border-right: solid 2px #f4f4f4;
      span {
        display: block;
        text-align: center;
        // border-right: solid 2px #f4f4f4;
        cursor: pointer;
        position: relative;
      }
    }
    .act {
      border-color: $mine !important;
      color: $mine !important;
    }
    .act::after {
      content: ' ';
      display: inline-block;
      background-color: $mine;
      width: 2px;
      height: 40px;
      position: absolute;
      right: -2px;
    }
    .tabList {
      flex: 1;
      padding: 15px;
      height: 400px;
      overflow-y: scroll;
      .table {
        border: solid 1px #f4f4f4;
        border-bottom: solid 1px #f4f4f4;
        .items {
          border-bottom: solid 1px #f4f4f4;
          padding: 0 10px;
          display: flex;
          .el-checkbox,
          .el-checkbox__label {
            width: 100%;
          }
          .item {
            display: flex;
            padding-right: 20px;
            span {
              display: inline-block;
              text-align: center;
              flex: 1;
              font-weight: normal;
            }
          }
        }
      }
    }
  }
  .ritCont {
    width: 40%;
    .tit {
      margin: 0 15px;
      font-weight: bold;
    }
    .items {
      height: 338px;
      padding: 4px 15px;
      overflow: scroll;
    }
    .item {
      box-shadow: 0px 1px 4px 3px rgba(0, 0, 0, 0.03);
      display: flex;
      text-align: center;
      padding: 0 10px;
      margin-bottom: 20px;
      border-radius: 6px;
      color: #818693;
      span:first-child {
        text-align: left;
        color: #20232a;
        flex: 70%;
      }
      .price {
        display: inline-block;
        flex: 70%;
        text-align: left;
      }
      .del {
        cursor: pointer;
        img {
          position: relative;
          top: 5px;
          width: 20px;
        }
      }
    }
  }
}
</style>
