<template>
  <div>
    <div class="container homecon">
      <h2 class="homeTitle homeTitleBtn">
        {{ $t('orders.orderInformation') }}
        <ul class="conTab">
          <li
            v-for="(item, index) in tabList"
            :key="index"
            :class="activeIndex === index ? 'active' : ''"
            @click="handleClass(index)"
          >
            <el-badge
              class="item"
              :class="item.num >= 10 ? 'badgeW' : ''"
              :value="item.num > 99 ? '99+' : item.num"
              :hidden="!([2, 3].includes(item.value) && item.num)"
              >{{ item.label }}</el-badge
            >
          </li>
        </ul>
      </h2>
      <div class="">
        <div v-loading="listLoading">
          <el-table
            :data="orderData"
            :fit="false"
            stripe
            class="tableBox workbench-order-table"
            style="width: 100%"
            @row-click="handleTable"
          >
            <el-table-column prop="number" :label="String($t('orders.orderNumber'))" min-width="220" align="center" header-align="center" class-name="order-number-column">
              <template slot-scope="{ row }">
                <span class="order-number-text">{{ row.number }}</span>
                <el-tag v-if="isScheduledDelivery(row)" size="mini" class="scheduled-tag" :title="String($t('orders.scheduledDelivery'))" :aria-label="String($t('orders.scheduledDelivery'))">{{ String($t('orders.scheduledDelivery')).slice(0, 1) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="String($t('orders.customerName'))" prop="consignee" min-width="140" align="center" header-align="center">
              <template slot-scope="{ row }">
                <span>{{ row.consignee || row.userName || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="String($t('orders.orderItems'))" width="360" class-name="workbench-order-items-column">
              <template slot-scope="{ row }">
                <div class="workbench-dishes">
                  <div v-for="(dish, index) in orderDishLines(row.orderDishes)" :key="index" class="workbench-dish-line">
                    {{ dish }}
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              prop="estimatedDeliveryTime"
              :label="String($t('orders.estimatedDeliveryTime'))"
              :sortable="status !== 3"
              class-name="orderTime"
              label-class-name="workbench-time-header"
              align="center"
              header-align="center"
              width="210"
            >
            </el-table-column>
            <el-table-column prop="amount" :label="String($t('orders.amountReceived'))" width="180" align="center" header-align="center" class-name="workbench-amount-column" label-class-name="workbench-amount-header">
              <template slot-scope="{ row }">￥{{ formatMoney(row.amount) }}</template>
            </el-table-column>
            <el-table-column
              :label="String($t('catalog.actions'))"
              align="center"
              class-name="workbench-actions-column"
              width="105"
            >
              <template slot-scope="{ row }">
                <div class="workbench-actions">
                  <el-button
                    v-if="row.status === 2"
                    type="text"
                    class="blueBug"
                    @click="
                      isTableOperateBtn = true; orderAccept(row, $event)
                    "
                    >
                    {{ $t('orders.accept') }}
                  </el-button>
                  <el-button
                    v-if="row.status === 2"
                    type="text"
                    class="delBut"
                    @click="
                      isTableOperateBtn = true; orderReject(row, $event)
                    "
                  >
                    {{ $t('orders.reject') }}
                  </el-button>
                  <el-button
                    v-if="row.status === 3"
                    type="text"
                    class="blueBug"
                    @click="cancelOrDeliveryOrComplete(3, row.id, $event)"
                  >
                    {{ $t('orders.deliver') }}
                  </el-button>
                  <el-button
                    v-if="[1, 3, 4, 5].includes(row.status)"
                    type="text"
                    class="delBut"
                    @click="cancelOrder(row, $event)"
                  >
                    {{ $t('common.cancel') }}
                  </el-button>
                  <el-button
                    type="text"
                    class="blueBug non"
                    @click="goDetail(row.id, row.status, row, $event)"
                  >
                    {{ $t('orders.view') }}
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <Empty v-if="!listLoading && !orderData.length" :is-search="isSearch" />
        <el-pagination
          v-if="counts > 10"
          class="pageList"
          :page-sizes="[10, 20, 30, 40]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="counts"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
    <el-dialog
      :title="String($t('orders.orderInformation'))"
      :visible.sync="dialogVisible"
      width="72%"
      :before-close="handleClose"
      class="order-dialog"
    >
      <el-scrollbar v-loading="detailLoading" style="height: 100%">
        <div class="order-top">
          <div class="order-top-main">
            <div class="order-identifier">
              <label style="font-size: 16px">{{ $t('orders.orderNumber') }}：</label>
              <div class="order-num">
                {{ diaForm.number || '—' }}
              </div>
            </div>
            <div
              class="order-status"
              :class="{ status3: [3, 4].includes(dialogOrderStatus) }"
            >
              {{
                statusLabel(dialogOrderStatus)
              }}
            </div>
            <p class="order-date"><label>{{ $t('orders.orderTime') }}：</label>{{ diaForm.orderTime || '—' }}</p>
          </div>
        </div>

        <div class="order-middle">
          <div class="user-info">
            <div class="user-info-box">
              <div class="delivery-summary" :class="{ scheduled: isScheduledDelivery(diaForm) }">
                <strong>{{ deliveryTypeLabel(diaForm.deliveryStatus) }}</strong>
                <span>{{ deliveryTimeLabel(diaForm.deliveryStatus, dialogOrderStatus) }}：{{ (dialogOrderStatus === 5 && Number(diaForm.deliveryStatus) === 1 ? diaForm.deliveryTime : diaForm.estimatedDeliveryTime) || '—' }}</span>
              </div>
              <div class="user-name">
                <label>{{ $t('orders.customerName') }}：</label>
                <span>{{ diaForm.consignee }}</span>
              </div>
              <div class="user-phone">
                <label>{{ $t('orders.phone') }}：</label>
                <span>{{ diaForm.phone || '—' }}</span>
              </div>
              <div class="user-address">
                <label>{{ $t('orders.address') }}：</label>
                <span :title="diaForm.address ? orderAddressLabel(diaForm.address, $i18n.locale) : '—'">{{ diaForm.address ? orderAddressLabel(diaForm.address, $i18n.locale) : '—' }}</span>
              </div>
            </div>
            <div class="fulfillment-info">
              <div class="user-remark">
                <div>{{ $t('orders.remark') }}</div>
                <span>{{ diaForm.remark || $t('orders.noRemark') }}</span>
              </div>
              <div class="user-remark">
                <div>{{ $t('orders.tablewareNumber') }}</div>
                <span>{{ tablewareLabel(diaForm.tablewareStatus, diaForm.tablewareNumber) }}</span>
              </div>
              <div v-if="dialogOrderStatus === 6" class="user-remark orderCancel">
                <div>{{ $t('orders.cancelReason') }}</div>
                <span>{{ orderReasonLabel(diaForm.cancelReason || diaForm.rejectionReason) || '—' }}</span>
              </div>
            </div>
          </div>

          <div class="dish-info">
            <div class="dish-label">{{ $t('orders.dishes') }}</div>
            <div class="dish-list">
              <div
                v-for="(item, index) in normalizedOrderDetails"
                :key="index"
                class="dish-item"
              >
                <span class="dish-name">{{ menuName(item.name) }}</span>
                <span class="dish-num">x{{ item.number }}</span>
                <span class="dish-price">￥{{ item.amount == null ? '—' : formatMoney(item.amount) }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="order-bottom">
          <div class="amount-info">
            <div class="amount-label">{{ $t('orders.fees') }}</div>
            <div class="amount-list">
              <div class="dish-amount">
                <span class="amount-name">{{ $t('orders.dishSubtotal') }}：</span>
                <span class="amount-price"
                  >￥{{
                    formatMoney(dishSubtotal)
                  }}</span
                >
              </div>
              <div class="send-amount">
                <span class="amount-name">{{ $t('orders.deliveryFee') }}：</span>
                <span class="amount-price">￥{{ formatMoney(diaForm.deliveryFee) }}</span>
              </div>
              <div class="package-amount">
                <span class="amount-name">{{ $t('orders.packingFee') }}：</span>
                <span class="amount-price"
                  >￥{{
                    formatMoney(diaForm.packAmount)
                  }}</span
                >
              </div>
              <div class="all-amount">
                <span class="amount-name">{{ $t('orders.total') }}：</span>
                <span class="amount-price"
                  >￥{{
                    formatMoney(diaForm.amount)
                  }}</span
                >
              </div>
              <div class="pay-type">
                <span class="pay-name">{{ $t('orders.paymentMethod') }}：</span>
                <span class="pay-value">{{
                  paymentMethodLabel(diaForm.payMethod)
                }}</span>
              </div>
              <div class="pay-time">
                <span class="pay-name">{{ $t('orders.paymentTime') }}：</span>
                <span class="pay-value">{{ diaForm.checkoutTime }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-scrollbar>
      <span slot="footer" class="dialog-footer">
        <el-button v-if="dialogOrderStatus === 6" @click="dialogVisible = false">{{ $t('catalog.back') }}</el-button>
        <el-checkbox
          v-if="dialogOrderStatus === 2 && status === 2"
          v-model="isAutoNext"
          >{{ $t('orders.autoNext') }}</el-checkbox
        >
        <el-button
          v-if="dialogOrderStatus === 2"
          @click="isTableOperateBtn = false; orderReject(row, $event)"
          >{{ $t('orders.reject') }}</el-button
        >
        <el-button
          v-if="dialogOrderStatus === 2"
          type="primary"
          @click="isTableOperateBtn = false; orderAccept(row, $event)"
          >{{ $t('orders.accept') }}</el-button
        >

        <el-button
          v-if="[1, 3, 4, 5].includes(dialogOrderStatus)"
          @click="dialogVisible = false"
          >{{ $t('catalog.back') }}</el-button
        >
        <el-button
          v-if="dialogOrderStatus === 3"
          type="primary"
          @click="cancelOrDeliveryOrComplete(3, row.id, $event)"
          >{{ $t('orders.deliver') }}</el-button
        >
        <el-button
          v-if="dialogOrderStatus === 4"
          type="primary"
          @click="cancelOrDeliveryOrComplete(4, row.id, $event)"
          >{{ $t('orders.complete') }}</el-button
        >
        <el-button
          v-if="[1].includes(dialogOrderStatus)"
          type="primary"
          @click="cancelOrder(row, $event)"
          >{{ $t('orders.cancelOrder') }}</el-button
        >
      </span>
    </el-dialog>
    <!-- end -->
    <el-dialog
      :title="String($t(cancelDialogTitle === '取消' ? 'orders.confirmCancel' : 'orders.confirmReject'))"
      :visible.sync="cancelDialogVisible"
      width="42%"
      :before-close="() => ((cancelDialogVisible = false), (cancelReason = ''))"
      class="cancelDialog"
    >
      <el-form label-width="90px">
        <el-form-item :label="String($t(cancelDialogTitle === '取消' ? 'orders.cancelReason' : 'orders.rejectionReason')) + '：'">
          <el-select
            v-model="cancelReason"
            :placeholder="String($t('orders.selectReason'))"
          >
            <el-option
              v-for="(item, index) in cancelDialogTitle === '取消'
                ? cancelrReasonList
                : cancelOrderReasonList"
              :key="index"
              :label="orderReasonLabel(item.label)"
              :value="item.label"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="cancelReason === '自定义原因'" :label="String($t('orders.reason')) + '：'">
          <el-input
            v-model.trim="remark"
            type="textarea"
            :placeholder="String($t('orders.enterCustomReason'))"
            maxlength="20"
          />
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click=";(cancelDialogVisible = false), (cancelReason = '')"
          >{{ $t('common.cancel') }}</el-button
        >
        <el-button type="primary" @click="confirmCancel">{{ $t('common.confirm') }}</el-button>
      </span>
    </el-dialog>
    <!-- end -->
  </div>
</template>

<script lang="ts">
import { menuName, menuDescription, orderMenuSummary } from '@/i18n/menu'
import { orderReasonLabel } from '@/i18n/orders'
import { orderAddressLabel } from '@/i18n/orderAddress'
import { isScheduledDelivery as isScheduledDeliveryStatus, resolveTablewareDisplay } from '@/utils/orderFulfillment'
import { Component, Vue, Prop } from 'vue-property-decorator'
import Empty from '@/components/Empty/index.vue'
import {
  getOrderDetailPage,
  queryOrderDetailById,
  completeOrder,
  deliveryOrder,
  orderCancel,
  orderReject,
  orderAccept,
  getOrderListBy,
} from '@/api/order'
@Component({
  name: 'Orderview',
  components: {
    Empty,
  },
})
export default class extends Vue {
  orderMenuSummary = orderMenuSummary
  menuName = menuName
  menuDescription = menuDescription
  orderReasonLabel = orderReasonLabel
  orderAddressLabel = orderAddressLabel
  @Prop({ default: '' }) orderStatics!: any

  private orderId = ''
  private dialogOrderStatus = 0
  private activeIndex = 0

  private dialogVisible = false
  private cancelDialogVisible = false
  private cancelDialogTitle = ''
  private cancelReason = ''
  private remark = ''
  private diaForm: any = {}
  private detailLoading = false
  private listLoading = false
  private autoNextAfterAction = false
  private autoNextExcludeOrderId = ''
  private detailRequestSequence = 0
  private row = {}
  private isAutoNext = true
  private isSearch: boolean = false
  private counts = 0
  private page: number = 1
  private pageSize: number = 10
  private status = 2
  private orderData = []
  private isTableOperateBtn = true
  private cancelOrderReasonList = [
    {
      value: 1,
      label: '订单量较多，暂时无法接单',
    },
    {
      value: 2,
      label: '菜品已销售完，暂时无法接单',
    },
    {
      value: 3,
      label: '餐厅已打烊，暂时无法接单',
    },
    {
      value: 0,
      label: '自定义原因',
    },
  ]

  private cancelrReasonList = [
    {
      value: 1,
      label: '订单量较多，暂时无法接单',
    },
    {
      value: 2,
      label: '菜品已销售完，暂时无法接单',
    },
    {
      value: 3,
      label: '骑手不足无法配送',
    },
    {
      value: 4,
      label: '客户电话取消',
    },
    {
      value: 0,
      label: '自定义原因',
    },
  ]
  get orderList() {
    return [
    {
      label: this.$t('orders.allOrders'),
      value: 0,
    },
    {
      label: this.$t('orders.pendingPayment'),
      value: 1,
    },
    {
      label: this.$t('orders.awaitingAcceptance'),
      value: 2,
    },
    {
      label: this.$t('orders.awaitingDelivery'),
      value: 3,
    },
    {
      label: this.$t('orders.delivering'),
      value: 4,
    },
    {
      label: this.$t('orders.completed'),
      value: 5,
    },
    {
      label: this.$t('orders.cancelled'),
      value: 6,
    },
    ]
  }

  isScheduledDelivery(order: any) {
    return order && isScheduledDeliveryStatus(order.deliveryStatus)
  }

  deliveryTypeLabel(status: any) {
    if (Number(status) === 1) return this.$t('orders.immediateDelivery')
    if (Number(status) === 0) return this.$t('orders.scheduledDelivery')
    return this.$t('orders.deliveryTypeUnknown')
  }

  deliveryTimeLabel(status: any, orderStatus: any) {
    if (Number(status) === 1 && Number(orderStatus) === 5) return this.$t('orders.deliveryTime')
    return Number(status) === 0 ? this.$t('orders.scheduledDeliveryTime') : this.$t('orders.estimatedDeliveryTime')
  }

  tablewareLabel(status: any, number: any) {
    const display = resolveTablewareDisplay(status, number)
    if (display.type === 'none') return this.$t('orders.noCutlery')
    if (display.type === 'sets') return this.$t('orders.tablewareSets', { count: display.count })
    if (display.type === 'byMeal') return this.$t('orders.tablewareByMeal', { count: display.count })
    return this.$t('orders.tablewareUnknown')
  }
  get tabList() {
    return [
      {
        label: this.$t('orders.awaitingAcceptance'),
        value: 2,
        num: this.orderStatics.toBeConfirmed,
      },
      {
        label: this.$t('orders.awaitingDelivery'),
        value: 3,
        num: this.orderStatics.confirmed,
      },
    ]
  }
  get dishSubtotal() {
    const amount = Number(this.diaForm.amount)
    const deliveryFee = Number(this.diaForm.deliveryFee) || 0
    const packingFee = Number(this.diaForm.packAmount) || 0
    return Number.isFinite(amount) ? Math.max(0, amount - deliveryFee - packingFee) : 0
  }
  formatMoney(value: any) {
    const amount = Number(value)
    return Number.isFinite(amount) ? amount.toFixed(2) : '0.00'
  }
  orderItemAmount(item: any) {
    const values = [item.amount, item.price, item.unitPrice, item.totalAmount, item.lineAmount]
    const validPrices = values.filter((value) => value !== null && value !== undefined && value !== '' && Number.isFinite(Number(value)))
    const positivePrice = validPrices.find((value) => Number(value) > 0)
    return positivePrice != null ? Number(positivePrice) : validPrices.length ? Number(validPrices[0]) : null
  }
  paymentMethodLabel(value: any) {
    if (Number(value) === 1) return this.$t('orders.wechatPay')
    if (Number(value) === 2) return this.$t('orders.alipayPay')
    return '—'
  }
  confirmOperation(messageKey: string) {
    return this.$confirm(
      String(this.$t(messageKey)),
      String(this.$t('orders.confirmationTitle')),
      {
        confirmButtonText: String(this.$t('common.confirm')),
        cancelButtonText: String(this.$t('common.cancel')),
        type: 'warning'
      }
    )
  }
  statusLabel(status: number) {
    const item = this.orderList.find((entry) => entry.value === status)
    return item ? item.label : '—'
  }
  orderDishLines(value: string) {
    if (!value) return []
    return this.orderMenuSummary(value).split(';').map((item) => item.trim()).filter(Boolean)
  }
  get normalizedOrderDetails() {
    const details = this.diaForm && (this.diaForm.orderDetailList || this.diaForm.orderDetails || this.diaForm.orderItems)
    if (Array.isArray(details) && details.length) {
      return details.map((item: any) => ({
        ...item,
        name: item.name || item.dishName || item.setmealName || item.itemName || '—',
        number: item.number != null ? item.number : (item.quantity != null ? item.quantity : 1),
        amount: this.orderItemAmount(item) == null ? null : this.orderItemAmount(item) * Number(item.number != null ? item.number : (item.quantity != null ? item.quantity : 1))
      }))
    }
    return this.orderDishLines(this.diaForm && this.diaForm.orderDishes).map((item) => {
      const match = item.match(/\*(\d+)$/)
      return { name: match ? item.slice(0, match.index) : item, number: match ? Number(match[1]) : 1 }
    })
  }
  created() {
    this.getOrderListData(this.status)
  }
  async getOrderListData(status) {
    const params = {
      page: this.page,
      pageSize: this.pageSize,
      status: status,
    }
    this.listLoading = true
    try {
      const response = await getOrderDetailPage(params)
      const result = response && response.data && response.data.data
      const records = result && Array.isArray(result.records) ? result.records : []
      this.orderData = records
      this.counts = Number((result && result.total) || 0)
      this.$emit('getOrderListBy3Status')
      if (this.dialogOrderStatus === 2 && this.status === 2 && this.isAutoNext && this.autoNextAfterAction && !this.isTableOperateBtn) {
        const nextOrder = records.find((item: any) => item.id !== this.autoNextExcludeOrderId)
        this.autoNextAfterAction = false
        this.autoNextExcludeOrderId = ''
        if (nextOrder) this.goDetail(nextOrder.id, nextOrder.status, nextOrder)
      } else {
        this.autoNextAfterAction = false
      }
    } catch (error) {
      this.orderData = []
      this.counts = 0
      const detail = error && error.response && error.response.data
      const reason = (detail && (detail.msg || detail.message)) || (error && error.message) || ''
      this.$message.error(String(this.$t('orders.requestError')) + reason)
    } finally {
      this.listLoading = false
    }
  }

  orderAccept(row: any, event) {
    event.stopPropagation()
    const fromDetail = !this.isTableOperateBtn
    this.orderId = row.id
    this.dialogOrderStatus = row.status
    this.confirmOperation('orders.confirmAccept')
      .then(() => orderAccept({ id: this.orderId }))
      .then((res) => {
        if (res.data.code === 1) {
          this.autoNextAfterAction = fromDetail && this.isAutoNext
          this.autoNextExcludeOrderId = this.orderId
          this.$message.success(String(this.$t('orders.operationSuccessful')))
          this.orderId = ''
          // this.dialogOrderStatus = 0
          this.dialogVisible = false
          this.getOrderListData(this.status)
        } else {
          this.$message.error(res.data.msg)
        }
      })
      .catch((err) => {
        if (err === 'cancel' || err === 'close') return
        this.$message.error(err && err.message ? err.message : String(this.$t('apiErrors.requestFailed')))
      })
  }
  cancelOrder(row: any, event) {
    event.stopPropagation()
    this.cancelDialogVisible = true
    this.orderId = row.id
    this.dialogOrderStatus = row.status
    this.cancelDialogTitle = '取消'
    this.dialogVisible = false
    this.cancelReason = ''
  }
  orderReject(row: any, event) {
    event.stopPropagation()
    this.cancelDialogVisible = true
    this.orderId = row.id
    this.dialogOrderStatus = row.status
    this.cancelDialogTitle = '拒绝'
    this.dialogVisible = false
    this.cancelReason = ''
  }
  confirmCancel(type) {
    if (!this.cancelReason) {
      return this.$message.error(String(this.$t('orders.selectReason')))
    } else if (this.cancelReason === '自定义原因' && !this.remark) {
      return this.$message.error(String(this.$t('orders.enterCustomReason')))
    }

    const fromDetail = !this.isTableOperateBtn
    const cancelRequest = this.cancelDialogTitle === '取消' ? orderCancel : orderReject
    cancelRequest({
      id: this.orderId,
      // eslint-disable-next-line standard/computed-property-even-spacing
      [this.cancelDialogTitle === '取消' ? 'cancelReason' : 'rejectionReason']:
        this.cancelReason === '自定义原因' ? this.remark : this.cancelReason,
    })
      .then((res) => {
        if (res.data.code === 1) {
          this.autoNextAfterAction = this.cancelDialogTitle === '拒绝' && fromDetail && this.isAutoNext
          this.autoNextExcludeOrderId = this.orderId
          this.$message.success(String(this.$t('orders.operationSuccessful')))
          this.cancelDialogVisible = false
          this.orderId = ''
          // this.dialogOrderStatus = 0
          this.getOrderListData(this.status)
        } else {
          this.$message.error(res.data.msg)
        }
      })
      .catch((err) => {
        this.$message.error(err && err.message ? err.message : String(this.$t('apiErrors.requestFailed')))
      })
  }

  cancelOrDeliveryOrComplete(status: number, id: string, event) {
    event.stopPropagation()
    const params = {
      status,
      id,
    }
    this.confirmOperation(status === 3 ? 'orders.confirmDeliver' : 'orders.confirmComplete')
      .then(() => (status === 3 ? deliveryOrder : completeOrder)(params))
      .then((res) => {
        if (res.data.code === 1) {
          this.$message.success(String(this.$t('orders.operationSuccessful')))
          this.orderId = ''
          // this.dialogOrderStatus = 0
          this.dialogVisible = false
          this.getOrderListData(this.status)
        } else {
          this.$message.error(res.data.msg)
        }
      })
      .catch((err) => {
        if (err === 'cancel' || err === 'close') return
        this.$message.error(err && err.message ? err.message : String(this.$t('apiErrors.requestFailed')))
      })
  }
  async goDetail(id: any, status: number, row: any, event?: any) {
    if (event && typeof event.stopPropagation === 'function') event.stopPropagation()
    const requestSequence = ++this.detailRequestSequence
    this.diaForm = {}
    this.dialogVisible = true
    this.dialogOrderStatus = status
    this.row = row
    this.orderId = id
    this.detailLoading = true
    try {
      const response = await queryOrderDetailById({ orderId: id })
      const body = response && response.data
      if (!body || body.code !== 1) {
        throw new Error((body && (body.msg || body.message)) || 'Invalid order detail response')
      }
      const detail = body.data
      if (!detail || typeof detail !== 'object') throw new Error('Invalid order detail response')
      if (requestSequence !== this.detailRequestSequence) return
      this.diaForm = Object.assign({}, detail)
      if (detail.status != null && Number.isFinite(Number(detail.status))) {
        this.dialogOrderStatus = Number(detail.status)
      }
      this.row = Object.assign({ id: id, status: status }, detail)
    } catch (error) {
      if (requestSequence !== this.detailRequestSequence) return
      this.$message.error(String(this.$t('orders.requestError')))
    } finally {
      if (requestSequence === this.detailRequestSequence) this.detailLoading = false
    }
  }
  handleClose(done?: () => void) {
    this.detailRequestSequence++
    this.dialogVisible = false
    this.detailLoading = false
    this.diaForm = {}
    if (done) done()
  }
  handleClass(index) {
    this.activeIndex = index
    if (index === 0) {
      this.status = 2
      this.getOrderListData(2)
    } else {
      this.status = 3
      this.getOrderListData(3)
    }
  }
  handleTable(row, column, event) {
    event.stopPropagation()
    this.goDetail(row.id, row.status, row, event)
  }
  private handleSizeChange(val: any) {
    this.pageSize = val
    this.getOrderListData(this.status)
  }

  private handleCurrentChange(val: any) {
    this.page = val
    this.getOrderListData(this.status)
  }
}
</script>
<style  lang="scss" scoped >
.dashboard-container.home .homecon {
  margin-bottom: 0;
}
.order-top {
  // height: 80px;
  border-bottom: 1px solid #e7e6e6;
  padding-bottom: 26px;
  padding-left: 22px;
  padding-right: 22px;
  // margin: 0 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  .order-top-main {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 14px;
    width: 100%;
    min-width: 0;
  }
  .order-identifier {
    display: flex;
    align-items: baseline;
    gap: 6px;
    white-space: nowrap;
  }
  .order-date {
    white-space: nowrap;
  }
  .order-status {
    width: auto;
    min-width: 100px;
    padding: 0 12px;
    height: 27px;
    background: #333333;
    border-radius: 13.5px;
    color: white;
    margin-left: 0;
    text-align: center;
    line-height: 27px;
    flex-shrink: 0;
  }
  .status3 {
    background: #f56c6c;
  }
  p {
    color: #333;
    label {
      color: #666;
    }
  }
  .order-num {
    font-size: 16px;
    color: #2a2929;
    font-weight: bold;
    display: inline-block;
  }
}

.order-middle {
  .user-info {
    min-height: 140px;
    background: #fbfbfa;
    margin-top: 23px;

    padding: 20px 43px;
    color: #333;
    .user-info-box {
      min-height: 55px;
      display: grid;
      grid-template-columns: minmax(0, 0.82fr) minmax(0, 1.18fr);
      column-gap: 36px;
      row-gap: 16px;
      align-items: center;

      > div {
        display: flex;
        align-items: center;
        min-width: 0;
      }

      .user-name {
        grid-column: 1;
        grid-row: 2;
      }
      .user-phone {
        grid-column: 2;
        grid-row: 2;
      }
      .delivery-summary {
        grid-column: 1 / -1;
        grid-row: 1;
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 12px;
        min-width: 0;
        padding: 10px 14px;
        border-radius: 4px;
        background: #fff8e8;
        border-left: 4px solid #e6a23c;
        color: #303133;
        strong {
          color: #b87412;
          font-size: 16px;
          white-space: nowrap;
        }
        span {
          white-space: normal;
          overflow-wrap: anywhere;
        }
        &.scheduled {
          background: #eef5ff;
          border-left-color: #409eff;
          strong {
            color: #2174c7;
          }
        }
      }
      label {
        flex: 0 0 auto;
        margin-right: 12px;
        color: #666;
        white-space: nowrap;
      }
      span {
        min-width: 0;
        text-align: left;
      }
      .user-name span,
      .user-phone span {
        white-space: nowrap;
      }

      .user-address {
        grid-column: 1 / -1;
        grid-row: 3;
        white-space: normal;
      }

      .user-address span {
        overflow-wrap: anywhere;
        white-space: normal;
      }
    }
    .user-remark {
      min-height: 43px;
      line-height: 1.4;
      background: #fffbf0;
      border: 1px solid #fbe396;
      border-radius: 4px;
      margin-top: 0;
      padding: 6px;
      display: flex;
      align-items: center;
      div {
        display: inline-block;
        flex: 0 0 120px;
        min-width: 120px;
        min-height: 32px;
        background: #fbe396;
        border-radius: 4px;
        text-align: center;
        line-height: 1.4;
        padding: 7px 4px;
        color: #333;
        margin-right: 12px;
        // padding: 12px 6px;
      }
      span {
        color: #f2a402;
        white-space: pre-wrap;
        overflow-wrap: anywhere;
        min-width: 0;
      }
    }
    .orderCancel > div {
      flex: 0 0 auto;
      min-width: 0;
      white-space: nowrap;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 7px 10px;
    }
    .fulfillment-info {
      display: grid;
      gap: 8px;
      margin-top: 14px;
    }
    .orderCancel {
      background: #ffffff;
      border: 1px solid #b6b6b6;

      div {
        padding: 7px 10px;
        background-color: #e5e4e4;
      }
      span {
        color: #f56c6c;
      }
    }
  }
  .dish-info {
    // min-height: 180px;
    display: flex;
    flex-wrap: wrap;
    padding: 20px 40px;
    border-bottom: 1px solid #e7e6e6;
    .dish-label {
      color: #666;
    }
    .dish-list {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-wrap: wrap;
      .dish-item {
        flex: 0 0 100%;
        margin-bottom: 14px;
        color: #333;
        display: flex;
        align-items: baseline;
        gap: 16px;
        .dish-item-box {
          display: flex;
          flex: 1;
          justify-content: flex-start;
          min-width: 0;
          gap: 16px;
        }
        .dish-name {
          flex: 1 1 auto;
          min-width: 0;
          word-break: normal;
          overflow-wrap: break-word;
          hyphens: auto;
        }
        .dish-num,
        .dish-price {
          white-space: nowrap;
        }
        .dish-price {
          margin-left: auto;
          min-width: 100px;
          text-align: right;
        }
      }
      // .dish-item:nth-child(odd) {
      //   flex: 60%;
      // }
      // .dish-item:nth-child(even) {
      //   flex: 40%;
      // }
    }
    .dish-label {
      margin-right: 65px;
    }
    .dish-all-amount {
      flex: 1;
      padding-left: 92px;
      margin-top: 10px;
      label {
        color: #333333;
        font-weight: bold;
        margin-right: 5px;
      }
      span {
        color: #f56c6c;
      }
    }
  }
}
.order-bottom {
  .amount-info {
    // min-height: 180px;
    display: flex;
    flex-wrap: wrap;
    padding: 20px 40px;
    padding-bottom: 0px;
    .amount-label {
      color: #666;
      margin-right: 65px;
    }
    .amount-list {
      flex: 80%;
      display: flex;
      flex-wrap: wrap;
      color: #333;
      // height: 65px;
      .dish-amount,
      .package-amount,
      .pay-type {
        display: inline-block;
        width: 300px;
        margin-bottom: 14px;
        flex: 50%;
      }
      .send-amount,
      .all-amount,
      .pay-time {
        display: inline-block;
        flex: 50%;
        padding-left: 10%;
      }
      .package-amount {
        .amount-name {
          margin-right: 14px;
        }
      }
      .all-amount {
        .amount-name {
          margin-right: 24px;
        }
        .amount-price {
          color: #f56c6c;
        }
      }
      .send-amount {
        .amount-name {
          margin-right: 10px;
        }
      }
    }
  }
}
@media (max-width: 760px) {
  .order-middle .user-info .user-info-box {
    grid-template-columns: minmax(0, 1fr);
    .user-name,
    .user-phone,
    .user-address {
      grid-column: 1;
      grid-row: auto;
    }
    .delivery-summary {
      grid-column: 1;
    }
    .user-address,
    .user-address span {
      white-space: normal;
      overflow-wrap: anywhere;
    }
  }
}
</style>
<style  lang="scss">
.dashboard-container.home .homeTitleBtn {
  .conTab {
    width: auto;
    min-width: 390px;

    li {
      flex: 1 0 auto;
      min-width: 190px;
      padding: 0 18px;
      white-space: nowrap;
    }

    .el-badge {
      white-space: nowrap;
    }
  }
}

.dashboard-container {
  .workbench-order-table {
    .el-table__header col:nth-child(3),
    .el-table__body col:nth-child(3) {
      width: 360px !important;
    }

    .workbench-time-header .cell,
    .workbench-amount-header .cell {
      white-space: nowrap;
    }

    td.workbench-amount-column .cell {
      white-space: nowrap;
      text-align: center;
    }

    th.workbench-order-items-column .cell,
    td.workbench-order-items-column .cell {
      padding-left: 20px;
    }

    .workbench-dish-line {
      line-height: 1.5;
      word-break: normal;
      overflow-wrap: break-word;
    }

    .workbench-actions {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 2px;
      white-space: nowrap;

      .el-button {
        margin: 0;
        padding: 3px 0;
      }
    }
  }
  .cancelDialog .el-form-item__label {
    width: 175px !important;
    white-space: nowrap;
  }

  .cancelTime {
    padding-left: 30px;
  }
  .orderTime {
    padding-left: 30px;
  }
}

  .order-dialog {
  .el-dialog {
    max-height: 90vh !important;
  }

  .el-dialog__body {
    height: 520px !important;
    max-height: calc(90vh - 150px);
    box-sizing: border-box;
  }

  .dish-item {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto auto;
    align-items: start;
    gap: 12px;
  }

  .dish-name {
    overflow-wrap: anywhere;
  }
  .cancelDialog .el-form-item__label {
    width: 175px !important;
    white-space: nowrap;
  }
}
</style>
